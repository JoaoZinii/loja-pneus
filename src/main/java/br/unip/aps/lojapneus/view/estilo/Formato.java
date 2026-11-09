package br.unip.aps.lojapneus.view.estilo;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

/** Formatação de valores para exibição (moeda, telefone) em pt-BR. */
public final class Formato {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private Formato() { }

    /** @return valor como moeda brasileira, ex.: R$ 589,90 */
    public static String moeda(BigDecimal valor) {
        return valor == null ? "" : NumberFormat.getCurrencyInstance(PT_BR).format(valor);
    }

    /** @return valor com vírgula e 2 casas, sem símbolo, ex.: 589,90 (para campos de edição) */
    public static String decimal(BigDecimal valor) {
        return valor == null ? ""
                : new DecimalFormat("0.00", new DecimalFormatSymbols(PT_BR)).format(valor);
    }

    /**
     * Formata dígitos como telefone brasileiro, aceitando digitação parcial.
     * Usado pelo campo de telefone enquanto o usuário digita.
     *
     * @param bruto qualquer texto; só os dígitos (até 11) são considerados
     * @return (DD) NNNN-NNNN ou (DD) NNNNN-NNNN
     */
    public static String telefone(String bruto) {
        if (bruto == null) {
            return "";
        }
        String d = bruto.replaceAll("\\D", "");
        if (d.length() > 11) {
            d = d.substring(0, 11);
        }
        int n = d.length();
        if (n == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder("(").append(d, 0, Math.min(2, n));
        if (n > 2) {
            sb.append(") ");
            int corte = n > 10 ? 7 : 6;
            sb.append(d, 2, Math.min(n, corte));
            if (n > corte) {
                sb.append('-').append(d, corte, n);
            }
        }
        return sb.toString();
    }

    /** @return telefone formatado para tabelas; se não tiver 10 ou 11 dígitos, devolve o texto original */
    public static String telefoneExibicao(String bruto) {
        if (bruto == null) {
            return "";
        }
        int n = bruto.replaceAll("\\D", "").length();
        return (n == 10 || n == 11) ? telefone(bruto) : bruto;
    }
}
