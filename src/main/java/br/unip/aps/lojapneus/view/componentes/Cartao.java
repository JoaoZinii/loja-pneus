package br.unip.aps.lojapneus.view.componentes;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JPanel;

import br.unip.aps.lojapneus.view.estilo.Tema;

/**
 * Painel "cartão": fundo branco, cantos arredondados, borda fina e sombra suave.
 * Com {@code recortar = true}, o conteúdo (ex.: tabela) também é recortado nos cantos.
 */
public class Cartao extends JPanel {

    private final int arco;
    private final boolean recortar;

    /** Cartão padrão (cantos de 14 px, sem recorte). */
    public Cartao() {
        this(14, false);
    }

    /**
     * @param arco     diâmetro dos cantos arredondados
     * @param recortar true para recortar os filhos nos cantos
     */
    public Cartao(int arco, boolean recortar) {
        this.arco = arco;
        this.recortar = recortar;
        setOpaque(false);
        setBackground(Tema.SUPERFICIE);
        setBorder(Tema.margem(14, 16));
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth() - 1;
        int h = getHeight() - 2;
        g.setColor(new Color(16, 24, 40, 12));
        g.fillRoundRect(0, 2, w, h, arco, arco);
        g.setColor(getBackground());
        g.fillRoundRect(0, 0, w, h, arco, arco);
        g.setColor(Tema.BORDA);
        g.drawRoundRect(0, 0, w, h, arco, arco);
        g.dispose();
    }

    @Override
    protected void paintChildren(Graphics g0) {
        if (!recortar) {
            super.paintChildren(g0);
            return;
        }
        Graphics2D g = (Graphics2D) g0.create();
        g.setClip(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 3, arco, arco));
        super.paintChildren(g);
        g.dispose();
    }
}
