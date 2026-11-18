# Rota Leve — relatório dos testes executados

Este registro é anterior à ativação da IA. A integração com modelo real foi concluída depois, em 17/11/2026; veja [VALIDACAO-SPRING-AI.md](VALIDACAO-SPRING-AI.md).

Execução em 17/11/2026. Relatório consolidado em 2026-11-17T15:28:23-03:00.

**Resultado: aprovado.** Foram aprovados 31 casos HTTP, além da busca pelo Swagger e do fluxo completo de alerta com RabbitMQ real.

A bateria isolada fez 44 requisições principais contra a API em execução, com zero falhas. Os cinco passos de limpeza dos registros temporários retornaram 204. Buscas privadas temporárias seguem a expiração normal do sistema.

## Casos HTTP

| Caso | Respostas observadas | Resultado |
|---|---|---|
| API disponível e saudável | 200 | APROVADO |
| Cadastro público cria cidadão mesmo com papel GESTOR solicitado | 201 | APROVADO |
| Segundo cidadão sintético cadastrado | 201 | APROVADO |
| Cadastro duplicado ignora diferença entre maiúsculas e minúsculas | 409 | APROVADO |
| Login real emite JWT para os dois cidadãos | 200, 200 | APROVADO |
| Senha incorreta é recusada com 401 | 401 | APROVADO |
| Requisição sem JWT é recusada com 401 | 401 | APROVADO |
| JWT adulterado é recusado com 401 | 401 | APROVADO |
| Consulta do próprio usuário preserva o hash de senha | 200 | APROVADO |
| Busca sem perfil retorna 422 | 422 | APROVADO |
| Perfil vazio retorna 400 e erros padronizados | 400 | APROVADO |
| Perfil de cadeirante força evitar escadas | 200 | APROVADO |
| Perfil gestante válido e busca demo reordenada por compatibilidade | 200, 200 | APROVADO |
| Limite de 50 m informa ausência de opção compatível | 200, 200, 200 | APROVADO |
| Rota é salva por referência da busca gerada no servidor | 201 | APROVADO |
| Consulta, edição e paginação das próprias rotas salvas | 200, 200, 200 | APROVADO |
| Outro usuário recebe 404 ao consultar, editar ou excluir rota alheia | 404, 404, 404 | APROVADO |
| Outra pessoa não pode salvar alternativas da busca privada | 404 | APROVADO |
| Busca e alternativa inexistentes retornam 404 | 404, 404 | APROVADO |
| Cidadão não pode cadastrar ponto crítico | 403 | APROVADO |
| Gestor autentica e cadastra ponto crítico sintético | 200, 201 | APROVADO |
| Gestor atualiza o próprio ponto de teste | 200 | APROVADO |
| Relato sem foto é recusado com 400 | 400 | APROVADO |
| Relato válido altera somente o ponto sintético para PROBLEMA | 201, 200 | APROVADO |
| Autor não confirma o próprio relato | 409 | APROVADO |
| Confirmação repetida é idempotente e mantém a mesma expiração | 200, 200 | APROVADO |
| Outro cidadão não pode editar relato alheio | 403 | APROVADO |
| Cidadão não pode resolver relato como gestor | 403 | APROVADO |
| Pontos e relatos oferecem paginação funcional | 200, 200 | APROVADO |
| Explicação informa REGRAS_LOCAIS_SEM_LLM; IA permanece adiada | 200 | APROVADO |
| Ponto vinculado ao relato não pode ser excluído | 409 | APROVADO |

As respostas de erro dessa tabela são esperadas nos cenários negativos: entrada inválida, falta de autenticação, papel incorreto, acesso a recurso de outro usuário e conflito de domínio. Elas não representam falhas da execução.

## Swagger

O token antigo foi recusado com 401 por estar expirado. A autorização antiga foi removida, o login foi renovado e o Swagger recebeu a nova sessão de cidadão.

A busca Praça da Sé → Hospital das Clínicas respondeu 200 e retornou três alternativas sintéticas:

| Alternativa | Nota | Avaliação |
|---|---:|---|
| Pela Sé | 93 | Compatível com os dados conhecidos |
| Pela República | 90 | Compatível com os dados conhecidos |
| Com escadas e caminhada longa | 0 | Incompatível |

A captura Teste-Swagger-Rota.png registra a resposta real, sem exibir o token. O Swagger foi deixado autorizado com a busca visível.

## RabbitMQ real

O cenário criou uma rota temporária, relatou o elevador da Sé como quebrado, aguardou a notificação dessa rota, verificou que a busca passou a priorizar a República, marcou o aviso como lido e resolveu o relato com a conta de gestor. A nova avaliação voltou a priorizar a Sé. A rota temporária e seus avisos foram removidos; o relato de demonstração permanece no histórico como resolvido.

O contador de confirmações do broker passou de 9 para 11. A fila rotaleve.alertas ficou com 1 consumidor e 0 mensagens pendentes.

A API e o RabbitMQ permaneceram disponíveis. Nenhuma mudança foi feita no código da aplicação para executar essa bateria.

## Escopo e limites

Os trajetos e pontos da demonstração são sintéticos. Estes testes validam o backend local; não representam uma certificação de acessibilidade do transporte real.

A IA generativa permaneceu desativada, conforme solicitado. A explicação retornou REGRAS_LOCAIS_SEM_LLM. Google, Oracle remoto, Azure e aplicativo Mobile não fazem parte desta rodada.

As evidências JSON foram sanitizadas e não contêm senhas, tokens ou cabeçalhos de autenticação.
