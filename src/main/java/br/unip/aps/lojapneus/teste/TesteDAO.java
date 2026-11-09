package br.unip.aps.lojapneus.teste;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import br.unip.aps.lojapneus.dao.ClienteDAO;
import br.unip.aps.lojapneus.dao.ClienteDAOJdbc;
import br.unip.aps.lojapneus.dao.PneuDAO;
import br.unip.aps.lojapneus.dao.PneuDAOJdbc;
import br.unip.aps.lojapneus.dao.VendaDAO;
import br.unip.aps.lojapneus.dao.VendaDAOJdbc;
import br.unip.aps.lojapneus.exception.PersistenciaException;
import br.unip.aps.lojapneus.model.Cliente;
import br.unip.aps.lojapneus.model.ClienteOrdem;
import br.unip.aps.lojapneus.model.Pneu;
import br.unip.aps.lojapneus.model.PneuOrdem;
import br.unip.aps.lojapneus.model.Venda;
import br.unip.aps.lojapneus.model.VendaOrdem;

/**
 * Testa todos os DAOs sem interface gráfica.
 * Cada teste deixa o banco como estava antes.
 *
 * <p>Como funciona: o programa executa três blocos de teste, um para cada
 * DAO (cliente, pneu e venda). Cada passo imprime na tela o que aconteceu e
 * o resultado esperado. Nos testes de erro, o passo só passa se o banco
 * <b>recusar</b> a operação com o código de erro previsto.</p>
 *
 * <p>Pré-requisitos: o banco criado com o script loja_pneus.sql (que traz
 * os dados de exemplo usados aqui, como o cliente 1, o cliente 2 e o pneu 4)
 * e o arquivo db.properties preenchido na raiz do projeto.</p>
 */
public class TesteDAO {

    /** DAO de clientes. O tipo é a interface; a implementação é a Jdbc. */
    private static final ClienteDAO clienteDao = new ClienteDAOJdbc();

    /** DAO de pneus. */
    private static final PneuDAO pneuDao = new PneuDAOJdbc();

    /** DAO de vendas. */
    private static final VendaDAO vendaDao = new VendaDAOJdbc();

    /**
     * Ponto de entrada: roda os três blocos de teste em sequência.
     * Qualquer erro que não era esperado em um teste chega até aqui e é
     * mostrado com o código de erro do MySQL.
     *
     * @param args não utilizado
     */
    public static void main(String[] args) {
        try {
            testarCliente();
            testarPneu();
            testarVenda();
        } catch (PersistenciaException e) {
            System.out.println("Erro inesperado " + e.getCodigoErro()
                    + ": " + e.getMessage());
        }
    }

    /**
     * Testa o ClienteDAO: inserir, atualizar, excluir, buscar e listar,
     * e confere os dois erros esperados (e-mail repetido e exclusão de
     * cliente que já tem venda).
     *
     * <p>O cliente criado aqui é apagado no passo 3, então o banco volta ao
     * que era antes do teste.</p>
     *
     * @throws PersistenciaException se acontecer algum erro não previsto
     */
    private static void testarCliente() throws PersistenciaException {
        System.out.println("===== CLIENTE =====");

        // Passo 1: inserir. O e-mail usa a hora atual para ser sempre
        // diferente, senão o UNIQUE barraria o teste na segunda execução.
        String email = "teste" + System.currentTimeMillis() + "@email.com";
        Cliente c = new Cliente("Ana Teste", email, "19999990000", "Rua Teste, 1");
        clienteDao.inserir(c);
        // O DAO preenche o id gerado pelo banco (getGeneratedKeys) no objeto.
        System.out.println("1) Inserido com id: " + c.getId());

        // Teste de erro: o mesmo e-mail de novo. O banco deve recusar
        // (UNIQUE), e o DAO repassa o código 1062 na exceção.
        try {
            clienteDao.inserir(new Cliente("Outra Pessoa", email, null, null));
            System.out.println("   ERRO: aceitou e-mail repetido!");
        } catch (PersistenciaException e) {
            System.out.println("   E-mail repetido barrado, código "
                    + e.getCodigoErro() + " (esperado 1062)");
        }

        // Passo 2: atualizar. Lê o cliente do banco, muda o nome, grava e
        // lê de novo para provar que a mudança foi salva.
        Cliente lido = clienteDao.buscarPorId(c.getId());
        lido.setNome("Ana Teste Editada");
        clienteDao.atualizar(lido);
        System.out.println("2) Atualizado: "
                + clienteDao.buscarPorId(c.getId()).getNome());

        // Passo 3: excluir. Depois de excluído, o buscarPorId deve devolver
        // null (é o combinado do contrato do DAO para "não encontrado").
        clienteDao.excluir(c.getId());
        System.out.println("3) Excluído. buscarPorId devolve: "
                + clienteDao.buscarPorId(c.getId()));

        // Teste de erro: o cliente 1 já tem venda nos dados de exemplo.
        // A chave estrangeira (ON DELETE RESTRICT) deve impedir a exclusão,
        // com o código 1451.
        try {
            clienteDao.excluir(1);
            System.out.println("4) ERRO: excluiu cliente que tem vendas!");
        } catch (PersistenciaException e) {
            System.out.println("4) Exclusão bloqueada pela FK, código "
                    + e.getCodigoErro() + " (esperado 1451)");
        }

        // Passo 5: listar. Filtra pelo nome ("Souza") e ordena por dois
        // critérios: primeiro nome A-Z e, em caso de empate, e-mail A-Z.
        System.out.println("5) Listagem: filtro 'Souza', nome A-Z");
        for (Cliente x : clienteDao.listar("Souza", ClienteOrdem.NOME_ASC,
                                           ClienteOrdem.EMAIL_ASC)) {
            System.out.println("   " + x.getId() + " - " + x.getNome());
        }
    }

