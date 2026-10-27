# Demonstração do Recommendi.a — 2 de dezembro de 2026

## Objetivo da demonstração

Mostrar como restrições e preferências mudam a recomendação de hobbies, como o usuário interage com o resultado e como um plano pode apoiar a prática. O tempo total da apresentação ainda precisa ser confirmado; o percurso abaixo pode ocupar aproximadamente 6–8 minutos, além da explicação técnica.

## Preparação técnica

- Trabalhar na branch correções_segurança. Nenhum commit foi criado pelo assistente.
- Validar as migrações V6 e V7 em banco descartável antes de atualizar qualquer banco existente. Não editar migrações V1–V5 já aplicadas.
- V6 define exclusão de conta: remove dados próprios, API key, planos, mensagens e sugestões criadas pelo usuário; preserva catálogo e comunidades compartilhadas, e limpa referências opcionais ao revisor excluído.
- V7 reúne comunidades duplicadas, preserva mensagens e a união dos membros, e impõe unicidade mesmo com localização nula. Exige PostgreSQL 15 ou superior.
- Guardar backup do banco e da chave mestra antes de qualquer atualização manual de um banco importante.
- As opções atuais de seeder, baseline e migração de regras geográficas foram preservadas. Confira se quer ativá-las no ambiente de ensaio; não copie automaticamente essas opções para produção.
- Provisione segredos fora do IDE; confira se o IntelliJ não voltou a gravá-los. Não exiba telas de configuração contendo chaves na banca.
- A sessão fica na memória: recarregar a página exige novo login. Prepare as contas antes e evite atualizar a página durante o percurso.

## Dois perfis fictícios para comparação

| Perfil | Tempo semanal | Orçamento inicial | Preferência | Interesses sugeridos |
|---|---:|---:|---|---|
| A — rotina curta | 2 horas | R$ 50 | Individual, em casa, baixa atividade física | Leitura, criação ou relaxamento, conforme o catálogo |
| B — maior disponibilidade | 10 horas | R$ 300 | Social, ao ar livre, atividade física moderada/alta | Esportes, natureza e socialização, conforme o catálogo |

Na interface atual, “Até 2 horas” armazena 2 horas, “De 5 a 10 horas” armazena 10 horas e “Até R$ 50” armazena R$ 50. As faixas são convertidas em valores representativos pelo questionário; explique essa simplificação se a banca perguntar.

Use nomes fictícios e endereços terminados em example.test. Defina senhas próprias de teste sem colocá-las em slides ou documentos compartilhados. Selecione interesses existentes pela interface; não presuma IDs fixos do banco.

## Como evitar problemas de origem no ensaio local

Use a mesma origem configurada no CORS do backend. O padrão atual é http://localhost:5173; iniciar o frontend em outra porta ou trocar localhost por 127.0.0.1 exige configurar essa origem explicitamente. Não use origem curinga para resolver erros.

Para um ensaio com o servidor de desenvolvimento, use localhost:5173 no frontend e confira o backend configurado. Para apresentar um build com preview, defina VITE_API_BASE_URL no build e configure no backend a origem/porta efetiva do preview; o build padrão de produção usa /api/v1 e pressupõe um proxy no mesmo domínio.

Depois de preparar o catálogo no banco de ensaio, desative o perfil test e a migração automática de regras geográficas para congelar os dados da apresentação. Essas opções podem ser passadas no processo de ensaio; não é necessário alterar permanentemente a configuração do IntelliJ. Mantenha baseline automático desativado no ensaio e não execute atualizações de schema durante a apresentação.

## Percurso principal

1. Apresente o problema: encontrar hobbies compatíveis com a rotina, orçamento e preferências.
2. Entre com o perfil A e mostre as recomendações e suas justificativas.
3. Entre com o perfil B e compare um mesmo hobby entre os dois perfis. Compare pontuação, motivos e alertas; não prometa antecipadamente uma ordem específica sem ensaiar no catálogo usado.
4. Explique um exemplo verificável do código: orçamento compatível soma 15 pontos, e tempo suficiente soma 15 pontos. Os demais critérios também influenciam o total; score não é porcentagem nem probabilidade de satisfação.
5. Marque um hobby como interessado e mostre a mudança na aba correspondente.
6. Exiba um plano já preparado. Explique a diferença entre o mecanismo de recomendação e a geração do texto pela IA.
7. Se houver tempo, mostre administração ou comunidade. Mantenha essas funções como complementos da proposta central.

## IA e contingência

Não dependa de uma chamada externa ao vivo para concluir a demonstração. Prepare previamente um plano e confirme que ele continua acessível antes da apresentação. Se usar um provedor simulado no ensaio, identifique explicitamente o resultado como simulação; isso não comprova qualidade do modelo real.

Mantenha capturas ou uma gravação curta do percurso como alternativa a falhas de rede. Para apresentar resultados armazenados, informe que foram gerados anteriormente. Localização e mapas dependem de serviços externos; deixe-os fora do percurso obrigatório.

## Evidências para levar à banca

- Uma comparação real de recomendações entre os dois perfis, capturada no ambiente de ensaio.
- Um exemplo de motivo e um de alerta, ligados aos respectivos critérios.
- Resumo dos testes de autorização e de persistência, com resultados e limitações.
- Arquitetura resumida: React → API Spring Boot → PostgreSQL; o backend monta o contexto e chama o provedor de IA.
- Limitações: recomendação baseada em regras/pesos, avaliação com usuários ainda a documentar, dependências externas e ausência de garantia de satisfação.

## Ensaio e datas sugeridas

- Até 6 de novembro: consolidar ambiente e corrigir falhas que interrompam os fluxos.
- De 7 a 16 de novembro: registrar as comparações dos perfis e organizar evidências.
- De 17 a 25 de novembro: ensaiar explicação e demonstração, anotando o tempo real.
- De 26 de novembro a 1º de dezembro: evitar funcionalidades novas, conferir backup e plano de contingência.
- Em 2 de dezembro: usar o percurso já ensaiado e deixar exploração livre para depois da demonstração principal.

## Checklist antes de cada ensaio

- Backend e frontend iniciam e as contas fictícias autenticam.
- Catálogo contém hobbies suficientes; questionários e interesses foram preenchidos.
- Recomendações e detalhes abrem sem erro.
- Interesse/feedback alteram o estado esperado.
- O plano preparado pode ser consultado sem solicitar nova geração.
- A conta administrativa é separada das contas comuns.
- Segredos e ferramentas administrativas do banco não aparecem na captura de tela.
- Exclusão de conta é testada apenas com uma conta descartável, fora do percurso principal.

Este roteiro orienta o ensaio; a duração final e as capturas dependem do tempo disponibilizado pela banca e do catálogo que será apresentado.
