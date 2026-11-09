package br.unip.aps.lojapneus.view;

import java.util.List;
import javax.swing.SwingUtilities;

import br.unip.aps.lojapneus.controller.ClienteController;
import br.unip.aps.lojapneus.model.Cliente;
import br.unip.aps.lojapneus.model.ClienteOrdem;
import br.unip.aps.lojapneus.view.componentes.CartaoIndicador;
import br.unip.aps.lojapneus.view.componentes.ModeloTabela;
import br.unip.aps.lojapneus.view.componentes.RenderizadorCelula;
import br.unip.aps.lojapneus.view.estilo.Formato;
import br.unip.aps.lojapneus.view.estilo.Icone;
import br.unip.aps.lojapneus.view.estilo.Tema;

/** Aba Clientes: listagem, busca por nome, ordenação, cadastro, edição e exclusão. */
public class ClientePanel extends PainelListagem<Cliente, ClienteOrdem> {

    private final ClienteController controller;
    private final CartaoIndicador indTotal;
    private final CartaoIndicador indEmail;
    private final CartaoIndicador indTelefone;

    /** @param controller controller de clientes */
    public ClientePanel(ClienteController controller) {
        super("cliente", "clientes", false, "Buscar por nome do cliente...",
                criarModelo(), opcoes(), 0, 2);
        this.controller = controller;
        indTotal = adicionarIndicador("Clientes listados", Tema.GRAFITE_CLARO, Icone.Tipo.USUARIO);
        indEmail = adicionarIndicador("Com e-mail", Tema.LARANJA, Icone.Tipo.EMAIL);
        indTelefone = adicionarIndicador("Com telefone", Tema.VERDE, Icone.Tipo.CELULAR);
        tabela.getColumnModel().getColumn(0).setCellRenderer(new RenderizadorCelula(false, true));
        tabela.getColumnModel().getColumn(1).setCellRenderer(new RenderizadorCelula.Avatar());
        iniciar();
    }

    private static ModeloTabela<Cliente> criarModelo() {
        return new ModeloTabela<Cliente>()
                .coluna("ID", Integer.class, 60, Cliente::getId)
                .coluna("Nome", String.class, 230, Cliente::getNome)
                .coluna("E-mail", String.class, 210, Cliente::getEmail)
                .coluna("Telefone", String.class, 135, c -> Formato.telefoneExibicao(c.getTelefone()))
                .coluna("Endereço", String.class, 270, Cliente::getEndereco);
    }

    private static List<Opcao<ClienteOrdem>> opcoes() {
        return List.of(
                new Opcao<>("Nome: A a Z", ClienteOrdem.NOME_ASC),
                new Opcao<>("Nome: Z a A", ClienteOrdem.NOME_DESC),
                new Opcao<>("E-mail: A a Z", ClienteOrdem.EMAIL_ASC),
                new Opcao<>("E-mail: Z a A", ClienteOrdem.EMAIL_DESC));
    }

    @Override
    protected List<Cliente> consultar(String filtro, ClienteOrdem o1, ClienteOrdem o2)
            throws Exception {
        return controller.listar(filtro, o1, o2);
    }

    @Override
    protected void aoCarregar(List<Cliente> lista) {
        indTotal.setValor(String.valueOf(lista.size()));
        indEmail.setValor(String.valueOf(lista.stream()
                .filter(c -> c.getEmail() != null && !c.getEmail().isBlank()).count()));
        indTelefone.setValor(String.valueOf(lista.stream()
                .filter(c -> c.getTelefone() != null && !c.getTelefone().isBlank()).count()));
    }

    @Override
    protected Object chave(Cliente item) {
        return item.getId();
    }

    @Override
    protected boolean abrirFormulario(Cliente itemOuNull) {
        return new ClienteDialog(SwingUtilities.getWindowAncestor(this), controller, itemOuNull)
                .exibir();
    }

    @Override
    protected void remover(Cliente item) throws Exception {
        controller.excluir(item.getId());
    }

    @Override
    protected String descricao(Cliente item) {
        return item.getNome();
    }
}
