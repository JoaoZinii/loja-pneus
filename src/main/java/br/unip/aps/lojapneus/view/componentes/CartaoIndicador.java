package br.unip.aps.lojapneus.view.componentes;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.SwingUtilities;
import javax.swing.JLabel;
import javax.swing.JPanel;

import br.unip.aps.lojapneus.view.estilo.Icone;
import br.unip.aps.lojapneus.view.estilo.Tema;

/** Cartão de resumo (indicador): ícone colorido à esquerda, título pequeno e número grande. */
public class CartaoIndicador extends Cartao {

    private final JLabel valor = new JLabel("—");

    /**
     * @param titulo descrição do indicador
     * @param cor    cor do ícone
     * @param icone  desenho do ícone
     */
    public CartaoIndicador(String titulo, Color cor, Icone.Tipo icone) {
        super(14, false);
        setLayout(new BorderLayout(14, 0));
        setBorder(Tema.margem(14, 16));
        JLabel t = new JLabel(titulo);
        t.setFont(Tema.FONTE_PEQUENA);
        t.setForeground(Tema.TEXTO_SUAVE);
        valor.setFont(Tema.FONTE.deriveFont(Font.BOLD, 22f));
        valor.setForeground(Tema.GRAFITE);
        JPanel textos = new JPanel(new BorderLayout(0, 1));
        textos.setOpaque(false);
        textos.add(t, BorderLayout.NORTH);
        textos.add(valor, BorderLayout.CENTER);
        add(new Medalha(cor, icone, 46), BorderLayout.WEST);
        add(textos, BorderLayout.CENTER);
        // Em janelas estreitas o número encolhe para caber, em vez de ser cortado.
        valor.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { ajustarFonte(); }
        });
    }

    /** Reduz a fonte do número (de 22 até 13 pt) até ele caber na largura disponível. */
    private void ajustarFonte() {
        int largura = valor.getWidth();
        if (largura <= 0) {
            return;
        }
        float tam = 22f;
        Font f = Tema.FONTE.deriveFont(Font.BOLD, tam);
        FontMetrics fm = valor.getFontMetrics(f);
        while (tam > 13f && fm.stringWidth(valor.getText()) > largura) {
            tam -= 1f;
            f = Tema.FONTE.deriveFont(Font.BOLD, tam);
            fm = valor.getFontMetrics(f);
        }
        if (valor.getFont().getSize2D() != tam) {
            valor.setFont(f);
        }
    }

    /** Atualiza o número exibido. */
    public void setValor(String texto) {
        valor.setText(texto);
        SwingUtilities.invokeLater(this::ajustarFonte);
    }

    /** Muda a cor do número (ex.: vermelho para alerta). */
    public void setCorValor(Color c) {
        valor.setForeground(c);
    }
}
