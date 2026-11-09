package br.unip.aps.lojapneus.dao.memoria;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import br.unip.aps.lojapneus.model.Cliente;
import br.unip.aps.lojapneus.model.Pneu;

/**
 * "Banco" na memória para demonstração e testes sem MySQL. Os DAOs desta pasta compartilham
 * uma instância e imitam as regras do banco real (UNIQUE, FK, CHECK) e os mesmos códigos de erro.
 * Os dados iniciais são os mesmos do script loja_pneus.sql.
 */
public final class BancoMemoria {

    /** Linha da tabela venda (guarda só os ids, como o banco). */
    record Registro(int id, int idCliente, int idPneu, int quantidade,
                    BigDecimal precoUnitario, LocalDateTime data) { }

    final List<Cliente> clientes = new ArrayList<>();
    final List<Pneu> pneus = new ArrayList<>();
    final List<Registro> vendas = new ArrayList<>();
    int proximoCliente = 1;
    int proximoPneu = 1;
    int proximaVenda = 1;

    /** Cria o banco já com 10 clientes, 10 pneus e 15 vendas. */
    public BancoMemoria() {
        String[][] cs = {
            {"Carlos Eduardo Souza", "carlos.souza@email.com", "19991234567", "Rua das Acacias, 120 - Campinas"},
            {"Mariana Alves Pereira", "mariana.alves@email.com", "19998765432", "Av. Brasil, 845 - Campinas"},
            {"Roberto Nogueira", "roberto.nog@email.com", "1932345678", "Rua Barao de Jaguara, 50 - Campinas"},
            {"Fernanda Costa Ribeiro", "fernanda.ribeiro@email.com", "19997654321", "Rua Sao Paulo, 300 - Valinhos"},
            {"Lucas Henrique Martins", "lucas.martins@email.com", "19996543210", "Rua das Palmeiras, 77 - Sumare"},
            {"Patricia Gomes", null, "1933456789", "Av. Norte-Sul, 1500 - Campinas"},
            {"Joao Pedro Almeida", "joao.almeida@email.com", "19995432109", "Rua XV de Novembro, 410 - Indaiatuba"},
            {"Camila Rocha", "camila.rocha@email.com", null, "Rua Doze, 23 - Paulinia"},
            {"Rafael Teixeira", null, "19994321098", "Av. Andrade Neves, 900 - Campinas"},
            {"Beatriz Fernandes", "bia.fernandes@email.com", "19993210987", "Rua Ipe, 15 - Vinhedo"},
        };
        for (String[] c : cs) {
            clientes.add(new Cliente(proximoCliente++, c[0], c[1], c[2], c[3]));
        }
        Object[][] ps = {
            {"Pirelli", "Cinturato P1", 15, "289.90", 24}, {"Pirelli", "Scorpion Verde", 17, "549.90", 12},
            {"Pirelli", "P400 Evo", 14, "259.90", 30}, {"Michelin", "Primacy 4", 17, "689.00", 8},
            {"Michelin", "Energy XM2", 15, "412.50", 15}, {"Goodyear", "Assurance", 16, "379.90", 20},
            {"Goodyear", "EfficientGrip", 17, "599.90", 10}, {"Bridgestone", "Turanza", 16, "465.00", 14},
            {"Bridgestone", "Dueler", 18, "819.90", 6}, {"Continental", "PowerContact 2", 15, "349.90", 18},
        };
        for (Object[] p : ps) {
            pneus.add(new Pneu(proximoPneu++, (String) p[0], (String) p[1], (Integer) p[2],
                    (Integer) p[4], new BigDecimal((String) p[3])));
        }
        Object[][] vs = {
            {1, 1, 4, "289.90", "2026-08-05T10:15"}, {2, 4, 2, "679.00", "2026-08-12T14:30"},
            {3, 3, 4, "259.90", "2026-08-19T09:40"}, {1, 5, 2, "412.50", "2026-08-27T16:05"},
            {4, 2, 4, "529.90", "2026-09-02T11:20"}, {5, 6, 4, "379.90", "2026-09-09T15:45"},
            {6, 1, 2, "279.90", "2026-09-14T10:00"}, {2, 7, 2, "599.90", "2026-09-18T13:10"},
            {7, 8, 4, "465.00", "2026-09-23T17:30"}, {8, 9, 1, "799.90", "2026-09-29T09:25"},
            {3, 10, 4, "349.90", "2026-10-01T14:50"}, {9, 4, 2, "689.00", "2026-10-05T12:15"},
            {10, 2, 2, "549.90", "2026-10-08T16:40"}, {4, 6, 2, "379.90", "2026-10-12T10:35"},
            {5, 3, 2, "259.90", "2026-10-14T15:20"},
        };
        for (Object[] v : vs) {
            vendas.add(new Registro(proximaVenda++, (Integer) v[0], (Integer) v[1], (Integer) v[2],
                    new BigDecimal((String) v[3]), LocalDateTime.parse((String) v[4])));
        }
    }

    static Cliente copia(Cliente c) {
        return new Cliente(c.getId(), c.getNome(), c.getEmail(), c.getTelefone(), c.getEndereco());
    }

    static Pneu copia(Pneu p) {
        return new Pneu(p.getId(), p.getMarca(), p.getModelo(), p.getAro(), p.getEstoque(), p.getPreco());
    }

    Cliente cliente(int id) {
        return clientes.stream().filter(c -> c.getId() == id).findFirst().orElse(null);
    }

    Pneu pneu(int id) {
        return pneus.stream().filter(p -> p.getId() == id).findFirst().orElse(null);
    }
}
