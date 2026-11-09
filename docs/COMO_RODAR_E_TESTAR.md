# Loja de Pneus: como rodar e como testar (Windows + VS Code)

Tempo: ~10 minutos na primeira vez. **Você pode ver o sistema funcionando em 3 minutos sem MySQL** (Parte 3).

## O que tem na pasta
```
pom.xml                    projeto Maven (já baixa o driver do MySQL e o FlatLaf)
README.md                  resumo do projeto e como rodar
database\loja_pneus.sql    script do banco (cria tabelas e dados de exemplo)
db.properties.example      modelo da configuração do banco (você copia para db.properties)
.vscode\launch.json        botões de execução prontos (sistema e testes)
src\main\java\...          o código (estrutura de pacotes no README.md)
docs\                      documentação de apoio (não afeta o código)
```

## Parte 1: instalar o que precisa (uma vez)
1. **JDK 17 ou 21.** No terminal (`Win+R` → `cmd`): `java -version` e `javac -version` precisam responder os dois.
   Se não responderem: `winget install EclipseAdoptium.Temurin.17.JDK` e **feche e abra o VS Code**.
2. **VS Code** com a extensão **Extension Pack for Java** (Microsoft). Ela inclui o Maven.
3. **MySQL 8.0.16 ou mais novo** (só para a Parte 4). Abaixo disso o banco ignora os `CHECK`.

## Parte 2: abrir o projeto
1. Extraia o zip numa pasta simples (ex.: `C:\projetos\lojapneus`). Não rode de dentro do zip.
2. VS Code → **File → Open Folder…** → escolha **a pasta que contém o `pom.xml`**.
3. "Do you trust the authors?" → **Yes, I trust**.
4. Espere o canto inferior terminar o "Java: Importing projects / Building workspace" (a 1ª vez baixa dependências).
5. Se aparecer *"Java Language Server configuration changed"* → clique **Reload**.

## Parte 3: rodar SEM banco (modo demonstração)
1. Aperte **F5** (ou menu **Run → Start Debugging**) e escolha **"2) Sistema (modo demonstracao, sem banco)"**.
   Outra forma: abra `src\main\java\br\unip\aps\lojapneus\Main.java` e clique em **▶ Run** acima do `main`.
2. A janela abre com um selo laranja **MODO DEMONSTRAÇÃO** no cabeçalho. Os dados ficam na memória (mesmos do SQL) e
   voltam ao original quando fechar.

## Parte 4: rodar COM o banco MySQL
**4.1 Criar o banco** (o script **apaga e recria** o banco `loja_pneus`). Escolha um jeito:
- **MySQL Workbench:** *File → Open SQL Script…* → `database\loja_pneus.sql` → clique no raio ⚡ (Execute).
- **Terminal** (na pasta do projeto): `mysql -u root -p < database\loja_pneus.sql`
  (se `mysql` não for reconhecido, use o caminho completo, ex.: `"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"`).

**4.2 Configurar a conexão**
1. No Explorer do VS Code, copie `db.properties.example` e cole na **mesma pasta** (raiz, onde está o `pom.xml`).
2. Renomeie a cópia para exatamente **`db.properties`** (sem `.txt` e sem `.example`).
3. Abra e preencha: `db.user` (normalmente `root`) e `db.password` (**a sua senha** do MySQL). Salve.
   Esse arquivo **nunca** vai para o Git (já está no `.gitignore`).

**4.3 Rodar:** **F5** → **"1) Sistema (com banco MySQL)"**. Se **não** aparecer o selo laranja, está no banco real.
Se o MySQL não estiver acessível, o sistema avisa e abre em modo demonstração sozinho.

## Parte 5: testes automáticos (F5 → escolha o teste)
Cada teste termina com uma linha de resultado. O que esperar:

| Teste | Precisa de banco? | Resultado esperado |
|---|---|---|
| **3) Teste: Validador** | Não | `RESULTADO: 54 aprovados, 0 reprovados` |
| **4) Teste: Controller** | Sim (Parte 4.1 e 4.2) | `RESULTADO: 56 aprovados, 0 reprovados` |
| **5) Teste: DAO** | Sim | 6 linhas com `(esperado ...)` e o código igual ao esperado: 1062, 1451, 1451, 3819, -1, 1452 |

