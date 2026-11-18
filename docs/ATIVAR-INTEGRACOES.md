# Ativar o RabbitMQ e iniciar a API no Windows

**Validado em 17/11/2026:** o iniciador executou a API, o teste Rabbit real passou e o fluxo HTTP de relato → evento → notificação → resolução foi aprovado. Veja [evidências da validação](VALIDACAO.md).

O iniciador abre a API Java no computador e usa um RabbitMQ real em um container. O banco e as rotas continuam no perfil `demo`, com dados sintéticos. A explicação de rota permanece local; a integração com IA generativa pode ficar para depois.

## Preparação

- Instale um **JDK 17 ou superior** e o **Docker Desktop** com suporte a containers Linux. O script confere essas ferramentas, sem alterar `PATH` ou `JAVA_HOME`.
- Deixe livres as portas `8080`, `5672` e `15672`. Se a API já estiver aberta, encerre essa execução com `Ctrl+C` primeiro.
- A primeira execução pode precisar de internet para baixar a imagem do RabbitMQ e compilar o projeto.

## Iniciar

Na pasta do projeto, execute:

```powershell
.\iniciar-integracoes.cmd
```

Também é possível abrir esse arquivo com dois cliques. Mantenha a janela aberta durante a demonstração.

O iniciador verifica o Docker e tenta iniciar o Docker Desktop se necessário. Em seguida, aguarda sua disponibilidade, sobe apenas o serviço `rabbitmq` do Compose e espera o broker ficar saudável. Quando esse RabbitMQ já estiver saudável, ele é reutilizado.

Depois, a API é iniciada com `RABBIT_ENABLED=true`. O arquivo `target/rota-leve.jar` é reutilizado quando existir. Se ainda não existir, o Maven Wrapper compila o projeto. Após alterar o código, atualize o executável antes de iniciar novamente:

```powershell
.\mvnw.cmd clean verify
.\iniciar-integracoes.cmd
```

Quando aparecer a mensagem `Started` da aplicação:

- Swagger: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html).
- Disponibilidade da API: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health).
- Painel do RabbitMQ: [http://localhost:15672](http://localhost:15672).

As credenciais locais do broker são **`rotaleve` / `rotaleve-dev`**. São os mesmos exemplos do README. Se você já usa credenciais diferentes nesse volume, defina `RABBIT_USER` e `RABBIT_PASSWORD` no ambiente antes de iniciar; o iniciador usa esses mesmos valores para o Compose e para a API.

Para autenticar no Swagger, use os usuários de teste do README e o token devolvido por `POST /api/v1/auth/login`.

## Verificar o fluxo

Use o roteiro do README para salvar uma rota, registrar um problema em um ponto dessa rota e consultar as notificações. Apenas iniciar o broker não comprova a entrega: é necessário observar o evento publicado, seu consumo e a notificação correspondente na API. Os resultados da validação devem ser registrados depois de executar esse fluxo.

## Encerrar

Pressione `Ctrl+C` na janela da API. O RabbitMQ permanece disponível para a próxima execução. Para parar o broker mantendo seus dados:

```powershell
docker compose -p rotaleve stop rabbitmq
```

## Se o Docker não iniciar

Abra o Docker Desktop e confira se o mecanismo de containers Linux está funcionando. O iniciador aguarda aproximadamente 90 segundos por essa disponibilidade e informa quando não consegue continuar. Ele não redefine configurações, reinstala componentes ou remove dados.

Se as portas `5672` ou `15672` estiverem sendo usadas por outro broker, encerre essa execução anterior antes de tentar novamente. O iniciador não encerra serviços de outros projetos.

Para consultar mensagens do broker:

```powershell
docker compose -p rotaleve logs rabbitmq
```

Se a porta da API estiver ocupada, encerre a instância anterior ou escolha outra porta apenas nessa janela:

```powershell
$env:PORT = '8081'
.\iniciar-integracoes.cmd
```

## Habilitar o Spring AI local

O fluxo padrão acima mantém a IA desativada. Para ligar a geração real sem chave de API:

Quando quiser usar o modelo real:

```powershell
.\iniciar-ia.cmd
```

Esse atalho usa o provedor nativo Ollama, prepara o contêiner local e baixa o modelo quando necessário. O script habilita `AI_ENABLED=true` e `AI_CHAT_MODEL=ollama` durante a execução. Veja [Spring AI real](SPRING-AI-REAL.md).

Para a alternativa OpenAI, use `iniciar-integracoes.cmd -ComIA -ProvedorIA openai`. Se `OPENAI_API_KEY` ainda não estiver no ambiente, o iniciador pede a chave em uma entrada protegida, sem mostrá-la ou gravá-la no projeto.

Ao encerrar, os valores de ambiente alterados pelo iniciador são restaurados. Nenhuma chave é impressa ou gravada em `.env`. Essa opção prepara a integração; a resposta do modelo deve ser verificada no fluxo de explicação de rota antes de registrar a integração como validada.
