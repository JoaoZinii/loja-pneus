package br.unip.aps.lojapneus.dao;

import java.util.List;

import br.unip.aps.lojapneus.exception.PersistenciaException;
import br.unip.aps.lojapneus.model.Pneu;
import br.unip.aps.lojapneus.model.PneuOrdem;

/**
 * Contrato de acesso aos dados de pneus.
 */
public interface PneuDAO {

    /** Grava um pneu novo e preenche o id gerado no objeto. */
    void inserir(Pneu pneu) throws PersistenciaException;

    /** Atualiza os dados de um pneu existente. */
    void atualizar(Pneu pneu) throws PersistenciaException;

    /** Exclui o pneu. Falha (código 1451) se ele tiver vendas. */
    void excluir(int id) throws PersistenciaException;

    /** @return o pneu com o id informado, ou null se não existir */
    Pneu buscarPorId(int id) throws PersistenciaException;

    /**
     * Lista pneus filtrando por marca ou modelo e ordenando por dois critérios.
     *
     * @param filtroMarcaOuModelo parte da marca ou do modelo (null ou vazio = sem filtro)
     * @param ordem1              primeiro critério de ordenação
     * @param ordem2              segundo critério de ordenação
     */
    List<Pneu> listar(String filtroMarcaOuModelo, PneuOrdem ordem1,
                      PneuOrdem ordem2) throws PersistenciaException;
}