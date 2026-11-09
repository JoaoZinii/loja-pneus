package br.unip.aps.lojapneus.view.componentes;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;

import br.unip.aps.lojapneus.view.estilo.Icone;

/** Círculo com fundo suave na cor informada e um ícone dentro. Usado em indicadores e diálogos. */
public class Medalha extends JComponent {

    private final Color cor;
    private final Icone icone;
    private final int tam;

    /**
     * @param cor   cor do ícone (o fundo é a mesma cor, bem clara)
     * @param tipo  desenho do ícone
     * @param tam   diâmetro do círculo em pixels
     */
    public Medalha(Color cor, Icone.Tipo tipo, int tam) {
        this.cor = cor;
        this.tam = tam;
        this.icone = new Icone(tipo, Math.round(tam * 0.5f), cor);
        setOpaque(false);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(tam, tam);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int y = (getHeight() - tam) / 2;
        g.setColor(new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), 32));
        g.fillOval(0, y, tam, tam);
        int o = (tam - icone.getIconWidth()) / 2;
        icone.paintIcon(this, g, o, y + o);
        g.dispose();
    }
}
