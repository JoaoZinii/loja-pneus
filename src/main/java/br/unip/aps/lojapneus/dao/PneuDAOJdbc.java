package br.unip.aps.lojapneus.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import br.unip.aps.lojapneus.exception.PersistenciaException;
import br.unip.aps.lojapneus.model.Pneu;
import br.unip.aps.lojapneus.model.PneuOrdem;
import br.unip.aps.lojapneus.util.ConnectionFactory;

/**
 * Implementação JDBC (MySQL) do acesso aos dados de pneus.
 */
public class PneuDAOJdbc implements PneuDAO {

    /**
     * Grava um pneu novo e preenche o id gerado no objeto.
     *
     * @param pneu o pneu a gravar
     * @throws PersistenciaException se o banco recusar a operação
     */
    @Override
    public void inserir(Pneu pneu) throws PersistenciaException {
        String sql = "INSERT INTO pneu (marca, modelo, aro, preco, estoque) "
                   + "VALUES (?, ?, ?, ?, ?)";

        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, pneu.getMarca());
            ps.setString(2, pneu.getModelo());
            ps.setInt(3, pneu.getAro());
            ps.setBigDecimal(4, pneu.getPreco());
            ps.setInt(5, pneu.getEstoque());
            ps.executeUpdate();

            try (ResultSet chaves = ps.getGeneratedKeys()) {
                if (chaves.next()) {
                    pneu.setId(chaves.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "Erro ao inserir pneu.", e.getErrorCode(), e);
        }
    }

    /**
     * Atualiza os dados de um pneu existente.
     *
     * @param pneu o pneu com os dados novos (o id identifica qual atualizar)
     * @throws PersistenciaException se o banco recusar ou o pneu não existir
     */
    @Override
    public void atualizar(Pneu pneu) throws PersistenciaException {
        String sql = "UPDATE pneu SET marca = ?, modelo = ?, aro = ?, "
                   + "preco = ?, estoque = ? WHERE id_pneu = ?";

        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, pneu.getMarca());
            ps.setString(2, pneu.getModelo());
            ps.setInt(3, pneu.getAro());
            ps.setBigDecimal(4, pneu.getPreco());
            ps.setInt(5, pneu.getEstoque());
            ps.setInt(6, pneu.getId());

            int linhas = ps.executeUpdate();
            if (linhas == 0) {
                throw new PersistenciaException("Pneu não encontrado.", 0, null);
            }
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "Erro ao atualizar pneu.", e.getErrorCode(), e);
        }
    }

    /**
     * Exclui o pneu. O banco recusa (código 1451) se ele já tiver vendas.
     *
     * @param id o id do pneu a excluir
     * @throws PersistenciaException se o banco recusar ou o pneu não existir
     */
    @Override
    public void excluir(int id) throws PersistenciaException {
        String sql = "DELETE FROM pneu WHERE id_pneu = ?";

        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);

            int linhas = ps.executeUpdate();
            if (linhas == 0) {
                throw new PersistenciaException("Pneu não encontrado.", 0, null);
            }
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "Erro ao excluir pneu.", e.getErrorCode(), e);
        }
    }

    /**
     * Busca um pneu pelo id.
     *
     * @param id o id procurado
     * @return o pneu encontrado, ou null se não existir
     * @throws PersistenciaException se o banco recusar a consulta
     */
    @Override
    public Pneu buscarPorId(int id) throws PersistenciaException {
        String sql = "SELECT id_pneu, marca, modelo, aro, preco, estoque "
                   + "FROM pneu WHERE id_pneu = ?";

        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return montarPneu(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "Erro ao buscar pneu.", e.getErrorCode(), e);
        }
    }

    /**
     * Lista pneus filtrando por marca ou modelo e ordenando por dois critérios.
     *
     * @param filtroMarcaOuModelo parte da marca ou do modelo (null ou vazio = sem filtro)
     * @param ordem1              primeiro critério de ordenação
     * @param ordem2              segundo critério de ordenação
     * @return a lista de pneus (vazia se nada for encontrado)
     * @throws PersistenciaException se o banco recusar a consulta
     */
    @Override
    public List<Pneu> listar(String filtroMarcaOuModelo, PneuOrdem ordem1,
                             PneuOrdem ordem2) throws PersistenciaException {
        String sql = "SELECT id_pneu, marca, modelo, aro, preco, estoque "
                   + "FROM pneu WHERE (marca LIKE ? OR modelo LIKE ?) "
                   + "ORDER BY " + colunaOrdem(ordem1) + ", " + colunaOrdem(ordem2);

        List<Pneu> lista = new ArrayList<>();

        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            String filtro = (filtroMarcaOuModelo == null)
                    ? "" : filtroMarcaOuModelo.trim();
            ps.setString(1, "%" + filtro + "%");
            ps.setString(2, "%" + filtro + "%");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(montarPneu(rs));
                }
            }
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "Erro ao listar pneus.", e.getErrorCode(), e);
        }
        return lista;
    }

    /**
     * Converte a linha atual do ResultSet em um objeto Pneu.
     */
    private Pneu montarPneu(ResultSet rs) throws SQLException {
        return new Pneu(
                rs.getInt("id_pneu"),
                rs.getString("marca"),
                rs.getString("modelo"),
                rs.getInt("aro"),
                rs.getInt("estoque"),
                rs.getBigDecimal("preco"));
    }

    /**
     * Traduz a opção de ordenação (enum) para o trecho de SQL correspondente.
     * Só valores escritos aqui entram na SQL, o que impede SQL injection.
     */
    private String colunaOrdem(PneuOrdem ordem) {
        if (ordem == null) {
            ordem = PneuOrdem.MARCA_ASC;
        }
        return switch (ordem) {
            case MARCA_ASC    -> "marca ASC";
            case MARCA_DESC   -> "marca DESC";
            case MODELO_ASC   -> "modelo ASC";
            case MODELO_DESC  -> "modelo DESC";
            case ARO_ASC      -> "aro ASC";
            case ARO_DESC     -> "aro DESC";
            case PRECO_ASC    -> "preco ASC";
            case PRECO_DESC   -> "preco DESC";
            case ESTOQUE_ASC  -> "estoque ASC";
            case ESTOQUE_DESC -> "estoque DESC";
        };
    }
}