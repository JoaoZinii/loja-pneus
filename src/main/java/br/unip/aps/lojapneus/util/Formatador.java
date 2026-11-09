package br.unip.aps.lojapneus.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Formata valores para exibir na tela no padrão brasileiro.
 *
 * <p>É o caminho inverso do {@link Validador}: o Validador transforma o
 * texto digitado em valor (ex.: "589,90" vira 589.90), e o Formatador
 * transforma o valor em texto para mostrar (589.90 vira "R$ 589,90").</p>
 *
 * <p>Uso opcional pelas telas, para o preço aparecer como moeda
 * e a data como dd/mm/aaaa em vez do formato interno do Java.</p>
 */
public final class Formatador {

    /** Idioma e país usados na formatação: português do Brasil. */
    private static final Locale BRASIL = Locale.forLanguageTag("pt-BR");

    private static final DateTimeFormatter DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final DateTimeFormatter DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Formatador() {
        // classe utilitária: só métodos estáticos, não deve ser instanciada
    }

    /**
     * Formata um valor como moeda brasileira.
     *
     * @param valor o valor (ex.: 589.90)
     * @return o texto formatado (ex.: "R$ 589,90"), ou "" se o valor for null
     */
    public static String moeda(BigDecimal valor) {
        if (valor == null) {
            return "";
        }
        // NumberFormat não é thread-safe, por isso é criado a cada chamada.
        return NumberFormat.getCurrencyInstance(BRASIL).format(valor);
    }

    /**
     * Formata um valor com vírgula e 2 casas, sem o "R$". Útil para preencher
     * o campo de preço no formulário de edição.
     *
     * @param valor o valor (ex.: 1299.90)
     * @return o texto formatado (ex.: "1.299,90"), ou "" se o valor for null
     */
    public static String decimal(BigDecimal valor) {
        if (valor == null) {
            return "";
        }
        NumberFormat formato = NumberFormat.getNumberInstance(BRASIL);
        formato.setMinimumFractionDigits(2);
        formato.setMaximumFractionDigits(2);
        return formato.format(valor);
    }

    /**
     * Formata uma data.
     *
     * @param data a data
     * @return o texto no formato dd/mm/aaaa, ou "" se a data for null
     */
    public static String data(LocalDate data) {
        return (data == null) ? "" : data.format(DATA);
    }

    /**
     * Formata uma data com hora.
     *
     * @param dataHora a data e hora
     * @return o texto no formato dd/mm/aaaa hh:mm, ou "" se for null
     */
    public static String dataHora(LocalDateTime dataHora) {
        return (dataHora == null) ? "" : dataHora.format(DATA_HORA);
    }

    /**
     * Formata um telefone guardado só com dígitos.
     *
     * @param digitos o telefone como está no banco (ex.: "19991234567")
     * @return o telefone com máscara (ex.: "(19) 99123-4567"), o texto
     *         original se não tiver 10 ou 11 dígitos, ou "" se for null
     */
    public static String telefone(String digitos) {
        if (digitos == null) {
            return "";
        }
        if (digitos.length() == 11) {
            return "(" + digitos.substring(0, 2) + ") "
                    + digitos.substring(2, 7) + "-" + digitos.substring(7);
        }
        if (digitos.length() == 10) {
            return "(" + digitos.substring(0, 2) + ") "
                    + digitos.substring(2, 6) + "-" + digitos.substring(6);
        }
        return digitos;
    }
}
