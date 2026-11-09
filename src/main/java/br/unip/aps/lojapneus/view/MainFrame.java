package br.unip.aps.lojapneus.view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import br.unip.aps.lojapneus.controller.ClienteController;
import br.unip.aps.lojapneus.controller.PneuController;
import br.unip.aps.lojapneus.controller.VendaController;
import br.unip.aps.lojapneus.view.componentes.BotaoAba;
import br.unip.aps.lojapneus.view.estilo.LogoPneu;
import br.unip.aps.lojapneus.view.estilo.Tema;

/**
 * Janela principal: cabeçalho com a marca e a navegação (Clientes, Pneus, Vendas) e o
 * conteúdo da aba escolhida. Lembra o tamanho da janela entre execuções.
 * Atalhos: Ctrl+1, Ctrl+2 e Ctrl+3 trocam de aba.
 */
public class MainFrame extends JFrame {

    private final CardLayout cartas = new CardLayout();
    private final JPanel conteudo = new JPanel(cartas);
    private final List<BotaoAba> botoes = new ArrayList<>();
    private final List<Runnable> recarregadores = new ArrayList<>();
    private final JPanel navegacao = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
    private final ButtonGroup grupo = new ButtonGroup();
    private final Preferences prefs = Preferences.userNodeForPackage(MainFrame.class);

    /**
     * Cria a janela com as três abas.
     *
     * @param clientes     controller de clientes
     * @param pneus        controller de pneus
     * @param vendas       controller de vendas
     * @param demonstracao true se os dados estão só na memória (mostra um aviso no cabeçalho)
     */
    public MainFrame(ClienteController clientes, PneuController pneus, VendaController vendas,
            boolean demonstracao) {
        super("Loja de Pneus — Gestão");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 660));
        setSize(prefs.getInt("largura", 1180), prefs.getInt("altura", 740));
        setLocationRelativeTo(null);
        if (prefs.getBoolean("maximizada", false)) {
            setExtendedState(MAXIMIZED_BOTH);
        }
        BufferedImage icone = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = icone.createGraphics();
        new LogoPneu(64).paintIcon(null, g, 0, 0);
        g.dispose();
        setIconImage(icone);

        navegacao.setOpaque(false);
        conteudo.setBackground(Tema.FUNDO);
        adicionarAba("Clientes", new ClientePanel(clientes));
        adicionarAba("Pneus", new PneuPanel(pneus));
        adicionarAba("Vendas", new VendaPanel(vendas, clientes, pneus));
        ResumoPanel resumo = new ResumoPanel(vendas, pneus);
        criarAba("Resumo", resumo, resumo::recarregar);

        getContentPane().setBackground(Tema.FUNDO);
        getContentPane().add(cabecalho(demonstracao), BorderLayout.NORTH);
        getContentPane().add(conteudo, BorderLayout.CENTER);
        selecionarAba(0, false);
        registrarAtalhos();

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { guardarTamanho(); }
        });
    }

    private void adicionarAba(String titulo, PainelListagem<?, ?> painel) {
        BotaoAba b = criarAba(titulo, painel, painel::recarregar);
        painel.addPropertyChangeListener("total", e -> b.setContador((Integer) e.getNewValue()));
    }

    /** Cria a aba no cabeçalho e registra o que fazer ao abri-la (recarregar os dados). */
    private BotaoAba criarAba(String titulo, JComponent painel, Runnable recarregar) {
        int indice = botoes.size();
        BotaoAba b = new BotaoAba(titulo);
        b.addActionListener(e -> selecionarAba(indice, true));
        b.setToolTipText("Ctrl+" + (indice + 1));
        grupo.add(b);
        navegacao.add(b);
        botoes.add(b);
        recarregadores.add(recarregar);
        conteudo.add(painel, titulo);
        return b;
    }

    /** Mostra a aba e, se pedido, atualiza a lista (o estoque pode ter mudado em outra aba). */
    private void selecionarAba(int indice, boolean recarregar) {
        botoes.get(indice).setSelected(true);
        cartas.show(conteudo, botoes.get(indice).getText());
        if (recarregar) {
            recarregadores.get(indice).run();
        }
    }

    private void registrarAtalhos() {
        int ctrl = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        for (int i = 0; i < botoes.size(); i++) {
            final int indice = i;
            String nome = "aba" + i;
            getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(KeyStroke.getKeyStroke(KeyEvent.VK_1 + i, ctrl), nome);
            getRootPane().getActionMap().put(nome, new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) { selecionarAba(indice, true); }
            });
        }
    }

    private void guardarTamanho() {
        boolean max = (getExtendedState() & MAXIMIZED_BOTH) == MAXIMIZED_BOTH;
        prefs.putBoolean("maximizada", max);
        if (!max) {
            prefs.putInt("largura", getWidth());
            prefs.putInt("altura", getHeight());
        }
    }

    private JPanel cabecalho(boolean demonstracao) {
        JPanel marca = new JPanel(new BorderLayout(14, 0));
        marca.setOpaque(false);
        marca.setBorder(Tema.margem(12, 22));
        JLabel titulo = new JLabel("Loja de Pneus");
        titulo.setFont(Tema.FONTE_TITULO);
        titulo.setForeground(Color.WHITE);
        JLabel sub = new JLabel("Gestão de clientes, estoque e vendas");
        sub.setForeground(new Color(0xCBD2D9));
        JPanel textos = new JPanel(new BorderLayout());
        textos.setOpaque(false);
        textos.add(titulo, BorderLayout.NORTH);
        textos.add(sub, BorderLayout.SOUTH);
        marca.add(new JLabel(new LogoPneu(46)), BorderLayout.WEST);
        marca.add(textos, BorderLayout.CENTER);
        if (demonstracao) {
            JLabel selo = new JLabel("MODO DEMONSTRAÇÃO · dados na memória");
            selo.setFont(Tema.FONTE_PEQUENA.deriveFont(java.awt.Font.BOLD));
            selo.setForeground(new Color(0xFFD9A8));
            selo.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 0));
            marca.add(selo, BorderLayout.EAST);
        }

        JPanel baixo = new JPanel(new BorderLayout());
        baixo.setOpaque(false);
        baixo.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 16));
        baixo.add(navegacao, BorderLayout.SOUTH); // abas coladas na base: o sublinhado encosta no conteúdo

        JPanel p = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setPaint(new java.awt.GradientPaint(0, 0, new Color(0x161E26), getWidth(), 0,
                        new Color(0x2E3D4B)));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.dispose();
            }
        };
        p.add(marca, BorderLayout.WEST);
        p.add(baixo, BorderLayout.CENTER);
        return p;
    }
}
