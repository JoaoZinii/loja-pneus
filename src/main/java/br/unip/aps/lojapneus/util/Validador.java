package br.unip.aps.lojapneus.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.regex.Pattern;

import br.unip.aps.lojapneus.exception.ValidacaoException;

/**
 * Métodos reutilizáveis de validação e conversão das entradas do usuário.
 *
 * <p>Todo método recebe o texto do jeito que veio do campo da tela, confere
 * as regras e devolve o valor já convertido para o tipo certo (texto limpo,
 * {@code int}, {@link BigDecimal} ou {@link LocalDate}). Se alguma regra
 * falhar, lança {@link ValidacaoException} com o campo e a mensagem.</p>
 *
 * <p>Dois parâmetros aparecem em quase todos os métodos:</p>
 * <ul>
 *   <li>{@code campo}: identificador do campo (ex.: "nome"). Vai dentro da
 *       exceção para a tela saber onde colocar o foco;</li>
 *   <li>{@code rotulo}: como o campo aparece na mensagem, já com o artigo.
 *       Ex.: o rótulo "o nome do cliente" gera "Informe o nome do cliente.".</li>
 * </ul>
 *
 * <p>A classe não depende de tela nem de banco, então pode ser testada
 * sozinha (veja {@code TesteValidador}).</p>
 */
public final class Validador {

    /** E-mail: texto, @, texto, ponto, texto, sem espaços. Simples de propósito. */
    private static final Pattern EMAIL =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    /** Telefone: só dígitos e símbolos de máscara (parênteses, hífen, ponto, espaço). */
    private static final Pattern TELEFONE_PERMITIDO =
            Pattern.compile("^[0-9()\\-.\\s]*$");

    /** Número inteiro com sinal opcional. Ex.: 15, -3. */
    private static final Pattern INTEIRO = Pattern.compile("^-?\\d+$");

    /** Decimal simples. Ex.: 589 | 589,9 | 589,90 | 589.90 | -5. */
    private static final Pattern DECIMAL_SIMPLES =
            Pattern.compile("^-?\\d+([.,]\\d{1,2})?$");

    /** Decimal com separador de milhar no padrão brasileiro. Ex.: 1.299 | 1.299,90. */
    private static final Pattern DECIMAL_COM_MILHAR =
            Pattern.compile("^-?\\d{1,3}(\\.\\d{3})+(,\\d{1,2})?$");

    /** Maior valor que cabe numa coluna DECIMAL(10,2) do banco. */
    private static final BigDecimal VALOR_MAXIMO = new BigDecimal("99999999.99");

    /**
     * Formato de data dd/MM/aaaa. O modo STRICT recusa datas que não existem,
     * como 31/02/2026 (sem ele, o Java "ajustaria" para 28/02).
     */
    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu")
                             .withResolverStyle(ResolverStyle.STRICT);

    private Validador() {
        // classe utilitária: só métodos estáticos, não deve ser instanciada
    }

    // ==================================================================
    // Texto
    // ==================================================================

    /**
     * Valida um texto obrigatório: não pode ficar vazio nem passar do tamanho.
     *
     * @param texto         o que o usuário digitou
     * @param tamanhoMaximo tamanho máximo (o mesmo da coluna VARCHAR no banco)
     * @param campo         identificador do campo
     * @param rotulo        nome do campo na mensagem, com artigo (ex.: "o nome do cliente")
     * @return o texto sem espaços sobrando no início e no fim
     * @throws ValidacaoException se estiver vazio ou for longo demais
     */
    public static String textoObrigatorio(String texto, int tamanhoMaximo,
                                          String campo, String rotulo)
            throws ValidacaoException {
        String valor = limpar(texto);
        if (valor.isEmpty()) {
            throw new ValidacaoException(campo, "Informe " + rotulo + ".");
        }
        verificarTamanho(valor, tamanhoMaximo, campo, rotulo);
        return valor;
    }

    /**
     * Valida um texto opcional: pode ficar vazio, mas não pode passar do tamanho.
     *
     * @param texto         o que o usuário digitou
     * @param tamanhoMaximo tamanho máximo (o mesmo da coluna VARCHAR no banco)
     * @param campo         identificador do campo
     * @param rotulo        nome do campo na mensagem, com artigo
     * @return o texto limpo, ou null se estiver vazio (o banco grava NULL)
     * @throws ValidacaoException se for longo demais
     */
    public static String textoOpcional(String texto, int tamanhoMaximo,
                                       String campo, String rotulo)
            throws ValidacaoException {
        String valor = limpar(texto);
        if (valor.isEmpty()) {
            return null;
        }
        verificarTamanho(valor, tamanhoMaximo, campo, rotulo);
        return valor;
    }

