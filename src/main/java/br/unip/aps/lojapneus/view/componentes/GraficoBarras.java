package br.unip.aps.lojapneus.view.componentes;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import javax.swing.JComponent;

import br.unip.aps.lojapneus.view.estilo.Tema;

/**
 * Gráfico de barras simples desenhado em código (sem biblioteca):
 * colunas verticais (ex.: faturamento por mês) ou linhas horizontais (rankings).
 */
public final class GraficoBarras extends JComponent {

    /** Orientação do gráfico. */
    public enum Modo { COLUNAS, LINHAS }

    /**
     * @param rotulo texto do item (mês, cliente, pneu)
     * @param valor  valor numérico que define o tamanho da barra
     * @param texto  valor já formatado, mostrado junto da barra
     * @param cor    cor da barra, ou null para a cor padrão do gráfico
     */
    public record Item(String rotulo, double valor, String texto, Color cor) { }

    private final Modo modo;
    private final Color cor;
    private List<Item> itens = List.of();
    private String textoVazio = "Sem dados";
    private double maximoFixo;

    public GraficoBarras(Modo modo, Color cor) {
        this.modo = modo;
        this.cor = cor;
        setOpaque(false);
        setPreferredSize(new Dimension(280, 130));
    }

    /** Troca os dados do gráfico e redesenha. */
    public void setItens(List<Item> novos, String quandoVazio) {
        this.itens = List.copyOf(novos);
        this.textoVazio = quandoVazio;
        repaint();
    }

    /** Fixa o valor que corresponde à barra cheia (0 = usa o maior valor dos dados). */
    public void setMaximo(double maximo) {
        this.maximoFixo = maximo;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        if (itens.isEmpty()) {
            g.setFont(Tema.FONTE);
            g.setColor(new Color(0xA0A8B3));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(textoVazio, (w - fm.stringWidth(textoVazio)) / 2,
                    h / 2 + (fm.getAscent() - fm.getDescent()) / 2);
            g.dispose();
            return;
        }
        double max = maximoFixo > 0 ? maximoFixo
                : itens.stream().mapToDouble(Item::valor).max().orElse(1);
        if (max <= 0) {
            max = 1;
        }
        if (modo == Modo.COLUNAS) {
            desenharColunas(g, w, h, max);
        } else {
            desenharLinhas(g, w, h, max);
        }
        g.dispose();
    }

    private void desenharColunas(Graphics2D g, int w, int h, double max) {
        g.setFont(Tema.FONTE_PEQUENA);
        FontMetrics fm = g.getFontMetrics();
        Font negrito = Tema.FONTE_PEQUENA.deriveFont(Font.BOLD);
        int base = h - fm.getHeight() - 4;
        int topo = fm.getHeight() + 6;
        int alturaMax = Math.max(10, base - topo);
        int n = itens.size();
        int faixa = w / n;
        int largura = Math.max(8, Math.min(46, faixa - 14));
        g.setColor(Tema.BORDA);
        g.drawLine(0, base, w, base);
        for (int i = 0; i < n; i++) {
            Item it = itens.get(i);
            int x = i * faixa + (faixa - largura) / 2;
            int altura = (int) Math.round(alturaMax * Math.min(1.0, it.valor() / max));
            if (it.valor() > 0) {
                altura = Math.max(altura, 3);
            }
            g.setColor(it.cor() != null ? it.cor() : cor);
            if (altura > 0) {
                g.fill(new RoundRectangle2D.Float(x, base - altura, largura, altura, 6, 6));
            }
            g.setFont(negrito);
            g.setColor(Tema.GRAFITE_CLARO);
            String valor = it.texto();
            int yValor = base - altura - 5;
            g.drawString(valor, i * faixa + (faixa - g.getFontMetrics().stringWidth(valor)) / 2, yValor);
            g.setFont(Tema.FONTE_PEQUENA);
            g.setColor(Tema.TEXTO_SUAVE);
            String rotulo = it.rotulo();
            g.drawString(rotulo, i * faixa + (faixa - fm.stringWidth(rotulo)) / 2,
                    base + fm.getAscent() + 3);
        }
    }

    private void desenharLinhas(Graphics2D g, int w, int h, double max) {
        g.setFont(Tema.FONTE_PEQUENA);
        FontMetrics fm = g.getFontMetrics();
        int n = itens.size();
        int alturaLinha = Math.min(34, h / n);
        int larguraRotulo = Math.min(Math.max(90, w / 3), 190);
        int larguraValor = 0;
        for (Item it : itens) {
            larguraValor = Math.max(larguraValor, fm.stringWidth(it.texto()));
        }
        int xBarra = larguraRotulo + 10;
        int larguraBarra = Math.max(20, w - xBarra - larguraValor - 12);
        int alturaBarra = 10;
        for (int i = 0; i < n; i++) {
            Item it = itens.get(i);
            int yc = i * alturaLinha + alturaLinha / 2;
            int linhaTexto = yc + (fm.getAscent() - fm.getDescent()) / 2;
            g.setColor(Tema.TEXTO);
            g.drawString(cortar(it.rotulo(), fm, larguraRotulo), 0, linhaTexto);
            g.setColor(Tema.LINHA);
            g.fill(new RoundRectangle2D.Float(xBarra, yc - alturaBarra / 2f, larguraBarra,
                    alturaBarra, alturaBarra, alturaBarra));
            int comprimento = (int) Math.round(larguraBarra * Math.min(1.0, it.valor() / max));
            if (it.valor() > 0) {
                comprimento = Math.max(comprimento, alturaBarra);
            }
            Color corBarra = it.cor() != null ? it.cor() : cor;
            if (comprimento > 0) {
                g.setColor(corBarra);
                g.fill(new RoundRectangle2D.Float(xBarra, yc - alturaBarra / 2f, comprimento,
                        alturaBarra, alturaBarra, alturaBarra));
            }
            g.setColor(it.cor() != null ? it.cor() : Tema.GRAFITE_CLARO);
            g.drawString(it.texto(), w - larguraValor, linhaTexto);
        }
    }

    /** Corta o texto com reticências para caber na largura. */
    private static String cortar(String s, FontMetrics fm, int largura) {
        if (fm.stringWidth(s) <= largura) {
            return s;
        }
        int fim = s.length();
        while (fim > 1 && fm.stringWidth(s.substring(0, fim) + "…") > largura) {
            fim--;
        }
        return s.substring(0, fim) + "…";
    }
}
