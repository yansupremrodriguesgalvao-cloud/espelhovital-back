# Backend no Render

O backend usa Java 17, Maven e Docker. O frontend sera publicado separadamente na Vercel.

## Variaveis obrigatorias

Configure no Render em Environment. Para executar localmente, configure as mesmas variaveis na configuracao de execucao do IntelliJ.

| Nome | Valor |
| --- | --- |
| SPRING_PROFILES_ACTIVE | prod (ja definido na imagem Docker) |
| DB_URL | URL JDBC do banco, incluindo porta, nome do banco e TLS |
| DB_USERNAME | Usuario do banco |
| DB_PASSWORD | Senha do banco |
| APP_CORS_ORIGINS | Origem HTTPS exata do frontend, sem barra final |
| APP_FRONTEND_URL | URL HTTPS do frontend, sem barra final |

Exemplo de URL TiDB: `jdbc:mysql://SEU_HOST:4000/espelhovital?sslMode=VERIFY_IDENTITY`.
As configuracoes anteriores do banco continuam comentadas em `src/main/resources/application.properties`, conforme solicitado. Comentarios tambem ficam visiveis para quem acessa o repositorio.

O Render fornece PORT automaticamente; o backend usa `server.port=${PORT:8081}` e escuta em `0.0.0.0`.

Opcionais: `AI_API_KEY` para IA; `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` e `MAIL_FROM` para envio de emails. Nao coloque esses valores em argumentos de build do Docker.

## Conferir o banco antes de publicar

O perfil prod usa Hibernate validate e Flyway sem baseline automatico. Nao execute migracoes manualmente nem habilite baseline sem conferir o schema existente.

No console SQL do provedor, confira:

```sql
SHOW TABLES;
```

Se existir `flyway_schema_history`, confira:

```sql
SELECT version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

O projeto contem migracoes V1 a V5. Banco vazio recebe as migracoes automaticamente. Banco existente sem historico precisa de revisao antes do primeiro deploy; o perfil prod recusara o baseline automatico. Confirme tambem que o provedor permite a conexao de rede do Render.

## Publicar

1. Envie Dockerfile, .dockerignore e as configuracoes para o repositorio do backend.
2. No Render, crie um Web Service conectado ao repositorio e a branch desejada.
3. Escolha Docker, com contexto na raiz e Dockerfile em `./Dockerfile`. Use o comando padrao da imagem.
4. Cadastre as variaveis acima. Enquanto o frontend nao tiver URL definitiva, atualize as URLs quando ele for publicado.
5. Configure Health Check Path como `/api/csrf`, endpoint publico existente que deve responder HTTP 200. Isso verifica o HTTP, sem substituir a verificacao de login e banco.
6. Publique e confira os logs de inicializacao e migracoes.

Depois de publicar: abra `https://SEU_BACKEND.onrender.com/api/csrf` e confirme a resposta JSON. Esse endpoint retorna um token e pode criar sessao.

## Etapa seguinte: frontend

O frontend atual monta a API com a porta 8081 e precisa ser ajustado antes da Vercel. O backend usa sessao e CSRF; apenas trocar a URL nao resolve a comunicacao entre dominios.

A estrategia prevista e usar rewrites da Vercel para encaminhar as rotas do backend pelo dominio do frontend. A configuracao deve abranger todas as rotas usadas, preservar cookies e nao armazenar respostas de autenticacao em cache. Depois, validar cadastro, login, sessao, logout e CSRF no navegador.

O cookie de producao e Secure; teste o deploy com HTTPS. O perfil local continua com SameSite=Lax. Nao desative CSRF para publicar.

## Verificar localmente

```powershell
.\mvnw.cmd test
docker build -t espelhovital-back .
docker run --rm --env-file .env.render -e PORT=8081 -p 8081:8081 espelhovital-back
```

Crie `.env.render` localmente com as variaveis necessarias, uma por linha, e nao versione esse arquivo. Para testar o perfil prod no navegador, use HTTPS; cookies Secure nao devem depender de HTTP local.
