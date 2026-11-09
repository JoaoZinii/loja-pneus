package br.unip.aps.lojapneus.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import br.unip.aps.lojapneus.dao.PneuDAO;
import br.unip.aps.lojapneus.dao.PneuDAOJdbc;
import br.unip.aps.lojapneus.dao.VendaDAO;
import br.unip.aps.lojapneus.dao.VendaDAOJdbc;
import br.unip.aps.lojapneus.exception.PersistenciaException;
import br.unip.aps.lojapneus.exception.ValidacaoException;
import br.unip.aps.lojapneus.model.Cliente;
import br.unip.aps.lojapneus.model.Pneu;
import br.unip.aps.lojapneus.model.Venda;
import br.unip.aps.lojapneus.model.VendaOrdem;
import br.unip.aps.lojapneus.util.Validador;

/**
 * Controller de vendas: a ponte entre a tela de vendas e o banco.
 *
 * <p>Além de validar, aplica as regras de negócio da venda:</p>
 * <ul>
 *   <li>a quantidade precisa ser maior que zero e caber no estoque atual;</li>
 *   <li>o preço unitário é preenchido automaticamente com o preço atual do
 *       pneu (o usuário não digita), e a data é a do momento do registro.</li>
 * </ul>
 *
 * <p>O controller não abre janelas e não contém SQL.</p>
 */
public class VendaController {

    /** Identificadores dos campos, usados em {@link ValidacaoException#getCampo()}. */
    public static final String CAMPO_CLIENTE = "cliente";
    /** Campo (combo) do pneu. */
    public static final String CAMPO_PNEU = "pneu";
    /** Campo quantidade. */
    public static final String CAMPO_QUANTIDADE = "quantidade";
    /** Campo data inicial da busca por período. */
    public static final String CAMPO_DATA_INICIO = "dataInicio";
    /** Campo data final da busca por período. */
    public static final String CAMPO_DATA_FIM = "dataFim";

    /** Nome usado nas mensagens de erro do banco. */
    private static final String ENTIDADE = "venda";

    private final VendaDAO vendaDao;
    private final PneuDAO pneuDao;

    /**
     * Cria o controller usando os DAOs JDBC, que acessam o MySQL.
     * É o construtor que as telas usam.
     */
    public VendaController() {
        this(new VendaDAOJdbc(), new PneuDAOJdbc());
    }

    /**
     * Cria o controller com DAOs escolhidos por quem chama (útil para testes).
     *
     * @param vendaDao implementação de VendaDAO a usar
     * @param pneuDao  implementação de PneuDAO, usada para conferir o
     *                 preço e o estoque atuais do pneu
     */
    public VendaController(VendaDAO vendaDao, PneuDAO pneuDao) {
        this.vendaDao = vendaDao;
        this.pneuDao = pneuDao;
    }

    /**
     * Registra uma venda.
     *
     * <p>Antes de gravar, busca o pneu de novo no banco, porque o objeto que
     * veio do combo pode estar desatualizado (outra venda pode ter baixado o
     * estoque, ou o preço pode ter mudado depois que a tela abriu).</p>
     *
     * @param cliente    cliente escolhido no combo
     * @param pneu       pneu escolhido no combo
     * @param quantidade quantidade digitada (inteiro, maior que zero, até o estoque)
     * @return a venda gravada, com id, preço unitário, total e data preenchidos
     * @throws ValidacaoException    se faltar cliente ou pneu, a quantidade for
     *                               inválida ou maior que o estoque
     * @throws PersistenciaException se o banco recusar, já com a mensagem amigável
     */
    public Venda registrar(Cliente cliente, Pneu pneu, String quantidade)
            throws ValidacaoException, PersistenciaException {
        // 1) Validação dos campos
        if (cliente == null || cliente.getId() <= 0) {
            throw new ValidacaoException(CAMPO_CLIENTE, "Selecione o cliente.");
        }
        if (pneu == null || pneu.getId() <= 0) {
            throw new ValidacaoException(CAMPO_PNEU, "Selecione o pneu.");
        }
        int qtd = Validador.inteiroPositivo(quantidade, CAMPO_QUANTIDADE,
                "a quantidade");

        // 2) Regra de negócio: a quantidade precisa caber no estoque ATUAL
        Pneu pneuAtual = buscarPneuAtual(pneu.getId());
        if (qtd > pneuAtual.getEstoque()) {
            throw new ValidacaoException(CAMPO_QUANTIDADE,
                    mensagemEstoque(pneuAtual.getEstoque()));
        }

        // 3) Monta a venda. O preço vem do pneu, não do usuário, e fica
        //    guardado na venda (se o preço do pneu mudar depois, o histórico
        //    não muda). A hora é cortada nos segundos, como o DATETIME do banco.
        Venda venda = new Venda(cliente, pneuAtual, qtd, pneuAtual.getPreco(),
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));

