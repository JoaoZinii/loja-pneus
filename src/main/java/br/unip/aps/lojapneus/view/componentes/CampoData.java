package br.unip.aps.lojapneus.view.componentes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.time.format.DateTimeParseException;
import javax.swing.text.AbstractDocument;

/** Campo de data com máscara dd/mm/aaaa. Fica vermelho se a data completa for inválida (ex.: 31/02). */
public class CampoData extends CampoTexto {

    private static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    /** Cria o campo com a máscara ativa. */
    public CampoData() {
        super(8, "dd/mm/aaaa");
        ((AbstractDocument) getDocument()).setDocumentFilter(
                new FiltroMascara(CampoData::mascara, s -> s));
    }

    private static String mascara(String bruto) {
        String d = bruto.replaceAll("\\D", "");
        if (d.length() > 8) {
            d = d.substring(0, 8);
        }
        StringBuilder sb = new StringBuilder(d);
        if (d.length() > 4) {
            sb.insert(4, '/');
        }
        if (d.length() > 2) {
            sb.insert(2, '/');
        }
        return sb.toString();
    }

    /** @return a data digitada, ou null se estiver incompleta ou inválida */
    public LocalDate getData() {
        String t = getText();
        if (t.length() < 10) {
            return null;
        }
        try {
            LocalDate d = LocalDate.parse(t, FORMATO);
            setErro(false);
            return d;
        } catch (DateTimeParseException e) {
            setErro(true);
            return null;
        }
    }

    /** Preenche o campo (null limpa). */
    public void setData(LocalDate data) {
        setText(data == null ? "" : data.format(FORMATO));
    }
}
