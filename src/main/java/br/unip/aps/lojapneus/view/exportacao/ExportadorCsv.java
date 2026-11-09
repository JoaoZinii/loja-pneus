package br.unip.aps.lojapneus.view.exportacao;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.table.TableModel;

import br.unip.aps.lojapneus.view.estilo.Formato;

/**
 * Grava o conteúdo de uma tabela em CSV no padrão que o Excel brasileiro abre direto:
 * separador ponto e vírgula, decimais com vírgula e UTF-8 com BOM (para os acentos).
 * Não mexe no banco: exporta exatamente o que está na tela.
 */
public final class ExportadorCsv {

    private static final String SEPARADOR = ";";
    private static final String QUEBRA = "\r\n";

    private ExportadorCsv() { }

    /**
     * @param destino arquivo a gravar (é substituído se existir)
     * @param modelo  tabela com os dados já filtrados/ordenados da tela
     * @throws IOException se o arquivo não puder ser gravado (ex.: aberto no Excel)
     */
    public static void salvar(Path destino, TableModel modelo) throws IOException {
        StringBuilder sb = new StringBuilder("\uFEFF");
        for (int c = 0; c < modelo.getColumnCount(); c++) {
            if (c > 0) {
                sb.append(SEPARADOR);
            }
            sb.append(celula(modelo.getColumnName(c)));
        }
        sb.append(QUEBRA);
        for (int l = 0; l < modelo.getRowCount(); l++) {
            for (int c = 0; c < modelo.getColumnCount(); c++) {
                if (c > 0) {
                    sb.append(SEPARADOR);
                }
                sb.append(celula(modelo.getValueAt(l, c)));
            }
            sb.append(QUEBRA);
        }
        Files.writeString(destino, sb.toString(), StandardCharsets.UTF_8);
    }

    /** Converte um valor em texto de CSV: números BR, aspas quando preciso e proteção de fórmula. */
    static String celula(Object valor) {
        String texto;
        if (valor == null) {
            texto = "";
        } else if (valor instanceof BigDecimal bd) {
            texto = Formato.decimal(bd);
        } else {
            texto = valor.toString();
        }
        // texto que começa com = + - @ viraria fórmula no Excel: o apóstrofo impede
        if (!texto.isEmpty() && "=+-@\t\r".indexOf(texto.charAt(0)) >= 0) {
            texto = "'" + texto;
        }
        if (texto.contains(SEPARADOR) || texto.contains("\"") || texto.contains("\n")
                || texto.contains("\r")) {
            texto = "\"" + texto.replace("\"", "\"\"") + "\"";
        }
        return texto;
    }
}
