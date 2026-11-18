# Integração do aplicativo com a API

Este documento descreve o backend consumido pelo app React Native do grupo, entregue em pacote separado. Os contratos completos podem ser consultados em `/v3/api-docs` e no Swagger. Há chamadas executáveis em [exemplos.http](exemplos.http).

## Endereço e autenticação

A base local é `http://localhost:8080/api/v1`. No emulador Android, use `http://10.0.2.2:8080/api/v1` para acessar a API do computador. Em dispositivo físico, use o IP do computador na mesma rede e configure a rede local conforme necessário. `localhost` no celular é o próprio celular. Para uma API publicada, configure a URL HTTPS do grupo.

Cadastro e login aceitam JSON e não exigem token:

```http
POST /api/v1/auth/cadastro
Content-Type: application/json

{"nome":"Júlia Demo","email":"julia.demo@example.com","senha":"JuliaDemo@123"}
```

```http
POST /api/v1/auth/login
Content-Type: application/json

{"email":"cidadao@rotaleve.local","senha":"Cidadao@123"}
```

O cadastro responde `201` com o usuário e papel `CIDADAO`. Ele não faz login automaticamente. O login retorna `accessToken`, `tokenType`, `expiresIn` (segundos) e `usuario`. O JWT expira em duas horas. Nas chamadas seguintes:

```http
Authorization: Bearer SEU_ACCESS_TOKEN
Content-Type: application/json
```

Guarde o token no armazenamento seguro da plataforma, limpe-o ao sair e redirecione ao login quando a API responder `401`. O protótipo não possui refresh token nem endpoint de logout: sair do app remove a sessão local. Nunca coloque a chave de assinatura JWT ou chaves Google/OpenAI no aplicativo. Elas pertencem ao backend.

## Endpoints

Todos os endpoints abaixo exigem autenticação, exceto cadastro/login. Dados próprios são obtidos pelo token; o cliente não envia `usuarioId` para acessar o próprio perfil ou suas rotas.

| Método | Caminho após `/api/v1` | Regra |
|---|---|---|
| POST | `/auth/cadastro` | Público, cadastro como cidadão |
| POST | `/auth/login` | Público, retorna JWT |
| GET / PUT / DELETE | `/usuarios/me` | Próprio usuário |
| GET / PUT / DELETE | `/perfis/me` | Próprio perfil; PUT cria ou atualiza |
| GET | `/pontos` | Lista paginada |
| GET | `/pontos/{id}` | Detalhe de ponto |
| POST | `/pontos` | Gestor |
| PUT / DELETE | `/pontos/{id}` | Gestor |
| GET | `/relatos` | Lista paginada; `status` opcional |
| GET | `/relatos/{id}` | Detalhe de relato |
| POST | `/relatos` | Usuário autenticado |
| PUT / DELETE | `/relatos/{id}` | Autor, enquanto ativo |
| POST | `/relatos/{id}/confirmacoes` | Outro usuário; repetição é idempotente |
| POST | `/relatos/{id}/resolver` | Gestor |
| GET | `/rotas/cenarios` | Consulta o cenário local de demonstração |
| POST | `/rotas/busca` | Exige perfil de mobilidade |
| POST | `/rotas/salvas` | Salva candidato de uma consulta própria ainda válida |
| GET | `/rotas/salvas` | Lista paginada das próprias rotas |
| GET / PUT / DELETE | `/rotas/salvas/{id}` | Próprio usuário; PUT altera o nome |
| POST | `/rotas/salvas/{id}/explicacao` | Explica a avaliação da própria rota |
| GET | `/notificacoes` | Lista paginada dos próprios avisos |
| PATCH | `/notificacoes/{id}/lida` | Marca um aviso próprio como lido |

## Perfil e busca

Crie ou atualize o perfil antes de buscar rotas:

```http
PUT /api/v1/perfis/me

{
  "tipo": "GESTANTE",
  "caminhadaMaximaMetros": 600,
  "evitarEscadas": true,
  "priorizarElevador": true,
  "priorizarAssento": true
}
```

