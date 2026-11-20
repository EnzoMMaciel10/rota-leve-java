# Rota Leve — Java Advanced

**Vídeo pitch:** [apresentação do Rota Leve — 4min42s](https://youtu.be/ISxR0zD9vo4).  

| Integrante | RM |
|---|---|
| Enzo Monteiro Maciel | 563734 |
| Matheus de Almeida Sousa | 563557 |
| Paulo Estalise | 563811 |
| Gabriel Bebé Silva | 562012 |
| Emanuel Italo | 561337 |

Turma dos cinco integrantes: **2TDSPO**, confirmada pelo grupo.

Backend da CitizenTech Rota Leve: avalia opções de transporte conforme um perfil de mobilidade, considera relatos sobre barreiras e avisa quem possui uma rota salva afetada. O foco é a camada de acessibilidade. As rotas candidatas vêm de um provedor; o projeto não implementa um motor de roteamento urbano.

## Escopo e situação da entrega

Este repositório contém a API Java, seu contrato OpenAPI, testes e configuração de execução. O modo `demo` usa rotas e pontos **sintéticos**, sem afirmar condições reais do transporte de São Paulo. Ele permite demonstrar o fluxo local sem chaves externas. No modo padrão sem LLM, a explicação é determinística e identificada como tal; para demonstrar o requisito de Spring AI é necessário habilitar o LLM real.

O LLM real foi ativado e validado em 17/11/2026 com **Spring AI + Ollama + Qwen 2.5 local**. Use `iniciar-ia.cmd` para iniciar esse modo, sem chave externa. Veja a [validação da funcionalidade](docs/VALIDACAO-SPRING-AI.md).

O aplicativo React Native do grupo foi integrado a esta API e é entregue em um pacote separado. Login, perfil, rotas, relatos com foto e notificações usam o servidor. A interface foi validada na versão web do React Native; essa validação não substitui teste em aparelho ou emulador. O Java está publicado neste repositório público, com nomes, RMs e turma 2TDSPO confirmados pelo grupo. O link do pitch fornecido pelo grupo está registrado neste README e em `ENTREGA.txt`. O envio pelo Portal e a apresentação presencial permanecem a cargo do grupo. A API, o RabbitMQ e o modelo devem estar disponíveis durante a apresentação; hospedagem em nuvem é opcional no enunciado de Java. Veja o [guia de fechamento de Java](docs/FECHAMENTO-JAVA.md), [requisitos e pendências](docs/REQUISITOS.md), [contrato para o app](docs/INTEGRACAO-MOBILE.md) e [guia de apresentação](docs/GUIA-APRESENTACAO.md).

## Tecnologias e organização

Java 17, Spring Boot 3.5.14, Spring Data JPA, Bean Validation, Spring Security com JWT, Spring AI 1.1.2, RabbitMQ e Swagger/OpenAPI. H2 permite execução local; o perfil `prod` possui configuração Oracle. Maven Wrapper 3.9.16 acompanha o projeto.

- `controller`: endpoints REST e autorização.
- `dto`: entradas validadas e respostas sem expor entidades ou senha.
- `model` e `repository`: entidades, relacionamentos e acesso paginado ao banco.
- `service`: regras de mobilidade, relatos, pontuação e rotas salvas.
- `routes`: provedores de candidatas, score, buscas privadas e explicações.
- `security`: autenticação, emissão e validação de JWT.
- `messaging`: evento, outbox, publicação e consumo de notificações.

As contas públicas são criadas como `CIDADAO`. A permissão `GESTOR` é administrativa. A autorização também verifica o proprietário dos dados, pois possuir um token não autoriza consultar ou alterar dados de outra pessoa.

Veja os diagramas, relacionamentos e regras do score em [arquitetura](docs/ARQUITETURA.md).

## Executar no Windows

Pré-requisito: JDK 17 ou superior, com `JAVA_HOME` configurado. Para a demonstração com **RabbitMQ e Spring AI reais**, tenha também Docker Desktop instalado e execute na pasta deste README:

```powershell
.\mvnw.cmd clean verify
.\iniciar-ia.cmd
```

O iniciador prepara RabbitMQ, Ollama e o modelo local antes de iniciar a API. Para executar somente o modo padrão, com explicação determinística e sem RabbitMQ externo, use:

```powershell
.\mvnw.cmd spring-boot:run
```

Mantenha uma única instância da API na porta `8080`. Se o iniciador já estiver executando a aplicação, use essa instância; iniciar outra pelo IntelliJ na mesma porta causa a mensagem `Port 8080 was already in use`.

No Linux/macOS, use `sh ./mvnw clean verify` e `sh ./mvnw spring-boot:run`.

Na primeira compilação, o Maven baixa dependências e precisa de acesso à internet. Depois de iniciado, o modo local padrão não precisa de Google, OpenAI, RabbitMQ nem banco externo. O H2 em memória reinicia junto com o processo.

- API: `http://localhost:8080`.
- Swagger: `http://localhost:8080/swagger-ui/index.html`.
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`.
- Disponibilidade: `http://localhost:8080/actuator/health`.

Contas criadas **apenas no perfil demo**:

| Papel | E-mail | Senha local |
|---|---|---|
| Cidadão | `cidadao@rotaleve.local` | `Cidadao@123` |
| Gestor | `gestor@rotaleve.local` | `Gestor@123` |

Faça login, copie `accessToken` e use o botão **Authorize** do Swagger com o token. As demais chamadas protegidas usam `Authorization: Bearer SEU_TOKEN`.

Endpoints principais: `POST /api/v1/auth/login`, `PUT /api/v1/perfis/me`, `GET /api/v1/pontos`, CRUD em `/api/v1/relatos`, `POST /api/v1/rotas/busca`, CRUD em `/api/v1/rotas/salvas` e `GET /api/v1/notificacoes`. A explicação fica em `POST /api/v1/rotas/salvas/{id}/explicacao`. Os detalhes e as permissões estão no [contrato Mobile](docs/INTEGRACAO-MOBILE.md).

## RabbitMQ real com Docker Compose

No Windows, o atalho [iniciar-integracoes.cmd](iniciar-integracoes.cmd) prepara o RabbitMQ e inicia a API no computador, com IA desativada. Veja o [guia de ativação](docs/ATIVAR-INTEGRACOES.md). Esse modo usa Java no computador e apenas o broker em contêiner. Em 17/11/2026, o teste `RabbitIntegracaoTests` passou com RabbitMQ real: publicação, consumo e notificação persistida foram confirmados.

Com Docker Desktop instalado e em execução:

```powershell
Copy-Item .env.example .env
docker compose up --build
```

O Compose sobe a API em `8080` e o RabbitMQ oficial em `5672`, com painel em `http://localhost:15672`. Os valores locais de exemplo são usuário `rotaleve` e senha `rotaleve-dev`. A API e o broker persistem dados em volumes. Os mesmos usuários demo são criados na primeira inicialização do banco.

O Compose ativa `RABBIT_ENABLED=true`. A opção escolhida para o requisito distribuído é **A: RabbitMQ**, com evento publicado e consumido de forma assíncrona. O RabbitMQ usa a imagem `4.4.0-management`; as filas e exchanges são duráveis. O modo local sem broker usa outro adaptador e **não demonstra RabbitMQ**.

As mudanças de ponto geram um evento persistido na mesma transação do domínio (outbox). Um processo separado tenta entregá-lo; o consumidor grava notificações para rotas salvas relacionadas. A identificação do evento evita duplicação em nova entrega. Falhas possuem novas tentativas e uma fila de mensagens rejeitadas. O aviso entregue é uma notificação consultável pela API, sem envio de push ou e-mail externo.

Para parar os serviços mantendo seus dados:

```powershell
docker compose down
```

## Spring AI real

Para usar o modelo local gratuito, sem chave de API, execute na pasta do projeto:

```powershell
.\iniciar-ia.cmd
```

O iniciador prepara RabbitMQ, Ollama e o modelo `qwen2.5:1.5b`, e inicia a API com o provedor nativo do Spring AI. O modelo e a imagem são baixados na primeira preparação; as gerações seguintes acontecem no computador. O modo sem IA permanece disponível em `iniciar-integracoes.cmd`. Veja o [guia completo de Spring AI local](docs/SPRING-AI-REAL.md).

Depois de alterar código ou dependências, encerre a API e gere novamente o JAR com `mvnw.cmd clean verify` antes de usar o iniciador. A API precisa ser reiniciada para trocar de configuração de IA.

A explicação fica em `POST /api/v1/rotas/salvas/{id}/explicacao`. O Java calcula a compatibilidade e o score. A IA reescreve as métricas em português; o backend mantém a conclusão e os dados calculados. O pedido ao modelo não contém e-mail, condição de saúde, origem/destino nem texto livre dos relatos. O tempo informado é o total da viagem, incluindo transporte e espera.

A alternativa OpenAI continua disponível com `iniciar-integracoes.cmd -ComIA -ProvedorIA openai`, usando uma chave preenchida localmente em entrada protegida ou já definida no ambiente. Não é necessária para a execução local escolhida neste projeto. `.env` é lido pelo Compose; Java e Maven não carregam esse arquivo automaticamente.

## Rotas reais e limites dos dados

`ROUTES_PROVIDER=demo` fornece candidatos sintéticos para a apresentação. Para consultar Google, configure `ROUTES_PROVIDER=google` e `GOOGLE_MAPS_API_KEY` no ambiente. A chave deve possuir acesso ao serviço de rotas utilizado e faturamento habilitado conforme as regras da conta Google.

A consulta externa retorna candidatos de transporte; o Rota Leve acrescenta sua avaliação. Dados de transporte não garantem acesso sem degraus, elevadores funcionando, assentos ou a condição de cada calçada. Neste protótipo, o adaptador Google deixa a acessibilidade como `DADOS_INSUFICIENTES`: o vínculo automático entre os trechos reais e o cadastro local de pontos ainda precisa ser desenvolvido. O vínculo com pontos críticos é demonstrado nas rotas sintéticas. A nota é uma heurística do protótipo, não uma certificação de acessibilidade nem uma previsão obtida por ML.

## Oracle e configuração externa

O perfil `prod` exige Oracle existente e os serviços escolhidos pelo grupo. Exemplo de configuração, substituindo valores antes de executar:

```powershell
$env:SPRING_PROFILES_ACTIVE = "prod"
$env:DB_URL = "jdbc:oracle:thin:@//SEU_HOST:1521/SEU_SERVICE_NAME"
$env:DB_USERNAME = "SEU_USUARIO"
$env:DB_PASSWORD = "SUA_SENHA"
$env:JWT_SECRET = "SEU_SEGREDO_ALEATORIO_COM_PELO_MENOS_32_BYTES"
$env:OPENAI_API_KEY = "SUA_CHAVE"
$env:GOOGLE_MAPS_API_KEY = "SUA_CHAVE"
$env:RABBIT_HOST = "SEU_HOST_RABBIT"
$env:RABBIT_USER = "SEU_USUARIO"
$env:RABBIT_PASSWORD = "SUA_SENHA"
.\mvnw.cmd spring-boot:run
```

O perfil `prod` não cria as contas demo. Para provisionar o primeiro gestor, forneça `BOOTSTRAP_GESTOR_EMAIL` e `BOOTSTRAP_GESTOR_SENHA` na inicialização de um ambiente vazio, conforme a configuração do serviço. Remova esses valores depois do provisionamento.

`ddl-auto=update` facilita a preparação acadêmica de um schema dedicado. Antes de operar com dados reais, substitua a evolução automática por migrações versionadas e use `DB_DDL_AUTO=validate`. A aplicação não cria o servidor Oracle e não inclui o trabalho de PL/SQL/MongoDB das outras disciplinas. `open-in-view=false` mantém o acesso ao banco nos serviços.

As credenciais locais do exemplo não devem ser reutilizadas em um ambiente publicado. Defina um segredo JWT próprio e use HTTPS no endereço da API consumido pelo app. Não publique `.env`, chaves nem credenciais no repositório.

## Verificação e apresentação

Em uma nova rodada de 17/11/2026, **31 casos HTTP foram aprovados** na API em execução, e a busca foi conferida pelo próprio Swagger. O fluxo de alerta foi repetido com RabbitMQ real. Veja o [relatório da bateria HTTP e do teste no Swagger](docs/TESTES-HTTP-17-11.md).

Execute `clean verify` para conferir compilação e testes antes de entregar. O Dockerfile compila o pacote com `-DskipTests`; portanto a construção da imagem não substitui essa verificação. Em 18/11/2026, a integração Mobile foi validada com **62 testes Java aprovados e dois testes externos opcionais ignorados**, além de verificações do cliente e do fluxo no navegador. Veja [validação Mobile](docs/VALIDACAO-MOBILE-18-11.md). A rodada anterior de 17/11 aprovou 59 testes, incluindo três cenários com o Ollama real; nesta integração a IA real também foi conferida na interface do app. Os casos de IA da suíte exigem `OLLAMA_INTEGRATION_TEST=true` e modelo preparado. Veja [resultados de Spring AI](docs/VALIDACAO-SPRING-AI.md) e [histórico de validação](docs/VALIDACAO.md).

Para validar a parte distribuída, use o Compose, salve uma rota que inclua um ponto, registre um problema nesse ponto e consulte as notificações do cidadão. Para validar Spring AI, habilite o provedor real e solicite a explicação de uma rota. Para validar Mobile, execute essas mesmas operações a partir do aplicativo desenvolvido pelo grupo.

Há também um teste opcional de integração RabbitMQ, ignorado na suíte padrão. Ele exige broker/vhost exclusivo, sem a aplicação consumindo as mesmas filas. Configure host, usuário, senha e, se necessário, `SPRING_RABBITMQ_VIRTUAL_HOST`; depois execute:

```powershell
$env:RABBIT_INTEGRATION_TEST = "true"
.\mvnw.cmd -Dtest=RabbitIntegracaoTests test
Remove-Item Env:RABBIT_INTEGRATION_TEST
```

Esse teste prepara seu domínio em H2 em memória, publica um evento real e aguarda o consumidor. Sua execução com RabbitMQ real foi registrada em 17/11/2026; ele continua opcional na suíte padrão para permitir a verificação sem broker externo.

## Identificação e entrega

O pacote de submissão `GS_2_JAVA.zip` contém somente `IDENTIFICACAO.txt`, com os integrantes, RMs e turma, e `ENTREGA.txt`, com os links abaixo. O código-fonte e este README ficam no repositório público:

- Repositório público: [Rota Leve Java](https://github.com/EnzoMMaciel10/rota-leve-java).
- Vídeo pitch fornecido pelo grupo: [apresentação do Rota Leve](https://youtu.be/ISxR0zD9vo4).
- Deploy, se houver: **NÃO REALIZADO NESTE PACOTE**.

O enunciado, páginas 62–66, define os seis requisitos Java e os entregáveis; a página 67 exige demonstração com o app Mobile na apresentação, e a página 68 traz as regras do vídeo. Confira a data da turma no Portal do Aluno.

## Uso de IA e referências

Foi utilizada assistência de IA (Codex/OpenAI) para analisar o enunciado, comparar os projetos anteriores, produzir código, documentação e apoiar verificações. O grupo deve revisar, adaptar, executar e compreender o código antes da apresentação. A assistência ao desenvolvimento é distinta da funcionalidade de Spring AI executada pela aplicação.

O padrão de organização foi baseado nos projetos anteriores do usuário: **CLYVO Vet / Sprint 4** (camadas, cadastro, papéis e Oracle), **CP5 Parte 2** (RabbitMQ e serviços) e **OrbitGuard** (DTOs, API REST e erros). Os projetos originais foram preservados, sem copiar credenciais. A nova aplicação usa Boot 3.5 para compatibilidade com a linha Spring AI 1.1 escolhida.

Referências técnicas: [Spring AI](https://docs.spring.io/spring-ai/reference/1.1/api/chatclient.html), [Spring Security JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html), [RabbitMQ](https://www.rabbitmq.com/docs), [imagem oficial RabbitMQ](https://hub.docker.com/_/rabbitmq), [imagem oficial Maven](https://hub.docker.com/_/maven) e [Google Routes para transporte público](https://developers.google.com/maps/documentation/routes/transit-route).
