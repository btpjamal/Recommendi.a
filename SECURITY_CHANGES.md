# Correções de segurança

As alterações estão na branch correções_segurança. Nenhum commit foi criado.

## Comportamento e migração

- JWT usa o ID imutável da conta, issuer fixo, validade de 15 minutos e versão de sessão.
- Tokens anteriores à atualização são rejeitados. Usuários precisarão entrar novamente.
- A migração Flyway V5 adiciona token_version e version à tabela users; não foi executada no banco real.
- Troca de dados/senha e POST /api/v1/auth/logout invalidam todos os tokens daquela conta.
- O frontend guarda sessão somente na memória da página. Atualizar ou fechar a página exige novo login.
- Logout tenta revogar no servidor antes de limpar a memória. Se estiver offline, informa que a revogação remota não pôde ser confirmada.
- Feedback, contexto/prompt e API keys exigem propriedade também nos serviços. ADMIN não obtém acesso automático aos contextos/chaves de terceiros.
- Senhas novas/alteradas exigem no mínimo 12 caracteres e no máximo 72 bytes UTF-8; algumas senhas comuns/repetitivas são rejeitadas. Senhas antigas continuam aceitas no login até serem alteradas.

## Configuração local e segredos

Não salve segredos em .idea, .env versionado ou configuração de execução compartilhada.
Os segredos locais antes presentes no IntelliJ são preservados em %LOCALAPPDATA%\RecommendiA\secrets.dpapi.json,
criptografados por Windows DPAPI CurrentUser, com diretório acessível somente à conta Windows atual.
Esse arquivo é vinculado ao usuário/ambiente Windows; não é um backup portável e não deve ser a única estratégia de recuperação de produção.
Nenhum segredo foi rotacionado e nenhuma API key do banco foi modificada.
O IntelliJ já aberto pode ainda ter os valores em memória: antes de compartilhar arquivos, confirme que ele não os gravou novamente em workspace.xml.

Para iniciar localmente, configure JAVA_HOME e DB_USERNAME na sessão PowerShell e execute scripts/Start-Local.ps1.
O script descriptografa apenas na memória do processo e inicia Maven; não imprime valores.
Para usar o IntelliJ, feche a instância anterior, carregue o ambiente com . .\scripts\Start-Local.ps1 -LoadOnly
e inicie o IntelliJ a partir dessa mesma sessão. Não copie os valores de volta para a configuração de execução.
Também é possível provisionar diretamente JWT_SECRET, AI_KEY_ENCRYPTION_SECRET e DB_PASSWORD pelo ambiente seguro.

Se os segredos antigos tiverem sido expostos, é necessária rotação operacional. A troca da chave AES sem
recriptografar as API keys tornará os registros existentes ilegíveis; planeje migração ou recadastro antes de trocar.

## Preparação de produção

- Provisione DB_USERNAME para uma conta dedicada com os grants necessários, distinta do usuário de migrações. A criação de roles/grants não foi executada.
- Baseline automático de Flyway está desabilitado. Para um banco legado ainda sem histórico Flyway, faça baseline explícito após verificar o schema e fazer backup.
- O perfil test não é ativado automaticamente. Ative-o somente se quiser executar o seeder local de catálogo.
- Defina CORS_ALLOWED_ORIGINS com as origens HTTPS autorizadas, separadas por vírgula.
- O frontend de produção usa /api/v1 no mesmo domínio, ou VITE_API_BASE_URL definido no build. HTTP remoto é rejeitado; localhost é permitido para desenvolvimento.
- Configure HTTPS no proxy e mantenha o backend fora da exposição direta. Nenhum certificado/proxy foi alterado.
- Limites de login: 10 solicitações por e-mail em 5 minutos e 60 por IP em 1 minuto. Cabeçalhos de IP encaminhados não são confiados.
- Geração de IA: até 3 tentativas por usuário em 5 minutos, uma geração simultânea por usuário e quatro no processo. Rejeições retornam 429.
- A fila de localização tem 16 posições e 4 workers, com um stream ativo por usuário. Clientes externos possuem timeouts explícitos.
- Os limites são locais ao processo. Em múltiplas instâncias, use limites/coordenação distribuídos e configure corretamente o proxy confiável; atrás de um proxy não configurado, clientes podem compartilhar a quota de IP.
- Chamadas de IA ocorrem fora de transações de banco; contexto e persistência usam transações curtas.
- Markdown de IA não renderiza HTML, imagens ou links externos. Contexto é serializado como dado não confiável e tem limite de tamanho. Isso mitiga consequências de prompt injection; não garante obediência do modelo.

## Verificação sem dados reais

Os testes security usam Mockito, contextos Spring limitados e chaves/contas sintéticas. Não iniciam a aplicação,
não executam Flyway, não usam PostgreSQL e não chamam Gemini. O teste contextLoads original não foi executado,
pois inicia o contexto completo e pode alcançar o banco configurado.

Com JAVA_HOME apontando para um JDK compatível:

    .\mvnw.cmd -o -Dtest=JwtSecurityTest,OwnershipSecurityTest,ResourceSecurityTest,UserSessionSecurityTest,HttpSecurityTest,AIKeySecurityTest test
    npm.cmd run test:security --prefix front-end
    npm.cmd run build --prefix front-end

Valide a migração V5 e as permissões PostgreSQL em uma cópia isolada antes da publicação.
Dependências não foram atualizadas em bloco: os avisos examinados na auditoria não demonstraram exploração
nos caminhos usados. Uma revisão completa das dependências transitivas continua sendo uma tarefa separada.
