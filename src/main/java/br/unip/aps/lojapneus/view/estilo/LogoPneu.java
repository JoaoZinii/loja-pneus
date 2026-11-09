package br.unip.aps.lojapneus.view.estilo;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.Icon;

/**
 * Logo da loja: um pneu visto de frente, desenhado em código (sem arquivo de imagem).
 * Tem banda de rodagem com blocos, lateral do pneu, roda cromada com 5 raios e cubo laranja.
 * Fica nítido em qualquer tamanho (usado no cabeçalho e no ícone da janela).
 */
public class LogoPneu implements Icon {

    private static final int BLOCOS = 24;
    private static final int RAIOS = 5;

    private final int tam;

    /** @param tam lado do desenho em pixels */
    public LogoPneu(int tam) {
        this.tam = tam;
    }

    @Override
    public void paintIcon(Component c, Graphics g0, int x, int y) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.translate(x, y);
        float s = tam;
        float cx = s / 2f;
        float cy = s / 2f;

        // 1) pneu (borracha)
        g.setColor(new Color(0x0B0F14));
        g.fill(circulo(cx, cy, s * .5f));

        // 2) blocos da banda de rodagem, em volta de todo o pneu
        float largBloco = Math.max(1.5f, s * .075f);
        float altBloco = s * .075f;
        g.setColor(new Color(0x2B3641));
        for (int i = 0; i < BLOCOS; i++) {
            Graphics2D b = (Graphics2D) g.create();
            b.rotate(Math.PI * 2 * i / BLOCOS, cx, cy);
            b.fill(new RoundRectangle2D.Float(cx - largBloco / 2f, cy - s * .5f + s * .02f,
                    largBloco, altBloco, largBloco * .4f, largBloco * .4f));
            b.dispose();
        }

        // 3) lateral do pneu, com um filete mais claro
        g.setColor(new Color(0x161D24));
        g.fill(circulo(cx, cy, s * .385f));
        g.setColor(new Color(0x3E4C59));
        g.setStroke(new BasicStroke(Math.max(1f, s * .015f)));
        g.draw(circulo(cx, cy, s * .35f));

        // 4) roda cromada (degradê claro -> escuro dá a ideia de metal)
        g.setPaint(new GradientPaint(cx - s * .3f, cy - s * .3f, new Color(0xF1F3F5),
                cx + s * .3f, cy + s * .3f, new Color(0x7B8794)));
        g.fill(circulo(cx, cy, s * .30f));
        g.setColor(new Color(0x52606D));
        g.setStroke(new BasicStroke(Math.max(1f, s * .012f)));
        g.draw(circulo(cx, cy, s * .30f));

        // 5) fundo escuro entre os raios
        g.setColor(new Color(0x1F2933));
        g.fill(circulo(cx, cy, s * .22f));

        // 6) raios (5, a partir do topo)
        float larguraRaio = s * .085f;
        g.setColor(new Color(0xDDE1E6));
        for (int i = 0; i < RAIOS; i++) {
            Graphics2D r = (Graphics2D) g.create();
            r.rotate(Math.PI * 2 * i / RAIOS, cx, cy);
            r.fill(new RoundRectangle2D.Float(cx - larguraRaio / 2f, cy - s * .245f,
                    larguraRaio, s * .245f, larguraRaio * .6f, larguraRaio * .6f));
            r.dispose();
        }

        // 7) cubo laranja com parafuso central
        g.setColor(Tema.LARANJA);
        g.fill(circulo(cx, cy, s * .095f));
        g.setColor(new Color(0x1F2933));
        g.fill(circulo(cx, cy, s * .032f));

        // 8) brilho no alto à esquerda
        g.setColor(new Color(255, 255, 255, 50));
        g.setStroke(new BasicStroke(Math.max(1f, s * .02f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Arc2D.Float(cx - s * .455f, cy - s * .455f, s * .91f, s * .91f, 105, 55, Arc2D.OPEN));

        g.dispose();
    }

    private static Ellipse2D.Float circulo(float cx, float cy, float raio) {
        return new Ellipse2D.Float(cx - raio, cy - raio, 2 * raio, 2 * raio);
    }

    @Override public int getIconWidth() { return tam; }
    @Override public int getIconHeight() { return tam; }
}