        // 4) Grava. O DAO faz a venda e a baixa de estoque numa transação.
        try {
            vendaDao.registrar(venda);
        } catch (PersistenciaException e) {
            if (e.getCodigoErro() == PersistenciaException.ESTOQUE_INSUFICIENTE) {
                // Alguém vendeu este pneu entre a conferência (passo 2) e a
                // gravação. O DAO desfez tudo; mostramos o estoque de agora.
                int estoqueAgora = buscarPneuAtual(pneu.getId()).getEstoque();
                throw new ValidacaoException(CAMPO_QUANTIDADE,
                        mensagemEstoque(estoqueAgora));
            }
            throw TradutorErros.traduzir(e, ENTIDADE);
        }

        // Mantém o objeto em memória igual ao banco depois da baixa
        pneuAtual.setEstoque(pneuAtual.getEstoque() - qtd);
        return venda;
    }

    /**
     * Calcula o total da venda enquanto o usuário digita a quantidade, para a
     * tela mostrar o valor na hora. Não grava nada e não lança exceção: se a
     * quantidade ainda não for válida, devolve zero.
     *
     * @param pneu       pneu escolhido no combo (pode ser null)
     * @param quantidade o que está digitado no campo quantidade
     * @return preço do pneu x quantidade, ou zero se ainda não der para calcular
     */
    public BigDecimal calcularTotal(Pneu pneu, String quantidade) {
        if (pneu == null || pneu.getPreco() == null) {
            return BigDecimal.ZERO;
        }
        try {
            int qtd = Validador.inteiroPositivo(quantidade, CAMPO_QUANTIDADE,
                    "a quantidade");
            return pneu.getPreco().multiply(BigDecimal.valueOf(qtd));
        } catch (ValidacaoException e) {
            return BigDecimal.ZERO; // campo vazio ou inválido: ainda não há total
        }
    }

    /**
     * Exclui uma venda. A quantidade vendida volta para o estoque do pneu
     * (o DAO faz isso numa transação).
     *
     * @param id id da venda selecionada na tabela
     * @throws ValidacaoException    se nenhuma venda foi selecionada
     * @throws PersistenciaException se a venda não existir mais ou o banco recusar
     */
    public void excluir(int id) throws ValidacaoException, PersistenciaException {
        if (id <= 0) {
            throw new ValidacaoException(null, "Selecione uma venda na lista.");
        }
        try {
            vendaDao.excluir(id);
        } catch (PersistenciaException e) {
            throw TradutorErros.traduzir(e, ENTIDADE);
        }
    }

    /**
     * Busca uma venda pelo id, com cliente e pneu preenchidos.
     *
     * @param id id da venda
     * @return a venda, ou null se não existir
     * @throws PersistenciaException se o banco recusar a consulta
     */
    public Venda buscarPorId(int id) throws PersistenciaException {
        try {
            return vendaDao.buscarPorId(id);
        } catch (PersistenciaException e) {
            throw TradutorErros.traduzir(e, ENTIDADE);
        }
    }

    /**
     * Lista vendas filtrando por nome do cliente e por período, ordenando
     * por dois critérios.
     *
     * @param filtroCliente parte do nome do cliente (vazio = todos)
     * @param dataInicio    data inicial digitada, dd/mm/aaaa (vazio = sem limite)
     * @param dataFim       data final digitada, dd/mm/aaaa, inclusive (vazio = sem limite)
     * @param ordem1        primeiro critério; null = data mais recente primeiro
     * @param ordem2        segundo critério; null = data mais recente primeiro
     * @return a lista encontrada (vazia se nada combinar com o filtro)
     * @throws ValidacaoException    se alguma data for inválida ou a final
     *                               vier antes da inicial
     * @throws PersistenciaException se o banco recusar a consulta
     */
    public List<Venda> listar(String filtroCliente, String dataInicio,
                              String dataFim, VendaOrdem ordem1,
                              VendaOrdem ordem2)
            throws ValidacaoException, PersistenciaException {
        LocalDate inicio = Validador.dataOpcional(dataInicio, CAMPO_DATA_INICIO,
                "a data inicial");
        LocalDate fim = Validador.dataOpcional(dataFim, CAMPO_DATA_FIM,
                "a data final");
        if (inicio != null && fim != null && fim.isBefore(inicio)) {
            throw new ValidacaoException(CAMPO_DATA_FIM,
                    "A data final deve ser igual ou posterior à data inicial.");
        }
        try {
            return vendaDao.listar(Validador.limpar(filtroCliente), inicio, fim,
                    ordem1, ordem2);
        } catch (PersistenciaException e) {
            throw TradutorErros.traduzir(e, ENTIDADE);
        }
    }

    /**
     * Lista todas as vendas, da mais recente para a mais antiga.
     *
     * @return todas as vendas
     * @throws PersistenciaException se o banco recusar a consulta
     */
    public List<Venda> listarTodas() throws PersistenciaException {
        try {
            return listar(null, null, null, VendaOrdem.DATA_DESC,
                    VendaOrdem.CLIENTE_ASC);
        } catch (ValidacaoException e) {
            // Não acontece: sem datas, não há o que validar.
            throw new IllegalStateException(e);
        }
    }

    // ------------------------------------------------------------------
    // Métodos auxiliares
    // ------------------------------------------------------------------

    /** Busca o pneu no banco; se não existir mais, avisa o usuário. */
    private Pneu buscarPneuAtual(int idPneu)
            throws ValidacaoException, PersistenciaException {
        Pneu atual;
        try {
            atual = pneuDao.buscarPorId(idPneu);
        } catch (PersistenciaException e) {
            throw TradutorErros.traduzir(e, ENTIDADE);
        }
        if (atual == null) {
            throw new ValidacaoException(CAMPO_PNEU,
                    "O pneu escolhido não existe mais. Atualize a lista.");
        }
        return atual;
    }

    /** Monta a mensagem de estoque insuficiente com o número certo de unidades. */
    private String mensagemEstoque(int estoque) {
        if (estoque == 0) {
            return "Estoque insuficiente: este pneu está sem estoque.";
        }
        if (estoque == 1) {
            return "Estoque insuficiente: há 1 unidade.";
        }
        return "Estoque insuficiente: há " + estoque + " unidades.";
    }

    /**
     * Edita uma venda já registrada (para corrigir um lançamento errado).
     * Se o pneu continuar o mesmo, o preço unitário original é mantido (histórico);
     * se o pneu mudar, vale o preço atual do novo pneu. A data da venda não muda.
     * O estoque é ajustado: a quantidade antiga volta e a nova sai.
     *
     * @param id         id da venda selecionada
     * @param cliente    cliente escolhido
     * @param pneu       pneu escolhido
     * @param quantidade quantidade digitada
     * @return a venda com os dados novos
     */
    public Venda atualizar(int id, Cliente cliente, Pneu pneu, String quantidade)
            throws ValidacaoException, PersistenciaException {
        if (id <= 0) {
            throw new ValidacaoException(null, "Selecione uma venda na lista.");
        }
        if (cliente == null || cliente.getId() <= 0) {
            throw new ValidacaoException(CAMPO_CLIENTE, "Selecione o cliente.");
        }
        if (pneu == null || pneu.getId() <= 0) {
            throw new ValidacaoException(CAMPO_PNEU, "Selecione o pneu.");
        }
        int qtd = Validador.inteiroPositivo(quantidade, CAMPO_QUANTIDADE, "a quantidade");
        Venda existente;
        try {
            existente = vendaDao.buscarPorId(id);
        } catch (PersistenciaException e) {
            throw TradutorErros.traduzir(e, ENTIDADE);
        }
        if (existente == null) {
            throw new ValidacaoException(null, "A venda não existe mais. Atualize a lista.");
        }
        Pneu pneuAtual = buscarPneuAtual(pneu.getId());
        boolean mesmoPneu = existente.getPneu().getId() == pneuAtual.getId();
        int disponivel = pneuAtual.getEstoque() + (mesmoPneu ? existente.getQuantidade() : 0);
        if (qtd > disponivel) {
            throw new ValidacaoException(CAMPO_QUANTIDADE, mensagemEstoque(disponivel));
        }
        BigDecimal preco = mesmoPneu ? existente.getPrecoUnitario() : pneuAtual.getPreco();
        Venda venda = new Venda(id, cliente, pneuAtual, qtd, preco, existente.getDataVenda());
        try {
            vendaDao.atualizar(venda);
        } catch (PersistenciaException e) {
            if (e.getCodigoErro() == PersistenciaException.ESTOQUE_INSUFICIENTE) {
                throw new ValidacaoException(CAMPO_QUANTIDADE,
                        mensagemEstoque(buscarPneuAtual(pneu.getId()).getEstoque()));
            }
            throw TradutorErros.traduzir(e, ENTIDADE);
        }
        return venda;
    }

}
