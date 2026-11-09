# Loja de Pneus (APS UNIP 2026/2)

Sistema desktop em Java (Swing) com MySQL, usando MVC + DAO. Tabelas: `cliente`, `pneu` e `venda` (associativa).

## Requisitos
- JDK 17 ou 21 instalado na máquina (não precisa estar dentro do projeto)
- Maven (já vem no Extension Pack for Java do VS Code)
- MySQL 8.0.16 ou mais novo (só para rodar com banco)

## Como rodar
1. **Sem banco (demonstração):** `mvn compile exec:java -Dexec.args="--memoria"`
   (ou F5 no VS Code, opção "2) Sistema (modo demonstracao, sem banco)").
2. **Com banco:**
   1. Execute `database/loja_pneus.sql` no MySQL.
      > ⚠️ O script faz `DROP DATABASE IF EXISTS loja_pneus`: ele **apaga e recria** o banco. Não rode em banco com dados importantes.
   2. Copie `db.properties.example` para `db.properties` e preencha usuário e senha (esse arquivo não vai para o Git).
   3. `mvn compile exec:java` (ou F5, opção "1) Sistema (com banco MySQL)").

## Testes
Pelo VS Code (F5, opções 3, 4 e 5) ou executando as classes do pacote `teste`:
`TesteValidador` e `TesteVendas` (sem banco), `TesteController` e `TesteDAO` (precisam do banco).

## Estrutura de pacotes (`br.unip.aps.lojapneus`)
| Pacote | Conteúdo |
|---|---|
| `model` | Cliente, Pneu, Venda |
| `dao` | interfaces e implementações JDBC |
| `dao.memoria` | implementações em memória (modo demonstração) |
| `controller` | ClienteController, PneuController, VendaController |
| `view` | janela principal, painéis e formulários |
| `view.componentes` | campos, botões, cartões, gráfico, tabelas e mensagens |
| `view.estilo` | tema, ícones, logo e formatação |
| `view.exportacao` | exportação CSV e recibo de venda |
| `util` | ConnectionFactory, Validador, tradução de erros |
| `exception` | exceções do sistema |
| `teste` | testes por `main` (Validador, Controller, DAO) |

A pasta `docs/` guarda a documentação de apoio (`COMO_RODAR_E_TESTAR.md`, `CONTROLLER_E_VALIDACAO.md`, `INTERFACE.md`, diagrama e plano de testes); ela não influencia o funcionamento do sistema.

## Trabalhando com Git
- **Nunca** versionar o `db.properties` (tem a sua senha). Ele já está no `.gitignore`; só o `db.properties.example` vai para o repositório.
- Também ficam de fora: `target/`, `out/`, arquivos de IDE e `.vscode/settings.json` (só `launch.json` e `extensions.json` são compartilhados).
- Antes de cada commit, confira com `git status` se não há nada que não deveria ir.
- Se uma senha for commitada sem querer, **trocar a senha do MySQL** (apagar o arquivo num commit novo não remove do histórico).

Fluxo sugerido:
```bash
git pull                               # pegar as últimas alterações do repositório
git checkout -b feature/nome-da-tarefa # trabalhar em uma branch
git add .
git commit -m "feat: descricao curta do que mudou"
git push -u origin feature/nome-da-tarefa
```
