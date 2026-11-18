# Integração Rota Leve — 18/11/2026

O front fornecido pelo colega foi integrado ao backend Java. A aparência, componentes, tema e navegação foram preservados. O ZIP original em Downloads foi mantido intacto.

## O que passou a usar o servidor

Login e cadastro com JWT; perfil de mobilidade; busca e classificação de rotas; criação, consulta, alteração de nome e exclusão de rotas salvas; relatos com foto enviada ao Java; confirmações; resolução pelo gestor; notificações; explicação com Spring AI.

Senhas e cadastros não ficam mais no AsyncStorage. No dispositivo o token usa SecureStore; no navegador, sessionStorage. O tema continua local. O app usa os erros do servidor e trata indisponibilidade sem retornar dados fictícios como se fossem respostas reais.

## Verificação concluída

| Escopo | Resultado |
|---|---|
| Java `verify` | 64 testes encontrados: 62 aprovados, zero falhas/erros, dois opcionais ignorados |
| Upload/CORS | Seis testes, incluídos nos 62; autenticação, imagem inválida, vazia/grande, nomes arbitrários e origens CORS |
| TypeScript | Aprovado |
| Cliente isolado | 32 verificações aprovadas: HTTP/sessão, concorrência e endereços de fotos |
| Contrato de rotas | 14 chamadas HTTP aprovadas nos serviços TS contra o Java; conta e rotas temporárias removidas |
| Compilação | Bundles web, Android e iOS exportados com sucesso; não são APK/IPA |
| Interface | Cadastro, perfil, busca, salvar/renomear, foto PNG, confirmação, resolução, avisos e logout conferidos |
| IA real | Texto identificado como SPRING_AI exibido no app com 300 m, 28 min e nota 91 |
| Fluxo de evento | Relato gerou aviso na rota salva; resolução gerou novo aviso; leitura persistida |
| Atualização de busca | Sé caiu de 91 para 31 com o problema; República ficou em primeiro. Após resolução, voltar à busca trouxe Sé com 91 novamente |

Os testes externos opcionais ignorados foram o de broker exclusivo e a classe opcional de Ollama da suíte. A IA local e os avisos foram exercitados separadamente pelo aplicativo nesta rodada.

Corrigimos também o acesso de gestor sem perfil de mobilidade, respostas de autenticação antigas após logout, atualização da busca ao retornar à tela, cor de dados insuficientes e URLs de fotos entre localhost e celular. A pasta `tests` do front contém verificações reproduzíveis com `npm run test:client`.

## Limites desta validação

A interface foi exercitada no navegador, inclusive com largura de celular. Não houve execução em aparelho/emulador Android ou iOS. A exportação de bundles comprova a compilação do código, sem substituir esse teste ou gerar um APK.

O provedor atual aceita apenas o cenário sintético **Praça da Sé → Hospital das Clínicas**. Não há roteamento real ativo, mapa de trajeto nem classificação de fotos por visão computacional. O app indica a demonstração e dados insuficientes. O modelo local explica métricas; a decisão de compatibilidade permanece nas regras Java. Fontes de fallback são identificadas.

O Java demo usa H2 em memória; reiniciar a API reinicia os registros. As fotos ficam em `data/fotos` na cópia Java, não no ZIP. Elas são comunitárias e acessíveis por URL aleatória; a regravação remove metadados, mas excluir um relato não exclui a imagem automaticamente. Um deploy precisa de banco e arquivos persistentes, HTTPS e configuração externa apropriada.

## Para concluir a entrega acadêmica

1. Testar o aplicativo em celular/emulador, com a API acessível pela rede.
2. Atualizar o repositório com esta versão e configurar o hash real em EXPO_PUBLIC_APP_COMMIT antes do build.
3. Disponibilizar a API durante a avaliação e publicar o app no Firebase App Distribution, adicionando o professor.
4. Conferir/gravar o vídeo de até cinco minutos mostrando esta versão integrada e atualizar os links da entrega.

O front original trouxe um repositório e dois links de vídeo diferentes. Eles foram preservados em `ENTREGA.txt` como referências não verificadas desta versão; não foram substituídos por links inventados. Em confirmação posterior, o grupo informou **2TDSPO** como turma dos cinco integrantes; esse dado foi registrado na identificação de Java.

## Integrantes

Enzo Monteiro Maciel — RM563734  
Matheus de Almeida Sousa — RM563557  
Paulo Estalise — RM563811  
Gabriel Bebé Silva — RM562012  
Emanuel Italo — RM561337