Os testes de banco deixam os dados como estavam (10 clientes, 10 pneus, 15 vendas). Se um teste falhar no meio e
você quiser garantir o estado original, rode de novo o `database\loja_pneus.sql`.
**Importante:** esses 3 testes foram aprovados no meu ambiente com o driver do MariaDB. Rodar no seu PC (driver oficial
do MySQL) é a confirmação final. Se algum reprovar, me mande a linha que falhou.

## Parte 6: roteiro de teste manual na tela (3 a 5 min)
Faça nesta ordem. **Esperado** entre parênteses.
1. **Trocar de aba:** clique em Clientes/Pneus/Vendas ou `Ctrl+1/2/3` (os números ao lado mostram o total de cada uma).
2. **Busca:** digite `souza` em Clientes (lista filtra sozinha). Digite `zzz` (aparece "Nenhum cliente encontrado" com a opção Limpar).
3. **Ordenação por 2 critérios:** em Pneus, "Ordenar por: Marca A a Z" e "Depois por: Preço maior para menor"
   (marcas em ordem e, dentro de cada marca, o preço do maior para o menor).
4. **Cadastrar com erro:** Clientes → **Novo** → deixe o nome vazio → **Salvar** (aviso "Informe o nome do cliente", campo em vermelho).
5. **Máscara e cadastro:** nome `Teste Silva`, telefone `19999998888` (vira `(19) 99999-8888`), endereço qualquer → **Salvar**
   (lista atualiza e aparece a mensagem verde "Cliente cadastrado com sucesso").
6. **E-mail repetido:** cadastre outro com o e-mail de um cliente existente (aviso "Já existe um cliente com este e-mail").
7. **Editar:** duplo clique na linha (ou selecione e `Enter`), mude o endereço → Salvar.
8. **Excluir com confirmação:** selecione o `Teste Silva` → **Excluir** → confirmar (some da lista).
9. **Chave estrangeira:** exclua o cliente **Carlos Eduardo Souza** (tem vendas): mensagem "possui vendas registradas e não pode ser excluído".
   Faça o mesmo em Pneus com o **Pirelli Cinturato P1**.
10. **Pneu com erro:** Pneus → Novo → aro `99` → Salvar ("Aro deve estar entre 10 e 24"); preço `0` → ("Preço deve ser um valor maior que zero"). Preço `589,90` e `589.90` são aceitos.
11. **Venda e estoque:** Vendas → **Novo** → escolha cliente e pneu (o estoque e o preço aparecem), quantidade `2` (o **Total** muda na hora) → Salvar.
    Vá em **Pneus** (o estoque do pneu diminuiu em 2) → volte a Vendas → exclua essa venda (confirmação avisa que o estoque volta) → confira em Pneus.
12. **Estoque insuficiente:** em Vendas → Novo, quantidade `999` → Salvar (aviso com o estoque atual).
13. **Período:** em Vendas, clique **30 dias** / **Tudo**, ou digite `01/09/2026` até `30/09/2026` (6 vendas). Data inicial depois da final mostra aviso.
14. **Tamanho da janela:** arraste para o menor tamanho e maximize; nada deve ficar cortado.
15. **Banco desligado:** feche o sistema, pare o MySQL (`net start`/`net stop MySQL80` num terminal como administrador) e abra de novo:
    deve avisar "Não foi possível conectar" e abrir em modo demonstração.

