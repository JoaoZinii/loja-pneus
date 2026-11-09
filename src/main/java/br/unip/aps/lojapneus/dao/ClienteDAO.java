package br.unip.aps.lojapneus.dao;

import java.util.List;

import br.unip.aps.lojapneus.exception.PersistenciaException;
import br.unip.aps.lojapneus.model.Cliente;
import br.unip.aps.lojapneus.model.ClienteOrdem;

/**
 * Contrato de acesso aos dados de clientes.
 */
public interface ClienteDAO {

    /** Grava um cliente novo e preenche o id gerado no objeto. */
    void inserir(Cliente cliente) throws PersistenciaException;

    /** Atualiza os dados de um cliente existente. */
    void atualizar(Cliente cliente) throws PersistenciaException;

    /** Exclui o cliente. Falha (código 1451) se ele tiver vendas. */
    void excluir(int id) throws PersistenciaException;

    /** @return o cliente com o id informado, ou null se não existir */
    Cliente buscarPorId(int id) throws PersistenciaException;

    /**
     * Lista clientes filtrando pelo nome e ordenando por dois critérios.
     *
     * @param filtroNome parte do nome (null ou vazio = sem filtro)
     * @param ordem1     primeiro critério de ordenação
     * @param ordem2     segundo critério de ordenação
     */
    List<Cliente> listar(String filtroNome, ClienteOrdem ordem1,
                         ClienteOrdem ordem2) throws PersistenciaException;
}