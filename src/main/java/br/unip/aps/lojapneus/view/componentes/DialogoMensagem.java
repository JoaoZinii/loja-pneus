package br.unip.aps.lojapneus.view.componentes;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import br.unip.aps.lojapneus.view.estilo.Icone;
import br.unip.aps.lojapneus.view.estilo.Tema;

/** Caixa de mensagem própria (substitui o JOptionPane): ícone colorido, título, texto e botões. */
public final class DialogoMensagem extends JDialog {

    /** Tipos de mensagem. */
    public enum Tipo {
        INFO(new Color(0x2563EB), Icone.Tipo.INFO),
        AVISO(new Color(0xD98200), Icone.Tipo.ALERTA),
        ERRO(Tema.VERMELHO, Icone.Tipo.X),
        EXCLUSAO(Tema.VERMELHO, Icone.Tipo.LIXEIRA);

        public final Color cor;
        final Icone.Tipo icone;

        Tipo(Color cor, Icone.Tipo icone) {
            this.cor = cor;
            this.icone = icone;
        }
    }

    private int resultado = -1;

    private DialogoMensagem(Window dono, Tipo tipo, String titulo, String corpoHtml,
            String[] botoes, int padrao, int perigo) {
        super(dono, "Loja de Pneus", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        JLabel t = new JLabel(titulo);
        t.setFont(Tema.FONTE.deriveFont(java.awt.Font.BOLD, 17f));
        t.setForeground(Tema.GRAFITE);
        JLabel c = new JLabel("<html><body style='width:310px'>" + corpoHtml + "</body></html>");
        c.setForeground(Tema.GRAFITE_CLARO);
        JPanel textos = new JPanel(new BorderLayout(0, 8));
        textos.setOpaque(false);
        textos.add(t, BorderLayout.NORTH);
        textos.add(c, BorderLayout.CENTER);

        JPanel corpo = new JPanel(new BorderLayout(18, 0));
        corpo.setBackground(Tema.SUPERFICIE);
        corpo.setBorder(Tema.margem(24, 26));
        JPanel lateral = new JPanel(new BorderLayout());
        lateral.setOpaque(false);
        lateral.add(new Medalha(tipo.cor, tipo.icone, 54), BorderLayout.NORTH);
        corpo.add(lateral, BorderLayout.WEST);
        corpo.add(textos, BorderLayout.CENTER);

        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rodape.setBackground(Tema.FUNDO);
        rodape.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.BORDA), Tema.margem(12, 18)));
        BotaoTema padraoBotao = null;
        for (int i = 0; i < botoes.length; i++) {
            BotaoTema.Tipo estilo = botoes.length == 1 ? BotaoTema.Tipo.PRIMARIO
                    : (i == perigo ? BotaoTema.Tipo.PERIGO : BotaoTema.Tipo.NEUTRO);
            BotaoTema b = new BotaoTema(botoes[i], estilo);
            final int indice = i;
            b.addActionListener(e -> {
                resultado = indice;
                dispose();
            });
            rodape.add(b);
            if (i == padrao) {
                padraoBotao = b;
            }
        }
        getContentPane().add(corpo, BorderLayout.CENTER);
        getContentPane().add(rodape, BorderLayout.SOUTH);
        if (padraoBotao != null) {
            getRootPane().setDefaultButton(padraoBotao);
        }
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        pack();
        setLocationRelativeTo(dono);
    }


    /**
     * Mostra a mensagem e espera.
     *
     * @param corpoHtml texto (pode ter &lt;b&gt; e &lt;br&gt;); o chamador escapa o conteúdo digitado
     * @param padrao    índice do botão acionado com Enter
     * @param perigo    índice do botão vermelho (ou -1)
     * @return índice do botão clicado, ou -1 se fechou com Esc/X
     */
    public static int mostrar(Component pai, Tipo tipo, String titulo, String corpoHtml,
            String[] botoes, int padrao, int perigo) {
        Window dono = pai instanceof Window w ? w : (pai == null ? null : SwingUtilities.getWindowAncestor(pai));
        DialogoMensagem d = new DialogoMensagem(dono, tipo, titulo, corpoHtml, botoes, padrao, perigo);
        d.setVisible(true);
        return d.resultado;
    }
}
