# Controller e validação

Documentação da camada de controller e validação. Compila com Java 17 e roda contra o `database/loja_pneus.sql`.

## Arquivos desta camada

| Arquivo | Pacote | Para que serve |
| --- | --- | --- |
| `ClienteController` | controller | Cadastrar, editar, excluir, buscar e listar clientes |
| `PneuController` | controller | Mesmo para pneus, mais `listarComEstoque()` para o combo de vendas |
| `VendaController` | controller | Registrar, excluir e listar vendas; regras de estoque e preço; `calcularTotal()` |
| `TradutorErros` | controller | Troca o código de erro do MySQL por mensagem amigável (uso interno dos controllers) |
| `ValidacaoException` | exception | Erro de validação: guarda o campo que falhou e a mensagem |
| `Validador` | util | Validação e conversão de texto, e-mail, telefone, inteiros, dinheiro e datas |
| `Formatador` | util | Opcional para as telas: `R$ 589,90`, `05/10/2026`, `(19) 99123-4567` |
| `TesteValidador` | teste | `main` que testa o Validador sem banco (54 casos) |
| `TesteController` | teste | `main` que testa os controllers contra o banco (56 casos); deixa o banco como estava |

## Como rodar os testes

1. Rodar o `loja_pneus.sql` num MySQL limpo.
2. Copiar `db.properties.example` para `db.properties` e preencher a senha.
3. Executar `TesteValidador` (não precisa de banco) e depois `TesteController`.
4. As duas classes terminam com `RESULTADO: N aprovados, 0 reprovados`.

Linhas `[ERRO BANCO] ...` no console durante o `TesteController` são esperadas: é o detalhe técnico que o controller registra para o programador e nunca mostra na tela.

## Contrato para as telas

A tela passa o texto dos campos do jeito que veio (`txtNome.getText()`), sem converter nada. O controller valida, converte, grava e devolve o objeto ou lança uma de duas exceções:

- `ValidacaoException`: o usuário digitou algo errado. `getMessage()` traz a mensagem pronta e `getCampo()` diz qual campo (use para dar foco). `getCampo()` é `null` quando o erro não é de um campo, como "Selecione um cliente na lista.".
- `PersistenciaException`: o banco recusou. `getMessage()` já vem traduzida ("Este cliente possui vendas registradas e não pode ser excluído.").

### ClienteController

| Método | Lança |
| --- | --- |
| `Cliente cadastrar(String nome, String email, String telefone, String endereco)` | Validação, Persistência |
| `Cliente atualizar(int id, String nome, String email, String telefone, String endereco)` | Validação, Persistência |
| `void excluir(int id)` | Validação, Persistência |
| `Cliente buscarPorId(int id)` (null se não existir) | Persistência |
| `List<Cliente> listar(String filtroNome, ClienteOrdem ordem1, ClienteOrdem ordem2)` | Persistência |
| `List<Cliente> listarTodos()` (A-Z, para o combo de vendas) | Persistência |

Campos: `CAMPO_NOME`, `CAMPO_EMAIL`, `CAMPO_TELEFONE`, `CAMPO_ENDERECO`.

### PneuController

| Método | Lança |
| --- | --- |
| `Pneu cadastrar(String marca, String modelo, String aro, String preco, String estoque)` | Validação, Persistência |
| `Pneu atualizar(int id, String marca, String modelo, String aro, String preco, String estoque)` | Validação, Persistência |
| `void excluir(int id)` | Validação, Persistência |
| `Pneu buscarPorId(int id)` | Persistência |
| `List<Pneu> listar(String filtroMarcaOuModelo, PneuOrdem ordem1, PneuOrdem ordem2)` | Persistência |
| `List<Pneu> listarComEstoque()` (para o combo de vendas) | Persistência |

Campos: `CAMPO_MARCA`, `CAMPO_MODELO`, `CAMPO_ARO`, `CAMPO_PRECO`, `CAMPO_ESTOQUE`.

Atenção: a ordem dos parâmetros segue o formulário (aro, preço, estoque). O construtor do `Pneu` usa outra ordem (aro, estoque, preço); o controller já cuida disso.

### VendaController

| Método | Lança |
| --- | --- |
| `Venda registrar(Cliente cliente, Pneu pneu, String quantidade)` | Validação, Persistência |
| `BigDecimal calcularTotal(Pneu pneu, String quantidade)` (zero se inválido, não lança) | nada |
| `void excluir(int id)` (devolve o estoque) | Validação, Persistência |
| `Venda buscarPorId(int id)` | Persistência |
| `List<Venda> listar(String filtroCliente, String dataInicio, String dataFim, VendaOrdem ordem1, VendaOrdem ordem2)` | Validação, Persistência |
| `List<Venda> listarTodas()` (mais recente primeiro) | Persistência |

