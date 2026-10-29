# Melhorias implementadas — Espelho Vital

Atualizado em 29/10/2026.

## Registrar o fim da menstruação

No calendário, toque na data e use **Minha menstruação terminou**. Informe o **último dia em que houve sangramento**. O diálogo identifica o início do período que será atualizado. A duração é calculada incluindo o primeiro e o último dia, permitindo registrar períodos mais curtos ou mais longos que a estimativa, inclusive em ciclos irregulares.

O histórico mostra a data de fim confirmada; enquanto não informada, identifica a duração como prevista. Para corrigir o fim, toque novamente em uma data do período e use **Atualizar fim da menstruação**. O calendário e as previsões são recalculados sem mudar os intervalos de regularidade informados.

O endpoint `POST /ciclo/{id}/fim` recebe `data`, exige autenticação, propriedade do registro e CSRF. Rejeita datas futuras, anteriores ao início ou que alcancem o próximo período. A migração V5 adiciona `fim_menstruacao`, sem preencher datas fictícias nos registros antigos.

## Registro rápido de uma nova menstruação

Depois do primeiro cadastro, não é necessário preencher novamente o formulário:

1. Toque no dia em que a menstruação realmente começou no calendário, ou no botão **Minha menstruação começou hoje**.
2. Confirme **Minha menstruação começou neste dia**.
3. O início é salvo e o calendário recalcula as próximas previsões usando o histórico atualizado.

A duração do sangramento e as configurações de regularidade/faixas são reaproveitadas dos registros anteriores. O intervalo estimado continua se adaptando aos intervalos observados, inclusive para ciclos irregulares.

- O primeiro registro continua usando o formulário completo.
- **Ajustar informações** e **Editar este período** permitem corrigir dados quando necessário.
- Um dia que já pertence a um período mostra o registro existente, sem cadastrar outro início.
- Datas futuras não podem ser confirmadas como inícios reais.
- O botão **Desfazer último registro** remove somente o início recém-criado e recalcula as previsões.
- Repetir a mesma confirmação não cria duplicações nem altera um início já registrado.
- O atalho vindo do diário também usa a confirmação simplificada quando o primeiro cadastro já existe.

O endpoint autenticado `POST /ciclo/inicio` recebe somente `data`. As demais informações vêm do histórico da própria conta, com proteção CSRF e gravação serializada por usuária. Não houve mudança de schema para esta melhoria.

## Diário, resumo para consulta, lembretes e privacidade

### Registrar meu dia

O painel inicial ganhou o botão **Registrar meu dia**, sem adicionar cartões de detalhe. A página **Meu diário** reúne, por data:

- Fluxo: não informado, sem fluxo, escape, leve, moderado ou intenso.
- Intensidade de dor opcional, de 0 a 10.
- Sintomas e um ou mais estados de humor.
- Horas e qualidade de sono.
- Observações de até 2.000 caracteres.
- Consulta, edição e exclusão do registro, com histórico filtrado e paginado.

Os registros de sono e humor são compartilhados com suas telas individuais. Novas gravações do mesmo dia atualizam o registro correspondente, sem criar duplicatas. O diário também aceita datas passadas; datas futuras são recusadas.

Excluir um dia remove também os registros de sono e humor vinculados àquele registro, conforme a confirmação apresentada na interface. Registros legados não vinculados permanecem armazenados.

A antiga página de sintomas abre o diário. Quando há um registro antigo de sintomas neste dispositivo associado à conta, a usuária pode importá-lo para revisão. O original só é removido após salvar. Registros antigos sem identificação da conta não são atribuídos automaticamente.

Informar fluxo não cria automaticamente um início menstrual. O diário oferece um link para abrir o calendário com a data escolhida; a usuária confirma o registro do ciclo separadamente.

### Variações do ciclo

O calendário apresenta **Como seu ciclo tem variado**, com média, menor e maior intervalo, amplitude e gráfico dos últimos 12 intervalos. Uma tabela permite consultar todos os intervalos.

Os cálculos usam apenas diferenças entre datas de início realmente registradas, sem previsões e sem contar datas duplicadas. Lacunas no histórico podem aumentar esses valores; o resumo não diagnostica irregularidade.

