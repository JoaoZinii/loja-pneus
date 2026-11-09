package br.unip.aps.lojapneus.view.estilo;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.Path2D;
import javax.swing.Icon;

/**
 * Ícones vetoriais desenhados em código (sem arquivos de imagem), nítidos em qualquer tamanho.
 * Sem cor fixa, o ícone usa a cor do texto do componente (acompanha hover e desabilitado).
 */
public class Icone implements Icon {

    /** Desenhos disponíveis. */
    public enum Tipo { MAIS, LAPIS, LIXEIRA, ATUALIZAR, LUPA, X, CHECK, INFO, ALERTA,
        USUARIO, EMAIL, CELULAR, PNEU, CAIXA, CIFRA, RECIBO, BAIXAR }

    private final Tipo tipo;
    private final int tam;
    private final Color cor;

    /**
     * @param tipo desenho
     * @param tam  lado em pixels
     * @param cor  cor fixa, ou null para usar a cor do texto do componente
     */
    public Icone(Tipo tipo, int tam, Color cor) {
        this.tipo = tipo;
        this.tam = tam;
        this.cor = cor;
    }

    @Override
    public void paintIcon(Component c, Graphics g0, int x, int y) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.translate(x, y);
        g.setColor(cor != null ? cor : (c != null ? c.getForeground() : Tema.TEXTO));
        float s = tam;
        g.setStroke(new BasicStroke(Math.max(1.6f, s / 9f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (tipo) {
            case MAIS -> {
                linha(g, .5f, .2f, .5f, .8f);
                linha(g, .2f, .5f, .8f, .5f);
            }
            case X -> {
                linha(g, .25f, .25f, .75f, .75f);
                linha(g, .75f, .25f, .25f, .75f);
            }
            case LUPA -> {
                g.drawOval(Math.round(.12f * s), Math.round(.12f * s), Math.round(.5f * s), Math.round(.5f * s));
                linha(g, .56f, .56f, .86f, .86f);
            }
            case LAPIS -> {
                g.setStroke(new BasicStroke(s * .2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                linha(g, .36f, .64f, .72f, .28f);
                g.setStroke(new BasicStroke(Math.max(1.6f, s / 9f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                linha(g, .16f, .86f, .3f, .7f);
                linha(g, .16f, .86f, .46f, .86f);
            }
            case LIXEIRA -> {
                linha(g, .2f, .28f, .8f, .28f);
                Path2D alca = new Path2D.Float();
                alca.moveTo(.4f * s, .28f * s);
                alca.lineTo(.4f * s, .15f * s);
                alca.lineTo(.6f * s, .15f * s);
                alca.lineTo(.6f * s, .28f * s);
                g.draw(alca);
                Path2D corpo = new Path2D.Float();
                corpo.moveTo(.28f * s, .36f * s);
                corpo.lineTo(.32f * s, .86f * s);
                corpo.lineTo(.68f * s, .86f * s);
                corpo.lineTo(.72f * s, .36f * s);
                g.draw(corpo);
                linha(g, .44f, .48f, .44f, .74f);
                linha(g, .56f, .48f, .56f, .74f);
            }
            case CHECK -> {
                Path2D p = new Path2D.Float();
                p.moveTo(.22f * s, .54f * s);
                p.lineTo(.43f * s, .74f * s);
                p.lineTo(.8f * s, .28f * s);
                g.draw(p);
            }
            case INFO -> {
                g.fill(new Ellipse2D.Float(.43f * s, .18f * s, .14f * s, .14f * s));
                linha(g, .5f, .44f, .5f, .8f);
            }
            case ALERTA -> {
                linha(g, .5f, .16f, .5f, .6f);
                g.fill(new Ellipse2D.Float(.43f * s, .74f * s, .14f * s, .14f * s));
            }
            case USUARIO -> {
                g.draw(new Ellipse2D.Float(.34f * s, .14f * s, .32f * s, .32f * s));
                g.draw(new Arc2D.Float(.2f * s, .56f * s, .6f * s, .6f * s, 0, 180, Arc2D.OPEN));
            }
            case EMAIL -> {
                g.draw(new RoundRectangle2D.Float(.12f * s, .24f * s, .76f * s, .52f * s, .1f * s, .1f * s));
                Path2D v = new Path2D.Float();
                v.moveTo(.14f * s, .3f * s);
                v.lineTo(.5f * s, .56f * s);
                v.lineTo(.86f * s, .3f * s);
                g.draw(v);
            }
            case CELULAR -> {
                g.draw(new RoundRectangle2D.Float(.3f * s, .1f * s, .4f * s, .8f * s, .1f * s, .1f * s));
                linha(g, .44f, .76f, .56f, .76f);
            }
            case PNEU -> {
                float c0 = s / 2f;
                float raioPneu = .30f * s;
                g.draw(new Ellipse2D.Float(c0 - raioPneu, c0 - raioPneu, 2 * raioPneu, 2 * raioPneu));
                // blocos da banda de rodagem (12 tracinhos em volta)
                for (int i = 0; i < 12; i++) {
                    double a = Math.PI * 2 * i / 12;
                    float x1 = c0 + (float) Math.cos(a) * .37f * s;
                    float y1 = c0 + (float) Math.sin(a) * .37f * s;
                    float x2 = c0 + (float) Math.cos(a) * .45f * s;
                    float y2 = c0 + (float) Math.sin(a) * .45f * s;
                    g.draw(new java.awt.geom.Line2D.Float(x1, y1, x2, y2));
                }
                // roda com 5 raios e cubo
                g.setStroke(new BasicStroke(Math.max(1.2f, s / 14f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int i = 0; i < 5; i++) {
                    double a = Math.PI * 2 * i / 5 - Math.PI / 2;
                    g.draw(new java.awt.geom.Line2D.Float(c0, c0,
                            c0 + (float) Math.cos(a) * .17f * s, c0 + (float) Math.sin(a) * .17f * s));
                }
                g.fill(new Ellipse2D.Float(c0 - .07f * s, c0 - .07f * s, .14f * s, .14f * s));
            }
            case BAIXAR -> {
                linha(g, .5f, .14f, .5f, .62f);
                Path2D seta = new Path2D.Float();
                seta.moveTo(.3f * s, .44f * s);
                seta.lineTo(.5f * s, .66f * s);
                seta.lineTo(.7f * s, .44f * s);
                g.draw(seta);
                linha(g, .18f, .86f, .82f, .86f);
            }
            case CAIXA -> {
                g.draw(new RoundRectangle2D.Float(.14f * s, .3f * s, .72f * s, .54f * s, .08f * s, .08f * s));
                linha(g, .14f, .46f, .86f, .46f);
                linha(g, .42f, .6f, .58f, .6f);
            }
            case CIFRA -> {
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.round(s * .9f)));
                FontMetrics fm = g.getFontMetrics();
                g.drawString("$", (s - fm.stringWidth("$")) / 2f, (s - fm.getHeight()) / 2f + fm.getAscent());
            }
            case RECIBO -> {
                g.draw(new RoundRectangle2D.Float(.24f * s, .12f * s, .52f * s, .76f * s, .08f * s, .08f * s));
                linha(g, .36f, .34f, .64f, .34f);
                linha(g, .36f, .5f, .64f, .5f);
                linha(g, .36f, .66f, .54f, .66f);
            }
            case ATUALIZAR -> {
                double cx = s / 2, cy = s / 2, r = s * .32;
                g.draw(new Arc2D.Double(cx - r, cy - r, 2 * r, 2 * r, 60, 270, Arc2D.OPEN));
                double ang = Math.toRadians(330);
                double ex = cx + r * Math.cos(ang), ey = cy - r * Math.sin(ang);
                double tx = -Math.sin(ang), ty = -Math.cos(ang); // tangente (sentido anti-horário)
                double nx = -ty, ny = tx, a = s * .2, b = s * .15;
                Path2D seta = new Path2D.Double();
                seta.moveTo(ex + tx * a, ey + ty * a);
                seta.lineTo(ex + nx * b, ey + ny * b);
                seta.lineTo(ex - nx * b, ey - ny * b);
                seta.closePath();
                g.fill(seta);
            }
        }
        g.dispose();
    }

    private void linha(Graphics2D g, float x1, float y1, float x2, float y2) {
        g.drawLine(Math.round(x1 * tam), Math.round(y1 * tam), Math.round(x2 * tam), Math.round(y2 * tam));
    }

    @Override public int getIconWidth() { return tam; }
    @Override public int getIconHeight() { return tam; }
}
