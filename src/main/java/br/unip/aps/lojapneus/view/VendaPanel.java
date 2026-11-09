package br.unip.aps.lojapneus.view;

import java.awt.Component;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import br.unip.aps.lojapneus.controller.ClienteController;
import br.unip.aps.lojapneus.controller.PneuController;
import br.unip.aps.lojapneus.controller.VendaController;
import br.unip.aps.lojapneus.model.Venda;
import br.unip.aps.lojapneus.model.VendaOrdem;
import br.unip.aps.lojapneus.view.componentes.BotaoTema;
import br.unip.aps.lojapneus.view.componentes.CampoData;
import br.unip.aps.lojapneus.view.componentes.CartaoIndicador;
import br.unip.aps.lojapneus.view.componentes.Mensagens;
import br.unip.aps.lojapneus.view.componentes.ModeloTabela;
import br.unip.aps.lojapneus.view.componentes.RenderizadorCelula;
import br.unip.aps.lojapneus.view.estilo.Formato;
import br.unip.aps.lojapneus.view.estilo.Icone;
import br.unip.aps.lojapneus.view.estilo.Tema;
import br.unip.aps.lojapneus.view.exportacao.ReciboVenda;

/** Aba Vendas: busca por cliente, filtro por período, ordenação, registro e exclusão. */
public class VendaPanel extends PainelListagem<Venda, VendaOrdem> {

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final VendaController controller;
    private final ClienteController clientes;
    private final PneuController pneus;
    private final CampoData campoInicio = new CampoData();
    private final CampoData campoFim = new CampoData();
    private final CartaoIndicador indVendas;
    private final CartaoIndicador indUnidades;
    private final CartaoIndicador indFaturamento;
    private volatile String inicio = "";
    private volatile String fim = "";

