# Correspondência com o enunciado

Fonte: documento fornecido pelo usuário, “2TDS Fevereiro - Global Solution 2026-2.pdf”, páginas 62–68. O escopo implementado neste pacote é Java Advanced. As disciplinas de banco, IA, Mobile, QA e DevOps têm entregas próprias.

| Requisito Java (p. 63) | Pontos (p. 64) | Aplicação no Rota Leve | Evidência a apresentar |
|---|---:|---|---|
| 1. API REST, 3 entidades relacionadas, CRUD JPA e paginação | 20 | Usuário, perfil, ponto crítico, relato, rota salva e notificação; repositórios JPA e listagens paginadas | Swagger e operações reais, banco persistindo alterações |
| 2. Bean Validation e erros globais padronizados | 10 | DTOs validados e tratamento com `@RestControllerAdvice` | Cadastro inválido, relato sem foto e recurso ausente |
| 3. JWT, dois papéis e autorização | 20 | `CIDADAO` e `GESTOR`, token Bearer e verificação de proprietário | 401 sem token, 403 no papel errado, proteção dos dados de outro usuário |
| 4. Spring AI integrado ao domínio | 20 | Explicação com provedor nativo Ollama e Qwen 2.5 local | Validado com três estados, duas chamadas HTTP e uma resposta no Swagger; iniciar com iniciar-ia.cmd |
| 5. Arquitetura distribuída, opção A ou B | 20 | **Opção A: RabbitMQ**, eventos de ponto crítico e avisos para rotas salvas | Broker ativo, publicação e consumidor assíncronos, evento persistido e notificação |
| 6. OpenAPI, README e consumo pelo app Mobile | 10 | Swagger, contrato e app React Native integrado em pacote separado | Consumo da API pelo app e demonstração ponta a ponta |

## O que os modos de execução comprovam

O modo padrão comprova o fluxo de domínio com banco H2, JWT e dados sintéticos. O adaptador local entrega eventos pelo processo de outbox, sem broker externo. Ele não é evidência do requisito RabbitMQ. A explicação determinística sem LLM também não comprova o requisito Spring AI.

O Compose ativa o broker e prepara a demonstração distribuída. O LLM local é iniciado por `iniciar-ia.cmd`, sem chave de API; os downloads são necessários apenas na preparação inicial. O perfil Oracle fornece configuração de conexão, mas não representa evidência de banco remoto conectado ou de deploy concluído.

## Checklist da entrega e da apresentação

- [x] Registrar os cinco nomes e RMs informados em `IDENTIFICACAO.txt`.
- [x] Registrar a turma **2TDSPO** dos cinco integrantes em `IDENTIFICACAO.txt`, conforme confirmação do grupo.
- [ ] Revisar e compreender o código com todos os integrantes.
- [x] Compilar e validar o Java localmente: 62 testes aprovados em 18/11/2026; dois testes externos opcionais ignorados na suíte, com IA e fluxo de avisos também exercitados no app.
- [x] Ativar e validar Spring AI com modelo local real, sem chave externa, em 17/11/2026; repetir a demonstração na apresentação do grupo.
- [x] Validar publicação e consumo pelo RabbitMQ real (teste de integração aprovado em 17/11/2026); preparar a demonstração do grupo com o iniciador fornecido.
- [x] Integrar o aplicativo React Native fornecido pelo grupo; fluxo no navegador validado em 18/11/2026.
- [ ] Ensaiar a demonstração ponta a ponta com o app integrado consumindo esta API; Expo Go ou emulador pode ser usado sem gerar APK próprio.
- [ ] Confirmar a execução do Java, RabbitMQ e LLM real no computador da apresentação e o acesso da API pelo app durante a demonstração.
- [x] Publicar o Java no repositório público [EnzoMMaciel10/rota-leve-java](https://github.com/EnzoMMaciel10/rota-leve-java) e registrar o link no README e em `ENTREGA.txt`.
- [x] Registrar o [vídeo pitch fornecido pelo grupo](https://youtu.be/ISxR0zD9vo4).
- [x] Atualizar README e `ENTREGA.txt` com os links reais.
- [x] Preparar `GS_2_JAVA.zip` somente com `IDENTIFICACAO.txt` e `ENTREGA.txt`; o código-fonte fica no GitHub.
- [x] Abrir a página do vídeo no navegador e conferir a duração de **4min42s**, dentro do limite de cinco minutos. A conferência não substitui a avaliação do conteúdo pelo professor.
- [x] Conferir os dois arquivos TXT, os links e a integridade do ZIP.
- [ ] Enviar no Portal do Aluno no prazo da turma.

O checklist acima cobre somente Java Advanced. A publicação de um aplicativo instalável no Firebase App Distribution e o hash na tela Sobre pertencem à disciplina de Mobile. Oracle/Azure, hospedagem em nuvem e roteamento real não são requisitos obrigatórios de Java nas páginas 62–68; configurações opcionais existentes foram preservadas.

## Apresentação e vídeo

A página 67 informa apresentação presencial entre 23 e 27/11, na aula da disciplina, valendo 30% da nota. Confirme a data efetiva com a turma. A demonstração ponta a ponta com Mobile é um item específico; também serão avaliadas modelagem, JWT, Spring AI, parte distribuída e colaboração com o cidadão.

A página 68 limita o pitch a 5 minutos e pede link público ou não listado com acesso aberto. Teste o link em uma janela anônima antes da submissão. Não deixe campos “PENDENTE” ou exemplos de URL na entrega final.