`tipo`: `GESTANTE`, `CARRINHO`, `IDOSO`, `CADEIRANTE` ou `TEMPORARIA`. A caminhada aceita valores entre 50 e 10.000 metros. Para cadeirante, evitar escadas é obrigatório na regra de domínio.

```http
POST /api/v1/rotas/busca

{"origem":"Praça da Sé","destino":"Hospital das Clínicas"}
```

O provedor demo aceita esse cenário, com tolerância a acentuação e maiúsculas. Outras buscas respondem `422`; isso evita apresentar uma rota inventada como trajeto real. A resposta contém `consultaId`, `expiraEm`, `provedor`, `demonstracao`, `temOpcaoCompativel` e `rotas`. Cada item possui `rota`, `score`, `compatibilidade`, `motivos` e `avaliadaEm`.

O aplicativo deve exibir os motivos e a compatibilidade, além da nota:

| Compatibilidade | Apresentação sugerida |
|---|---|
| `COMPATIVEL_COM_DADOS_CONHECIDOS` | Compatível com os dados disponíveis; não é garantia |
| `DADOS_INSUFICIENTES` | Informações insuficientes para confirmar acessibilidade |
| `INCOMPATIVEL` | Existe uma restrição incompatível com o perfil |

Se `temOpcaoCompativel=false`, mostre essa situação de forma explícita e ofereça revisar o perfil ou a busca. Não destaque a maior nota como acessível. O provedor Google atual mantém o resultado como dados insuficientes até existir enriquecimento dos trechos com evidências locais.

## Salvar, consultar e explicar uma rota

Envie o `consultaId` retornado e um `rota.id` da mesma busca:

```http
POST /api/v1/rotas/salvas

{"consultaId":"UUID_RETORNADO_PELA_BUSCA","rotaId":"demo-se","nome":"Pré-natal"}
```

A consulta expira após 30 minutos e pertence ao usuário. Depois disso, é necessário buscar novamente (`410` para consulta expirada). O cliente não envia uma rota completa nem determina o seu score. A rota salva contém um retrato dos candidatos do servidor, e a avaliação considera novamente o perfil e as condições atuais ao consultar.

`PUT /rotas/salvas/{id}` aceita `{"nome":"Novo nome"}`. `DELETE` responde `204`, sem corpo. `POST /rotas/salvas/{id}/explicacao` responde `texto`, `fonte`, `dadosVerificados`, `aviso` e `textoGerado`. O último campo contém somente o resumo gerado e validado pelo LLM; fica nulo nos modos locais/fallback. `texto` inclui a conclusão de compatibilidade mantida pelo Java e os dados da avaliação.

Exiba a fonte da explicação: `SPRING_AI` identifica resposta aprovada do LLM; `REGRAS_LOCAIS_SEM_LLM` identifica texto local; `FALLBACK_LLM_INDISPONIVEL` indica falha da integração; `FALLBACK_LLM_RESPOSTA_INVALIDA` indica resposta rejeitada após nova tentativa. Não apresente fallback como geração bem-sucedida de IA.

## Relatos, fotos e confirmação

O app obtém o ponto de `/pontos` e envia:

```http
POST /api/v1/relatos

{
  "pontoCriticoId": 1,
  "descricao": "Elevador indisponível na entrada da estação, conforme cenário de demonstração.",
  "fotoUrl": "https://example.com/foto-elevador-demo.jpg",
  "tipoProblema": "ELEVADOR_QUEBRADO"
}
```

O ID e a URL acima são ilustrativos: use o ID selecionado em `/pontos` e a URL devolvida por `POST /arquivos/fotos`. Esse upload exige JWT e recebe `multipart/form-data` com um campo `arquivo` JPEG/PNG de até 5 MB. A resposta `201` contém `url`, que o app envia como `fotoUrl`. Não envie `file://`, `blob:` nem uma data URI como URL do relato. O servidor verifica o formato decodificando a imagem, limita resolução e remove metadados ao regravar. Não executa classificação visual por IA.

