package br.unip.aps.lojapneus.dao.memoria;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import br.unip.aps.lojapneus.dao.PneuDAO;
import br.unip.aps.lojapneus.exception.PersistenciaException;
import br.unip.aps.lojapneus.model.Pneu;
import br.unip.aps.lojapneus.model.PneuOrdem;

/** PneuDAO em memória: mesmas regras e códigos de erro do MySQL (1048, 1406, 3819, 1451). */
public class PneuDAOMemoria implements PneuDAO {

    private static final Collator PT = Collator.getInstance(Locale.forLanguageTag("pt-BR"));
    private final BancoMemoria banco;

    /** @param banco banco em memória compartilhado entre os DAOs */
    public PneuDAOMemoria(BancoMemoria banco) {
        this.banco = banco;
    }

    @Override
    public void inserir(Pneu p) throws PersistenciaException {
        validar(p);
        p.setId(banco.proximoPneu++);
        banco.pneus.add(BancoMemoria.copia(p));
    }

    @Override
    public void atualizar(Pneu p) throws PersistenciaException {
        Pneu alvo = banco.pneu(p.getId());
        if (alvo == null) {
            throw new PersistenciaException("Pneu não encontrado.", 0, null);
        }
        validar(p);
        alvo.setMarca(p.getMarca());
        alvo.setModelo(p.getModelo());
        alvo.setAro(p.getAro());
        alvo.setEstoque(p.getEstoque());
        alvo.setPreco(p.getPreco());
    }

    @Override
    public void excluir(int id) throws PersistenciaException {
        if (banco.pneu(id) == null) {
            throw new PersistenciaException("Pneu não encontrado.", 0, null);
        }
        if (banco.vendas.stream().anyMatch(v -> v.idPneu() == id)) {
            throw new PersistenciaException("Erro ao excluir pneu.", 1451, null);
        }
        banco.pneus.removeIf(p -> p.getId() == id);
    }

    @Override
    public Pneu buscarPorId(int id) {
        Pneu p = banco.pneu(id);
        return p == null ? null : BancoMemoria.copia(p);
    }

    @Override
    public List<Pneu> listar(String filtro, PneuOrdem o1, PneuOrdem o2) {
        String f = filtro == null ? "" : filtro.trim().toLowerCase();
        List<Pneu> r = new ArrayList<>();
        for (Pneu p : banco.pneus) {
            if (p.getMarca().toLowerCase().contains(f) || p.getModelo().toLowerCase().contains(f)) {
                r.add(BancoMemoria.copia(p));
            }
        }
        r.sort(comparador(o1 == null ? PneuOrdem.MARCA_ASC : o1)
                .thenComparing(comparador(o2 == null ? PneuOrdem.MARCA_ASC : o2)));
        return r;
    }

    private void validar(Pneu p) throws PersistenciaException {
        if (p.getMarca() == null || p.getMarca().isBlank()
                || p.getModelo() == null || p.getModelo().isBlank()) {
            throw new PersistenciaException("Erro ao gravar pneu.", 1048, null);
        }
        if (p.getMarca().length() > 50 || p.getModelo().length() > 80) {
            throw new PersistenciaException("Erro ao gravar pneu.", 1406, null);
        }
        if (p.getAro() < 10 || p.getAro() > 24 || p.getEstoque() < 0
                || p.getPreco() == null || p.getPreco().signum() <= 0) {
            throw new PersistenciaException("Erro ao gravar pneu.", 3819, null);
        }
    }

    private static Comparator<Pneu> comparador(PneuOrdem o) {
        Comparator<Pneu> c = switch (o) {
            case MARCA_ASC, MARCA_DESC -> Comparator.comparing(Pneu::getMarca, PT);
            case MODELO_ASC, MODELO_DESC -> Comparator.comparing(Pneu::getModelo, PT);
            case ARO_ASC, ARO_DESC -> Comparator.comparingInt(Pneu::getAro);
            case PRECO_ASC, PRECO_DESC -> Comparator.comparing(Pneu::getPreco);
            case ESTOQUE_ASC, ESTOQUE_DESC -> Comparator.comparingInt(Pneu::getEstoque);
        };
        return o.name().endsWith("_DESC") ? c.reversed() : c;
    }
}
