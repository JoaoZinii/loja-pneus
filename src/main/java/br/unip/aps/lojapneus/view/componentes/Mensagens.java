package br.unip.aps.lojapneus.view.componentes;

import java.awt.Component;
import java.util.Set;

import br.unip.aps.lojapneus.exception.PersistenciaException;
import br.unip.aps.lojapneus.exception.ValidacaoException;

/**
 * Caixas de mensagem padronizadas. A view nunca mostra erro técnico:
 * só mensagens que o controller já traduziu, ou um texto genérico.
 */
public final class Mensagens {

    /** Códigos do MySQL que o controller traduz para mensagens amigáveis. */
    private static final Set<Integer> CODIGOS_TRADUZIDOS =
            Set.of(1062, 1451, 1452, 3819, 1048, 1406,
                    PersistenciaException.ESTOQUE_INSUFICIENTE);

    private Mensagens() { }

    /**
     * Mostra a mensagem certa para cada tipo de exceção.
     * Validação e regra de banco (ex.: FK) viram aviso; o resto vira erro genérico,
     * com o detalhe técnico só no console.
     */
    public static void erro(Component pai, Throwable ex) {
        if (ex instanceof ValidacaoException) {
            aviso(pai, "Verifique os dados", ex.getMessage());
        } else if (ex instanceof PersistenciaException pe
                && CODIGOS_TRADUZIDOS.contains(pe.getCodigoErro())) {
            aviso(pai, "Não foi possível concluir", ex.getMessage());
        } else if (ex instanceof PersistenciaException && ex.getMessage() != null) {
            // mensagem já traduzida pelo controller (ex.: banco fora do ar)
            DialogoMensagem.mostrar(pai, DialogoMensagem.Tipo.ERRO, "Algo deu errado",
                    escapar(ex.getMessage()), new String[] {"OK"}, 0, -1);
        } else {
            ex.printStackTrace();
            DialogoMensagem.mostrar(pai, DialogoMensagem.Tipo.ERRO, "Algo deu errado",
                    "Ocorreu um erro inesperado.<br>Tente novamente ou procure o suporte.",
                    new String[] {"OK"}, 0, -1);
        }
    }

    /** Mensagem de aviso (amarelo). */
    public static void aviso(Component pai, String titulo, String texto) {
        DialogoMensagem.mostrar(pai, DialogoMensagem.Tipo.AVISO, titulo, escapar(texto),
                new String[] {"OK"}, 0, -1);
    }

    /** Mensagem informativa. */
    public static void info(Component pai, String titulo, String texto) {
        DialogoMensagem.mostrar(pai, DialogoMensagem.Tipo.INFO, titulo, escapar(texto),
                new String[] {"OK"}, 0, -1);
    }

    /**
     * Pede confirmação antes de excluir, mostrando o nome do registro.
     *
     * @param itemComArtigo ex.: "o cliente", "a venda"
     * @param descricao     nome do registro
     * @return true se o usuário confirmou (o botão padrão é Cancelar)
     */
    public static boolean confirmarExclusao(Component pai, String itemComArtigo, String descricao) {
        return confirmarExclusao(pai, itemComArtigo, descricao, null);
    }

    /** Igual ao anterior, com uma linha extra de aviso (ex.: "O estoque será devolvido"). */
    public static boolean confirmarExclusao(Component pai, String itemComArtigo, String descricao,
            String avisoExtra) {
        int r = DialogoMensagem.mostrar(pai, DialogoMensagem.Tipo.EXCLUSAO, "Confirmar exclusão",
                "Deseja excluir " + itemComArtigo + " <b>" + escapar(descricao)
                        + "</b>?<br>Esta ação não pode ser desfeita."
                        + (avisoExtra == null ? "" : "<br><br>" + escapar(avisoExtra)),
                new String[] {"Cancelar", "Excluir"}, 0, 1);
        return r == 1;
    }

    private static String escapar(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\n", "<br>");
    }
}