Campos: `CAMPO_CLIENTE`, `CAMPO_PNEU`, `CAMPO_QUANTIDADE`, `CAMPO_DATA_INICIO`, `CAMPO_DATA_FIM`.

O usuário não digita preço nem data da venda. O controller busca o pneu de novo no banco, usa o preço atual dele e a hora do registro. Datas da busca vêm como texto `dd/mm/aaaa`; campo vazio ou máscara vazia significa "sem limite".

### Exemplo de uso numa tela (formulário de cliente)

```java
private final ClienteController controller = new ClienteController();

private void btnSalvarActionPerformed(java.awt.event.ActionEvent evt) {
    try {
        if (clienteEmEdicao == null) {
            controller.cadastrar(txtNome.getText(), txtEmail.getText(),
                    txtTelefone.getText(), txtEndereco.getText());
        } else {
            controller.atualizar(clienteEmEdicao.getId(), txtNome.getText(),
                    txtEmail.getText(), txtTelefone.getText(), txtEndereco.getText());
        }
        JOptionPane.showMessageDialog(this, "Cliente salvo.", "Pronto",
                JOptionPane.INFORMATION_MESSAGE);
        dispose();
    } catch (ValidacaoException e) {
        JOptionPane.showMessageDialog(this, e.getMessage(), "Confira os dados",
                JOptionPane.WARNING_MESSAGE);
        if (ClienteController.CAMPO_NOME.equals(e.getCampo())) txtNome.requestFocus();
        if (ClienteController.CAMPO_EMAIL.equals(e.getCampo())) txtEmail.requestFocus();
        if (ClienteController.CAMPO_TELEFONE.equals(e.getCampo())) txtTelefone.requestFocus();
    } catch (PersistenciaException e) {
        JOptionPane.showMessageDialog(this, e.getMessage(), "Erro",
                JOptionPane.ERROR_MESSAGE);
    }
}
```

Os combos "Ordenar por" e "Depois por" viram um valor do enum: "Nome" + "Crescente" vira `ClienteOrdem.NOME_ASC`. Para mostrar preço, data e telefone formatados na `JTable`, usem o `Formatador`.

## Regras de validação (base para o plano de testes)

Todas as mensagens abaixo foram conferidas rodando o `TesteValidador` e o `TesteController`.