### Resumo para consulta em PDF

A página **Resumo para consulta** permite escolher um período de até 366 dias e as seções: diário, sono, humor, ciclo e consultas. A prévia mostra as quantidades antes do download.

O PDF inclui todos os registros das seções selecionadas no período, preserva acentos e possui paginação. Desmarcar sono ou humor também retira esses campos dos itens do diário no relatório. Observações livres continuam incluídas quando a seção diário é escolhida.

O arquivo é gerado no back-end e baixado pela própria usuária. Não é enviado a profissionais ou terceiros. Senhas, tokens e biometria não entram no resumo para consulta.

Foi usada a biblioteca [Apache PDFBox](https://pdfbox.apache.org/) e a fonte Noto Sans, com licença SIL OFL incluída em `src/main/resources/fonts`. Símbolos sem representação na fonte, como alguns emojis, são omitidos no PDF; os textos originais permanecem nos registros e na exportação JSON.

### Lembretes personalizados

Em **Conta e privacidade > Lembretes**, a usuária pode:

- Ativar ou pausar os lembretes; o padrão é desativado.
- Escolher diário, sono, compromissos ou estimativas do ciclo.
- Definir horário, dias da semana, fuso horário e antecedências.
- Consultar e dispensar os lembretes de hoje no sino do painel.
- Autorizar notificações deste navegador ou receber um resumo discreto por e-mail.

Registros já preenchidos deixam de gerar lembretes correspondentes. Dispensar um aviso fica salvo para a conta naquele dia.

**Navegador:** requer permissão concedida por ação explícita e uma aba do Espelho Vital aberta. Não há promessa de notificações com o navegador fechado.

**E-mail:** requer SMTP configurado e o back-end em execução. É enviado somente após a usuária ativar essa opção, no máximo um resumo diário. A mensagem não contém sintomas ou detalhes de saúde. Falhas têm intervalo mínimo de 15 minutos entre tentativas e limite de três tentativas no dia. Com SMTP ausente, a opção aparece indisponível.

O agendador verifica preferências periodicamente. `app.reminders.scheduler-enabled=false` desativa o processamento de e-mail; `app.reminders.poll-ms` ajusta o intervalo, cujo padrão é 60 segundos. Nos testes o agendador fica desativado e o envio é simulado.

### Controle dos dados da conta

Em **Conta e privacidade > Meus dados e privacidade**:

- Exportação JSON do perfil, diário, sono, humor, ciclos, consultas, preferências e histórico de lembretes.
- O descritor facial só entra na exportação se a usuária marcar a opção explicitamente.
- Senhas e tokens nunca são exportados.
- Exclusão separada do cadastro facial.
- Exclusão dos registros de saúde e compromissos, mantendo conta, perfil e biometria e pausando lembretes.
- Exclusão permanente da conta e de todos os registros vinculados, invalidando a sessão.

Essas ações exigem confirmação da senha. Exclusões de registros ou da conta exigem também uma frase específica apresentada no diálogo. Nada é removido automaticamente por acessar a página.

### Endpoints principais adicionados

| Caminho | Uso |
| --- | --- |
| `GET/PUT/DELETE /diario/{data}` | Consultar, salvar ou excluir o dia |
| `GET /diario` | Histórico por período e página |
| `GET /ciclo/variacoes` | Intervalos entre inícios confirmados |
| `POST /ciclo/inicio` | Registrar um início usando somente a data e reaproveitar as informações anteriores |
| `GET /relatorios/resumo` | Prévia do período escolhido |
| `GET /relatorios/resumo.pdf` | Baixar PDF |
| `GET/PUT /lembretes/config` | Preferências pessoais |
| `GET /lembretes/hoje` | Avisos pendentes |
| `POST /lembretes/lido` | Dispensar um aviso no dia |
| `GET /conta/privacidade` | Quantidades e estado do cadastro facial |
| `POST /conta/exportar` | Exportação com confirmação de senha |
| `DELETE /conta/biometria` | Remover descritor facial |
| `DELETE /conta/dados` | Apagar registros de saúde |
| `DELETE /conta` | Excluir a conta |

Todos exigem sessão autenticada. Operações de escrita exigem CSRF. Os filtros e exclusões são feitos pela identidade da sessão, sem confiar em IDs de usuária enviados pelo navegador.

## Interface e painel inicial

O front-end recebeu uma atualização visual mantendo o logo e a paleta de roxo profundo, violeta e lilás. As telas compartilham tipografia, bordas, campos, cartões e uma navegação lateral mais discreta. A área de login também recebeu os mesmos padrões visuais.

A página inicial autenticada (`perfil-pronto.html`) agora apresenta:

- Saudação pelo primeiro nome e data atual.
- Um único destaque contextual: compromisso hoje/amanhã, atualização do ciclo, registro de humor ou registro de sono. A prioridade usa os dados da conta.
- Check-in de humor e sono que identifica registros feitos na data atual.
- Três resumos: ciclo menstrual, média registrada de sono e último humor.
- Gráfico simples dos registros de sono dos últimos sete dias, com ausência de registro representada como ausência, sem inventar valores.
- Até três próximos compromissos, com acesso à agenda completa.
- Estados vazios explicativos, tratamento de erro com botão para tentar novamente e adaptação a celular.

As análises detalhadas ficam em suas próprias telas. O painel não usa notas artificiais de saúde, metas obrigatórias ou séries fictícias de registros.

Arquivos principais no front-end: `css/theme.css`, `css/dashboard.css`, `css/sidebar.css`, `js/icons.js`, `js/dashboard.js`, `js/sidebar.js` e `perfil-pronto.html`.

Verificação no Chrome com banco temporário: painel vazio e preenchido, destaque contextual, check-in diário, layout de celular, menu por teclado, falha de carregamento e nova tentativa. O teste de navegador em `scripts/smoke-browser.cjs` também contempla esses cenários.

## Se o sistema não abrir

Em 29/10/2026, a inicialização falhou com `Communications link failure` e `Connection refused`: o MySQL do XAMPP estava parado. O servidor do front-end na porta `5500` também não estava em execução. Após iniciar o banco, o back-end voltou a responder normalmente na porta `8081`.

Para usar o projeto após reiniciar o computador:

1. Inicie o **MySQL** no painel do XAMPP.
2. Execute `EspelhovitalBackApplication` no IntelliJ e aguarde a mensagem de inicialização concluída.
3. Abra o front-end com Live Server na porta `5500` ou, a partir da pasta do back-end, execute `node scripts/serve-frontend.cjs`.
4. Acesse `http://localhost:5500/pages/login.html`.

O servidor alternativo do front-end usa apenas Node.js e atende somente a conexões locais. Para outro caminho, defina `FRONTEND_DIR`. Não execute dois servidores na mesma porta.

Alterações implementadas no back-end deste repositório e no front-end em `C:\Users\Valdiney\Documents\workspace\EspelhoVital`.

**A chave de API existente e o nome legado `xai.api.key` foram preservados, conforme solicitado.** Este documento não contém seu valor.

## Calendário com vários ciclos

- Visualizações mensal e anual, com os 12 meses na mesma tela.
- Navegação entre meses e anos e botão para voltar ao mês atual.
- Previsões de vários ciclos, com horizonte de até 24 meses a partir de hoje.
- Cores distintas para menstruação registrada, menstruação estimada, faixa de início possível, ovulação estimada e período fértil estimado.
- Novos registros sem apagar o histórico anterior.
- Edição e exclusão de registros, recalculando as previsões.
- Seleção de uma data no calendário para consultar detalhes ou registrar um início real.
- Histórico persistido no servidor e separado por usuária; não depende mais de um único registro em `localStorage`.
- Aviso quando a previsão anterior passou e falta confirmar uma nova menstruação.
- Layout adaptado para celular e rótulos de acessibilidade nos controles.

### Como registrar ou corrigir

1. Entre na conta e abra **Ciclo Menstrual**.
2. Selecione **Registrar menstruação** e informe o primeiro dia real do sangramento.
3. Informe a duração do sangramento e o intervalo estimado entre os inícios.
4. Para ciclos irregulares, escolha **Irregular / varia bastante** e informe o menor e o maior intervalo habitual.
5. Salve. O histórico e as previsões serão atualizados.
6. Nas próximas menstruações, toque na data no calendário e confirme o início, sem repetir o formulário. Para corrigir informações, use **Editar** no histórico ou **Ajustar informações**.

Salvar novamente a mesma data atualiza o registro correspondente. As gravações são serializadas por usuária para evitar duplicações em chamadas simultâneas. Datas futuras e intervalos incoerentes são rejeitados.

### Estimativas e ciclos irregulares

O motor usa a mediana de até seis intervalos recentes observados entre inícios registrados. Sem um intervalo completo, utiliza a duração informada. Intervalos menores que 15 ou maiores que 180 dias permanecem no histórico, mas não entram automaticamente no cálculo.

No modo irregular, considera a faixa informada e a variação observada. Uma amplitude superior a sete dias em pelo menos dois intervalos também ativa a apresentação com incerteza. Esse limiar é uma regra de apresentação do aplicativo, **não um diagnóstico clínico**.

As faixas de início ficam mais amplas em previsões distantes. No modo irregular, não são apresentadas previsões de ovulação ou período fértil. Novos registros reais passam a ser a referência das próximas projeções.

**Previsões não são gravadas como menstruações confirmadas.** O calendário não confirma ovulação e não deve ser usado como método contraceptivo. Referência: [NHS — Natural family planning](https://www.nhs.uk/contraception/methods-of-contraception/natural-family-planning/).

Arquivos principais: `service/CycleProjection.java`, `service/CicloMenstrualService.java`, `controller/CicloMenstrualController.java`, `dto/CalendarioResponse.java`; no front-end, `pages/ciclo.html`, `js/ciclo.js` e `css/ciclo.css`.

## Segurança e isolamento dos dados

- Rotas privadas exigem autenticação por sessão no servidor.
- Login cria uma sessão e troca seu identificador.
- Cookie `HttpOnly`, 30 minutos de inatividade e `SameSite=Lax`; produção exige cookie seguro por HTTPS.
- Proteção CSRF nas operações que alteram dados, incluindo login e logout.
- Cliente centralizado do front-end envia cookies e token CSRF e trata erros HTTP.
- Sono, humor, ciclo, consultas, dashboard, análises e recomendações usam a identidade autenticada.
- IDs recebidos no corpo não permitem atribuir registros a outra conta. DTOs de entrada não expõem IDs de persistência.
- Alterações e exclusões conferem a proprietária do registro.
- Logout invalida a sessão no servidor e limpa os dados locais da conta.
- Redefinição de senha revoga sessões anteriores na próxima requisição.
- Limitação de tentativas em cadastro, login, recuperação de senha e chamadas de IA.
- CORS centralizado com origens explícitas e suporte a credenciais.
- Senha e descritor facial não são serializados em respostas de entidades.

O controle de frequência é em memória, por processo. Uma implantação com várias instâncias deve compartilhar esse controle em infraestrutura apropriada.

Arquivos principais: `config/SecurityConfig.java`, `config/CurrentUser.java`, `config/SessionVersionFilter.java`, `config/RateLimitFilter.java`, `service/SessionService.java` e `js/api.js` no front-end.

## Reconhecimento facial

- Cadastro e substituição exigem sessão autenticada e confirmação da senha.
- A conta afetada é determinada pela sessão, mesmo que outro ID seja enviado.
- O descritor precisa ter 128 números válidos e finitos.
- O acesso facial compara apenas a conta confirmada por e-mail e senha; não varre todas as usuárias.
- Ação para excluir o cadastro facial.
- Modelos externos referenciados por versão fixa, em vez de branch em movimento.

O descritor enviado pelo navegador, sozinho, não comprova uma captura ao vivo. O rosto passou a ser uma confirmação adicional; **e-mail e senha continuam obrigatórios** nesse fluxo. Não foi implementada prova de presença física.

## Validação, erros e front-end

- DTOs de entrada para sono, humor, consultas, ciclo, login e recuperação de senha.
- Validação de campos obrigatórios, tamanhos, faixas numéricas, datas e valores de tipo/status.
- Erros JSON padronizados, sem detalhes internos de exceções.
- HTTP 409 para conflitos e HTTP 400 para entradas inválidas.
- Verificação de erros também em atualizações e exclusões no front-end.
- Cards de consultas e histórico menstrual criados com DOM e `textContent`.
- Conteúdos externos das demais telas revisadas e nome na barra lateral escapados antes da inserção no HTML.
- Sintomas locais separados por conta e limpos na troca de sessão/logout.
- URL da API centralizada em `js/config.js`.

## Sono, consultas e análises

- Horas fracionadas, como `7.5`, aceitas pelo back-end.
- Qualidade não informada recebe classificação simples pela duração, identificada assim na interface.
- Histórico de sono e humor paginado com botão **Carregar mais**.
- Endpoints paginados para sono, humor e consultas, com máximo de 100 registros por página.
- Ordenação explícita por data e ID como desempate.
- Próximas consultas e lembretes filtrados no banco por usuária, status e data.
- Endpoint específico para alterar o status de consultas, sem buscar a lista completa.
- Dashboard alinhado ao motor do calendário, incluindo faixas para ciclos irregulares.
- Correção das condições de ovulação e fases do ciclo no dashboard.
- Cruzamento de sono e humor conta datas distintas com conjuntos, evitando comparar todas as combinações de registros.
- Injeção por construtor e remoção dos `System.out.println` de dados pessoais.

As análises consolidadas ainda leem o histórico da própria usuária para suas estatísticas. A paginação atende às telas de histórico; volumes muito grandes podem justificar agregações adicionais no banco.

## Integração de IA

- Chave existente preservada.
- Endereço padrão alinhado à Groq: `https://api.groq.com/openai/v1`.
- Modelo padrão: `llama-3.1-8b-instant`.
- Endereço e modelo configuráveis por `AI_API_URL` e `AI_MODEL`.
- Requisição e resposta tipadas; cinco segundos para conectar e 30 segundos para receber a resposta.
- Mensagens limitadas a 4.000 caracteres e saída limitada a 800 tokens.
- Falhas externas convertidas em erro controlado, sem expor detalhes do provedor.
- Assistente do front-end conectado ao endpoint protegido `/grok`, exibindo respostas como texto.

O nome `GrokService` e a rota `/grok` foram mantidos por compatibilidade. Não foi feita chamada à conta real do provedor: contrato e falhas foram testados com servidor local simulado. Referências oficiais: [Groq — integração](https://console.groq.com/docs/overview) e [modelos](https://console.groq.com/docs/models).

## Recuperação de senha

- Solicitação por e-mail com token aleatório, de uso único, válido por 30 minutos.
- Somente o hash SHA-256 do token fica no banco; novo pedido substitui o anterior.
- Nova página `pages/redefinir-senha.html`.
- Token transportado no fragmento do link e retirado da barra de endereço após leitura.
- Nova senha com BCrypt; tokens usados ou expirados são recusados.
- Revogação das sessões anteriores após a redefinição.
- Sem SMTP, informa indisponibilidade em vez de simular envio.

### Configuração para envio real

| Variável | Finalidade |
| --- | --- |
| `MAIL_HOST` | Servidor SMTP |
| `MAIL_PORT` | Porta; padrão 587 |
| `MAIL_USERNAME` | Usuário SMTP |
| `MAIL_PASSWORD` | Credencial SMTP |
| `MAIL_FROM` | Remetente autorizado |
| `MAIL_AUTH` | Autenticação; padrão `true` |
| `MAIL_STARTTLS` | STARTTLS; padrão `true` |
| `APP_FRONTEND_URL` | URL pública para gerar o link |

A geração do link foi testada com remetente simulado. A entrega real depende dessas configurações.

## Banco, perfis e migrações

- Conexão por `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`.
- Padrões locais preservados; perfil `prod` exige as variáveis do banco.
- SQL desabilitado nos logs e `open-in-view` desativado.
- Hibernate valida o schema; Flyway aplica as alterações versionadas.
- `V1`: schema inicial para banco novo.
- `V2`: ciclo irregular, índices por usuária/data, versão de autenticação e tokens de recuperação.
- `V3`: horas de sono decimais.
- `V4`: diário integrado, preferências e controle de entrega dos lembretes.
- Testes em H2 isolado, sem usar o MySQL de desenvolvimento.
- Configuração do Maven Wrapper adicionada para tornar utilizáveis os scripts existentes.

### Banco existente

O perfil padrão permite baseline em V1 para o schema legado e depois executa V2 e V3. Antes da primeira inicialização atualizada, faça backup e confira a correspondência do schema com as entidades anteriores. Produção desabilita baseline automático; a adoção em banco existente precisa estabelecer a versão inicial de forma controlada.

Em 29/10/2026, a migração V4 foi aplicada com sucesso no banco local do XAMPP, após salvar um backup em `target/espelhovital-before-features-20261029-102919.sql`. O back-end voltou a iniciar na porta `8081`. As migrações também foram verificadas nos testes em H2 no modo MySQL.

Registros legados sem `usuario_id` permanecem armazenados e não aparecem nas rotas pessoais. Não foram atribuídos automaticamente a nenhuma conta.

Em produção, use uma conta própria do banco com permissões limitadas. Criar essa conta depende da administração do banco; suas credenciais reais não foram alteradas.

## Como executar

1. Configure o MySQL e as variáveis de conexão quando necessário.
2. Reinicie o back-end atualizado; a porta padrão continua sendo `8081`.
3. Sirva o front-end por HTTP, por exemplo na porta `5500`.
4. Use o mesmo host nos dois lados: `localhost` com `localhost` ou `127.0.0.1` com `127.0.0.1`.
5. Para outra API, ajuste `js/config.js` e configure `APP_CORS_ORIGINS` no servidor.
6. Faça login novamente: os dados locais anteriores não estabelecem sessão no servidor.

Em produção, ative `SPRING_PROFILES_ACTIVE=prod`, use HTTPS e defina as variáveis de banco, `APP_CORS_ORIGINS` e `APP_FRONTEND_URL`. Sites diferentes entre front-end e API exigem planejar a política de cookies e a hospedagem em conjunto.

## Validação

**51 testes automatizados passaram, sem falhas**, incluindo sete testes do registro rápido: reaproveitamento das informações anteriores, recálculo das previsões, preservação de ciclos irregulares, confirmação idempotente, prevenção de sobreposição de períodos, validação de datas, isolamento da conta e restauração após desfazer. A suíte também cobre os demais recursos já descritos: diário, PDF, privacidade, autenticação e lembretes.

No Chrome, foram verificados o toque no calendário sem abertura do formulário completo, confirmação, recálculo, falha de rede com nova tentativa, dias já registrados, datas futuras e desfazer o registro, além dos fluxos anteriores.

As operações destrutivas foram testadas somente com contas e dados temporários. Nenhuma conta real foi excluída durante a implementação.

Também passou a verificação no **Chrome com back-end e H2 temporários**: login pela interface, calendário anual e meses futuros, ciclo irregular sem afirmações de fertilidade, layout móvel, histórico persistente, novo início sem apagar os anteriores, conteúdo malicioso exibido como texto, logout e redirecionamento das páginas protegidas.

Testes do back-end:

```powershell
.\mvnw.cmd test
```

Para repetir a verificação de navegador no Windows, com Node.js e Chrome disponíveis:

```powershell
.\mvnw.cmd test-compile dependency:build-classpath "-Dmdep.includeScope=test" "-Dmdep.outputFile=target/test-classpath.txt"
$env:FRONTEND_DIR = "C:\Users\Valdiney\Documents\workspace\EspelhoVital"
node scripts/smoke-browser.cjs
```

O script usa H2 em memória e as portas de teste `18081` e `15500`. Não utiliza a chave real nem o MySQL. `JAVA_HOME` e `CHROME_PATH` permitem ajustar os executáveis.

Os arquivos anteriores do front-end foram copiados para `target/frontend-backup-*`. A relação dos arquivos aplicados está em `target/frontend-applied.json`.

O registro de fim também foi validado com duração de um dia, encurtamento, extensão, correção repetida, preservação de irregularidade, isolamento entre contas e impedimento de sobreposição. No Chrome, foram testados o salvamento, a correção do fim e o layout móvel.
