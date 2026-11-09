package br.unip.aps.lojapneus.view.componentes;

import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.BasicStroke;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.function.IntPredicate;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import br.unip.aps.lojapneus.view.estilo.Icone;
import br.unip.aps.lojapneus.view.estilo.Tema;

/**
 * Campo de texto do sistema: cantos arredondados, texto de dica quando vazio, anel laranja
 * no foco, vermelho em caso de erro (some quando o usuário volta a digitar), lupa opcional
 * e filtros de tamanho e de caracteres permitidos.
 */
public class CampoTexto extends JTextField {

    private static final int ARCO = 10;

    private final String dica;
    private boolean erro;
    private boolean lupa;

    /**
     * @param colunas largura em colunas
     * @param dica    texto cinza exibido enquanto o campo está vazio (pode ser null)
     */
    public CampoTexto(int colunas, String dica) {
        super(colunas);
        this.dica = dica;
        setOpaque(false);
        setFont(Tema.FONTE);
        setForeground(Tema.TEXTO);
        setCaretColor(Tema.TEXTO);
        setSelectionColor(Tema.SELECAO);
        setSelectedTextColor(Tema.TEXTO);
        atualizarBorda();
        addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { atualizarBorda(); repaint(); }
            @Override public void focusLost(FocusEvent e) { atualizarBorda(); repaint(); }
        });
        getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { limparErro(); }
            @Override public void removeUpdate(DocumentEvent e) { limparErro(); }
            @Override public void changedUpdate(DocumentEvent e) { limparErro(); }
        });
    }

    /** Mostra uma lupa dentro do campo (para campos de busca). @return este campo */
    public CampoTexto comLupa() {
        lupa = true;
        atualizarBorda();
        return this;
    }

    private void limparErro() {
        if (erro) {
            setErro(false);
        }
    }

    /** Liga ou desliga a borda vermelha de erro. */
    public void setErro(boolean erro) {
        this.erro = erro;
        atualizarBorda();
        repaint();
    }

    private void atualizarBorda() {
        boolean foco = isFocusOwner();
        Color cor = erro ? Tema.VERMELHO : (foco ? Tema.LARANJA : Tema.BORDA);
        setBorder(new BordaCampo(cor, (erro || foco) ? 2 : 1, lupa ? 34 : 12));
    }

    /** Limita o número de caracteres. @return este campo, para encadear */
    public CampoTexto limite(int max) {
        return permitir(c -> true, max);
    }

    /** Aceita só dígitos, até {@code max}. @return este campo */
    public CampoTexto somenteDigitos(int max) {
        return permitir(Character::isDigit, max);
    }

    /**
     * Aceita só os caracteres aprovados pelo predicado, até {@code max}.
     * Vale também para texto colado.
     */
    public CampoTexto permitir(IntPredicate aceito, int max) {
        ((AbstractDocument) getDocument()).setDocumentFilter(new Filtro(aceito, max));
        return this;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(isEnabled() ? Color.WHITE : Tema.FUNDO);
        g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARCO, ARCO);
        g.dispose();
        super.paintComponent(g0);

        Graphics2D g2 = (Graphics2D) g0.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        if (lupa) {
            new Icone(Icone.Tipo.LUPA, 16, new Color(0x9AA5B1))
                    .paintIcon(this, g2, 12, (getHeight() - 16) / 2);
        }
        if (dica != null && getText().isEmpty()) {
            g2.setColor(new Color(0xA0A8B3));
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(dica, getInsets().left, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
        }
        g2.dispose();
    }

    /** Borda arredondada de espessura variável; o espaço interno é constante (sem "pulo" ao focar). */
    private static final class BordaCampo implements Border {
        private final Color cor;
        private final int espessura;
        private final int esquerda;

        BordaCampo(Color cor, int espessura, int esquerda) {
            this.cor = cor;
            this.espessura = espessura;
            this.esquerda = esquerda;
        }

        @Override
        public void paintBorder(Component c, Graphics g0, int x, int y, int w, int h) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(cor);
            g.setStroke(new BasicStroke(espessura));
            int o = espessura / 2;
            g.drawRoundRect(x + o, y + o, w - espessura - (espessura == 1 ? 1 : 0),
                    h - espessura - (espessura == 1 ? 1 : 0), ARCO, ARCO);
            g.dispose();
        }

        @Override public Insets getBorderInsets(Component c) { return new Insets(9, esquerda, 9, 12); }
        @Override public boolean isBorderOpaque() { return false; }
    }

    /** Filtro de documento: caracteres permitidos + tamanho máximo. */
    private static class Filtro extends DocumentFilter {
        private final IntPredicate aceito;
        private final int max;

        Filtro(IntPredicate aceito, int max) {
            this.aceito = aceito;
            this.max = max;
        }

        @Override
        public void insertString(FilterBypass fb, int off, String s, AttributeSet a)
                throws BadLocationException {
            replace(fb, off, 0, s, a);
        }

        @Override
        public void replace(FilterBypass fb, int off, int len, String texto, AttributeSet a)
                throws BadLocationException {
            StringBuilder ok = new StringBuilder();
            if (texto != null) {
                texto.codePoints().filter(aceito).forEach(ok::appendCodePoint);
            }
            int espaco = Math.max(0, max - (fb.getDocument().getLength() - len));
            if (ok.length() > espaco) {
                ok.setLength(espaco);
            }
            super.replace(fb, off, len, ok.toString(), a);
        }
    }
}
