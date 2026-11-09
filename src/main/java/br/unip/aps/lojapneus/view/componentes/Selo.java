package br.unip.aps.lojapneus.view.componentes;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JLabel;

import br.unip.aps.lojapneus.view.estilo.Icone;
import br.unip.aps.lojapneus.view.estilo.Tema;

/** Texto de status do rodapé. Em caso de sucesso vira uma "pílula" verde com um ✓. */
public final class Selo extends JLabel {

    private boolean sucesso;

    public Selo() {
        super(" ");
        setFont(Tema.FONTE_PEQUENA.deriveFont(java.awt.Font.BOLD));
        setForeground(Tema.TEXTO_SUAVE);
        setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        setIconTextGap(6);
    }

    /** Mostra o texto; {@code sucesso} liga o visual de pílula verde. */
    public void mostrar(String texto, boolean sucesso) {
        this.sucesso = sucesso;
        setText(texto);
        setForeground(sucesso ? Tema.VERDE : Tema.TEXTO_SUAVE);
        setIcon(sucesso ? new Icone(Icone.Tipo.CHECK, 12, Tema.VERDE) : null);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        if (sucesso) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(46, 125, 50, 30));
            int w = Math.min(getWidth(), getPreferredSize().width);
            g.fillRoundRect(0, 0, w, getHeight(), getHeight(), getHeight());
            g.dispose();
        }
        super.paintComponent(g0);
    }
}