    /**
     * Valida um e-mail opcional. Se preenchido, precisa ter o formato
     * nome@dominio.algo (um @ e pelo menos um ponto depois dele).
     *
     * @param texto         o que o usuário digitou
     * @param tamanhoMaximo tamanho máximo da coluna no banco
     * @param campo         identificador do campo
     * @return o e-mail limpo, ou null se estiver vazio
     * @throws ValidacaoException se o formato for inválido ou for longo demais
     */
    public static String email(String texto, int tamanhoMaximo, String campo)
            throws ValidacaoException {
        String valor = textoOpcional(texto, tamanhoMaximo, campo, "o e-mail");
        if (valor != null && !EMAIL.matcher(valor).matches()) {
            throw new ValidacaoException(campo,
                    "E-mail inválido. Use o formato nome@dominio.com.");
        }
        return valor;
    }

    /**
     * Valida um telefone opcional. Aceita o número com ou sem máscara,
     * tira tudo que não é dígito e confere se sobraram 10 ou 11 dígitos
     * (DDD + número fixo ou celular).
     *
     * @param texto o que o usuário digitou (ex.: "(19) 99123-4567")
     * @param campo identificador do campo
     * @return só os dígitos (ex.: "19991234567"), ou null se estiver vazio
     * @throws ValidacaoException se tiver letras ou a quantidade de dígitos errada
     */
    public static String telefone(String texto, String campo)
            throws ValidacaoException {
        String valor = limpar(texto);
        if (!TELEFONE_PERMITIDO.matcher(valor).matches()) {
            throw new ValidacaoException(campo, "O telefone deve ter só números "
                    + "(parênteses, espaço e hífen são aceitos).");
        }
        String digitos = valor.replaceAll("\\D", ""); // \D = tudo que não é dígito
        if (digitos.isEmpty()) {
            return null; // campo vazio ou só a máscara "(  )      -    "
        }
        if (digitos.length() != 10 && digitos.length() != 11) {
            throw new ValidacaoException(campo,
                    "Telefone deve ter DDD e número (10 ou 11 dígitos).");
        }
        return digitos;
    }

    // ==================================================================
    // Números inteiros
    // ==================================================================

