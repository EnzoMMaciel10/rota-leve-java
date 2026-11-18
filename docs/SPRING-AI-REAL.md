# Spring AI com geração local — Rota Leve

Esta integração usa **Spring AI 1.1.2 e o provedor nativo Ollama** para explicar uma rota salva. O modelo é `qwen2.5:1.5b`. A geração acontece no computador, sem chave de API, conta de provedor ou cobrança por chamada de uma API externa.

O enunciado de Java Advanced, páginas 62–63, exige Spring AI e uma funcionalidade com LLM integrada à solução. Ele não exige OpenAI nem um provedor em nuvem. A escolha deste projeto é executar o LLM localmente e integrá-lo ao fluxo de avaliação da rota.

## Como iniciar

Pré-requisitos: Docker Desktop iniciado e JDK 17 ou superior. Na pasta principal do projeto:

```powershell
.\iniciar-ia.cmd
```

O iniciador prepara RabbitMQ e Ollama, verifica se o modelo já está disponível e baixa o que faltar. Na primeira execução, é necessário acesso à internet para obter a imagem do Ollama e o modelo. O modelo possui aproximadamente **986 MB**; a imagem e os demais componentes ocupam espaço adicional. Depois de baixados, a geração com esse modelo é local e não depende de uma API de IA na internet.

O script executa a API Java no computador e o Ollama em um contêiner. A porta do modelo é publicada somente em `127.0.0.1:11434`. O arquivo Compose limita esse contêiner a **4 GB de memória e 4 CPUs**, com uma geração por vez e um modelo carregado. Essa configuração foi escolhida para execução em CPU no computador com 16 GB de RAM; a velocidade depende também da memória disponível e dos outros programas em execução.

