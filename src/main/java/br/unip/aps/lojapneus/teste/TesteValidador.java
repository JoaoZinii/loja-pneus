package br.unip.aps.lojapneus.teste;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import br.unip.aps.lojapneus.exception.ValidacaoException;
import br.unip.aps.lojapneus.util.Formatador;
import br.unip.aps.lojapneus.util.Validador;

/**
 * Testa o {@link Validador} sozinho, sem tela e sem banco.
 *
 * <p>Cada linha testa uma entrada: as boas precisam ser aceitas e convertidas
 * para o valor certo; as ruins precisam ser recusadas com
 * {@link ValidacaoException}. No fim aparece o placar.</p>
 *
 * <p>Para rodar: executar o {@code main} desta classe. Não precisa do MySQL.</p>
 */
public class TesteValidador {

    /** Uma chamada ao Validador que pode lançar ValidacaoException. */
    @FunctionalInterface
    private interface Conversao {
        Object executar() throws ValidacaoException;
    }

    private static int aprovados = 0;
    private static int reprovados = 0;

    /**
     * Ponto de entrada: roda todos os grupos de teste e mostra o placar.
     *
     * @param args não utilizado
     */
    public static void main(String[] args) {
        testarTexto();
        testarEmail();
        testarTelefone();
        testarInteiros();
        testarDinheiro();
        testarDatas();

        System.out.println();
        System.out.println("RESULTADO: " + aprovados + " aprovados, "
                + reprovados + " reprovados");
    }

    private static void testarTexto() {
        System.out.println("===== TEXTO =====");
        aceitar("nome normal", "Ana Souza",
                () -> Validador.textoObrigatorio("Ana Souza", 100, "nome", "o nome"));
        aceitar("tira espaços das pontas", "Ana Souza",
                () -> Validador.textoObrigatorio("   Ana Souza  ", 100, "nome", "o nome"));
        recusar("nome vazio", "nome",
                () -> Validador.textoObrigatorio("", 100, "nome", "o nome do cliente"));
        recusar("nome só com espaços", "nome",
                () -> Validador.textoObrigatorio("    ", 100, "nome", "o nome do cliente"));
        recusar("nome null", "nome",
                () -> Validador.textoObrigatorio(null, 100, "nome", "o nome do cliente"));
        recusar("nome com 101 caracteres", "nome",
                () -> Validador.textoObrigatorio("A".repeat(101), 100, "nome", "o nome do cliente"));
        aceitar("nome com exatamente 100 caracteres", "A".repeat(100),
                () -> Validador.textoObrigatorio("A".repeat(100), 100, "nome", "o nome"));
        aceitar("opcional vazio vira null", null,
                () -> Validador.textoOpcional("  ", 150, "endereco", "o endereço"));
        recusar("opcional longo demais", "endereco",
                () -> Validador.textoOpcional("R".repeat(151), 150, "endereco", "o endereço"));
    }

    private static void testarEmail() {
        System.out.println("===== E-MAIL =====");
        aceitar("e-mail válido", "ana@email.com",
                () -> Validador.email("ana@email.com", 100, "email"));
        aceitar("e-mail vazio é permitido", null,
                () -> Validador.email("", 100, "email"));
        recusar("sem @", "email", () -> Validador.email("ana.email.com", 100, "email"));
        recusar("sem ponto no domínio", "email", () -> Validador.email("ana@email", 100, "email"));
        recusar("com espaço", "email", () -> Validador.email("ana souza@email.com", 100, "email"));
        recusar("só o @", "email", () -> Validador.email("@", 100, "email"));
    }

    private static void testarTelefone() {
        System.out.println("===== TELEFONE =====");
        aceitar("celular com máscara", "19991234567",
                () -> Validador.telefone("(19) 99123-4567", "telefone"));
        aceitar("fixo só dígitos", "1932345678",
                () -> Validador.telefone("1932345678", "telefone"));
        aceitar("vazio é permitido", null, () -> Validador.telefone("", "telefone"));
        aceitar("máscara vazia é permitida", null,
                () -> Validador.telefone("(  )      -    ", "telefone"));
        recusar("sem DDD (8 dígitos)", "telefone",
                () -> Validador.telefone("91234567", "telefone"));
        recusar("dígitos demais", "telefone",
                () -> Validador.telefone("119912345678", "telefone"));
        recusar("com letras", "telefone",
                () -> Validador.telefone("19abc234567", "telefone"));
    }

