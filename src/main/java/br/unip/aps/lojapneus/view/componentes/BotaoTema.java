package br.unip.aps.lojapneus.view.componentes;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;

import br.unip.aps.lojapneus.view.estilo.Icone;
import br.unip.aps.lojapneus.view.estilo.Tema;

/**
 * Botão com visual próprio (cantos arredondados, hover), igual em qualquer Look and Feel.
 * PRIMARIO = ação principal (laranja), PERIGO = exclusão (vermelho), NEUTRO = demais.
 */
public class BotaoTema extends JButton {

    /** Estilos de botão do sistema. */
    public enum Tipo { PRIMARIO, PERIGO, NEUTRO }

    private final Tipo tipo;
    private boolean sobre;

    /**
     * @param texto  rótulo do botão
     * @param tipo   estilo visual
     * @param icone  ícone à esquerda do texto
     */
    public BotaoTema(String texto, Tipo tipo, Icone.Tipo icone) {
        this(texto, tipo);
        Icone i = new Icone(icone, 16, null);
        setIcon(i);
        setDisabledIcon(i);
        setIconTextGap(8);
        setBorder(BorderFactory.createEmptyBorder(9, 16, 9, 18));
    }

    /** Deixa o botão menor (para filtros e atalhos). @return este botão */
    public BotaoTema compacto() {
        setFont(Tema.FONTE_PEQUENA.deriveFont(java.awt.Font.BOLD));
        setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        return this;
    }

    /**
     * @param texto rótulo do botão
     * @param tipo  estilo visual
     */
    public BotaoTema(String texto, Tipo tipo) {
        super(texto);
        this.tipo = tipo;
        setFont(Tema.FONTE_NEGRITO);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createEmptyBorder(9, 20, 9, 20));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { sobre = true; repaint(); }
            @Override public void mouseExited(MouseEvent e) { sobre = false; repaint(); }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        boolean ativo = isEnabled();
        boolean destaque = ativo && (sobre || getModel().isPressed());

        Color fundo;
        Color borda;
        Color texto;
        if (!ativo) {
            fundo = Tema.FUNDO; borda = Tema.BORDA; texto = new Color(0xA0A8B3);
        } else if (tipo == Tipo.PRIMARIO) {
            fundo = destaque ? Tema.LARANJA_ESCURO : Tema.LARANJA;
            borda = fundo; texto = Color.WHITE;
        } else if (tipo == Tipo.PERIGO) {
            fundo = destaque ? Tema.VERMELHO : Color.WHITE;
            borda = Tema.VERMELHO; texto = destaque ? Color.WHITE : Tema.VERMELHO;
        } else {
            fundo = destaque ? Tema.FUNDO : Color.WHITE;
            borda = Tema.BORDA; texto = Tema.TEXTO;
        }
        g2.setColor(fundo);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
        g2.setColor(borda);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
        if (ativo && hasFocus()) {
            g2.setColor(new Color(Tema.LARANJA.getRed(), Tema.LARANJA.getGreen(),
                    Tema.LARANJA.getBlue(), 140));
            g2.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 8, 8);
        }
        g2.dispose();
        setForeground(texto);
        super.paintComponent(g);
    }
}
