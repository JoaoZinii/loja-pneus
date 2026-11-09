package br.unip.aps.lojapneus.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import br.unip.aps.lojapneus.exception.PersistenciaException;
import br.unip.aps.lojapneus.model.Cliente;
import br.unip.aps.lojapneus.model.ClienteOrdem;
import br.unip.aps.lojapneus.util.ConnectionFactory;

/**
 * Implementação JDBC (MySQL) do acesso aos dados de clientes.
 */
public class ClienteDAOJdbc implements ClienteDAO {

    /**
     * Grava um cliente novo e preenche o id gerado no objeto.
     *
     * @param cliente o cliente a gravar
     * @throws PersistenciaException se o banco recusar a operação
     */
    @Override
    public void inserir(Cliente cliente) throws PersistenciaException {
        String sql = "INSERT INTO cliente (nome, email, telefone, endereco) "
                   + "VALUES (?, ?, ?, ?)";

        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, cliente.getNome());
            ps.setString(2, nuloSeVazio(cliente.getEmail()));
            ps.setString(3, nuloSeVazio(cliente.getTelefone()));
            ps.setString(4, nuloSeVazio(cliente.getEndereco()));
            ps.executeUpdate();

            try (ResultSet chaves = ps.getGeneratedKeys()) {
                if (chaves.next()) {
                    cliente.setId(chaves.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "Erro ao inserir cliente.", e.getErrorCode(), e);
        }
    }

    /**
     * Atualiza os dados de um cliente existente.
     *
     * @param cliente o cliente com os dados novos (o id identifica qual atualizar)
     * @throws PersistenciaException se o banco recusar ou o cliente não existir
     */
    @Override
    public void atualizar(Cliente cliente) throws PersistenciaException {
        String sql = "UPDATE cliente SET nome = ?, email = ?, "
                   + "telefone = ?, endereco = ? WHERE id_cliente = ?";

        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, cliente.getNome());
            ps.setString(2, nuloSeVazio(cliente.getEmail()));
            ps.setString(3, nuloSeVazio(cliente.getTelefone()));
            ps.setString(4, nuloSeVazio(cliente.getEndereco()));
            ps.setInt(5, cliente.getId());

            int linhas = ps.executeUpdate();
            if (linhas == 0) {
                throw new PersistenciaException(
                        "Cliente não encontrado.", 0, null);
            }
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "Erro ao atualizar cliente.", e.getErrorCode(), e);
        }
    }

    /**
     * Exclui o cliente. O banco recusa (código 1451) se ele tiver vendas.
     *
     * @param id o id do cliente a excluir
     * @throws PersistenciaException se o banco recusar ou o cliente não existir
     */
    @Override
    public void excluir(int id) throws PersistenciaException {
        String sql = "DELETE FROM cliente WHERE id_cliente = ?";

        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);

            int linhas = ps.executeUpdate();
            if (linhas == 0) {
                throw new PersistenciaException(
                        "Cliente não encontrado.", 0, null);
            }
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "Erro ao excluir cliente.", e.getErrorCode(), e);
        }
    }

    /**
     * Busca um cliente pelo id.
     *
     * @param id o id procurado
     * @return o cliente encontrado, ou null se não existir
     * @throws PersistenciaException se o banco recusar a consulta
     */
    @Override
    public Cliente buscarPorId(int id) throws PersistenciaException {
        String sql = "SELECT id_cliente, nome, email, telefone, endereco "
                   + "FROM cliente WHERE id_cliente = ?";

        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return montarCliente(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "Erro ao buscar cliente.", e.getErrorCode(), e);
        }
    }

    /**
     * Lista clientes filtrando pelo nome e ordenando por dois critérios.
     *
     * @param filtroNome parte do nome (null ou vazio = sem filtro)
     * @param ordem1     primeiro critério de ordenação
     * @param ordem2     segundo critério de ordenação
     * @return a lista de clientes (vazia se nada for encontrado)
     * @throws PersistenciaException se o banco recusar a consulta
     */
    @Override
    public List<Cliente> listar(String filtroNome, ClienteOrdem ordem1,
                                ClienteOrdem ordem2) throws PersistenciaException {
        String sql = "SELECT id_cliente, nome, email, telefone, endereco "
                   + "FROM cliente WHERE nome LIKE ? "
                   + "ORDER BY " + colunaOrdem(ordem1) + ", " + colunaOrdem(ordem2);

        List<Cliente> lista = new ArrayList<>();

        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            String filtro = (filtroNome == null) ? "" : filtroNome.trim();
            ps.setString(1, "%" + filtro + "%");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(montarCliente(rs));
                }
            }
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "Erro ao listar clientes.", e.getErrorCode(), e);
        }
        return lista;
    }

    /**
     * Converte a linha atual do ResultSet em um objeto Cliente.
     */
    private Cliente montarCliente(ResultSet rs) throws SQLException {
        return new Cliente(
                rs.getInt("id_cliente"),
                rs.getString("nome"),
                rs.getString("email"),
                rs.getString("telefone"),
                rs.getString("endereco"));
    }

    /**
     * Traduz a opção de ordenação (enum) para o trecho de SQL correspondente.
     * Só valores escritos aqui entram na SQL, o que impede SQL injection.
     */
    private String colunaOrdem(ClienteOrdem ordem) {
        if (ordem == null) {
            ordem = ClienteOrdem.NOME_ASC;
        }
        return switch (ordem) {
            case NOME_ASC   -> "nome ASC";
            case NOME_DESC  -> "nome DESC";
            case EMAIL_ASC  -> "email ASC";
            case EMAIL_DESC -> "email DESC";
        };
    }

    /**
     * Transforma texto vazio em null, para o banco gravar NULL.
     * Necessário porque o e-mail é UNIQUE: dois "" colidiriam, dois NULL não.
     */
    private String nuloSeVazio(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return null;
        }
        return texto.trim();
    }
}