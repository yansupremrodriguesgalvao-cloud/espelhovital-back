# Espelho Vital — Backend

## Rodar no IntelliJ

1. Use **File → New → Project from Version Control** e cole a URL deste repositório.
2. Abra o projeto como Maven e selecione **Java 17 ou superior** como Project SDK.
3. Na raiz do projeto, ao lado do `pom.xml`, crie um arquivo chamado `.env.local`. Coloque esta linha e substitua o exemplo pela senha atual do MySQL que o professor forneceu.

   ```properties
   DB_PASSWORD=SUA_SENHA_DO_AIVEN
   ```

4. Abra [EspelhovitalBackApplication.java](src/main/java/com/example/espelhovitalback/EspelhovitalBackApplication.java) e clique no botão verde ao lado de `main`.
5. Abra [localhost:8081/api/csrf](http://localhost:8081/api/csrf). Se aparecer um JSON com um token, o backend está rodando.

O projeto já usa o banco **`espelhovital`** no Aiven. O Git ignora o arquivo `.env.local`. Se a porta 8081 estiver ocupada, encerre a execução anterior do backend no IntelliJ antes de tentar novamente.

## Publicar no Render

1. No Render, escolha **New → Blueprint** e selecione uma cópia deste repositório no GitHub com a branch `main`. Use **Fork** no GitHub se precisar criar uma cópia na sua conta.
2. Em `DB_PASSWORD`, cole a mesma senha usada na execução local. Em `APP_FRONTEND_URL`, cole a URL HTTPS do frontend publicado, sem barra no final. Para testar só o backend, use `https://example.com` temporariamente.
3. Clique em **Deploy Blueprint**. Quando o serviço ficar **Live**, abra a URL do backend com `/api/csrf` no final e confirme que aparece um token.

O [guia de deploy](DEPLOY-RENDER.md) também explica como atualizar um serviço que já existe no Render.
