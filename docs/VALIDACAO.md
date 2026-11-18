# Validação da entrega

## Spring AI real ativado em 17/11/2026

A versão com provedor nativo Ollama e Qwen 2.5 local passou em 59 testes, incluindo três cenários com geração real, duas explicações HTTP na porta 8080 e uma explicação exibida pelo Swagger. A IA pode ser iniciada por `iniciar-ia.cmd`, sem chave externa. Veja [VALIDACAO-SPRING-AI.md](VALIDACAO-SPRING-AI.md). Os registros abaixo descrevem as etapas anteriores.

A rodada HTTP mais recente aprovou 31 casos, confirmou a busca pelo Swagger e repetiu o alerta no RabbitMQ real. Os resultados detalhados estão em [TESTES-HTTP-17-11.md](TESTES-HTTP-17-11.md).

## Atualização de 17/11/2026: RabbitMQ real

O teste opcional `RabbitIntegracaoTests` foi executado com `RABBIT_INTEGRATION_TEST=true` e passou: **1 teste, zero falhas, zero erros e nenhum ignorado**. O broker real foi RabbitMQ 4.4.0 Management em Docker Engine 29.4.0, limitado às portas locais 5672 e 15672.

O teste gravou a alteração e o evento em uma transação, publicou no broker, confirmou a entrega, consumiu a mensagem e persistiu a notificação para a rota afetada. A fila principal e a DLQ foram verificadas. Somado aos 44 testes aprovados na suíte anterior, há **45 testes aprovados nas duas execuções**.

Nessa primeira etapa de RabbitMQ, a IA generativa havia sido adiada. Ela foi posteriormente ativada e validada na rodada descrita no início deste documento. A seção abaixo registra a validação original de 16/11/2026.

### Fluxo HTTP com broker real

O iniciador `iniciar-integracoes.cmd` foi executado na pasta do projeto em Java Advanced. A API iniciou em `http://localhost:8080`, com `RABBIT_ENABLED=true` e IA desativada. Chamadas HTTP reais confirmaram login, busca, salvamento, relato, notificação, reordenação das alternativas e resolução pelo gestor. O contador de confirmações do broker passou de 1 para 3: os eventos de problema e resolução foram consumidos. Ao fim da verificação, a fila `rotaleve.alertas` tinha um consumidor e zero mensagens pendentes; a DLQ estava vazia.

O resultado sem credenciais está em [RESULTADO-RABBITMQ.json](RESULTADO-RABBITMQ.json). A API e o broker foram deixados disponíveis para uso local. Para encerrar a API, pressione `Ctrl+C` na janela aberta pelo iniciador. Para parar somente o broker, use `docker compose -p rotaleve stop rabbitmq` na pasta do projeto. Os dados da demonstração Java ficam no H2 em memória e reiniciam quando a API encerra.

### Recuperação do ambiente Docker

O Docker Desktop 4.69.0 falhava ao acessar um endpoint temporário antigo, situação também descrita no [relato do Docker](https://github.com/docker/desktop-feedback/issues/532). Com o Docker encerrado, foram preservados os diretórios temporários `Docker/run` e `docker-secrets-engine` em cópias irmãs com sufixo `.rotaleve-backup-20261117-084018`; o Docker recriou esses endpoints e iniciou normalmente. Imagens, volumes, disco WSL e configurações não foram redefinidos.

Executada em 16/11/2026 no projeto entregue. O comando Maven `verify` terminou com sucesso e gerou o JAR executável. Compilação com Java 22.0.2 e alvo Java 17, confirmado no bytecode (versão 61).

## Testes automáticos

| Suíte | Aprovados | Ignorados | O que foi verificado |
|---|---:|---:|---|
| CoreFluxosTests | 9 | 0 | JWT real, papel, cadastro, validações, perfil, propriedade, confirmações, expiração e resolução |
| AcessibilidadeServiceTest | 6 | 0 | Escadas, limite de caminhada, cadeirante, dados desconhecidos, elevador quebrado e verificação antiga |
| RotasFluxosTests | 10 | 0 | Busca, CRUD de rotas salvas, privacidade, consulta expirada, alteração do score, nenhuma opção compatível, perfil ausente e remoção de avisos |
| MensageriaTests | 8 | 0 | Consumidor idempotente, publicação confirmada, falha e novas tentativas |
| OutboxTransacionalTests | 2 | 0 | Persistência no commit e ausência de evento quando há rollback |
| ProvedorGoogleTests | 5 | 0 | Contrato TRANSIT, alternativas, caminhada, acessibilidade desconhecida e falhas HTTP/configuração |
| SpringAiIntegracaoTests | 4 | 0 | ChatClient e OpenAiChatModel reais contra protocolo HTTP simulado localmente; payload sem dados pessoais e fallback explícito |
| RabbitIntegracaoTests | 0 | 1 | Execução opcional; exige broker real e não foi habilitada |
| **Total** | **44** | **1** | **Zero falhas e zero erros** |

Os testes Google usam respostas controladas. O teste Spring AI executa as classes da biblioteca contra um servidor HTTP local que simula o provedor. Isso valida o código de integração, sem comprovar disponibilidade, credenciais, qualidade de um LLM externo ou condições reais do transporte.

## Conferência do aplicativo executável

A API foi iniciada a partir do JAR, em porta local temporária. As chamadas HTTP reais confirmaram:

1. Saúde da aplicação e login JWT de cidadão e gestor.
2. Busca sintética Praça da Sé → Hospital das Clínicas e salvamento da alternativa pela Sé.
3. Relato de elevador quebrado e criação de notificação pelo processamento local assíncrono do outbox.
4. Nova busca priorizando a alternativa pela República; a rota salva original passou a incompatível.
5. Explicação determinística identificada como `REGRAS_LOCAIS_SEM_LLM`.
6. Marcação de leitura e resolução pelo gestor, seguida de nova avaliação compatível.
7. Swagger acessível e OpenAPI gerado com esquema Bearer.

A execução temporária foi encerrada. Os dados desse ensaio eram H2 em memória e não alteraram os projetos anteriores.

## Integrações que ainda exigem ambiente do grupo

Na rodada original de 16/11, as integrações externas não foram validadas. Em 17/11, o broker RabbitMQ real e o contêiner correspondente foram validados conforme a atualização no início deste documento. Conta/chave Google, LLM externo com chave válida, banco Oracle remoto, contêiner da API, Azure e app Mobile continuam sem validação ponta a ponta. O contrato exportado está em [openapi.json](openapi.json). As instruções de execução estão no README.
