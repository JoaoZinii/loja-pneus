package br.unip.aps.lojapneus.view.componentes;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicScrollBarUI;

/** Barra de rolagem fina e arredondada, sem setas. */
public class BarraRolagemTema extends BasicScrollBarUI {

    private static JButton semBotao() {
        JButton b = new JButton();
        b.setPreferredSize(new Dimension(0, 0));
        b.setMinimumSize(new Dimension(0, 0));
        b.setMaximumSize(new Dimension(0, 0));
        return b;
    }

    @Override protected JButton createDecreaseButton(int orientation) { return semBotao(); }
    @Override protected JButton createIncreaseButton(int orientation) { return semBotao(); }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        return new Dimension(13, 13);
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle area) {
        // trilho transparente
    }

    @Override
    protected void paintThumb(Graphics g0, JComponent c, Rectangle area) {
        if (area.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(isThumbRollover() || isDragging ? new Color(0x9AA5B1) : new Color(0xC3CAD3));
        g.fillRoundRect(area.x + 3, area.y + 3, area.width - 6, area.height - 6, 10, 10);
        g.dispose();
    }
}
