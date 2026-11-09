package br.unip.aps.lojapneus.dao.memoria;

import java.text.Collator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import br.unip.aps.lojapneus.dao.VendaDAO;
import br.unip.aps.lojapneus.exception.PersistenciaException;
import br.unip.aps.lojapneus.model.Pneu;
import br.unip.aps.lojapneus.model.Venda;
import br.unip.aps.lojapneus.model.VendaOrdem;

/**
 * VendaDAO em memória: registrar baixa o estoque e excluir devolve, com os mesmos erros do banco
 * (1452 cliente/pneu inexistente, 3819 CHECK, -1 estoque insuficiente).
 */
public class VendaDAOMemoria implements VendaDAO {

    private static final Collator PT = Collator.getInstance(Locale.forLanguageTag("pt-BR"));
    private final BancoMemoria banco;

    /** @param banco banco em memória compartilhado entre os DAOs */
    public VendaDAOMemoria(BancoMemoria banco) {
        this.banco = banco;
    }

    @Override
    public void registrar(Venda v) throws PersistenciaException {
        if (v.getCliente() == null || v.getPneu() == null) {
            throw new PersistenciaException("Venda sem cliente ou sem pneu.", 0, null);
        }
        if (banco.cliente(v.getCliente().getId()) == null || banco.pneu(v.getPneu().getId()) == null) {
            throw new PersistenciaException("Erro ao registrar venda.", 1452, null);
        }
        if (v.getQuantidade() <= 0 || v.getPrecoUnitario() == null || v.getPrecoUnitario().signum() <= 0) {
            throw new PersistenciaException("Erro ao registrar venda.", 3819, null);
        }
        Pneu p = banco.pneu(v.getPneu().getId());
        if (p.getEstoque() < v.getQuantidade()) {
            throw new PersistenciaException("Estoque insuficiente.",
                    PersistenciaException.ESTOQUE_INSUFICIENTE, null);
        }
        p.setEstoque(p.getEstoque() - v.getQuantidade());
        if (v.getDataVenda() == null) {
            v.setDataVenda(LocalDateTime.now());
        }
        v.setId(banco.proximaVenda++);
        banco.vendas.add(new BancoMemoria.Registro(v.getId(), v.getCliente().getId(),
                v.getPneu().getId(), v.getQuantidade(), v.getPrecoUnitario(), v.getDataVenda()));
    }

    @Override
    public void excluir(int id) throws PersistenciaException {
        BancoMemoria.Registro r = banco.vendas.stream().filter(x -> x.id() == id).findFirst().orElse(null);
        if (r == null) {
            throw new PersistenciaException("Venda não encontrada.", 0, null);
        }
        Pneu p = banco.pneu(r.idPneu());
        p.setEstoque(p.getEstoque() + r.quantidade());
        banco.vendas.remove(r);
    }

    @Override
    public Venda buscarPorId(int id) {
        return banco.vendas.stream().filter(x -> x.id() == id).findFirst().map(this::montar).orElse(null);
    }

    @Override
    public List<Venda> listar(String filtroCliente, LocalDate ini, LocalDate fim,
            VendaOrdem o1, VendaOrdem o2) {
        String f = filtroCliente == null ? "" : filtroCliente.trim().toLowerCase();
        List<Venda> r = new ArrayList<>();
        for (BancoMemoria.Registro x : banco.vendas) {
            Venda v = montar(x);
            LocalDate d = x.data().toLocalDate();
            if (v.getCliente().getNome().toLowerCase().contains(f)
                    && (ini == null || !d.isBefore(ini)) && (fim == null || !d.isAfter(fim))) {
                r.add(v);
            }
        }
        r.sort(comparador(o1 == null ? VendaOrdem.DATA_DESC : o1)
                .thenComparing(comparador(o2 == null ? VendaOrdem.DATA_DESC : o2)));
        return r;
    }

    private Venda montar(BancoMemoria.Registro x) {
        return new Venda(x.id(), BancoMemoria.copia(banco.cliente(x.idCliente())),
                BancoMemoria.copia(banco.pneu(x.idPneu())), x.quantidade(), x.precoUnitario(), x.data());
    }

    private static Comparator<Venda> comparador(VendaOrdem o) {
        Comparator<Venda> c = switch (o) {
            case DATA_ASC, DATA_DESC -> Comparator.comparing(Venda::getDataVenda);
            case TOTAL_ASC, TOTAL_DESC -> Comparator.comparing(Venda::getValorTotal);
            case CLIENTE_ASC, CLIENTE_DESC -> Comparator.comparing(v -> v.getCliente().getNome(), PT);
        };
        return o.name().endsWith("_DESC") ? c.reversed() : c;
    }

    /** Atualiza a venda e ajusta o estoque (mesma regra do DAO JDBC). */
    @Override
    public void atualizar(Venda v) throws PersistenciaException {
        int idx = -1;
        for (int i = 0; i < banco.vendas.size(); i++) {
            if (banco.vendas.get(i).id() == v.getId()) {
                idx = i;
                break;
            }
        }
        if (idx < 0) {
            throw new PersistenciaException("Venda não encontrada.", 0, null);
        }
        if (v.getCliente() == null || v.getPneu() == null) {
            throw new PersistenciaException("Venda sem cliente ou sem pneu.", 0, null);
        }
        if (banco.cliente(v.getCliente().getId()) == null || banco.pneu(v.getPneu().getId()) == null) {
            throw new PersistenciaException("Erro ao atualizar venda.", 1452, null);
        }
        if (v.getQuantidade() <= 0 || v.getPrecoUnitario() == null || v.getPrecoUnitario().signum() <= 0) {
            throw new PersistenciaException("Erro ao atualizar venda.", 3819, null);
        }
        BancoMemoria.Registro antigo = banco.vendas.get(idx);
        Pneu pneuAntigo = banco.pneu(antigo.idPneu());
        Pneu pneuNovo = banco.pneu(v.getPneu().getId());
        int disponivel = pneuNovo.getEstoque()
                + (antigo.idPneu() == pneuNovo.getId() ? antigo.quantidade() : 0);
        if (disponivel < v.getQuantidade()) {
            throw new PersistenciaException("Estoque insuficiente.",
                    PersistenciaException.ESTOQUE_INSUFICIENTE, null);
        }
        pneuAntigo.setEstoque(pneuAntigo.getEstoque() + antigo.quantidade());
        pneuNovo.setEstoque(pneuNovo.getEstoque() - v.getQuantidade());
        banco.vendas.set(idx, new BancoMemoria.Registro(v.getId(), v.getCliente().getId(),
                pneuNovo.getId(), v.getQuantidade(), v.getPrecoUnitario(), antigo.data()));
    }

}
