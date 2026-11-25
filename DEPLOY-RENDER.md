# Publicar o backend no Render com MySQL do Aiven

1. Envie o projeto para o GitHub, incluindo o arquivo `render.yaml`.
2. No Render, clique em **New → Blueprint**, conecte o repositório do backend e selecione a branch.
3. Preencha os dois campos abaixo e clique em **Deploy Blueprint**.
4. Quando o serviço estiver **Live**, abra a URL do backend com `/api/csrf` no final. Deve aparecer uma resposta JSON com o token.

| Campo no Render | O que preencher |
| --- | --- |
| `DB_PASSWORD` | Senha atual do usuário do MySQL. No Aiven, copie o campo **Password** em **Connection information**. |
| `APP_FRONTEND_URL` | URL HTTPS do frontend publicado, sem barra final, por exemplo `https://seu-frontend.vercel.app`. Se o frontend ainda não estiver publicado, use `https://example.com` temporariamente e troque esse valor em **Environment** depois. |

O Blueprint já configura Docker, o plano Free e a verificação de saúde. O backend usa o banco `espelhovital` do Aiven informado para a turma, com usuário `avnadmin` e TLS obrigatório. O Flyway cria as tabelas automaticamente se o banco estiver vazio. Se já houver tabelas sem histórico do Flyway, use um banco vazio ou revise o schema antes de publicar.

Se o serviço já existe no Render, preencha os mesmos campos em **Environment** e publique a nova versão. Se já houver `DB_URL`, `DB_USERNAME` ou `APP_CORS_ORIGINS` antigos nesse serviço, remova-os para usar os padrões atuais.

### Se você tiver seu próprio banco no Aiven

Em **Environment**, adicione `DB_USERNAME` com o valor de **User** e `DB_URL` montada com **Host**, **Port** e **Database name**. Atualize `DB_PASSWORD` com a senha desse banco.

```text
jdbc:mysql://HOST:PORTA/NOME_DO_BANCO?sslMode=REQUIRED
```

Guarde a senha em **Environment** no Render. Caso o Aiven retorne `Access denied`, confira se a senha atual pertence ao usuário do banco configurado.

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

Crie `.env.render` localmente com `DB_PASSWORD` e `APP_FRONTEND_URL`, uma por linha, e nao versione esse arquivo. Para testar o perfil prod no navegador, use HTTPS; cookies Secure nao devem depender de HTTP local.
