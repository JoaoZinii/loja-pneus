package br.unip.aps.lojapneus.view.componentes;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;

import br.unip.aps.lojapneus.view.estilo.Tema;

/** Visual moderno para combos: cantos arredondados, seta fina e lista com seleção laranja clara. */
public class ComboTema extends BasicComboBoxUI {

    /** Aplica o visual a um combo (o mesmo resultado em qualquer Look and Feel). */
    public static void aplicar(JComboBox<?> combo) {
        combo.setUI(new ComboTema());
        combo.setOpaque(false);
        combo.setFont(Tema.FONTE);
        combo.setForeground(Tema.TEXTO);
        combo.setBackground(Color.WHITE);
        combo.setBorder(BorderFactory.createEmptyBorder());
        if (combo.getRenderer() == null || combo.getRenderer() instanceof UIResource) {
            combo.setRenderer(new RenderizadorLista());
        }
        combo.setPreferredSize(null); // recalcula com o valor de referência (prototype) já definido
        Dimension d = combo.getPreferredSize();
        combo.setPreferredSize(new Dimension(d.width + 14, Math.max(d.height, 38)));
    }

    /** Cria o visual (seta com largura própria, em vez de botão quadrado). */
    public ComboTema() {
        squareButton = false;
    }

    @Override
    protected JButton createArrowButton() {
        JButton b = new JButton() {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(Tema.TEXTO_SUAVE);
                g.setStroke(new java.awt.BasicStroke(2f, java.awt.BasicStroke.CAP_ROUND,
                        java.awt.BasicStroke.JOIN_ROUND));
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                g.drawLine(cx - 4, cy - 2, cx, cy + 2);
                g.drawLine(cx, cy + 2, cx + 4, cy - 2);
                g.dispose();
            }
        };
        b.setBorder(BorderFactory.createEmptyBorder());
        b.setContentAreaFilled(false);
        b.setFocusable(false);
        b.setOpaque(false);
        b.setPreferredSize(new Dimension(30, 30));
        return b;
    }

    @Override
    protected Insets getInsets() {
        return new Insets(2, 6, 2, 2);
    }

    @Override
    public void paint(Graphics g0, JComponent c) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = c.getWidth() - 1;
        int h = c.getHeight() - 1;
        g.setColor(c.isEnabled() ? Color.WHITE : Tema.FUNDO);
        g.fillRoundRect(0, 0, w, h, 10, 10);
        boolean foco = c.hasFocus() || (comboBox.isEditable() && comboBox.getEditor()
                .getEditorComponent().hasFocus()) || comboBox.isPopupVisible();
        g.setColor(foco ? Tema.LARANJA : Tema.BORDA);
        g.setStroke(new java.awt.BasicStroke(foco ? 2f : 1f));
        int o = foco ? 1 : 0;
        g.drawRoundRect(o, o, w - 2 * o, h - 2 * o, 10, 10);
        g.dispose();
        super.paint(g0, c);
    }

    @Override
    public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
        // o fundo já foi desenhado em paint()
    }

    @Override
    protected ComboPopup createPopup() {
        BasicComboPopup p = (BasicComboPopup) super.createPopup();
        p.setBorder(BorderFactory.createLineBorder(Tema.BORDA));
        p.getList().setSelectionBackground(Tema.SELECAO);
        p.getList().setSelectionForeground(Tema.TEXTO);
        return p;
    }

    /** Item da lista com respiro e seleção laranja clara. */
    static class RenderizadorLista extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice,
                boolean selecionado, boolean foco) {
            super.getListCellRendererComponent(lista, valor, indice, false, false);
            setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 10));
            setOpaque(true);
            setBackground(selecionado && indice >= 0 ? Tema.SELECAO : Color.WHITE);
            setForeground(Tema.TEXTO);
            return this;
        }
    }
}