    private static void testarInteiros() {
        System.out.println("===== INTEIROS =====");
        aceitar("estoque 0", 0, () -> Validador.inteiroNaoNegativo("0", "estoque", "o estoque"));
        aceitar("estoque 15 com espaços", 15,
                () -> Validador.inteiroNaoNegativo(" 15 ", "estoque", "o estoque"));
        recusar("estoque negativo", "estoque",
                () -> Validador.inteiroNaoNegativo("-1", "estoque", "o estoque"));
        recusar("estoque com letra", "estoque",
                () -> Validador.inteiroNaoNegativo("1O", "estoque", "o estoque"));
        recusar("estoque com vírgula", "estoque",
                () -> Validador.inteiroNaoNegativo("2,5", "estoque", "o estoque"));
        recusar("estoque vazio", "estoque",
                () -> Validador.inteiroNaoNegativo("", "estoque", "o estoque"));
        recusar("número que não cabe no int", "estoque",
                () -> Validador.inteiroNaoNegativo("99999999999", "estoque", "o estoque"));
        aceitar("quantidade 4", 4,
                () -> Validador.inteiroPositivo("4", "quantidade", "a quantidade"));
        recusar("quantidade zero", "quantidade",
                () -> Validador.inteiroPositivo("0", "quantidade", "a quantidade"));
        aceitar("aro 10 (limite de baixo)", 10,
                () -> Validador.inteiroEntre("10", 10, 24, "aro", "o aro"));
        aceitar("aro 24 (limite de cima)", 24,
                () -> Validador.inteiroEntre("24", 10, 24, "aro", "o aro"));
        recusar("aro 9", "aro", () -> Validador.inteiroEntre("9", 10, 24, "aro", "o aro"));
        recusar("aro 25", "aro", () -> Validador.inteiroEntre("25", 10, 24, "aro", "o aro"));
    }

    private static void testarDinheiro() {
        System.out.println("===== DINHEIRO =====");
        aceitar("preço com vírgula", new BigDecimal("589.90"),
                () -> Validador.valorPositivo("589,90", "preco", "o preço"));
        aceitar("preço com ponto", new BigDecimal("589.90"),
                () -> Validador.valorPositivo("589.90", "preco", "o preço"));
        aceitar("preço sem centavos", new BigDecimal("589.00"),
                () -> Validador.valorPositivo("589", "preco", "o preço"));
        aceitar("preço com uma casa", new BigDecimal("589.90"),
                () -> Validador.valorPositivo("589,9", "preco", "o preço"));
        aceitar("preço com milhar", new BigDecimal("1299.90"),
                () -> Validador.valorPositivo("1.299,90", "preco", "o preço"));
        aceitar("preço com R$", new BigDecimal("1299.90"),
                () -> Validador.valorPositivo("R$ 1.299,90", "preco", "o preço"));
        aceitar("preço formatado pelo Formatador", new BigDecimal("1299.90"),
                () -> Validador.valorPositivo(Formatador.moeda(new BigDecimal("1299.90")),
                        "preco", "o preço"));
        recusar("preço zero", "preco", () -> Validador.valorPositivo("0", "preco", "o preço"));
        recusar("preço negativo", "preco", () -> Validador.valorPositivo("-5", "preco", "o preço"));
        recusar("preço com letra", "preco", () -> Validador.valorPositivo("abc", "preco", "o preço"));
        recusar("preço com 3 casas", "preco",
                () -> Validador.valorPositivo("589,905", "preco", "o preço"));
        recusar("preço vazio", "preco", () -> Validador.valorPositivo("", "preco", "o preço"));
        recusar("preço maior que o banco aceita", "preco",
                () -> Validador.valorPositivo("100000000", "preco", "o preço"));
    }

    private static void testarDatas() {
        System.out.println("===== DATAS =====");
        aceitar("data válida", LocalDate.of(2026, 10, 5),
                () -> Validador.dataOpcional("05/10/2026", "dataInicio", "a data inicial"));
        aceitar("data vazia vira null", null,
                () -> Validador.dataOpcional("", "dataInicio", "a data inicial"));
        aceitar("máscara vazia vira null", null,
                () -> Validador.dataOpcional("  /  /    ", "dataInicio", "a data inicial"));
        recusar("31 de fevereiro", "dataInicio",
                () -> Validador.dataOpcional("31/02/2026", "dataInicio", "a data inicial"));
        recusar("data incompleta", "dataInicio",
                () -> Validador.dataOpcional("05/10/", "dataInicio", "a data inicial"));
        recusar("formato americano", "dataInicio",
                () -> Validador.dataOpcional("2026-10-05", "dataInicio", "a data inicial"));
    }

    // ------------------------------------------------------------------
    // Auxiliares de teste
    // ------------------------------------------------------------------

    /** A entrada precisa ser aceita e convertida exatamente para o esperado. */
    private static void aceitar(String descricao, Object esperado, Conversao conversao) {
        try {
            Object obtido = conversao.executar();
            if (Objects.equals(esperado, obtido)) {
                passou(descricao, "-> " + obtido);
            } else {
                falhou(descricao, "esperado " + esperado + ", veio " + obtido);
            }
        } catch (ValidacaoException e) {
            falhou(descricao, "foi recusado: " + e.getMessage());
        }
    }

    /** A entrada precisa ser recusada, apontando o campo certo. */
    private static void recusar(String descricao, String campoEsperado, Conversao conversao) {
        try {
            Object obtido = conversao.executar();
            falhou(descricao, "foi aceito (" + obtido + "), mas devia ser recusado");
        } catch (ValidacaoException e) {
            if (Objects.equals(campoEsperado, e.getCampo())) {
                passou(descricao, "-> \"" + e.getMessage() + "\"");
            } else {
                falhou(descricao, "campo errado: " + e.getCampo());
            }
        }
    }

    private static void passou(String descricao, String detalhe) {
        aprovados++;
        System.out.println("  OK     " + descricao + " " + detalhe);
    }

    private static void falhou(String descricao, String detalhe) {
        reprovados++;
        System.out.println("  FALHOU " + descricao + " | " + detalhe);
    }
}
