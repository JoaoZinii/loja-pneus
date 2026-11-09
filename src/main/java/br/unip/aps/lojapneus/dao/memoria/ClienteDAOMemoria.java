package br.unip.aps.lojapneus.dao.memoria;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import br.unip.aps.lojapneus.dao.ClienteDAO;
import br.unip.aps.lojapneus.exception.PersistenciaException;
import br.unip.aps.lojapneus.model.Cliente;
import br.unip.aps.lojapneus.model.ClienteOrdem;

/** ClienteDAO em memória: mesmas regras e códigos de erro do MySQL (1048, 1406, 1062, 1451). */
public class ClienteDAOMemoria implements ClienteDAO {

    private static final Collator PT = Collator.getInstance(Locale.forLanguageTag("pt-BR"));
    private final BancoMemoria banco;

    /** @param banco banco em memória compartilhado entre os DAOs */
    public ClienteDAOMemoria(BancoMemoria banco) {
        this.banco = banco;
    }

    @Override
    public void inserir(Cliente c) throws PersistenciaException {
        validar(c, -1);
        c.setId(banco.proximoCliente++);
        banco.clientes.add(BancoMemoria.copia(c));
    }

    @Override
    public void atualizar(Cliente c) throws PersistenciaException {
        Cliente alvo = banco.cliente(c.getId());
        if (alvo == null) {
            throw new PersistenciaException("Cliente não encontrado.", 0, null);
        }
        validar(c, c.getId());
        alvo.setNome(c.getNome());
        alvo.setEmail(vazioParaNulo(c.getEmail()));
        alvo.setTelefone(vazioParaNulo(c.getTelefone()));
        alvo.setEndereco(vazioParaNulo(c.getEndereco()));
    }

    @Override
    public void excluir(int id) throws PersistenciaException {
        if (banco.cliente(id) == null) {
            throw new PersistenciaException("Cliente não encontrado.", 0, null);
        }
        if (banco.vendas.stream().anyMatch(v -> v.idCliente() == id)) {
            throw new PersistenciaException("Erro ao excluir cliente.", 1451, null);
        }
        banco.clientes.removeIf(c -> c.getId() == id);
    }

    @Override
    public Cliente buscarPorId(int id) {
        Cliente c = banco.cliente(id);
        return c == null ? null : BancoMemoria.copia(c);
    }

    @Override
    public List<Cliente> listar(String filtroNome, ClienteOrdem o1, ClienteOrdem o2) {
        String f = filtroNome == null ? "" : filtroNome.trim().toLowerCase();
        List<Cliente> r = new ArrayList<>();
        for (Cliente c : banco.clientes) {
            if (c.getNome().toLowerCase().contains(f)) {
                r.add(BancoMemoria.copia(c));
            }
        }
        r.sort(comparador(o1 == null ? ClienteOrdem.NOME_ASC : o1)
                .thenComparing(comparador(o2 == null ? ClienteOrdem.NOME_ASC : o2)));
        return r;
    }

    private void validar(Cliente c, int idAtual) throws PersistenciaException {
        if (c.getNome() == null || c.getNome().isBlank()) {
            throw new PersistenciaException("Erro ao gravar cliente.", 1048, null);
        }
        if (c.getNome().length() > 100 || tamanho(c.getEmail()) > 100
                || tamanho(c.getTelefone()) > 11 || tamanho(c.getEndereco()) > 150) {
            throw new PersistenciaException("Erro ao gravar cliente.", 1406, null);
        }
        String email = vazioParaNulo(c.getEmail());
        if (email != null && banco.clientes.stream()
                .anyMatch(x -> x.getId() != idAtual && email.equalsIgnoreCase(x.getEmail()))) {
            throw new PersistenciaException("Erro ao gravar cliente.", 1062, null);
        }
        c.setEmail(email);
        c.setTelefone(vazioParaNulo(c.getTelefone()));
        c.setEndereco(vazioParaNulo(c.getEndereco()));
    }

    private static int tamanho(String s) {
        return s == null ? 0 : s.trim().length();
    }

    private static String vazioParaNulo(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }

    private static Comparator<Cliente> comparador(ClienteOrdem o) {
        Comparator<String> txt = Comparator.nullsFirst(PT);
        Comparator<Cliente> c = switch (o) {
            case NOME_ASC, NOME_DESC -> Comparator.comparing(Cliente::getNome, txt);
            case EMAIL_ASC, EMAIL_DESC -> Comparator.comparing(Cliente::getEmail, txt);
        };
        return o.name().endsWith("_DESC") ? c.reversed() : c;
    }
}