    /**
     * @param controller controller de vendas
     * @param clientes   controller de clientes (alimenta o combo do formulário)
     * @param pneus      controller de pneus (alimenta o combo do formulário)
     */
    public VendaPanel(VendaController controller, ClienteController clientes, PneuController pneus) {
        super("venda", "vendas", true, "Buscar por nome do cliente...",
                criarModelo(), opcoes(), 1, 4);
        this.controller = controller;
        this.clientes = clientes;
        this.pneus = pneus;

        JLabel ate = new JLabel("até");
        ate.setForeground(Tema.TEXTO_SUAVE);
        ate.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 8, 0, 8));
        adicionarFiltroExtra("Período", campoInicio, ate, campoFim, Box.createHorizontalStrut(14),
                atalho("Hoje", 0), Box.createHorizontalStrut(6),
                atalho("7 dias", 6), Box.createHorizontalStrut(6),
                atalho("30 dias", 29), Box.createHorizontalStrut(6),
                atalho("Tudo", -1));
        javax.swing.event.DocumentListener recarregar = new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { agendarRecarga(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { agendarRecarga(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { agendarRecarga(); }
        };
        campoInicio.getDocument().addDocumentListener(recarregar);
        campoFim.getDocument().addDocumentListener(recarregar);

        indVendas = adicionarIndicador("Vendas no período", Tema.GRAFITE_CLARO, Icone.Tipo.RECIBO);
        indUnidades = adicionarIndicador("Pneus vendidos (un.)", Tema.LARANJA, Icone.Tipo.PNEU);
        indFaturamento = adicionarIndicador("Faturamento", Tema.VERDE, Icone.Tipo.CIFRA);
        tabela.getColumnModel().getColumn(0).setCellRenderer(new RenderizadorCelula(false, true));
        tabela.getColumnModel().getColumn(2).setCellRenderer(new RenderizadorCelula.Avatar());
        tabela.getColumnModel().getColumn(6).setCellRenderer(new RenderizadorCelula(true, false));
        adicionarAcao("Recibo", Icone.Tipo.RECIBO, this::emitirRecibo);
        iniciar();
    }

    private BotaoTema atalho(String texto, int diasAtras) {
        BotaoTema b = new BotaoTema(texto, BotaoTema.Tipo.NEUTRO).compacto();
        b.addActionListener(e -> {
            if (diasAtras < 0) {
                campoInicio.setData(null);
                campoFim.setData(null);
            } else {
                campoInicio.setData(LocalDate.now().minusDays(diasAtras));
                campoFim.setData(LocalDate.now());
            }
        });
        return b;
    }

    private static ModeloTabela<Venda> criarModelo() {
        return new ModeloTabela<Venda>()
                .coluna("ID", Integer.class, 50, Venda::getId)
                .coluna("Data", String.class, 140, v -> v.getDataVenda().format(DATA_HORA))
                .coluna("Cliente", String.class, 250, v -> v.getCliente().getNome())
                .coluna("Pneu", String.class, 250, v -> v.getPneu().toString())
                .coluna("Qtd.", Integer.class, 55, Venda::getQuantidade)
                .coluna("Preço unit.", BigDecimal.class, 105, Venda::getPrecoUnitario)
                .coluna("Total", BigDecimal.class, 115, Venda::getValorTotal);
    }

    private static List<Opcao<VendaOrdem>> opcoes() {
        return List.of(
                new Opcao<>("Data: mais antiga primeiro", VendaOrdem.DATA_ASC),
                new Opcao<>("Data: mais recente primeiro", VendaOrdem.DATA_DESC),
                new Opcao<>("Total: menor para maior", VendaOrdem.TOTAL_ASC),
                new Opcao<>("Total: maior para menor", VendaOrdem.TOTAL_DESC),
                new Opcao<>("Cliente: A a Z", VendaOrdem.CLIENTE_ASC),
                new Opcao<>("Cliente: Z a A", VendaOrdem.CLIENTE_DESC));
    }

    /**
     * Guarda as datas digitadas. Só valem quando completas (dd/mm/aaaa); enquanto o usuário
     * ainda digita, o campo conta como vazio ("sem limite"). Quem valida é o controller.
     */
    @Override
    protected void prepararConsulta() {
        campoInicio.getData(); // só para pintar a borda de vermelho se a data completa for inválida
        campoFim.getData();
        inicio = campoInicio.getText().length() == 10 ? campoInicio.getText() : "";
        fim = campoFim.getText().length() == 10 ? campoFim.getText() : "";
    }

    @Override
    protected List<Venda> consultar(String filtro, VendaOrdem o1, VendaOrdem o2) throws Exception {
        return controller.listar(filtro, inicio, fim, o1, o2);
    }

    @Override
    protected boolean temFiltroExtra() {
        return !inicio.isEmpty() || !fim.isEmpty();
    }

    @Override
    protected void aoCarregar(List<Venda> lista) {
        BigDecimal total = lista.stream().map(Venda::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        indVendas.setValor(String.valueOf(lista.size()));
        indUnidades.setValor(String.valueOf(lista.stream().mapToInt(Venda::getQuantidade).sum()));
        indFaturamento.setValor(Formato.moeda(total));
    }

    @Override
    protected Object chave(Venda item) {
        return item.getId();
    }

    @Override
    protected boolean abrirFormulario(Venda itemOuNull) {
        return new VendaDialog(SwingUtilities.getWindowAncestor(this), controller, clientes, pneus, itemOuNull)
                .exibir();
    }

    @Override
    protected void remover(Venda item) throws Exception {
        controller.excluir(item.getId());
    }

    @Override
    protected String descricao(Venda item) {
        return "#" + item.getId() + " — " + item.getCliente().getNome()
                + " (" + item.getQuantidade() + "× " + item.getPneu() + ")";
    }

    @Override
    protected String avisoExclusao() {
        return "A quantidade vendida volta para o estoque do pneu.";
    }

    /** Gera o recibo da venda selecionada e abre no navegador (de lá: Imprimir ou Salvar em PDF). */
    private void emitirRecibo(Venda venda) {
        try {
            java.nio.file.Path arquivo = ReciboVenda.gerar(venda);
            if (java.awt.Desktop.isDesktopSupported()
                    && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                java.awt.Desktop.getDesktop().browse(arquivo.toUri());
            } else {
                Mensagens.info(this, "Recibo gerado", "O recibo foi salvo em:\n" + arquivo);
            }
        } catch (java.io.IOException ex) {
            Mensagens.aviso(this, "Não foi possível gerar o recibo", ex.getMessage());
        }
    }
}
