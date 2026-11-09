package br.unip.aps.lojapneus.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import br.unip.aps.lojapneus.dao.PneuDAO;
import br.unip.aps.lojapneus.dao.PneuDAOJdbc;
import br.unip.aps.lojapneus.exception.PersistenciaException;
import br.unip.aps.lojapneus.exception.ValidacaoException;
import br.unip.aps.lojapneus.model.Pneu;
import br.unip.aps.lojapneus.model.PneuOrdem;
import br.unip.aps.lojapneus.util.Validador;

/**
 * Controller de pneus: a ponte entre a tela de pneus e o banco.
 *
 * <p>Segue o mesmo caminho do {@link ClienteController}: recebe texto,
 * valida, converte, chama o DAO e traduz os erros do banco.</p>
 *
 * <p>O controller não abre janelas e não contém SQL.</p>
 */
public class PneuController {

    /** Identificadores dos campos, usados em {@link ValidacaoException#getCampo()}. */
    public static final String CAMPO_MARCA = "marca";
    /** Campo modelo. */
    public static final String CAMPO_MODELO = "modelo";
    /** Campo aro. */
    public static final String CAMPO_ARO = "aro";
    /** Campo preço. */
    public static final String CAMPO_PRECO = "preco";
    /** Campo estoque. */
    public static final String CAMPO_ESTOQUE = "estoque";

    /** Menor aro aceito (o mesmo do CHECK no banco). */
    public static final int ARO_MINIMO = 10;
    /** Maior aro aceito (o mesmo do CHECK no banco). */
    public static final int ARO_MAXIMO = 24;

    // Tamanhos máximos: os mesmos das colunas no loja_pneus.sql
    private static final int TAMANHO_MARCA = 50;
    private static final int TAMANHO_MODELO = 80;

    /** Nome usado nas mensagens de erro do banco. */
    private static final String ENTIDADE = "pneu";

    /** O DAO usado. O tipo é a interface, então qualquer implementação serve. */
    private final PneuDAO dao;

    /**
     * Cria o controller usando o DAO JDBC, que acessa o MySQL.
     * É o construtor que as telas usam.
     */
    public PneuController() {
        this(new PneuDAOJdbc());
    }

    /**
     * Cria o controller com um DAO escolhido por quem chama (útil para testes).
     *
     * @param dao a implementação de PneuDAO a usar
     */
    public PneuController(PneuDAO dao) {
        this.dao = dao;
    }

    /**
     * Cadastra um pneu novo.
     *
     * @param marca   marca digitada (obrigatória, até 50 caracteres)
     * @param modelo  modelo digitado (obrigatório, até 80 caracteres)
     * @param aro     aro digitado (inteiro de 10 a 24)
     * @param preco   preço digitado (maior que zero; aceita "589,90" e "589.90")
     * @param estoque estoque digitado (inteiro, zero ou mais)
     * @return o pneu gravado, já com o id gerado pelo banco
     * @throws ValidacaoException    se algum campo não passar nas regras
     * @throws PersistenciaException se o banco recusar, já com a mensagem amigável
     */
    public Pneu cadastrar(String marca, String modelo, String aro, String preco,
                          String estoque)
            throws ValidacaoException, PersistenciaException {
        Pneu pneu = validarEMontar(marca, modelo, aro, preco, estoque);
        try {
            dao.inserir(pneu);
        } catch (PersistenciaException e) {
            throw TradutorErros.traduzir(e, ENTIDADE);
        }
        return pneu;
    }

    /**
     * Atualiza os dados de um pneu que já existe (inclusive preço e estoque).
     *
     * @param id      id do pneu selecionado na tabela
     * @param marca   marca digitada
     * @param modelo  modelo digitado
     * @param aro     aro digitado
     * @param preco   preço digitado
     * @param estoque estoque digitado
     * @return o pneu com os dados novos
     * @throws ValidacaoException    se nenhum pneu foi selecionado ou algum
     *                               campo não passar nas regras
     * @throws PersistenciaException se o banco recusar ou o pneu não existir mais
     */
    public Pneu atualizar(int id, String marca, String modelo, String aro,
                          String preco, String estoque)
            throws ValidacaoException, PersistenciaException {
        exigirSelecao(id);
        Pneu pneu = validarEMontar(marca, modelo, aro, preco, estoque);
        pneu.setId(id);
        try {
            dao.atualizar(pneu);
        } catch (PersistenciaException e) {
            throw TradutorErros.traduzir(e, ENTIDADE);
        }
        return pneu;
    }

