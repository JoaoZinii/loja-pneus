package br.unip.aps.lojapneus.view.estilo;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.Icon;

/** Círculo colorido com as iniciais do nome (a cor depende do nome, então é sempre a mesma). */
public final class IconeAvatar implements Icon {

    private static final Color[] PALETA = {
        new Color(0xE8751A), new Color(0x4F6D8F), new Color(0x3A9D6B),
        new Color(0x7C6BD6), new Color(0x0E8A95), new Color(0xC25570)
    };
    private static final int TAM = 32;

    private final String iniciais;
    private final Color cor;

    public IconeAvatar(String nome) {
        String[] partes = nome.trim().split("\\s+");
        String ini = partes[0].substring(0, 1);
        if (partes.length > 1) {
            ini += partes[partes.length - 1].substring(0, 1);
        }
        this.iniciais = ini.toUpperCase();
        this.cor = PALETA[Math.abs(nome.hashCode()) % PALETA.length];
    }

    @Override
    public void paintIcon(Component c, Graphics g0, int x, int y) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(cor);
        g.fillOval(x, y, TAM, TAM);
        g.setColor(Color.WHITE);
        g.setFont(Tema.FONTE_PEQUENA.deriveFont(Font.BOLD, 12f));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(iniciais, x + (TAM - fm.stringWidth(iniciais)) / 2,
                y + (TAM - fm.getHeight()) / 2 + fm.getAscent());
        g.dispose();
    }

    @Override public int getIconWidth() { return TAM; }
    @Override public int getIconHeight() { return TAM; }
}