As fotos comunitárias podem ser vistas sem JWT pelo endereço aleatório retornado (`GET /arquivos/fotos/{uuid}.png` ou `.jpeg`). Elas ficam em `UPLOAD_DIRECTORY`, por padrão `data/fotos`, fora do controle de versão. São destinadas à comunidade; o app informa isso antes do envio. O envio é autenticado, a listagem do diretório não é exposta e o nome original não é utilizado. Para publicação, configure HTTPS e armazenamento persistente/backup e uma política de remoção; apagar o relato não apaga automaticamente a imagem já enviada.

O navegador Expo usa CORS com origens exatas de desenvolvimento em `http://localhost:8081` e `http://127.0.0.1:8081` (também 19006 por compatibilidade). Ajuste `CORS_ALLOWED_ORIGINS` para a origem real publicada. Não é necessário abrir CORS para um app nativo.

Problemas aceitos: `ELEVADOR_QUEBRADO`, `BURACO`, `DEGRAU`, `OBSTACULO`, `SEM_COBERTURA`. O tipo precisa corresponder ao ponto; por exemplo, elevador quebrado exige um ponto `ELEVADOR`. Os estados de relato são `ATIVO`, `RESOLVIDO`, `EXPIRADO`.

Um relato tem validade de 48 horas sem confirmação nova. O autor não confirma o próprio relato (`409`). Outro usuário confirma com `POST /relatos/{id}/confirmacoes`, sem corpo. Repetir a chamada responde `200`, com `jaConfirmado=true`, sem incrementar a contagem nem renovar o prazo. Editar o texto também não renova a validade. Expiração muda a condição para desconhecida quando não restam relatos ativos; ela não comprova reparo.

Somente gestor pode resolver, com `POST /relatos/{id}/resolver`. A verificação de funcionamento de um ponto tem validade de 24 horas. Ao exibir um ponto, trate `DESCONHECIDO` separadamente de `OPERACIONAL` e `PROBLEMA`.

## Notificações, paginação e erros

Consulte `/notificacoes?page=0&size=20&sort=criadaEm,desc` ao abrir “Minhas rotas” ou a caixa de avisos; a gravação é assíncrona e pode ocorrer alguns segundos depois do relato. O endpoint não é um serviço de push. `PATCH /notificacoes/{id}/lida` não exige corpo.

Listagens paginadas aceitam `page` começando em zero, `size` e `sort=campo,asc|desc`. Leia `content`, `totalElements`, `totalPages`, `number` e `size` na resposta. Não suponha que uma página represente todos os registros. Os objetos específicos do domínio ficam em `content`.

As falhas seguem o formato `ProblemDetail`, com `type`, `title`, `status`, `detail` e, quando disponível, `instance`/erros de campo. Trate `400` como dados inválidos, `401` como autenticação necessária, `403` como operação não permitida, `404` como recurso ausente, `409` como conflito de domínio, `410` como consulta expirada e `422` como perfil/cenário incompatível com a operação. Exiba `detail` de forma clara e mantenha indicadores de carregamento.

## Telas sugeridas para integração

Login, cadastro, perfil, busca, resultado, detalhe/explicação, novo relato e minhas rotas/avisos consomem os endpoints acima. O app mantém somente o token de sessão (SecureStore no dispositivo e sessionStorage na web), sem gravar senhas ou banco de usuários local. Para Java Advanced, o requisito é mostrar esse consumo e realizar a demonstração ponta a ponta com a API ativa. A apresentação pode usar Expo Go ou emulador; o enunciado de Java não pede APK próprio nem publicação do app.

O hash em `EXPO_PUBLIC_APP_COMMIT`, a distribuição no Firebase e o professor como tester pertencem à entrega separada de Mobile. Esses recursos do aplicativo permanecem disponíveis, mas não são pendências da entrega Java. O pitch e o repositório público do Java estão descritos em [FECHAMENTO-JAVA.md](FECHAMENTO-JAVA.md).