    /**
     * Testa o PneuDAO: inserir, atualizar (preço e estoque), excluir, listar
     * e confere os dois erros esperados (exclusão de pneu que já tem venda
     * e aro fora do intervalo permitido).
     *
     * <p>O pneu criado aqui é apagado no passo 3, então o banco volta ao
     * que era antes do teste.</p>
     *
     * @throws PersistenciaException se acontecer algum erro não previsto
     */
    private static void testarPneu() throws PersistenciaException {
        System.out.println("===== PNEU =====");

        // Passo 1: inserir. O preço é BigDecimal (valor exato para dinheiro),
        // criado a partir de um texto para não ter erro de arredondamento.
        Pneu p = new Pneu("TesteMarca", "Modelo X", 16, 5, new BigDecimal("399.90"));
        pneuDao.inserir(p);
        System.out.println("1) Inserido com id: " + p.getId());

        // Passo 2: atualizar. Muda o preço e o estoque e lê de novo do banco
        // para confirmar que os dois valores foram salvos.
        Pneu lido = pneuDao.buscarPorId(p.getId());
        lido.setPreco(new BigDecimal("429.90"));
        lido.setEstoque(8);
        pneuDao.atualizar(lido);
        Pneu depois = pneuDao.buscarPorId(p.getId());
        System.out.println("2) Atualizado: R$ " + depois.getPreco()
                + " | estoque " + depois.getEstoque());

        // Passo 3: excluir. O buscarPorId deve devolver null depois.
        pneuDao.excluir(p.getId());
        System.out.println("3) Excluído. buscarPorId devolve: "
                + pneuDao.buscarPorId(p.getId()));

        // Teste de erro: o pneu 1 já tem venda nos dados de exemplo, então
        // a chave estrangeira deve impedir a exclusão (código 1451).
        try {
            pneuDao.excluir(1);
            System.out.println("4) ERRO: excluiu pneu que tem vendas!");
        } catch (PersistenciaException e) {
            System.out.println("4) Exclusão bloqueada pela FK, código "
                    + e.getCodigoErro() + " (esperado 1451)");
        }

        // Teste de erro: aro 99 viola o CHECK do banco (aro entre 10 e 24).
        // O banco recusa com o código 3819.
        try {
            pneuDao.inserir(new Pneu("TesteMarca", "Modelo Y", 99, 5,
                                     new BigDecimal("399.90")));
            System.out.println("5) ERRO: aceitou aro 99!");
        } catch (PersistenciaException e) {
            System.out.println("5) Aro 99 recusado, código "
                    + e.getCodigoErro() + " (esperado 3819)");
        }

        // Passo 6: listar. Filtro nulo = sem filtro (traz todos). Ordena por
        // marca A-Z e, dentro da mesma marca, do preço maior para o menor.
        System.out.println("6) Listagem: marca A-Z, depois preço do maior para o menor");
        for (Pneu x : pneuDao.listar(null, PneuOrdem.MARCA_ASC,
                                     PneuOrdem.PRECO_DESC)) {
            System.out.println("   " + x.getId() + " | " + x
                    + " | R$ " + x.getPreco());
        }
    }

