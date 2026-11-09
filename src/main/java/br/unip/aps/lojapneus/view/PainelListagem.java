package br.unip.aps.lojapneus.view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.util.concurrent.ExecutionException;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import br.unip.aps.lojapneus.view.componentes.BarraRolagemTema;
import br.unip.aps.lojapneus.view.componentes.BotaoTema;
import br.unip.aps.lojapneus.view.componentes.CampoTexto;
import br.unip.aps.lojapneus.view.componentes.Cartao;
import br.unip.aps.lojapneus.view.componentes.CartaoIndicador;
import br.unip.aps.lojapneus.view.componentes.ComboTema;
import br.unip.aps.lojapneus.view.componentes.DialogoMensagem;
import br.unip.aps.lojapneus.view.componentes.Mensagens;
import br.unip.aps.lojapneus.view.componentes.ModeloTabela;
import br.unip.aps.lojapneus.view.componentes.RenderizadorCelula;
import br.unip.aps.lojapneus.view.componentes.Selo;
import br.unip.aps.lojapneus.view.estilo.Icone;
import br.unip.aps.lojapneus.view.estilo.Tema;
import br.unip.aps.lojapneus.view.exportacao.ExportadorCsv;

/**
 * Aba de listagem padrão: busca (ao digitar), dois combos de ordenação, indicadores opcionais,
 * tabela e botões Novo / Editar / Excluir / Atualizar.
 * <p>
 * Para criar uma aba nova, estenda esta classe e implemente os 4 métodos abstratos; todo o
 * comportamento e o visual vêm prontos. A consulta roda em segundo plano (a janela não trava).
 * A propriedade {@code "total"} avisa quantos registros existem (sem filtro) a quem quiser ouvir.
 *
 * @param <T> tipo do objeto exibido em cada linha
 * @param <O> enum de ordenação aceito pelo controller
 */
public abstract class PainelListagem<T, O> extends JPanel {

    /** Item de combo: rótulo amigável + valor do enum de ordenação. */
    public record Opcao<V>(String rotulo, V valor) {
        @Override public String toString() { return rotulo; }
    }

    private static final String CARTA_TABELA = "tabela";
    private static final String CARTA_VAZIO = "vazio";

    private final String singular;
    private final String plural;
    private final boolean feminino;

    /** Modelo da tabela (somente leitura). */
    protected final ModeloTabela<T> modelo;
    /** Tabela de registros. */
    protected final JTable tabela;

    private final CampoTexto campoBusca;
    private final JComboBox<Opcao<O>> comboOrdem1;
    private final JComboBox<Opcao<O>> comboOrdem2;
    private final BotaoTema btnNovo;
    private final BotaoTema btnEditar;
    private final BotaoTema btnExcluir;
    private final BotaoTema btnExportar =
            new BotaoTema("Exportar CSV", BotaoTema.Tipo.NEUTRO, Icone.Tipo.BAIXAR);
    private final JPanel acoesEsquerda = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final List<BotaoTema> botoesSelecao = new ArrayList<>();
    private final List<JMenuItem> itensMenuSelecao = new ArrayList<>();
    private final JMenuItem miEditar = new JMenuItem("Editar");
    private final JMenuItem miExcluir = new JMenuItem("Excluir");
    private final JPopupMenu menu = new JPopupMenu();
    private final Selo lblStatus = new Selo();
    private final JLabel lblVazio = new JLabel("", SwingConstants.CENTER);
    private final CardLayout cartas = new CardLayout();
    private final JPanel centro = new JPanel(cartas);
    private final JPanel indicadores = new JPanel(new GridLayout(1, 0, Tema.ESPACO, 0));
    private final JPanel filtrosExtras = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    private final Timer relogioStatus;
    private final Timer relogioBusca;
    private JLabel lblOrdenar;
    private final JLabel lblDica = new JLabel();
    private String statusPadrao = " ";
    private boolean edicao = true;
    private int sequencia;
    private int totalGeral = -1;