    /**
     * Converte um texto obrigatório em número inteiro.
     *
     * @param texto  o que o usuário digitou
     * @param campo  identificador do campo
     * @param rotulo nome do campo na mensagem, com artigo (ex.: "o estoque")
     * @return o número convertido
     * @throws ValidacaoException se estiver vazio, tiver letras ou não couber num int
     */
    public static int inteiro(String texto, String campo, String rotulo)
            throws ValidacaoException {
        String valor = limpar(texto);
        if (valor.isEmpty()) {
            throw new ValidacaoException(campo, "Informe " + rotulo + ".");
        }
        if (!INTEIRO.matcher(valor).matches()) {
            throw new ValidacaoException(campo, maiuscula(rotulo)
                    + " deve ser um número inteiro, sem letras, pontos ou vírgulas.");
        }
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            // O texto só tem dígitos, então o único motivo é não caber num int.
            throw new ValidacaoException(campo, maiuscula(rotulo) + " é grande demais.");
        }
    }

    /**
     * Converte em inteiro e confere se está dentro de um intervalo.
     *
     * @param texto  o que o usuário digitou
     * @param minimo menor valor aceito
     * @param maximo maior valor aceito
     * @param campo  identificador do campo
     * @param rotulo nome do campo na mensagem, com artigo (ex.: "o aro")
     * @return o número convertido
     * @throws ValidacaoException se não for inteiro ou estiver fora do intervalo
     */
    public static int inteiroEntre(String texto, int minimo, int maximo,
                                   String campo, String rotulo)
            throws ValidacaoException {
        int numero = inteiro(texto, campo, rotulo);
        if (numero < minimo || numero > maximo) {
            throw new ValidacaoException(campo, maiuscula(rotulo)
                    + " deve estar entre " + minimo + " e " + maximo + ".");
        }
        return numero;
    }

    /**
     * Converte em inteiro e confere se é maior ou igual a zero.
     *
     * @param texto  o que o usuário digitou
     * @param campo  identificador do campo
     * @param rotulo nome do campo na mensagem, com artigo (ex.: "o estoque")
     * @return o número convertido
     * @throws ValidacaoException se não for inteiro ou for negativo
     */
    public static int inteiroNaoNegativo(String texto, String campo, String rotulo)
            throws ValidacaoException {
        int numero = inteiro(texto, campo, rotulo);
        if (numero < 0) {
            throw new ValidacaoException(campo,
                    maiuscula(rotulo) + " não pode ser negativo.");
        }
        return numero;
    }

    /**
     * Converte em inteiro e confere se é maior que zero.
     *
     * @param texto  o que o usuário digitou
     * @param campo  identificador do campo
     * @param rotulo nome do campo na mensagem, com artigo (ex.: "a quantidade")
     * @return o número convertido
     * @throws ValidacaoException se não for inteiro ou for zero ou negativo
     */
    public static int inteiroPositivo(String texto, String campo, String rotulo)
            throws ValidacaoException {
        int numero = inteiro(texto, campo, rotulo);
        if (numero <= 0) {
            throw new ValidacaoException(campo,
                    maiuscula(rotulo) + " deve ser maior que zero.");
        }
        return numero;
    }

    // ==================================================================
    // Dinheiro
    // ==================================================================

    /**
     * Converte um valor em dinheiro obrigatório e maior que zero.
     *
     * <p>Aceita vírgula ou ponto nos centavos ("589,90" e "589.90"), o
     * separador de milhar brasileiro ("1.299,90") e o prefixo "R$". Devolve
     * {@link BigDecimal}, que guarda o valor exato (double arredondaria).</p>
     *
     * @param texto  o que o usuário digitou
     * @param campo  identificador do campo
     * @param rotulo nome do campo na mensagem, com artigo (ex.: "o preço")
     * @return o valor com 2 casas decimais (ex.: 589.90)
     * @throws ValidacaoException se estiver vazio, mal formatado, zero,
     *                            negativo ou maior que o banco aceita
     */
    public static BigDecimal valorPositivo(String texto, String campo, String rotulo)
            throws ValidacaoException {
        // Tira o "R$" e os espaços (inclusive o espaço especial   que o
        // NumberFormat coloca em "R$ 589,90").
        String valor = limpar(texto).replace("R$", "").replaceAll("[\\s\\u00A0]", "");
        if (valor.isEmpty()) {
            throw new ValidacaoException(campo, "Informe " + rotulo + ".");
        }

        // Deixa o número no formato que o BigDecimal entende: 1299.90
        String normalizado;
        if (DECIMAL_SIMPLES.matcher(valor).matches()) {
            normalizado = valor.replace(',', '.');
        } else if (DECIMAL_COM_MILHAR.matcher(valor).matches()) {
            normalizado = valor.replace(".", "").replace(',', '.');
        } else {
            throw new ValidacaoException(campo, maiuscula(rotulo)
                    + " deve ser um número com até 2 casas decimais (ex.: 589,90).");
        }

        BigDecimal numero = new BigDecimal(normalizado).setScale(2);
        if (numero.signum() <= 0) {
            throw new ValidacaoException(campo,
                    maiuscula(rotulo) + " deve ser um valor maior que zero.");
        }
        if (numero.compareTo(VALOR_MAXIMO) > 0) {
            throw new ValidacaoException(campo,
                    maiuscula(rotulo) + " deve ser de no máximo 99.999.999,99.");
        }
        return numero;
    }

    // ==================================================================
    // Datas
    // ==================================================================

    /**
     * Converte uma data opcional no formato dd/mm/aaaa.
     *
     * @param texto  o que o usuário digitou (ex.: "05/10/2026")
     * @param campo  identificador do campo
     * @param rotulo nome do campo na mensagem, com artigo (ex.: "a data inicial")
     * @return a data convertida, ou null se o campo estiver vazio
     * @throws ValidacaoException se estiver incompleta ou a data não existir
     */
    public static LocalDate dataOpcional(String texto, String campo, String rotulo)
            throws ValidacaoException {
        String valor = limpar(texto);
        if (valor.replaceAll("\\D", "").isEmpty()) {
            return null; // campo vazio ou só a máscara "  /  /    "
        }
        try {
            return LocalDate.parse(valor, FORMATO_DATA);
        } catch (DateTimeParseException e) {
            throw new ValidacaoException(campo, maiuscula(rotulo)
                    + " é inválida. Use dd/mm/aaaa (ex.: 05/10/2026).");
        }
    }

    // ==================================================================
    // Auxiliares
    // ==================================================================

    /**
     * Transforma null em texto vazio e tira os espaços do início e do fim.
     * Assim nenhum outro método precisa se preocupar com null.
     *
     * @param texto o texto original (pode ser null)
     * @return o texto limpo, nunca null
     */
    public static String limpar(String texto) {
        return (texto == null) ? "" : texto.trim();
    }

    /** Lança a exceção se o texto passar do tamanho da coluna no banco. */
    private static void verificarTamanho(String valor, int tamanhoMaximo,
                                         String campo, String rotulo)
            throws ValidacaoException {
        if (valor.length() > tamanhoMaximo) {
            throw new ValidacaoException(campo, maiuscula(rotulo)
                    + " deve ter no máximo " + tamanhoMaximo + " caracteres"
                    + " (você digitou " + valor.length() + ").");
        }
    }

    /** Deixa a primeira letra maiúscula: "o preço" vira "O preço". */
    private static String maiuscula(String texto) {
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}