## Problemas comuns
| Mensagem / sintoma | O que fazer |
|---|---|
| `redhat.java failed ... java.home ... missing folder` | `Ctrl+Shift+P` → *Preferences: Open User Settings (JSON)*, apague `"java.home"` e `"java.jdt.ls.java.home"`, salve e *Developer: Reload Window*. Veja também a aba *Workspace*. |
| `Main class ... doesn't exist in the workspace` | O projeto ainda não carregou: **Reload**, espere o "Importing projects", confira que `pom.xml` e `src` estão na raiz do Explorer. Persistindo: *Java: Clean Java Language Server Workspace*. |
| `Cannot find Java 1.8 or higher` | Java fora do PATH. Instale o JDK 17 e reabra o VS Code. |
| `git repository at c:\Users\... has too many active changes` | Clique OK (não afeta). Para sumir: configuração `"git.openRepositoryInParentFolders": "never"`. |
| Abre com selo **MODO DEMONSTRAÇÃO** sem você querer | Não conectou ao MySQL: ligue o serviço, rode o `.sql` (4.1) e confira o `db.properties` (4.2). A mensagem do aviso diz o motivo. |
| `Access denied for user` | Usuário ou senha errados no `db.properties`. |
| `Unknown database 'loja_pneus'` | Falta rodar o `database\loja_pneus.sql` (4.1). |
| `Communications link failure` | MySQL desligado (serviço `MySQL80`) ou porta/URL diferentes no `db.properties`. |
| `Nao foi possivel ler o arquivo db.properties` | O arquivo não existe na raiz ou está com outro nome (ex.: `db.properties.txt`). |
| `pom.xml` com erro | A 1ª linha precisa começar exatamente com `<?xml` (sem espaço antes). |
| FlatLaf não baixa (rede da faculdade) | Sem problema: o sistema abre igual, só com o visual padrão do Java. |

## Terminal (opcional, se você tiver o Maven instalado)
```
mvn compile exec:java                              (com banco)
mvn compile exec:java -Dexec.args="--memoria"      (modo demonstração)
```

## Erro de caminho do JDK no VS Code (pasta Downloads / "missing folder")

Causa: alguma configuração do **seu** VS Code aponta para uma pasta fixa de JDK que não existe mais.
O projeto não define caminho de JDK em lugar nenhum (`launch.json` e `pom.xml` não têm), e o
`.vscode/settings.json` nem vai para o Git. Como corrigir, sem caminho fixo:

1. `Ctrl+Shift+P` → **Preferences: Open User Settings (JSON)** e também **Open Workspace Settings (JSON)**.
2. Apague, se existirem: `java.jdt.ls.java.home`, `java.home`, `jdk.jdkhome` e entradas de
   `java.configuration.runtimes` com caminho em `Downloads`.
3. Instale o **Extension Pack for Java** (`.vscode/extensions.json` já recomenda). Ele detecta o JDK
   instalado (JAVA_HOME/PATH) ou oferece baixar um sozinho; precisa de **JDK 17 ou mais novo**.
4. `Ctrl+Shift+P` → **Java: Configure Java Runtime** para conferir; depois **Developer: Reload Window**.
5. Opcional: copie `.vscode/settings.json.example` para `.vscode/settings.json` (não tem caminho de JDK).

## Novidades (v4) — nada muda no banco de dados
- **Editar venda**: aba Vendas → selecionar → Editar (ou Enter / duplo clique). Cliente, pneu e quantidade podem mudar;
  o estoque é ajustado na mesma transação; data e preço original (se o pneu for o mesmo) são mantidos.
- **Estoque baixo em destaque** (aba Pneus): linhas com até 5 un. ficam em laranja claro e as sem estoque em vermelho claro;
  o filtro "Só estoque baixo" mostra apenas esses pneus.
- **Exportar CSV** (todas as abas): botão ao lado de "Limpar". Salva a lista que está na tela (com busca, período e ordem),
  em ponto e vírgula + UTF-8, pronto para abrir no Excel.
- **Aba Resumo** (Ctrl+4): faturamento, vendas, ticket médio, estoque baixo, faturamento por mês, melhores clientes e pneus
  mais vendidos, com filtro de período. Tudo calculado no Java a partir dos dados existentes.
- **Recibo da venda** (aba Vendas): selecione uma venda → Recibo. Abre no navegador; clique em "Imprimir / salvar em PDF".
- **TesteController** tem 5 verificações a mais para a edição de venda (esperado: 56 aprovados).
- Banco: o `loja_pneus.sql` é o original (sem tabelas, colunas ou views novas).