    /**
     * Exclui um pneu. O banco não deixa excluir pneu que já foi vendido.
     *
     * @param id id do pneu selecionado na tabela
     * @throws ValidacaoException    se nenhum pneu foi selecionado
     * @throws PersistenciaException se o pneu tiver vendas (mensagem amigável)
     *                               ou o banco recusar por outro motivo
     */
    public void excluir(int id) throws ValidacaoException, PersistenciaException {
        exigirSelecao(id);
        try {
            dao.excluir(id);
        } catch (PersistenciaException e) {
            throw TradutorErros.traduzir(e, ENTIDADE);
        }
    }

    /**
     * Busca um pneu pelo id.
     *
     * @param id id do pneu
     * @return o pneu, ou null se não existir
     * @throws PersistenciaException se o banco recusar a consulta
     */
    public Pneu buscarPorId(int id) throws PersistenciaException {
        try {
            return dao.buscarPorId(id);
        } catch (PersistenciaException e) {
            throw TradutorErros.traduzir(e, ENTIDADE);
        }
    }

    /**
     * Lista pneus filtrando por marca ou modelo e ordenando por dois critérios.
     *
     * @param filtroMarcaOuModelo parte da marca ou do modelo (vazio = todos)
     * @param ordem1              primeiro critério; null = marca A-Z
     * @param ordem2              segundo critério; null = marca A-Z
     * @return a lista encontrada (vazia se nada combinar com a busca)
     * @throws PersistenciaException se o banco recusar a consulta
     */
    public List<Pneu> listar(String filtroMarcaOuModelo, PneuOrdem ordem1,
                             PneuOrdem ordem2) throws PersistenciaException {
        try {
            return dao.listar(Validador.limpar(filtroMarcaOuModelo), ordem1, ordem2);
        } catch (PersistenciaException e) {
            throw TradutorErros.traduzir(e, ENTIDADE);
        }
    }

    /**
     * Lista só os pneus que têm estoque, por marca e modelo. Útil para o
     * combo de pneus da tela de vendas: não faz sentido oferecer pneu
     * que não pode ser vendido.
     *
     * @return os pneus com estoque maior que zero
     * @throws PersistenciaException se o banco recusar a consulta
     */
    public List<Pneu> listarComEstoque() throws PersistenciaException {
        List<Pneu> disponiveis = new ArrayList<>();
        for (Pneu p : listar(null, PneuOrdem.MARCA_ASC, PneuOrdem.MODELO_ASC)) {
            if (p.getEstoque() > 0) {
                disponiveis.add(p);
            }
        }
        return disponiveis;
    }

    // ------------------------------------------------------------------
    // Métodos auxiliares
    // ------------------------------------------------------------------

    /**
     * Valida todos os campos, na ordem do formulário, e monta o objeto.
     * Para no primeiro erro.
     */
    private Pneu validarEMontar(String marca, String modelo, String aro,
                                String preco, String estoque)
            throws ValidacaoException {
        String marcaOk = Validador.textoObrigatorio(marca, TAMANHO_MARCA,
                CAMPO_MARCA, "a marca");
        String modeloOk = Validador.textoObrigatorio(modelo, TAMANHO_MODELO,
                CAMPO_MODELO, "o modelo");
        int aroOk = Validador.inteiroEntre(aro, ARO_MINIMO, ARO_MAXIMO,
                CAMPO_ARO, "o aro");
        BigDecimal precoOk = Validador.valorPositivo(preco, CAMPO_PRECO, "o preço");
        int estoqueOk = Validador.inteiroNaoNegativo(estoque, CAMPO_ESTOQUE,
                "o estoque");

        // Atenção à ordem do construtor do Pneu: aro, estoque e só depois preço.
        return new Pneu(marcaOk, modeloOk, aroOk, estoqueOk, precoOk);
    }

    /** Barra editar ou excluir quando nenhuma linha da tabela foi selecionada. */
    private void exigirSelecao(int id) throws ValidacaoException {
        if (id <= 0) {
            throw new ValidacaoException(null, "Selecione um pneu na lista.");
        }
    }
}
