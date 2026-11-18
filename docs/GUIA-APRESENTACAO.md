# Guia de apresentação — Rota Leve

## Antes da demonstração

Execute `clean verify` e inicie com `iniciar-ia.cmd`: esse modo prepara RabbitMQ, Ollama e a API com geração real local, sem chave externa. Faça uma explicação antes da apresentação para carregar o modelo. Confirme o login de cidadão e gestor, o perfil de mobilidade, os pontos sintéticos e a consulta de rotas. Prepare o app Mobile consumindo essa mesma API.

Não apresentem os exemplos da base demo como condições reais da cidade. Digam que os candidatos sintéticos tornam a demonstração reproduzível; mostrem separadamente a consulta Google se ela estiver configurada. O score do protótipo usa regras explícitas, e a IA gera a explicação.

## Roteiro de pitch, até 5 minutos

| Tempo | Conteúdo | Demonstração |
|---|---|---|
| 0:00–0:30 | Problema e proposta | Júlia precisa chegar ao pré-natal; evitar barreiras muda a escolha de rota. Identificar o cenário como sintético. |
| 0:30–1:05 | Login e segurança | Entrar no app, mostrar JWT e os papéis cidadão/gestor; explicar que o servidor confere permissões e propriedade. |
| 1:05–1:45 | Domínio e API | Mostrar usuário/perfil, relato/ponto e rota salva; demonstrar CRUD/paginação no Swagger e um exemplo de erro validado. |
| 1:45–2:25 | App integrado | Perfil, busca, comparação por score e motivos; salvar e alterar o nome de uma rota pela API. |
| 2:25–3:10 | Spring AI | Solicitar explicação real e mostrar a fonte SPRING_AI; explicar que o Java determina score/compatibilidade e o modelo resume métricas. |
| 3:10–4:10 | RabbitMQ e gestor | Criar relato com foto, mostrar aviso na rota salva, resolver como gestor e atualizar a busca. Exibir a fila/consumidor enquanto explicam o evento. |
| 4:10–4:45 | Arquitetura e cidadão | Explicar outbox, prevenção de avisos duplicados e como a informação ajuda a planejar o deslocamento. Relacionar ODS 10 e 11. |
| 4:45–4:55 | Entrega | Mostrar repositório público e README; encerrar com a proposta de valor. Reservar cinco segundos de margem. |

Ensaiem o fluxo antes de gravar. Carreguem o modelo antes da apresentação e deixem um relato e uma rota de demonstração preparados para não depender de digitação longa. Não atribuam resultados de impacto medidos a este protótipo. O foco deste roteiro é Java Advanced; o app participa como consumidor da API.

## Defesa dos seis requisitos Java

1. **Domínio e JPA.** Expliquem usuário/perfil, relato/ponto e rota salva/usuário. Mostrem como a referência de pontos na rota permite descobrir quem será avisado. Demonstrem paginação e CRUD pelos endpoints.
2. **Validação e erros.** Enviem um perfil vazio ou relato sem foto. Expliquem a diferença entre entrada inválida, recurso inexistente e acesso negado. Mostrem a resposta padronizada.
3. **JWT e papéis.** O login verifica a senha e emite um token assinado com expiração. O cliente envia Bearer; a API valida assinatura e prazo, depois verifica papel e proprietário. Cadastro público não recebe permissão de gestor. Mostrem 401 e 403.
4. **Spring AI.** Mostrem o serviço de explicação e os fatos estruturados enviados ao modelo. O LLM descreve esses fatos; ele não verifica elevadores, não atribui o score e não substitui a fonte de rotas.
5. **RabbitMQ.** Expliquem o evento de ponto, a outbox gravada na transação, o publicador e o consumidor. Mostrem a topologia e a notificação persistida. Entrega repetida não deve duplicar o aviso. Se o broker estiver indisponível, o evento precisa permanecer pendente para nova tentativa.
6. **Documentação e Mobile.** Abram Swagger, façam o fluxo no app e mostrem como o token é enviado. Um contrato escrito não comprova consumo pelo aplicativo; essa etapa deve estar funcionando na apresentação.

## Perguntas para ensaiar em grupo

- Por que o projeto não calcula o transporte do zero? Porque um provedor gera candidatos, e o produto é avaliar acessibilidade sobre esses candidatos.
- O que ocorre quando não há opção compatível? A resposta deve deixar a restrição explícita; não chamar uma rota bloqueada de acessível só porque ela obteve a maior nota.
- Como diferenciar “funciona” e “não sabemos”? Estado desconhecido é informação própria. Falta de relato não é prova de acessibilidade.
- Como o relato permanece atual? Confirmações e prazo de expiração limitam a validade da informação. Resolver um relato depende da autorização prevista para o gestor.
- O que acontece com duas confirmações da mesma pessoa? A operação é idempotente e não deve aumentar artificialmente a contagem.
- A notificação chega por push? O protótipo disponibiliza um aviso persistido pela API; push é uma integração futura.
- Como adaptar para uma nova mobilidade? Acrescentar a classificação e definir as restrições/pesos; manter os motivos da avaliação observáveis.
- Como passar H2 para Oracle? Ativar perfil, fornecer conexão e segredo, executar validações no schema dedicado; substituir criação automática por migrações antes de usar dados reais.

## Verificações finais do grupo

- [ ] Todas as pessoas conseguem explicar o fluxo, os relacionamentos, JWT, IA e mensageria.
- [ ] O app integrado consome a API no ambiente escolhido para a demonstração; em celular/emulador, o endereço aponta para o computador que executa Java.
- [ ] RabbitMQ e LLM reais estão disponíveis no momento da demonstração.
- [ ] Os dados de teste estão identificados como sintéticos.
- [ ] O pitch tem até 5 minutos e seu link abre sem solicitar acesso.
- [ ] Identificação, repositório e links de entrega foram preenchidos.
- [ ] A data de submissão foi conferida no Portal do Aluno.

Para esta apresentação de Java, o app pode executar pelo Expo Go ou emulador. Gerar APK, publicar no Firebase e configurar uma distribuição não são requisitos de Java. O backend pode executar localmente durante a demonstração; confirme o acesso pelo app e mantenha RabbitMQ e LLM reais ativos.
