package br.unip.aps.lojapneus.view.componentes;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.swing.table.AbstractTableModel;

/**
 * Modelo de tabela genérico, somente leitura. Cada coluna é definida por um título,
 * um tipo (que decide o alinhamento) e uma função que extrai o valor do objeto.
 *
 * @param <T> tipo do objeto de cada linha (Cliente, Pneu, Venda...)
 */
public class ModeloTabela<T> extends AbstractTableModel {

    private record Coluna<E>(String titulo, Class<?> tipo, int largura, Function<E, Object> valor) { }

    private final List<Coluna<T>> colunas = new ArrayList<>();
    private List<T> dados = new ArrayList<>();
    private Function<T, Color> regraCor;

    /**
     * Define uma regra para pintar o fundo de linhas específicas (ex.: estoque baixo).
     *
     * @param regra recebe o item da linha e devolve a cor de fundo, ou null para o fundo normal
     * @return este modelo, para encadear
     */
    public ModeloTabela<T> colorirLinhas(Function<T, Color> regra) {
        this.regraCor = regra;
        return this;
    }

    /** @return a cor de fundo especial da linha, ou null se ela usa o fundo normal */
    public Color corDaLinha(int linha) {
        if (regraCor == null || linha < 0 || linha >= dados.size()) {
            return null;
        }
        return regraCor.apply(dados.get(linha));
    }

    /**
     * Adiciona uma coluna.
     *
     * @param titulo  título no cabeçalho
     * @param tipo    classe do valor (Integer, BigDecimal, String...)
     * @param largura largura preferida em pixels
     * @param valor   função que lê o valor da coluna a partir do objeto da linha
     * @return este modelo, para encadear
     */
    public ModeloTabela<T> coluna(String titulo, Class<?> tipo, int largura, Function<T, Object> valor) {
        colunas.add(new Coluna<>(titulo, tipo, largura, valor));
        return this;
    }

    /** Substitui os dados exibidos. */
    public void setDados(List<T> novos) {
        dados = new ArrayList<>(novos);
        fireTableDataChanged();
    }

    /** @return o objeto da linha informada */
    public T get(int linha) { return dados.get(linha); }

    /** @return largura preferida da coluna */
    public int largura(int coluna) { return colunas.get(coluna).largura(); }

    @Override public int getRowCount() { return dados.size(); }
    @Override public int getColumnCount() { return colunas.size(); }
    @Override public String getColumnName(int c) { return colunas.get(c).titulo(); }
    @Override public Class<?> getColumnClass(int c) { return colunas.get(c).tipo(); }
    @Override public boolean isCellEditable(int l, int c) { return false; }

    @Override
    public Object getValueAt(int linha, int coluna) {
        return colunas.get(coluna).valor().apply(dados.get(linha));
    }
}
