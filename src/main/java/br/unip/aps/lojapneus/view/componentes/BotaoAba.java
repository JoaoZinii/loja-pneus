package br.unip.aps.lojapneus.view.componentes;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JToggleButton;

import br.unip.aps.lojapneus.view.estilo.Tema;

/** Aba de navegação do cabeçalho: texto, contador de registros e sublinhado laranja quando ativa. */
public class BotaoAba extends JToggleButton {

    private final String titulo;
    private int contador = -1;
    private boolean sobre;

    /** @param titulo texto da aba */
    public BotaoAba(String titulo) {
        super(titulo);
        this.titulo = titulo;
        setFont(Tema.FONTE_NEGRITO);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { sobre = true; repaint(); }
            @Override public void mouseExited(MouseEvent e) { sobre = false; repaint(); }
        });
    }

    /** Define o número exibido ao lado do título (negativo = esconde). */
    public void setContador(int n) {
        contador = n;
        revalidate();
        repaint();
    }

    private String textoContador() {
        return String.valueOf(contador);
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        int w = fm.stringWidth(titulo) + 40;
        if (contador >= 0) {
            w += fm.stringWidth(textoContador()) + 22;
        }
        return new Dimension(w, 46);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        boolean ativa = isSelected();
        g.setFont(getFont());
        FontMetrics fm = g.getFontMetrics();
        int base = (getHeight() - fm.getHeight()) / 2 + fm.getAscent() - 2;

        if (sobre && !ativa) {
            g.setColor(new Color(255, 255, 255, 18));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        g.setColor(ativa ? Color.WHITE : (sobre ? new Color(0xE4E7EB) : new Color(0x9AA5B1)));
        g.drawString(titulo, 20, base);

        if (contador >= 0) {
            String n = textoContador();
            int bw = fm.stringWidth(n) + 14;
            int bx = 20 + fm.stringWidth(titulo) + 8;
            int by = base - fm.getAscent() + 1;
            g.setColor(ativa ? Tema.LARANJA : Tema.GRAFITE_CLARO);
            g.fillRoundRect(bx, by, bw, fm.getHeight() - 2, 14, 14);
            g.setColor(Color.WHITE);
            g.drawString(n, bx + 7, base);
        }
        if (ativa) {
            g.setColor(Tema.LARANJA);
            g.fillRect(0, getHeight() - 4, getWidth(), 4);
        }
        g.dispose();
    }
}