| ID | Entidade | Campo | Regra | Entrada de teste | Mensagem esperada |
| --- | --- | --- | --- | --- | --- |
| V01 | Cliente | Nome | Obrigatório | vazio ou só espaços | Informe o nome do cliente. |
| V02 | Cliente | Nome | Até 100 caracteres | 101 letras | O nome do cliente deve ter no máximo 100 caracteres (você digitou 101). |
| V03 | Cliente | E-mail | Opcional; se preenchido, precisa de @ e ponto no domínio | `ana@` | E-mail inválido. Use o formato nome@dominio.com. |
| V04 | Cliente | E-mail | Não pode repetir (banco, erro 1062) | e-mail de outro cliente | Já existe um cliente com este e-mail. |
| V05 | Cliente | Telefone | Opcional; só números e símbolos de máscara | `19abc234567` | O telefone deve ter só números (parênteses, espaço e hífen são aceitos). |
| V06 | Cliente | Telefone | 10 ou 11 dígitos depois de tirar a máscara | `9123-4567` | Telefone deve ter DDD e número (10 ou 11 dígitos). |
| V07 | Cliente | Endereço | Opcional, até 150 caracteres | 151 letras | O endereço deve ter no máximo 150 caracteres (você digitou 151). |
| V08 | Cliente | – | Editar ou excluir exige linha selecionada | nada selecionado | Selecione um cliente na lista. |
| V09 | Cliente | – | Cliente com vendas não pode ser excluído (banco, erro 1451) | excluir Carlos Eduardo Souza | Este cliente possui vendas registradas e não pode ser excluído. |
| V10 | Pneu | Marca | Obrigatória, até 50 caracteres | vazio | Informe a marca. |
| V11 | Pneu | Modelo | Obrigatório, até 80 caracteres | vazio | Informe o modelo. |
| V12 | Pneu | Aro | Número inteiro | `abc` | O aro deve ser um número inteiro, sem letras, pontos ou vírgulas. |
| V13 | Pneu | Aro | Entre 10 e 24 | `30` | O aro deve estar entre 10 e 24. |
| V14 | Pneu | Preço | Obrigatório | vazio | Informe o preço. |
| V15 | Pneu | Preço | Número com até 2 casas; aceita `589,90`, `589.90`, `1.299,90` e `R$` | `abc` ou `589,905` | O preço deve ser um número com até 2 casas decimais (ex.: 589,90). |
| V16 | Pneu | Preço | Maior que zero | `0` ou `-5` | O preço deve ser um valor maior que zero. |
| V17 | Pneu | Preço | Até 99.999.999,99 (limite do DECIMAL(10,2)) | `100000000` | O preço deve ser de no máximo 99.999.999,99. |
| V18 | Pneu | Estoque | Número inteiro | `2,5` | O estoque deve ser um número inteiro, sem letras, pontos ou vírgulas. |
| V19 | Pneu | Estoque | Zero ou mais | `-1` | O estoque não pode ser negativo. |
| V20 | Pneu | – | Editar ou excluir exige linha selecionada | nada selecionado | Selecione um pneu na lista. |
| V21 | Pneu | – | Pneu com vendas não pode ser excluído (banco, erro 1451) | excluir Pirelli Cinturato P1 | Este pneu possui vendas registradas e não pode ser excluído. |
| V22 | Venda | Cliente | Obrigatório | combo sem seleção | Selecione o cliente. |
| V23 | Venda | Pneu | Obrigatório | combo sem seleção | Selecione o pneu. |
| V24 | Venda | Quantidade | Obrigatória | vazio | Informe a quantidade. |
| V25 | Venda | Quantidade | Número inteiro | `dois` | A quantidade deve ser um número inteiro, sem letras, pontos ou vírgulas. |
| V26 | Venda | Quantidade | Maior que zero | `0` | A quantidade deve ser maior que zero. |
| V27 | Venda | Quantidade | Até o estoque atual do pneu | 9 unidades do Michelin Primacy 4 (estoque 8) | Estoque insuficiente: há 8 unidades. |
| V28 | Venda | Preço unitário | Automático: preço atual do pneu, guardado na venda | mudar o preço do pneu depois da venda | A venda antiga mantém o preço da época |
| V29 | Venda | Data inicial/final | Formato dd/mm/aaaa e data que existe | `31/02/2026` | A data inicial é inválida. Use dd/mm/aaaa (ex.: 05/10/2026). |
| V30 | Venda | Data final | Igual ou depois da inicial | de 10/10/2026 até 01/10/2026 | A data final deve ser igual ou posterior à data inicial. |
| V31 | Venda | – | Excluir exige linha selecionada; excluir devolve o estoque | excluir uma venda de 2 unidades | Estoque do pneu sobe 2 |
| V32 | Sistema | – | Sem conexão com o banco | MySQL desligado | Não foi possível conectar ao banco de dados. Verifique se o MySQL está ligado e se o arquivo db.properties está configurado. |

## Tradução dos erros do banco

| Código MySQL | Situação | Mensagem na tela |
| --- | --- | --- |
| 1062 | E-mail repetido | Já existe um cliente com este e-mail. |
| 1451 | Excluir cliente ou pneu que tem venda | Este cliente/pneu possui vendas registradas e não pode ser excluído. |
| 1452 | Venda com cliente ou pneu que não existe mais | O cliente ou o pneu escolhido não existe mais. Atualize a lista e tente de novo. |
| 3819 | `CHECK` violado | Algum valor está fora das regras do sistema. Confira os campos e tente de novo. |
| 1048 | `NOT NULL` recebeu nulo | Preencha todos os campos obrigatórios. |
| 1406 | Texto maior que o `VARCHAR` | Algum campo passou do tamanho máximo permitido. |
| 1264 | Número maior que a coluna aceita | Algum número passou do limite permitido. |
| -1 (código do DAO) | Estoque acabou entre a conferência e a gravação | Estoque insuficiente: há N unidades. |
| 1045, 1049, SQLState 08xxx | Senha errada, banco não criado, MySQL desligado | Não foi possível conectar ao banco de dados... |
| 0 sem causa | Erro criado pelo próprio DAO ("Cliente não encontrado.") | A mensagem do DAO, que já é amigável |
| Qualquer outro | Desconhecido | Não foi possível concluir a operação. Tente de novo; se o erro continuar, avise o responsável pelo sistema. |

Na prática, os erros 3819, 1048, 1406 e 1264 nunca chegam ao banco, porque o `Validador` barra antes. A tradução fica como segunda proteção.

## Observações
- Aqui a tela conhece o controller, o controller não conhece a tela, e a comunicação é por retorno de método e exceção.
- O `db.properties` guarda a senha do banco e não vai para o repositório. Só o `.example` é versionado.
