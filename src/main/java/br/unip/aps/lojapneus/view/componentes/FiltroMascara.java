package br.unip.aps.lojapneus.view.componentes;

import java.util.function.UnaryOperator;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * Filtro de documento que reformata todo o conteúdo a cada edição (máscara dinâmica).
 * Backspace sobre um símbolo da máscara apaga o dígito anterior.
 */
class FiltroMascara extends DocumentFilter {

    private final UnaryOperator<String> formatador;
    private final UnaryOperator<String> limpezaDeColagem;

    /**
     * @param formatador       recebe o texto bruto e devolve o texto já com máscara
     * @param limpezaDeColagem ajusta textos colados (mais de 1 caractere); pode ser identidade
     */
    FiltroMascara(UnaryOperator<String> formatador, UnaryOperator<String> limpezaDeColagem) {
        this.formatador = formatador;
        this.limpezaDeColagem = limpezaDeColagem;
    }

    @Override
    public void insertString(FilterBypass fb, int off, String s, AttributeSet a)
            throws BadLocationException {
        replace(fb, off, 0, s, a);
    }

    @Override
    public void remove(FilterBypass fb, int off, int len) throws BadLocationException {
        String atual = texto(fb);
        if (len == 1 && off < atual.length() && !Character.isDigit(atual.charAt(off))) {
            while (off > 0 && !Character.isDigit(atual.charAt(off))) {
                off--;
            }
            if (!Character.isDigit(atual.charAt(off))) {
                return;
            }
        }
        aplicar(fb, new StringBuilder(atual).delete(off, off + len).toString());
    }

    @Override
    public void replace(FilterBypass fb, int off, int len, String texto, AttributeSet a)
            throws BadLocationException {
        String novo = texto == null ? "" : texto;
        if (novo.length() > 1) {
            novo = limpezaDeColagem.apply(novo);
        }
        aplicar(fb, new StringBuilder(texto(fb)).replace(off, off + len, novo).toString());
    }

    private static String texto(FilterBypass fb) throws BadLocationException {
        return fb.getDocument().getText(0, fb.getDocument().getLength());
    }

    private void aplicar(FilterBypass fb, String novo) throws BadLocationException {
        fb.replace(0, fb.getDocument().getLength(), formatador.apply(novo), null);
    }
}
