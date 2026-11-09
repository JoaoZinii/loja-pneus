package br.unip.aps.lojapneus.view.componentes;

import javax.swing.text.AbstractDocument;

import br.unip.aps.lojapneus.view.estilo.Formato;

/**
 * Campo de telefone com máscara dinâmica: (11) 3456-7890 ou (11) 98765-4321.
 * Aceita só dígitos (até 11). Ao colar um número com código do país (+55), o 55 é ignorado.
 * Para carregar um valor existente basta usar {@code setText(...)}.
 */
public class CampoTelefone extends CampoTexto {

    /** Cria o campo já com a máscara ativa. */
    public CampoTelefone() {
        super(16, "(11) 99999-9999");
        ((AbstractDocument) getDocument()).setDocumentFilter(
                new FiltroMascara(Formato::telefone, CampoTelefone::tirarCodigoPais));
    }

    private static String tirarCodigoPais(String colado) {
        String d = colado.replaceAll("\\D", "");
        return (d.length() >= 12 && d.startsWith("55")) ? d.substring(2) : colado;
    }
}
