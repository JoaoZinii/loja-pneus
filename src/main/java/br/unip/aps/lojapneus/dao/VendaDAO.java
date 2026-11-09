package br.unip.aps.lojapneus.dao;

import java.time.LocalDate;
import java.util.List;

import br.unip.aps.lojapneus.exception.PersistenciaException;
import br.unip.aps.lojapneus.model.Venda;
import br.unip.aps.lojapneus.model.VendaOrdem;

/**
 * Contrato de acesso aos dados de vendas.
 */
public interface VendaDAO {

    /** Registra a venda, baixando o estoque do pneu (em transação). */
    void registrar(Venda venda) throws PersistenciaException;

    /** Exclui a venda e devolve a quantidade ao estoque. */
    void excluir(int id) throws PersistenciaException;

    /** @return a venda com o id informado, ou null se não existir */
    Venda buscarPorId(int id) throws PersistenciaException;

    /**
     * Lista vendas filtrando por cliente e período, ordenando por dois critérios.
     *
     * @param filtroCliente parte do nome do cliente (null ou vazio = sem filtro)
     * @param dataInicio    início do período (null = sem limite)
     * @param dataFim       fim do período (null = sem limite)
     * @param ordem1        primeiro critério de ordenação
     * @param ordem2        segundo critério de ordenação
     */
    List<Venda> listar(String filtroCliente, LocalDate dataInicio,
                       LocalDate dataFim, VendaOrdem ordem1,
                       VendaOrdem ordem2) throws PersistenciaException;

    /**
     * Atualiza uma venda existente (cliente, pneu, quantidade e preço unitário; a data não muda).
     * Numa única transação: devolve o estoque da venda antiga e baixa o da nova.
     * Falha com ESTOQUE_INSUFICIENTE se o novo pneu não tiver unidades suficientes.
     */
    void atualizar(Venda venda) throws PersistenciaException;

}
