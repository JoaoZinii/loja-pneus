package br.unip.aps.lojapneus.view.exportacao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import br.unip.aps.lojapneus.model.Cliente;
import br.unip.aps.lojapneus.model.Pneu;
import br.unip.aps.lojapneus.model.Venda;
import br.unip.aps.lojapneus.view.estilo.Formato;

/**
 * Gera o recibo de uma venda em HTML (abre no navegador; de lá é só imprimir ou "Salvar como PDF").
 * Não usa biblioteca externa e não mexe no banco: lê os dados da venda que já está na tela.
 */
public final class ReciboVenda {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private static final String MODELO = """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head>
            <meta charset="UTF-8">
            <title>Recibo da venda Nº @@NUMERO@@</title>
            <style>
              body { font-family: 'Segoe UI', Arial, sans-serif; color: #1F2933; background: #F4F5F7; margin: 0; padding: 32px; }
              .acoes { max-width: 720px; margin: 0 auto 14px; text-align: right; }
              .acoes button { background: #F2780C; color: #fff; border: 0; border-radius: 8px; padding: 10px 18px; font-size: 14px; font-weight: 600; cursor: pointer; }
              .folha { max-width: 720px; margin: 0 auto; background: #fff; border: 1px solid #D5D9E0; border-radius: 12px; padding: 36px 40px; }
              .topo { display: flex; align-items: center; gap: 16px; border-bottom: 3px solid #F2780C; padding-bottom: 18px; }
              .topo h1 { font-size: 22px; margin: 0; }
              .sub { color: #6B7785; font-size: 13px; }
              .num { margin-left: auto; text-align: right; color: #6B7785; font-size: 13px; }
              .num b { display: block; font-size: 22px; color: #D3650A; }
              h2 { font-size: 12px; letter-spacing: .08em; text-transform: uppercase; color: #6B7785; margin: 26px 0 8px; }
              .nome { font-size: 17px; font-weight: 600; margin-bottom: 4px; }
              .dado { font-size: 14px; color: #3E4C59; margin: 2px 0; }
              .dado span { color: #6B7785; display: inline-block; min-width: 76px; }
              table { width: 100%; border-collapse: collapse; }
              th { text-align: left; font-size: 12px; color: #6B7785; text-transform: uppercase; border-bottom: 1px solid #D5D9E0; padding: 8px 6px; }
              td { padding: 12px 6px; border-bottom: 1px solid #EEF0F3; font-size: 14px; }
              .d { text-align: right; }
              .total { display: flex; justify-content: flex-end; align-items: baseline; gap: 16px; margin-top: 20px; }
              .total b { font-size: 28px; color: #D3650A; }
              .rodape { margin-top: 36px; color: #6B7785; font-size: 12px; text-align: center; border-top: 1px dashed #D5D9E0; padding-top: 14px; }
              @media print {
                body { background: #fff; padding: 0; }
                .folha { border: 0; padding: 0; }
                .acoes { display: none; }
              }
            </style>
            </head>
            <body>
            <div class="acoes"><button onclick="window.print()">Imprimir / salvar em PDF</button></div>
            <div class="folha">
              <div class="topo">
                <svg width="54" height="54" viewBox="0 0 100 100" aria-hidden="true">
                  <circle cx="50" cy="50" r="48" fill="#0B0F14"/>
                  <circle cx="50" cy="50" r="37" fill="#161D24" stroke="#3E4C59" stroke-width="2"/>
                  <circle cx="50" cy="50" r="29" fill="#DDE1E6"/>
                  <circle cx="50" cy="50" r="21" fill="#1F2933"/>
                  <g stroke="#DDE1E6" stroke-width="7" stroke-linecap="round">
                    <line x1="50" y1="50" x2="50" y2="30"/>
                    <line x1="50" y1="50" x2="69" y2="43.8"/>
                    <line x1="50" y1="50" x2="61.8" y2="66.2"/>
                    <line x1="50" y1="50" x2="38.2" y2="66.2"/>
                    <line x1="50" y1="50" x2="31" y2="43.8"/>
                  </g>
                  <circle cx="50" cy="50" r="9" fill="#F2780C"/>
                </svg>
                <div>
                  <h1>Loja de Pneus</h1>
                  <div class="sub">Recibo de venda</div>
                </div>
                <div class="num">Nº<b>@@NUMERO@@</b></div>
              </div>

              <h2>Data da venda</h2>
              <div class="dado">@@DATA@@</div>

              <h2>Cliente</h2>
              @@CLIENTE@@

              <h2>Item</h2>
              <table>
                <tr><th>Pneu</th><th class="d">Qtd.</th><th class="d">Preço unit.</th><th class="d">Total</th></tr>
                <tr>
                  <td>@@PNEU@@</td>
                  <td class="d">@@QTD@@</td>
                  <td class="d">@@UNIT@@</td>
                  <td class="d">@@TOTAL@@</td>
                </tr>
              </table>
              <div class="total"><span>Total pago</span><b>@@TOTAL@@</b></div>

              <div class="rodape">
                Documento sem valor fiscal · emitido em @@EMISSAO@@<br>
                Obrigado pela preferência!
              </div>
            </div>
            </body>
            </html>
            """;

    private ReciboVenda() { }

    /**
     * Grava o recibo em um arquivo temporário (apagado quando o sistema fecha).
     *
     * @param venda venda já carregada, com cliente e pneu
     * @return o arquivo HTML criado
     * @throws IOException se não for possível gravar o arquivo
     */
    public static Path gerar(Venda venda) throws IOException {
        Path arquivo = Files.createTempFile("recibo_venda_" + venda.getId() + "_", ".html");
        arquivo.toFile().deleteOnExit();
        Files.writeString(arquivo, html(venda), StandardCharsets.UTF_8);
        return arquivo;
    }

    /** @return o HTML completo do recibo (público no pacote para facilitar testes) */
    static String html(Venda venda) {
        Cliente c = venda.getCliente();
        Pneu p = venda.getPneu();
        NumberFormat moeda = NumberFormat.getCurrencyInstance(PT_BR);

        StringBuilder cliente = new StringBuilder();
        cliente.append("<div class=\"nome\">").append(esc(c.getNome())).append("</div>");
        linhaDado(cliente, "Telefone", Formato.telefoneExibicao(c.getTelefone()));
        linhaDado(cliente, "E-mail", c.getEmail());
        linhaDado(cliente, "Endereço", c.getEndereco());

        String numero = String.format("%06d", venda.getId());
        String total = moeda.format(venda.getValorTotal());
        return MODELO
                .replace("@@NUMERO@@", numero)
                .replace("@@DATA@@", esc(venda.getDataVenda().format(DATA_HORA)))
                .replace("@@CLIENTE@@", cliente.toString())
                .replace("@@PNEU@@", esc(p.getMarca() + " " + p.getModelo() + " · aro " + p.getAro()))
                .replace("@@QTD@@", String.valueOf(venda.getQuantidade()))
                .replace("@@UNIT@@", esc(moeda.format(venda.getPrecoUnitario())))
                .replace("@@TOTAL@@", esc(total))
                .replace("@@EMISSAO@@", esc(LocalDateTime.now().format(DATA_HORA)));
    }

    private static void linhaDado(StringBuilder sb, String rotulo, String valor) {
        if (valor == null || valor.isBlank()) {
            return;
        }
        sb.append("<div class=\"dado\"><span>").append(rotulo).append("</span>")
          .append(esc(valor)).append("</div>");
    }

    /** Escapa o texto para HTML (nomes com &, < ou aspas não quebram o recibo). */
    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