    /**
     * Testa o VendaDAO: registrar (com baixa de estoque), excluir (com
     * devolução do estoque), listar com filtro de período, e confere os dois
     * erros esperados (estoque insuficiente e cliente inexistente).
     *
     * <p>Usa o cliente 2 e o pneu 4 dos dados de exemplo (o pneu 4 começa
     * com estoque 8). A venda criada é excluída no passo 3, que devolve o
     * estoque, então o banco volta ao que era antes do teste.</p>
     *
     * @throws PersistenciaException se acontecer algum erro não previsto
     */
    private static void testarVenda() throws PersistenciaException {
        System.out.println("===== VENDA =====");

        // Passo 0: pega um cliente e um pneu que já existem e mostra o
        // estoque inicial, para comparar depois.
        Cliente cliente = clienteDao.buscarPorId(2);
        Pneu pneu = pneuDao.buscarPorId(4);
        System.out.println("0) Estoque inicial do pneu 4: " + pneu.getEstoque());

        // Passo 1: registrar a venda de 3 unidades. O preço unitário é o
        // preço atual do pneu, copiado para a venda. O DAO faz isso em uma
        // transação: grava a venda e baixa o estoque, ou nenhum dos dois.
        Venda venda = new Venda(cliente, pneu, 3, pneu.getPreco(),
                                LocalDateTime.now());
        vendaDao.registrar(venda);
        System.out.println("1) Venda " + venda.getId() + " registrada, total R$ "
                + venda.getValorTotal() + " | estoque agora: "
                + pneuDao.buscarPorId(4).getEstoque());

        // Teste de erro: vender 999 unidades, mais do que há em estoque.
        // O DAO lança a exceção com o código próprio -1 e desfaz tudo
        // (rollback): a venda não fica gravada e o estoque não muda.
        try {
            vendaDao.registrar(new Venda(cliente, pneu, 999, pneu.getPreco(),
                                         LocalDateTime.now()));
            System.out.println("2) ERRO: aceitou venda sem estoque!");
        } catch (PersistenciaException e) {
            System.out.println("2) Venda barrada, código " + e.getCodigoErro()
                    + " (esperado -1) | estoque continua: "
                    + pneuDao.buscarPorId(4).getEstoque());
        }

        // Passo 3: excluir a venda do passo 1. O DAO devolve a quantidade
        // vendida ao estoque (também em transação).
        vendaDao.excluir(venda.getId());
        System.out.println("3) Venda excluída. Estoque devolvido: "
                + pneuDao.buscarPorId(4).getEstoque());

        // Teste de erro: venda para um cliente que não existe (id 999).
        // A chave estrangeira recusa com o código 1452.
        try {
            Cliente fantasma = new Cliente(999, "Fantasma", null, null, null);
            vendaDao.registrar(new Venda(fantasma, pneu, 1, pneu.getPreco(),
                                         LocalDateTime.now()));
            System.out.println("4) ERRO: aceitou cliente inexistente!");
        } catch (PersistenciaException e) {
            System.out.println("4) Cliente inexistente barrado, código "
                    + e.getCodigoErro() + " (esperado 1452)");
        }

        // Passo 5: listar as vendas de setembro de 2026. Filtro de cliente
        // nulo = todos. Ordena por nome do cliente A-Z e, em caso de empate,
        // pela data mais recente primeiro.
        System.out.println("5) Listagem: setembro de 2026, cliente A-Z, depois data mais recente");
        for (Venda v : vendaDao.listar(null, LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                VendaOrdem.CLIENTE_ASC, VendaOrdem.DATA_DESC)) {
            System.out.println("   " + v.getId() + " | "
                    + v.getCliente().getNome() + " | " + v.getPneu()
                    + " | R$ " + v.getValorTotal());
        }
    }
}