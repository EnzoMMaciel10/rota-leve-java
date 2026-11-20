# Fechamento da entrega — Java Advanced

Escopo confirmado pelo grupo: somente a disciplina de Java. Referência: enunciado fornecido, páginas 62–68.

## Implementado e validado

| Requisito | Implementação |
|---|---|
| API REST e JPA | Entidades relacionadas, CRUD e listagens paginadas |
| Validação e erros | Bean Validation, DTOs e tratamento global padronizado |
| Segurança | JWT, cidadão/gestor e verificação de propriedade |
| Spring AI | Explicação de rota com Ollama/Qwen local real; fonte e fallback identificados |
| Arquitetura distribuída | Opção A: RabbitMQ, outbox, publicador e consumidor de avisos |
| Documentação e integração | Swagger, README e app do grupo consumindo o Java |

Foram aprovados 62 testes Java em 18/11/2026; dois testes externos opcionais ficaram ignorados na suíte padrão. IA real e notificações também foram conferidas pelo app na rodada de integração. Nenhum teste foi repetido apenas por esta revisão de documentação.

Código Java publicado em [EnzoMMaciel10/rota-leve-java](https://github.com/EnzoMMaciel10/rota-leve-java). O endereço real consta no README e em `ENTREGA.txt`.

Identificação completa: nomes e RMs dos cinco integrantes registrados, com turma **2TDSPO** confirmada pelo grupo.

## Entrega preparada e próximos passos do grupo

1. **Pitch registrado.** O grupo forneceu o [vídeo de apresentação](https://youtu.be/ISxR0zD9vo4), registrado no README e em `ENTREGA.txt`. A página do YouTube foi aberta no navegador e o player informa **4min42s**, dentro do limite de cinco minutos. Essa conferência de acesso e duração não representa avaliação integral do conteúdo gravado.
2. **Ensaio da apresentação.** Executar Java, RabbitMQ e IA real e mostrar o app consumindo a API. Todos precisam explicar modelagem, JWT, validação, IA, evento e benefício ao cidadão. A página 67 inclui demonstração ponta a ponta.
3. **Envio.** O ZIP foi preparado com identificação e links, sem campos de preenchimento. Enviar pelo Portal do Aluno no prazo da turma.

O pacote `GS_2_JAVA.zip` contém somente `IDENTIFICACAO.txt` e `ENTREGA.txt`. O código-fonte, a documentação e os testes ficam no repositório público. O envio no Portal e a apresentação presencial são ações do grupo. Não foi realizado deploy em nuvem.

## Como iniciar a demonstração

1. Tenha Java 17+ e Docker Desktop preparados no computador da apresentação.
2. Na raiz da API, execute `iniciar-ia.cmd`. Ele prepara RabbitMQ, Ollama, o modelo e a API. Faça isso antes do dia da apresentação para concluir eventuais downloads.
3. Abra o Swagger em http://localhost:8080/swagger-ui/index.html. O cidadão demo é `cidadao@rotaleve.local` / `Cidadao@123`; o gestor demo é `gestor@rotaleve.local` / `Gestor@123`.
4. Use o app integrado já fornecido pelo grupo como cliente da API. Para a apresentação ponta a ponta, ensaie com Expo Go ou emulador e configure o endereço do computador quando necessário. Não é preciso gerar um APK próprio.
5. Configure o perfil e busque **Praça da Sé → Hospital das Clínicas**, salve uma rota e solicite a explicação. Registre um problema no elevador usado pela rota, consulte o aviso, resolva como gestor e atualize a busca.

O modo `iniciar-integracoes.cmd` deixa a IA desativada; para comprovar Spring AI na apresentação, use `iniciar-ia.cmd` e confira a fonte `SPRING_AI`. Fonte de fallback não deve ser apresentada como uma geração bem-sucedida.

O ambiente demo usa H2 em memória: reiniciar a API reinicia os registros. Prepare os dados da demonstração após iniciar a aplicação. A rota é sintética; não representa condições reais da cidade. O score é calculado pelas regras Java e o LLM explica as métricas.

## Delimitação desta disciplina

A publicação de um app instalável no Firebase e o hash no Sobre são exigências da disciplina de Mobile. Oracle/Azure, hospedagem em nuvem, mapa real e visão computacional não aparecem como obrigatórios de Java nas páginas 62–68. As configurações opcionais foram preservadas. A integração com o app e sua demonstração continuam fazendo parte de Java.

Os links antigos do pacote original do front são referências históricas. Para esta entrega de Java, use o repositório e o vídeo registrados no README e em `ENTREGA.txt`.

O roteiro técnico está em `GUIA-APRESENTACAO.md`; o checklist detalhado está em `REQUISITOS.md`.