    /**
     * @param singular     nome da entidade no singular, minúsculo (ex.: "cliente")
     * @param plural       nome no plural, minúsculo (ex.: "clientes")
     * @param feminino     true para entidades femininas (ex.: "venda"), ajusta os textos
     * @param dicaBusca    texto de dica do campo de busca
     * @param modelo       modelo da tabela já com as colunas definidas
     * @param opcoesOrdem  opções dos combos de ordenação
     * @param indiceOrdem1 opção inicial de "Ordenar por"
     * @param indiceOrdem2 opção inicial de "Depois por"
     */
    protected PainelListagem(String singular, String plural, boolean feminino,
            String dicaBusca, ModeloTabela<T> modelo, List<Opcao<O>> opcoesOrdem,
            int indiceOrdem1, int indiceOrdem2) {
        super(new BorderLayout(0, Tema.ESPACO));
        this.singular = singular;
        this.plural = plural;
        this.feminino = feminino;
        this.modelo = modelo;
        setBackground(Tema.FUNDO);
        setBorder(Tema.margem(18, 26));

        campoBusca = new CampoTexto(30, dicaBusca).comLupa();
        campoBusca.setToolTipText("A lista filtra enquanto você digita (Ctrl+F volta para cá)");
        comboOrdem1 = criarCombo(opcoesOrdem, indiceOrdem1);
        comboOrdem2 = criarCombo(opcoesOrdem, indiceOrdem2);

        BotaoTema btnLimpar = new BotaoTema("Limpar", BotaoTema.Tipo.NEUTRO, Icone.Tipo.X);
        btnNovo = new BotaoTema("Novo", BotaoTema.Tipo.PRIMARIO, Icone.Tipo.MAIS);
        btnEditar = new BotaoTema("Editar", BotaoTema.Tipo.NEUTRO, Icone.Tipo.LAPIS);
        btnExcluir = new BotaoTema("Excluir", BotaoTema.Tipo.PERIGO, Icone.Tipo.LIXEIRA);
        BotaoTema btnAtualizar = new BotaoTema("Atualizar lista", BotaoTema.Tipo.NEUTRO,
                Icone.Tipo.ATUALIZAR);
        btnNovo.setToolTipText("Ctrl+N");
        btnEditar.setToolTipText("Enter ou duplo clique na linha");
        btnExcluir.setToolTipText("Delete");
        btnAtualizar.setToolTipText("F5");
        btnExportar.setToolTipText("Salva a lista que está na tela em um arquivo CSV (abre no Excel)");

        tabela = new JTable(modelo);
        configurarTabela();
        montarMenu();

        JPanel norte = new JPanel(new BorderLayout());
        norte.setOpaque(false);
        norte.add(montarTopo(btnLimpar, btnExportar), BorderLayout.NORTH);
        indicadores.setOpaque(false);
        indicadores.setBorder(BorderFactory.createEmptyBorder(Tema.ESPACO, 0, 0, 0));
        indicadores.setVisible(false);
        norte.add(indicadores, BorderLayout.CENTER);

        add(norte, BorderLayout.NORTH);
        add(montarCentro(), BorderLayout.CENTER);
        add(montarRodape(btnAtualizar), BorderLayout.SOUTH);

        relogioStatus = new Timer(5000, e -> restaurarStatus());
        relogioStatus.setRepeats(false);
        relogioBusca = new Timer(350, e -> carregar(null));
        relogioBusca.setRepeats(false);

        // ligações (depois de tudo criado, para não disparar eventos durante a montagem)
        campoBusca.addActionListener(e -> {
            relogioBusca.stop();
            carregar(null);
        });
        campoBusca.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { agendarRecarga(); }
            @Override public void removeUpdate(DocumentEvent e) { agendarRecarga(); }
            @Override public void changedUpdate(DocumentEvent e) { agendarRecarga(); }
        });
        btnLimpar.addActionListener(e -> {
            campoBusca.setText("");
            relogioBusca.stop();
            carregar(null);
            campoBusca.requestFocusInWindow();
        });
        comboOrdem1.addItemListener(this::aoMudarOrdem);
        comboOrdem2.addItemListener(this::aoMudarOrdem);
        btnNovo.addActionListener(e -> novo());
        btnEditar.addActionListener(e -> editar());
        btnExcluir.addActionListener(e -> excluir());
        btnAtualizar.addActionListener(e -> carregar(null));
        btnExportar.addActionListener(e -> exportar());
        tabela.getSelectionModel().addListSelectionListener(e -> atualizarBotoes());
        registrarAtalhos();
        atualizarBotoes();
    }

    // ------------------------------------------------------------------ o que cada aba define

    /** Busca os registros no controller. Roda em segundo plano: não mexa em componentes aqui. */
    protected abstract List<T> consultar(String filtro, O ordem1, O ordem2) throws Exception;

    /**
     * Abre o formulário de cadastro (item null) ou de edição.
     *
     * @return true se o usuário salvou
     */
    protected abstract boolean abrirFormulario(T itemOuNull);

    /** Exclui o registro pelo controller. */
    protected abstract void remover(T item) throws Exception;

    /** @return nome do registro para a confirmação de exclusão */
    protected abstract String descricao(T item);

    // ------------------------------------------------------------------ ganchos opcionais

    /**
     * Chamado na tela (thread da interface) antes de cada consulta. Use para ler campos
     * extras (ex.: datas) e guardá-los em variáveis que {@link #consultar} vai usar.
     * Pode lançar ValidacaoException.
     */
    protected void prepararConsulta() throws Exception { }

    /** Chamado depois de cada carga, com a lista exibida. Use para atualizar indicadores. */
    protected void aoCarregar(List<T> lista) { }

    /** @return true se a aba tem filtros além do texto de busca (ex.: período), para o contador e o rodapé */
    protected boolean temFiltroExtra() { return false; }

    /** @return identificador estável do registro, para manter a seleção após recarregar */
    protected Object chave(T item) { return item; }

    /** @return linha extra de aviso na confirmação de exclusão, ou null */
    protected String avisoExclusao() { return null; }

    // ------------------------------------------------------------------ API para as subclasses

    /** Deve ser chamado no fim do construtor da subclasse: faz a primeira carga da lista. */
    protected final void iniciar() {
        SwingUtilities.invokeLater(() -> carregar(null));
    }

    /** Consulta de novo, respeitando a busca e as ordenações atuais. */
    public final void recarregar() {
        carregar(null);
    }

    /** Agenda uma recarga em 350 ms (evita consultar a cada tecla). */
    protected final void agendarRecarga() {
        relogioBusca.restart();
    }

    /** Remove o botão e o duplo clique de edição (ex.: vendas não se editam). */
    protected final void desativarEdicao() {
        edicao = false;
        btnEditar.setVisible(false);
        miEditar.setVisible(false);
        lblDica.setText("Delete exclui  ·  Ctrl+N novo  ·  F5 atualiza");
    }

    /** Adiciona uma linha de filtros extras abaixo da ordenação (ex.: período de datas). */
    protected final void adicionarFiltroExtra(String rotulo, Component... componentes) {
        JLabel l = criarRotulo(rotulo);
        l.setPreferredSize(lblOrdenar.getPreferredSize());
        filtrosExtras.add(l);
        filtrosExtras.add(Box.createHorizontalStrut(8));
        for (Component c : componentes) {
            filtrosExtras.add(c);
        }
        filtrosExtras.setVisible(true);
        revalidate();
    }

    /** Adiciona um cartão de resumo acima da tabela. */
    protected final CartaoIndicador adicionarIndicador(String titulo, Color cor, Icone.Tipo icone) {
        CartaoIndicador c = new CartaoIndicador(titulo, cor, icone);
        indicadores.add(c);
        indicadores.setVisible(true);
        revalidate();
        return c;
    }

    /** Cria um rótulo no padrão visual (negrito, cinza-grafite). */
    protected static JLabel criarRotulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(Tema.FONTE_NEGRITO);
        l.setForeground(Tema.GRAFITE_CLARO);
        return l;
    }

    /** Cria um combo de ordenação a partir das opções. */
    protected static <V> JComboBox<Opcao<V>> criarCombo(List<Opcao<V>> opcoes, int indice) {
        DefaultComboBoxModel<Opcao<V>> m = new DefaultComboBoxModel<>();
        opcoes.forEach(m::addElement);
        JComboBox<Opcao<V>> combo = new JComboBox<>(m);
        combo.setSelectedIndex(indice);
        combo.setPrototypeDisplayValue(new Opcao<>("Estoque: menor para maior   ", null));
        ComboTema.aplicar(combo);
        return combo;
    }

    // ------------------------------------------------------------------ montagem

    private JPanel montarTopo(BotaoTema btnLimpar, BotaoTema btnExportar) {
        JPanel cartao = new Cartao();
        cartao.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 8);
        c.anchor = GridBagConstraints.WEST;

        JLabel lblBuscar = criarRotulo("Buscar");
        lblOrdenar = criarRotulo("Ordenar por");
        lblBuscar.setPreferredSize(lblOrdenar.getPreferredSize());

        c.gridx = 0; c.gridy = 0;
        cartao.add(lblBuscar, c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        cartao.add(campoBusca, c);
        c.gridx = 2; c.weightx = 0; c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(0, 0, 0, 8);
        cartao.add(btnLimpar, c);
        c.gridx = 3;
        c.insets = new Insets(0, 0, 0, 0);
        cartao.add(btnExportar, c);

        JPanel ordem = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        ordem.setOpaque(false);
        ordem.add(lblOrdenar);
        ordem.add(Box.createHorizontalStrut(8));
        ordem.add(comboOrdem1);
        ordem.add(Box.createHorizontalStrut(24));
        ordem.add(criarRotulo("Depois por"));
        ordem.add(Box.createHorizontalStrut(8));
        ordem.add(comboOrdem2);

        c.gridx = 0; c.gridy = 1; c.gridwidth = 4; c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(10, 0, 0, 0);
        cartao.add(ordem, c);

        filtrosExtras.setOpaque(false);
        filtrosExtras.setVisible(false);
        c.gridy = 2;
        cartao.add(filtrosExtras, c);
        return cartao;
    }

    private JPanel montarCentro() {
        JScrollPane rolagem = new JScrollPane(tabela);
        rolagem.setBorder(null);
        rolagem.getViewport().setBackground(Tema.SUPERFICIE);
        rolagem.getVerticalScrollBar().setUI(new BarraRolagemTema());
        rolagem.getVerticalScrollBar().setUnitIncrement(30);
        rolagem.getHorizontalScrollBar().setUI(new BarraRolagemTema());
        JPanel canto = new JPanel();
        canto.setBackground(Tema.CABECALHO);
        rolagem.setCorner(JScrollPane.UPPER_RIGHT_CORNER, canto);

        JPanel vazio = new JPanel(new BorderLayout());
        vazio.setBackground(Tema.SUPERFICIE);
        lblVazio.setForeground(Tema.TEXTO_SUAVE);
        lblVazio.setHorizontalTextPosition(SwingConstants.CENTER);
        lblVazio.setVerticalTextPosition(SwingConstants.BOTTOM);
        lblVazio.setIconTextGap(14);
        vazio.add(lblVazio, BorderLayout.CENTER);

        centro.setOpaque(false);
        centro.add(rolagem, CARTA_TABELA);
        centro.add(vazio, CARTA_VAZIO);

        Cartao cartao = new Cartao(14, true);
        cartao.setLayout(new BorderLayout());
        cartao.setBorder(BorderFactory.createEmptyBorder(1, 1, 2, 1));
        cartao.add(centro, BorderLayout.CENTER);
        return cartao;
    }

    private JPanel montarRodape(BotaoTema btnAtualizar) {
        JPanel esquerda = acoesEsquerda;
        esquerda.setOpaque(false);
        esquerda.add(btnNovo);
        esquerda.add(btnEditar);
        esquerda.add(btnExcluir);

        lblDica.setText("Enter edita  ·  Delete exclui  ·  Ctrl+N novo  ·  F5 atualiza");
        lblDica.setFont(Tema.FONTE_PEQUENA);
        lblDica.setForeground(new Color(0xA0A8B3));
        lblDica.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        JPanel meio = new JPanel(new BorderLayout());
        meio.setOpaque(false);
        meio.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 14));
        meio.add(lblStatus, BorderLayout.NORTH);
        meio.add(lblDica, BorderLayout.SOUTH);

        JPanel direita = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        direita.setOpaque(false);
        direita.add(btnAtualizar);

        JPanel rodape = new JPanel(new BorderLayout());
        rodape.setOpaque(false);
        rodape.add(esquerda, BorderLayout.WEST);
        rodape.add(meio, BorderLayout.CENTER);
        rodape.add(direita, BorderLayout.EAST);
        return rodape;
    }

    private void configurarTabela() {
        tabela.setRowHeight(46);
        tabela.setShowGrid(false);
        tabela.setIntercellSpacing(new Dimension(0, 0));
        tabela.setFillsViewportHeight(true);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.setAutoCreateRowSorter(false); // a ordenação vem do banco, não da tabela
        tabela.setDefaultRenderer(Object.class, new RenderizadorCelula());
        tabela.setDefaultRenderer(Number.class, new RenderizadorCelula());
        tabela.getTableHeader().setDefaultRenderer(new RenderizadorCelula.Cabecalho());
        tabela.getTableHeader().setReorderingAllowed(false);
        tabela.getTableHeader().setPreferredSize(new Dimension(0, 40));
        tabela.getTableHeader().setOpaque(true);
        tabela.getTableHeader().setBackground(Tema.CABECALHO);
        tabela.setBackground(Tema.SUPERFICIE);
        for (int i = 0; i < modelo.getColumnCount(); i++) {
            tabela.getColumnModel().getColumn(i).setPreferredWidth(modelo.largura(i));
        }
        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) { hover(tabela.rowAtPoint(e.getPoint())); }
            @Override public void mouseExited(MouseEvent e) { hover(-1); }
            @Override public void mousePressed(MouseEvent e) { popup(e); }
            @Override public void mouseReleased(MouseEvent e) { popup(e); }
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1
                        && tabela.rowAtPoint(e.getPoint()) >= 0) {
                    editar();
                }
            }
        };
        tabela.addMouseListener(mouse);
        tabela.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) { hover(tabela.rowAtPoint(e.getPoint())); }
        });
    }

    private void montarMenu() {
        JMenuItem miNovo = new JMenuItem("Novo");
        miNovo.addActionListener(e -> novo());
        miEditar.addActionListener(e -> editar());
        miExcluir.addActionListener(e -> excluir());
        menu.add(miEditar);
        menu.add(miExcluir);
        menu.addSeparator();
        menu.add(miNovo);
    }

    private void registrarAtalhos() {
        tabela.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "editar");
        tabela.getActionMap().put("editar", acao(this::editar));
        tabela.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "excluir");
        tabela.getActionMap().put("excluir", acao(this::excluir));

        int ctrl = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        var mapa = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        mapa.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, ctrl), "novo");
        mapa.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, ctrl), "buscar");
        mapa.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "atualizar");
        getActionMap().put("novo", acao(this::novo));
        getActionMap().put("buscar", acao(() -> campoBusca.requestFocusInWindow()));
        getActionMap().put("atualizar", acao(this::recarregar));
    }

    private static AbstractAction acao(Runnable r) {
        return new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { r.run(); }
        };
    }

    // ------------------------------------------------------------------ ações extras e exportação

    /**
     * Adiciona um botão (e um item no menu do botão direito) que age sobre a linha selecionada.
     * Fica desabilitado enquanto nenhuma linha estiver selecionada.
     *
     * @param texto  texto do botão
     * @param icone  ícone do botão
     * @param acao   o que fazer com o item selecionado
     */
    protected final void adicionarAcao(String texto, Icone.Tipo icone, Consumer<T> acao) {
        BotaoTema b = new BotaoTema(texto, BotaoTema.Tipo.NEUTRO, icone);
        b.setEnabled(tabela.getSelectedRow() >= 0);
        b.addActionListener(e -> executarAcao(acao));
        acoesEsquerda.add(b);
        botoesSelecao.add(b);
        JMenuItem mi = new JMenuItem(texto);
        mi.setEnabled(tabela.getSelectedRow() >= 0);
        mi.addActionListener(e -> executarAcao(acao));
        menu.insert(mi, 2);
        itensMenuSelecao.add(mi);
        lblDica.setText("Enter edita  ·  Delete exclui  ·  Ctrl+N novo");
        acoesEsquerda.revalidate();
    }

    private void executarAcao(Consumer<T> acao) {
        T item = selecionado();
        if (item == null) {
            return;
        }
        try {
            acao.accept(item);
        } catch (RuntimeException ex) {
            Mensagens.erro(this, ex);
        }
    }

    /** Salva em CSV exatamente a lista que está na tela (já com busca, período e ordem). */
    private void exportar() {
        if (modelo.getRowCount() == 0) {
            Mensagens.aviso(this, "Nada para exportar",
                    "A lista está vazia. Ajuste a busca ou o filtro e tente de novo.");
            return;
        }
        JFileChooser seletor = new JFileChooser();
        seletor.setDialogTitle("Exportar " + plural + " para CSV");
        seletor.setFileFilter(new FileNameExtensionFilter("Planilha CSV (*.csv)", "csv"));
        seletor.setSelectedFile(new File(plural + "_" + LocalDate.now() + ".csv"));
        if (seletor.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File arquivo = seletor.getSelectedFile();
        if (!arquivo.getName().toLowerCase().endsWith(".csv")) {
            arquivo = new File(arquivo.getPath() + ".csv");
        }
        if (arquivo.exists()) {
            String nome = arquivo.getName().replace("&", "&amp;").replace("<", "&lt;");
            int r = DialogoMensagem.mostrar(this, DialogoMensagem.Tipo.AVISO, "Substituir arquivo?",
                    "O arquivo <b>" + nome + "</b> já existe.<br>Deseja substituí-lo?",
                    new String[] {"Cancelar", "Substituir"}, 0, 1);
            if (r != 1) {
                return;
            }
        }
        try {
            ExportadorCsv.salvar(arquivo.toPath(), modelo);
            mostrarStatus("Exportado: " + arquivo.getName() + " (" + modelo.getRowCount()
                    + (modelo.getRowCount() == 1 ? " linha)" : " linhas)"), Tema.VERDE);
        } catch (IOException ex) {
            Mensagens.aviso(this, "Não foi possível salvar o arquivo",
                    "Verifique se ele não está aberto no Excel e se você tem permissão para gravar nessa pasta.");
        }
    }

    // ------------------------------------------------------------------ comportamento

    private void hover(int linha) {
        if (!Integer.valueOf(linha).equals(tabela.getClientProperty("linhaHover"))) {
            tabela.putClientProperty("linhaHover", linha);
            tabela.repaint();
        }
    }

    private void popup(MouseEvent e) {
        if (!e.isPopupTrigger()) {
            return;
        }
        int linha = tabela.rowAtPoint(e.getPoint());
        if (linha >= 0) {
            tabela.setRowSelectionInterval(linha, linha);
            menu.show(tabela, e.getX(), e.getY());
        }
    }

    private void aoMudarOrdem(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            carregar(null);
        }
    }

    @SuppressWarnings("unchecked")
    private O valor(JComboBox<Opcao<O>> combo) {
        return ((Opcao<O>) combo.getSelectedItem()).valor();
    }

    private T selecionado() {
        int linha = tabela.getSelectedRow();
        return linha < 0 ? null : modelo.get(linha);
    }

    private void atualizarBotoes() {
        boolean tem = tabela.getSelectedRow() >= 0;
        btnEditar.setEnabled(tem);
        btnExcluir.setEnabled(tem);
        miEditar.setEnabled(tem);
        miExcluir.setEnabled(tem);
        botoesSelecao.forEach(b -> b.setEnabled(tem));
        itensMenuSelecao.forEach(i -> i.setEnabled(tem));
    }

    /** Consulta em segundo plano e atualiza a tela; descarta respostas antigas. */
    private void carregar(String mensagemSucesso) {
        relogioBusca.stop();
        final int minha = ++sequencia;
        final String filtro = campoBusca.getText().trim();
        final O o1 = valor(comboOrdem1);
        final O o2 = valor(comboOrdem2);
        T atual = selecionado();
        final Object chaveSelecionada = atual == null ? null : chave(atual);
        try {
            prepararConsulta();
        } catch (Exception ex) {
            Mensagens.erro(this, ex);
            return;
        }
        lblStatus.mostrar("Carregando…", false);
        new SwingWorker<List<T>, Void>() {
            @Override protected List<T> doInBackground() throws Exception {
                return consultar(filtro, o1, o2);
            }

            @Override protected void done() {
                if (minha != sequencia) {
                    return; // chegou uma consulta mais nova; esta resposta está velha
                }
                try {
                    aplicar(get(), filtro, chaveSelecionada);
                    if (mensagemSucesso != null) {
                        mostrarStatus(mensagemSucesso, Tema.VERDE);
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    restaurarStatus();
                    Mensagens.erro(PainelListagem.this, ex.getCause());
                }
            }
        }.execute();
    }

    private void aplicar(List<T> lista, String filtro, Object chaveSelecionada) {
        modelo.setDados(lista);
        if (chaveSelecionada != null) {
            for (int i = 0; i < modelo.getRowCount(); i++) {
                if (chaveSelecionada.equals(chave(modelo.get(i)))) {
                    tabela.setRowSelectionInterval(i, i);
                    Rectangle r = tabela.getCellRect(i, 0, true);
                    tabela.scrollRectToVisible(r);
                    break;
                }
            }
        }
        mostrarLista(lista.size(), filtro);
        aoCarregar(lista);
        if (filtro.isEmpty() && !temFiltroExtra()) {
            int antigo = totalGeral;
            totalGeral = lista.size();
            firePropertyChange("total", antigo, totalGeral);
        }
    }

    private void novo() {
        if (abrirFormulario(null)) {
            carregar(textoSucesso("cadastrad"));
        }
    }

    private void editar() {
        T item = selecionado();
        if (edicao && item != null && abrirFormulario(item)) {
            carregar(textoSucesso("atualizad"));
        }
    }

    private void excluir() {
        T item = selecionado();
        if (item == null) {
            return;
        }
        String artigoItem = (feminino ? "a " : "o ") + singular;
        if (!Mensagens.confirmarExclusao(this, artigoItem, descricao(item), avisoExclusao())) {
            return;
        }
        try {
            remover(item);
            carregar(textoSucesso("excluíd"));
        } catch (Exception ex) {
            Mensagens.erro(this, ex);
        }
    }

    private String textoSucesso(String participioSemFinal) {
        String nome = Character.toUpperCase(singular.charAt(0)) + singular.substring(1);
        return nome + " " + participioSemFinal + (feminino ? "a" : "o") + " com sucesso.";
    }

    private void mostrarLista(int total, String filtro) {
        boolean filtrando = !filtro.isEmpty() || temFiltroExtra();
        String nenhum = feminino ? "Nenhuma " : "Nenhum ";
        if (total == 0) {
            String titulo;
            String dica;
            if (filtrando) {
                titulo = nenhum + singular + " encontrad" + (feminino ? "a" : "o")
                        + " para “" + filtro.replace("<", "&lt;") + "”";
                dica = "Confira a grafia ou clique em Limpar para ver todos.";
                lblVazio.setIcon(new Icone(Icone.Tipo.LUPA, 56, new Color(0xC3CAD3)));
            } else {
                titulo = nenhum + singular + " para mostrar";
                dica = "Clique em Novo para cadastrar o primeiro registro.";
                lblVazio.setIcon(new Icone(Icone.Tipo.MAIS, 56, new Color(0xC3CAD3)));
            }
            lblVazio.setText("<html><div style='text-align:center'><span style='font-size:16pt;"
                    + "color:#3E4C59'><b>" + titulo + "</b></span><br><br>" + dica + "</div></html>");
            cartas.show(centro, CARTA_VAZIO);
        } else {
            cartas.show(centro, CARTA_TABELA);
        }
        statusPadrao = (filtrando ? "Resultado do filtro: " : "Total: ") + total + " "
                + (total == 1 ? singular : plural);
        restaurarStatus();
    }

    private void mostrarStatus(String texto, Color cor) {
        lblStatus.mostrar(texto, true);
        relogioStatus.restart();
    }

    private void restaurarStatus() {
        lblStatus.mostrar(statusPadrao, false);
    }
}
