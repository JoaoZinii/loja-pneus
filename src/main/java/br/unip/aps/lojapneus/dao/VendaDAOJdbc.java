package br.unip.aps.lojapneus.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import br.unip.aps.lojapneus.exception.PersistenciaException;
import br.unip.aps.lojapneus.model.Cliente;
import br.unip.aps.lojapneus.model.Pneu;
import br.unip.aps.lojapneus.model.Venda;
import br.unip.aps.lojapneus.model.VendaOrdem;
import br.unip.aps.lojapneus.util.ConnectionFactory;

/**
 * Implementação JDBC (MySQL) do acesso aos dados de vendas.
 */
public class VendaDAOJdbc implements VendaDAO {

    /** Consulta base: junta venda, cliente e pneu numa única linha. */
    private static final String SQL_BASE =
            "SELECT v.id_venda, v.quantidade, v.preco_unitario, v.data_venda, "
          + "c.id_cliente, c.nome, c.email, c.telefone, c.endereco, "
          + "p.id_pneu, p.marca, p.modelo, p.aro, p.preco, p.estoque "
          + "FROM venda v "
          + "JOIN cliente c ON c.id_cliente = v.id_cliente "
          + "JOIN pneu p ON p.id_pneu = v.id_pneu";

    /**
     * Registra a venda e baixa o estoque do pneu, em uma transação:
     * ou os dois acontecem, ou nenhum.
     *
     * @param venda a venda a registrar (com cliente, pneu, quantidade e preço)
     * @throws PersistenciaException se o banco recusar ou o estoque for insuficiente
     */
    @Override
    public void registrar(Venda venda) throws PersistenciaException {
        if (venda.getCliente() == null || venda.getPneu() == null) {
            throw new PersistenciaException(
                    "Venda sem cliente ou sem pneu.", 0, null);
        }
        if (venda.getDataVenda() == null) {
            venda.setDataVenda(LocalDateTime.now());
        }

        try (Connection con = ConnectionFactory.getConnection()) {
            con.setAutoCommit(false);
            try {
                inserirVenda(con, venda);
                baixarEstoque(con, venda);
                con.commit();
            } catch (SQLException | PersistenciaException e) {
                con.rollback();
                venda.setId(0);
                throw e;
            }
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "Erro ao registrar venda.", e.getErrorCode(), e);
        }
    }

    /**
     * Exclui a venda e devolve a quantidade vendida ao estoque,
     * em uma transação.
     *
     * @param id o id da venda a excluir
     * @throws PersistenciaException se a venda não existir ou o banco recusar
     */
    @Override
    public void excluir(int id) throws PersistenciaException {
        try (Connection con = ConnectionFactory.getConnection()) {
            con.setAutoCommit(false);
            try {
                int[] dados = lerPneuEQuantidade(con, id);
                apagarVenda(con, id);
                devolverEstoque(con, dados[0], dados[1]);
                con.commit();
            } catch (SQLException | PersistenciaException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "Erro ao excluir venda.", e.getErrorCode(), e);
        }
    }

    /**
     * Busca uma venda pelo id, já com o cliente e o pneu preenchidos.
     *
     * @param id o id procurado
     * @return a venda encontrada, ou null se não existir
     * @throws PersistenciaException se o banco recusar a consulta
     */
    @Override
    public Venda buscarPorId(int id) throws PersistenciaException {
        String sql = SQL_BASE + " WHERE v.id_venda = ?";

        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return montarVenda(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "Erro ao buscar venda.", e.getErrorCode(), e);
        }
    }

    /**
     * Lista vendas filtrando por cliente e período, ordenando por dois critérios.
     *
     * @param filtroCliente parte do nome do cliente (null ou vazio = sem filtro)
     * @param dataInicio    primeiro dia do período (null = sem limite)
     * @param dataFim       último dia do período, inclusive (null = sem limite)
     * @param ordem1        primeiro critério de ordenação
     * @param ordem2        segundo critério de ordenação
     * @return a lista de vendas (vazia se nada for encontrado)
     * @throws PersistenciaException se o banco recusar a consulta
     */
    @Override
    public List<Venda> listar(String filtroCliente, LocalDate dataInicio,
                              LocalDate dataFim, VendaOrdem ordem1,
                              VendaOrdem ordem2) throws PersistenciaException {
        StringBuilder sql = new StringBuilder(SQL_BASE);
        sql.append(" WHERE c.nome LIKE ?");
        if (dataInicio != null) {
            sql.append(" AND v.data_venda >= ?");
        }
        if (dataFim != null) {
            sql.append(" AND v.data_venda < ?");
        }
        sql.append(" ORDER BY ").append(colunaOrdem(ordem1))
           .append(", ").append(colunaOrdem(ordem2));

        List<Venda> lista = new ArrayList<>();

        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            String filtro = (filtroCliente == null) ? "" : filtroCliente.trim();
            int i = 1;
            ps.setString(i++, "%" + filtro + "%");
            if (dataInicio != null) {
                ps.setTimestamp(i++, Timestamp.valueOf(dataInicio.atStartOfDay()));
            }
            if (dataFim != null) {
                ps.setTimestamp(i++,
                        Timestamp.valueOf(dataFim.plusDays(1).atStartOfDay()));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(montarVenda(rs));
                }
            }
        } catch (SQLException e) {
            throw new PersistenciaException(
                    "Erro ao listar vendas.", e.getErrorCode(), e);
        }
        return lista;
    }

    // ------------------------------------------------------------------
    // Métodos auxiliares (passos das transações e conversões)
    // ------------------------------------------------------------------

    /** Passo 1 de registrar: grava a linha da venda e pega o id gerado. */
    private void inserirVenda(Connection con, Venda venda) throws SQLException {
        String sql = "INSERT INTO venda "
                   + "(id_cliente, id_pneu, quantidade, preco_unitario, data_venda) "
                   + "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, venda.getCliente().getId());
            ps.setInt(2, venda.getPneu().getId());
            ps.setInt(3, venda.getQuantidade());
            ps.setBigDecimal(4, venda.getPrecoUnitario());
            ps.setTimestamp(5, Timestamp.valueOf(venda.getDataVenda()));
            ps.executeUpdate();

            try (ResultSet chaves = ps.getGeneratedKeys()) {
                if (chaves.next()) {
                    venda.setId(chaves.getInt(1));
                }
            }
        }
    }

    /**
     * Passo 2 de registrar: confere e baixa o estoque em um único comando.
     * O "AND estoque >= ?" faz a conferência; se não houver estoque
     * suficiente, nenhuma linha é alterada.
     */
    private void baixarEstoque(Connection con, Venda venda)
            throws SQLException, PersistenciaException {
        String sql = "UPDATE pneu SET estoque = estoque - ? "
                   + "WHERE id_pneu = ? AND estoque >= ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, venda.getQuantidade());
            ps.setInt(2, venda.getPneu().getId());
            ps.setInt(3, venda.getQuantidade());

            if (ps.executeUpdate() == 0) {
                throw new PersistenciaException("Estoque insuficiente.",
                        PersistenciaException.ESTOQUE_INSUFICIENTE, null);
            }
        }
    }

    /** Passo 1 de excluir: descobre qual pneu e quantas unidades a venda tinha. */
    private int[] lerPneuEQuantidade(Connection con, int idVenda)
            throws SQLException, PersistenciaException {
        String sql = "SELECT id_pneu, quantidade FROM venda WHERE id_venda = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idVenda);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new PersistenciaException("Venda não encontrada.", 0, null);
                }
                return new int[] { rs.getInt("id_pneu"), rs.getInt("quantidade") };
            }
        }
    }

    /** Passo 2 de excluir: apaga a linha da venda. */
    private void apagarVenda(Connection con, int idVenda)
            throws SQLException, PersistenciaException {
        String sql = "DELETE FROM venda WHERE id_venda = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idVenda);
            if (ps.executeUpdate() == 0) {
                throw new PersistenciaException("Venda não encontrada.", 0, null);
            }
        }
    }

    /** Passo 3 de excluir: devolve as unidades ao estoque do pneu. */
    private void devolverEstoque(Connection con, int idPneu, int quantidade)
            throws SQLException {
        String sql = "UPDATE pneu SET estoque = estoque + ? WHERE id_pneu = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, quantidade);
            ps.setInt(2, idPneu);
            ps.executeUpdate();
        }
    }

    /** Converte a linha do JOIN em uma Venda com Cliente e Pneu dentro. */
    private Venda montarVenda(ResultSet rs) throws SQLException {
        Cliente cliente = new Cliente(
                rs.getInt("id_cliente"),
                rs.getString("nome"),
                rs.getString("email"),
                rs.getString("telefone"),
                rs.getString("endereco"));

        Pneu pneu = new Pneu(
                rs.getInt("id_pneu"),
                rs.getString("marca"),
                rs.getString("modelo"),
                rs.getInt("aro"),
                rs.getInt("estoque"),
                rs.getBigDecimal("preco"));

        return new Venda(
                rs.getInt("id_venda"),
                cliente,
                pneu,
                rs.getInt("quantidade"),
                rs.getBigDecimal("preco_unitario"),
                rs.getTimestamp("data_venda").toLocalDateTime());
    }

    /**
     * Traduz a opção de ordenação (enum) para o trecho de SQL correspondente.
     * Só valores escritos aqui entram na SQL, o que impede SQL injection.
     */
    private String colunaOrdem(VendaOrdem ordem) {
        if (ordem == null) {
            ordem = VendaOrdem.DATA_DESC;
        }
        return switch (ordem) {
            case DATA_ASC     -> "v.data_venda ASC";
            case DATA_DESC    -> "v.data_venda DESC";
            case TOTAL_ASC    -> "(v.quantidade * v.preco_unitario) ASC";
            case TOTAL_DESC   -> "(v.quantidade * v.preco_unitario) DESC";
            case CLIENTE_ASC  -> "c.nome ASC";
            case CLIENTE_DESC -> "c.nome DESC";
        };
    }

    /** Atualiza a venda e ajusta o estoque, tudo na mesma transação. */
    @Override
    public void atualizar(Venda venda) throws PersistenciaException {
        if (venda.getCliente() == null || venda.getPneu() == null) {
            throw new PersistenciaException("Venda sem cliente ou sem pneu.", 0, null);
        }
        try (Connection con = ConnectionFactory.getConnection()) {
            con.setAutoCommit(false);
            try {
                int[] antiga = lerPneuEQuantidade(con, venda.getId());
                devolverEstoque(con, antiga[0], antiga[1]);
                atualizarLinha(con, venda);
                baixarEstoque(con, venda);
                con.commit();
            } catch (SQLException | PersistenciaException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Erro ao atualizar venda.", e.getErrorCode(), e);
        }
    }

    private void atualizarLinha(Connection con, Venda venda)
            throws SQLException, PersistenciaException {
        String sql = "UPDATE venda SET id_cliente = ?, id_pneu = ?, quantidade = ?, "
                   + "preco_unitario = ? WHERE id_venda = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, venda.getCliente().getId());
            ps.setInt(2, venda.getPneu().getId());
            ps.setInt(3, venda.getQuantidade());
            ps.setBigDecimal(4, venda.getPrecoUnitario());
            ps.setInt(5, venda.getId());
            if (ps.executeUpdate() == 0) {
                throw new PersistenciaException("Venda não encontrada.", 0, null);
            }
        }
    }

}
