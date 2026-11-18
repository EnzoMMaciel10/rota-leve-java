# Spring AI real — validação do Rota Leve

Concluído em 2026-11-17T16:49:41-03:00.

**Resultado: funcionalidade de explicação com LLM ativada e validada na API local.**

O projeto usa Spring AI 1.1.2 com o provedor nativo Ollama e o modelo qwen2.5:1.5b. A geração acontece no computador, sem chave de API externa. A aplicação está na porta 8080, com RabbitMQ e IA habilitados.

## Como iniciar novamente

Na pasta do projeto, execute `iniciar-ia.cmd`. O iniciador prepara os serviços e usa o modelo já baixado. O modo sem IA continua disponível em `iniciar-integracoes.cmd`.

## Verificações executadas

- Compilação e pacote Maven concluídos com sucesso.
- 59 testes aprovados, incluindo três chamadas ao modelo real e testes de rejeição de respostas incorretas.
- Um teste de RabbitMQ exclusivo ignorado nesta suíte; essa integração já havia sido validada separadamente com o broker real.
- Duas chamadas HTTP à explicação na API 8080 aprovadas, com dados temporários próprios removidos ao terminar.
- Uma rota salva da conta de demonstração foi explicada pelo próprio Swagger: HTTP 200, fonte SPRING_AI e textoGerado preenchido.

## Resultados com o modelo real

| Caso | Caminhada | Viagem total | Nota | Fonte | Tempo observado |
|---|---:|---:|---:|---|---:|
| Controlado: COMPATIVEL | 300 m | 30 min | 90 | SPRING_AI | 36.884 s |
| Controlado: BLOQUEADA | 1100 m | 30 min | 0 | SPRING_AI | 4.738 s |
| Controlado: INSUFICIENTE | 350 m | 30 min | 55 | SPRING_AI | 4.901 s |
| HTTP: COMPATIVEL | 300 m | 28 min | 93 | SPRING_AI | 4.279 s |
| HTTP: BLOQUEADA | 1100 m | 25 min | 0 | SPRING_AI | 10.146 s |

Os tempos dependem da carga do computador e do carregamento do modelo. Na avaliação controlada, a primeira resposta levou cerca de 37 segundos; as duas seguintes ficaram em torno de 5 segundos. Não são compromissos de latência para outros computadores.

## Exemplo gerado no Swagger

> A rota prevê 300 metros a pé, 28 minutos de viagem no total e nota de adequação de 93 pontos.

A resposta também mantém a classificação e os motivos calculados pelo Java. A captura Spring-AI-Funcionando.png mostra o retorno real; Resposta-Spring-AI.json contém a resposta sem token de autenticação.

## Qualidade e privacidade

A primeira versão do prompt foi rejeitada na revisão porque acrescentava afirmações sobre segurança e interpretava incorretamente dados insuficientes. A versão final mantém a conclusão crítica no Java, envia apenas cinco fatos agregados ao LLM e usa a IA para resumir os números. O campo textoGerado permite identificar exatamente a parte produzida pelo modelo.

O backend valida tamanho, números, unidades e classes de afirmações sem fundamento. Uma resposta inválida pode ser gerada novamente uma vez. Se continuar inadequada, ela é descartada e a API informa FALLBACK_LLM_RESPOSTA_INVALIDA, sem expor o rascunho rejeitado. Falha de conexão usa FALLBACK_LLM_INDISPONIVEL.

As cinco respostas da avaliação controlada/HTTP foram conferidas pelo assistente contra os dados estruturados: números e unidades corretos, duração total identificada, sem garantias ou equipamentos inventados. Os três estados permanecem explícitos no texto montado pelo Java. Essa revisão não substitui a revisão do grupo nem comprova comportamento correto para toda entrada possível.

O modelo não recebe nome, e-mail, condição de saúde, endereços ou texto livre de relatos. A API mantém a avaliação e o aviso disponíveis para conferência.

## Ambiente e limites

- Spring Boot 3.5.14, Spring AI 1.1.2 e Ollama 0.40.16.
- Modelo qwen2.5:1.5b, identificador 65ec06548149, aproximadamente 986 MB.
- Execução local em CPU, com limite de 4 GB de memória e 4 CPUs no contêiner.
- Modelo publicado somente em 127.0.0.1:11434; imagem fixada por digest no Compose.
- As rotas permanecem sintéticas. Ativar o LLM não transforma esses dados em trajetos reais.

## Referências

[Spring AI e Ollama](https://docs.spring.io/spring-ai/reference/1.0/api/chat/ollama-chat.html), [execução oficial em Docker](https://docs.ollama.com/docker) e [modelo Qwen 2.5](https://ollama.com/library/qwen2.5:1.5b).
