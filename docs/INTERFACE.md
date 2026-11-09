# Loja de Pneus: interface e integração

Guia de execução no VS Code: **COMO_RODAR_E_TESTAR.md**

## Como o sistema se liga (MVC + DAO)
```
view (Swing)  →  controller (valida, converte, traduz erros)  →  DAO (interface)  →  JDBC/MySQL  ou  memória
```
- `Main` é o único lugar que escolhe a implementação do DAO. A **view só conhece controllers** (nenhum
  import de `dao` em `view/`).
- Banco: `db.properties` com `db.url`, `db.user`, `db.password` (copie de `db.properties.example`). Sem banco, ou com `--memoria`, abre em **modo
  demonstração** (selo laranja no cabeçalho), com os mesmos dados do `loja_pneus.sql` na memória.

## Contrato view ↔ controller (detalhes e regras em `docs/CONTROLLER_E_VALIDACAO.md`)
A tela só passa o **texto dos campos**. Quem valida, converte e traduz erros é o controller.
```java
// ClienteController
Cliente cadastrar(nome, email, telefone, endereco)            Cliente atualizar(id, nome, email, telefone, endereco)
void excluir(int id)   List<Cliente> listar(filtro, ordem1, ordem2)   List<Cliente> listarTodos()
// PneuController  (atenção: a ordem é aro, PREÇO, estoque)
Pneu cadastrar(marca, modelo, aro, preco, estoque)            Pneu atualizar(id, marca, modelo, aro, preco, estoque)
void excluir(int id)   List<Pneu> listar(filtro, ordem1, ordem2)     List<Pneu> listarComEstoque()
// VendaController  (datas da busca como texto dd/mm/aaaa; vazio = sem limite)
Venda registrar(Cliente, Pneu, String quantidade)             BigDecimal calcularTotal(Pneu, String quantidade)
void excluir(int id)   List<Venda> listar(filtroCliente, String dataInicio, String dataFim, ordem1, ordem2)
```
Erros: `ValidacaoException` (mensagem pronta + `getCampo()`) e `PersistenciaException` (mensagem já traduzida).
A tela mostra os dois em `Mensagens` e dá foco ao campo indicado. Os controllers recebem o DAO por construtor
(`new ClienteController(dao)`), então o `Main` troca JDBC por memória sem mudar nada.

## Guia curto de padrão visual
- Cores e fontes só via `Tema`; laranja = ação principal, vermelho = exclusão.
- Botões: **Novo, Editar, Excluir** à esquerda, **Atualizar lista** à direita; formulário: **Cancelar, Salvar**.
- Campos: `CampoTexto` (dica, foco laranja, erro vermelho), `CampoTelefone`, `CampoData`. Obrigatório = `*` vermelho.
- Mensagens sempre por `Mensagens` (aviso / erro / confirmar exclusão). Dinheiro `R$ 589,90` via `Formato`.
- Nova aba: estender `PainelListagem<T, Ordem>` (veja `ClientePanel`, ~80 linhas).

## Atalhos
Busca filtra ao digitar · Ctrl+F busca · Ctrl+N novo · Enter/duplo clique edita · Delete exclui · F5 atualiza ·
Ctrl+1/2/3 troca de aba · clique direito na linha abre o menu.