O modelo fica guardado no volume `ollama-data`. Não é necessário instalá-lo novamente a cada inicialização. O `qwen2.5:1.5b` é um modelo de 1,54 bilhão de parâmetros, quantizado em Q4_K_M, distribuído sob Apache 2.0, com suporte multilíngue inclusive português. A família informa suporte a mais de 29 idiomas. [Modelo oficial](https://ollama.com/library/qwen2.5:1.5b)

### API já aberta e alterações no código

Ativar a IA exige **iniciar novamente a API com essa configuração**. Uma API que já estava aberta com IA desativada continua usando a configuração anterior. Se a porta `8080` estiver ocupada, o iniciador informa o conflito; encerre a execução anterior no seu terminal antes de iniciar a nova.

Para um teste separado, sem interromper a execução anterior, é possível escolher outra porta no terminal:

```powershell
$env:PORT = "18087"
.\iniciar-ia.cmd
```

Nesse caso, use a API e o Swagger em `http://localhost:18087`, e remova a variável `PORT` depois de encerrar o teste se quiser voltar à porta padrão.

O iniciador reutiliza `target/rota-leve.jar` quando ele já existe. **Depois de modificar o código ou as dependências, gere o pacote novamente antes de iniciar:**

```powershell
.\mvnw.cmd clean verify
.\iniciar-ia.cmd
```

Não execute a nova compilação enquanto a mesma aplicação estiver usando esse JAR. O comando de verificação também executa os testes da suíte padrão; sua existência neste guia não significa que uma verificação específica tenha sido executada.

## Onde a IA entra no produto

O usuário autentica, configura seu perfil, pesquisa e salva uma rota. Depois, a API permite pedir uma explicação:

```http
POST /api/v1/rotas/salvas/{id}/explicacao
Authorization: Bearer SEU_TOKEN
```

O `{id}` é o identificador retornado ao salvar a rota, e a rota deve pertencer ao usuário autenticado. Essa chamada também pode ser feita pelo Swagger ou pelas requisições de exemplo do projeto.

Antes de chamar o modelo, o backend calcula o score e a compatibilidade por regras explícitas. O score é uma **heurística do protótipo**, não uma previsão de aprendizado de máquina. O LLM recebe apenas cinco informações:

| Campo enviado | Significado |
|---|---|
| `caminhadaMetros` | Caminhada estimada da alternativa |
| `duracaoMinutos` | Duração total da viagem, incluindo transporte, caminhada e espera |
| `score` | Nota calculada pelas regras do backend |
| `compatibilidade` | Conclusão calculada pelo Java, apresentada ao modelo em português |
| `demonstracao` | Identifica os dados sintéticos |

O pedido ao modelo não inclui nome, e-mail, condição de saúde, origem/destino nem o texto livre dos relatos. A consulta e a classificação da rota continuam sendo responsabilidade dos serviços Java. O LLM transforma as métricas em uma explicação curta em português; ele não altera o score, o perfil ou o estado dos pontos críticos.

A resposta mantém a conclusão crítica e a identificação de demonstração em frases escritas pelo Java. O LLM gera somente o resumo das métricas, apresentado também no campo `textoGerado`. O campo `texto` reúne essa conclusão, o resumo aprovado e os dados calculados. A geração local é real mesmo quando o cenário da rota é sintético; esses dois aspectos devem ser apresentados separadamente.

Antes de exibir o resumo, o backend verifica seu tamanho, números e termos que acrescentariam condições sem fundamento. Uma resposta rejeitada pode ser gerada novamente uma vez. Se continuar inválida, o texto do modelo é descartado e a API informa fallback. Esse controle nasceu da avaliação real: a primeira versão do prompt produziu afirmações não fundamentadas sobre conforto e acessibilidade. A decisão de compatibilidade continua no Java.

## Como identificar geração real e fallback

Confira o campo `fonte` da resposta:

| Valor | O que aconteceu |
|---|---|
| `SPRING_AI` | O LLM gerou o resumo e esse texto passou na validação do backend |
| `REGRAS_LOCAIS_SEM_LLM` | A IA estava desativada; o texto foi montado pelas regras locais |
| `FALLBACK_LLM_INDISPONIVEL` | A chamada ao LLM falhou ou não retornou texto; a API usou o texto local |
| `FALLBACK_LLM_RESPOSTA_INVALIDA` | O modelo respondeu fora dos critérios após as tentativas permitidas; o texto foi descartado |

Uma resposta HTTP `200` sozinha não comprova geração por IA. Verifique `fonte=SPRING_AI` e `textoGerado` preenchido. Esse valor identifica o caminho de execução; os filtros não representam prova geral de fidelidade. Em qualquer fallback, `textoGerado` fica nulo e os dados locais continuam disponíveis.

## Três casos para avaliar

Use rotas salvas do usuário de teste e preserve ou restaure as configurações do perfil e dos pontos depois de cada demonstração.

| Caso | Condição preparada | O que conferir na parte gerada pelo LLM |
|---|---|---|
| Normal | Uma alternativa com `COMPATIVEL_COM_DADOS_CONHECIDOS` | Português claro, números coerentes e ausência de promessa de segurança ou acessibilidade garantida |
| Bloqueado | Caminhada acima do limite, escada incompatível ou problema em ponto associado; classificação `INCOMPATIVEL` | A explicação não recomenda a alternativa como adequada nem ignora a incompatibilidade |
| Desconhecido | Verificação antiga, ponto em `DESCONHECIDO` ou avaliação real ainda sem enriquecimento; classificação `DADOS_INSUFICIENTES` | A explicação preserva a incerteza e não transforma ausência de informação em comprovação de acesso |

Em todos os casos, quando `demonstracao=true`, confira se o texto combinado identifica os dados sintéticos. A parte escrita pelo modelo está isolada em `textoGerado`; a conclusão crítica e o aviso pertencem ao Java. Não basta localizar números no bloco **Dados verificados**: confira o resumo gerado separadamente.

O modelo não recebe evidência sobre elevadores, bancos, calçadas ou cobertura. Portanto, afirmações novas sobre esses elementos não são justificadas pelos cinco campos enviados. O resumo permitido descreve a caminhada prevista, o tempo total da viagem e a nota, sem concluir se o trajeto é seguro ou adequado. A validação rejeita as classes de erros observadas nos testes; a avaliação estruturada permanece disponível para conferência.

Para a defesa, explique: **“O Java avalia a rota e mantém os fatos; o LLM explica as métricas disponíveis. Uma resposta do modelo não decide a segurança de um trajeto.”**

## Limites e operação

Um modelo pequeno pode errar, omitir uma ressalva ou produzir uma formulação ruim. Compare as respostas com os fatos estruturados. A execução em CPU também pode levar mais tempo na primeira chamada, enquanto o modelo é carregado. Meça separadamente a primeira resposta e as seguintes; não prometa um tempo de geração antes de observar o computador da apresentação.

A configuração limita a saída a 160 tokens e usa contexto de 2.048 tokens, temperatura `0.0`, `seed=42` e `top-k=20`. O pedido textual orienta uma resposta curta. Esses limites ajudam a controlar a execução, mas não tornam o texto infalível. O tempo adicional de download na primeira preparação não representa o tempo de uma chamada já instalada.

Para encerrar a API, use `Ctrl+C` no terminal correspondente. Para parar temporariamente somente o Ollama, mantendo o modelo no volume, execute na pasta do projeto:

```powershell
docker compose -p rotaleve --profile ia stop ollama
```

Ao parar o modelo com a API ainda aberta, futuras explicações podem usar fallback. Inicie novamente pelo mesmo iniciador para preparar os serviços. A remoção de volumes não faz parte desse procedimento.

## Registro de validação desta integração

Validado em 17/11/2026: 59 testes aprovados, três cenários com modelo real, duas explicações HTTP na API 8080 e uma explicação executada pelo Swagger. Os textos aceitos foram conferidos pelo assistente; a revisão do código e a defesa continuam sendo responsabilidade do grupo.

A imagem Ollama 0.40.16 foi fixada pelo digest `sha256:910d0d8107b538fb0894e63163eb195de0cc7aefcbc666dc62d3b18f71124592`; o modelo instalado foi `qwen2.5:1.5b`, digest iniciado por `65ec06548149`.

Veja [resultados detalhados](VALIDACAO-SPRING-AI.md) e [evidências JSON](RESULTADO-SPRING-AI-REAL.json).

## Referências

O provedor nativo utiliza `spring-ai-starter-model-ollama`, gerenciado pelo BOM 1.1.2 do projeto, e a configuração da linha 1.x em `spring.ai.ollama.chat.options`. [Documentação oficial Spring AI](https://docs.spring.io/spring-ai/reference/1.0/api/chat/ollama-chat.html)

A execução em CPU, o volume para modelos e o acesso ao servidor local seguem a orientação oficial do Ollama. O projeto acrescenta limites de recursos e publicação somente em loopback. [Ollama em Docker](https://docs.ollama.com/docker)
