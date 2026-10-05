# Gerenciador Diniz — Portal contábil do escritório e dos clientes

Sistema web que organiza, num só lugar, **documentos, prazos e obrigações fiscais** entre um escritório de
contabilidade (Diniz Assessoria Contábil, Palmas-TO) e as empresas clientes — incluindo indústrias e comércios
do Tocantins. Também traz automaticamente as **comunicações da SEFAZ-TO** recebidas no Domicílio Eletrônico do
Contribuinte (DEC), com a contagem da ciência tácita.

> Este repositório é a **API (backend)**. O front-end está em
> [GerenciadorDinizFront](https://github.com/rafaelsdiniz/GerenciadorDinizFront).

## Acesse a demonstração

| | |
|---|---|
| **Sistema** | https://gerenciador-diniz.vercel.app |
| **API** | https://gerenciador-diniz-api-408278380894.southamerica-east1.run.app (documentação: `/q/swagger-ui`) |

A API roda no Google Cloud Run (São Paulo), que desliga quando ninguém usa: **o primeiro acesso pode levar alguns segundos**.

**Logins de demonstração** (senha `123456` para todos):

| Perfil | E-mail | O que vê |
|---|---|---|
| Escritório (administrador) | `rafael@diniz.com.br` | Toda a carteira de empresas |
| Cliente | `maria@paoquente.com.br` | Só a Padaria Pão Quente |
| Cliente | `carlos@topecas.com.br` | Só a Auto Peças Tocantins |

Dentro do sistema, a página **Como usar** (menu lateral) traz um roteiro de teste de 5 minutos para cada perfil.
Os dados são fictícios; as comunicações do DEC na demonstração também são fictícias (LGPD).

## O problema

O escritório acompanha dezenas de empresas, cada uma com obrigações mensais (DAS, FGTS, INSS, ICMS, DCTFWeb…),
documentos que o cliente precisa enviar (extratos, notas de entrada, folha de ponto) e comunicações da SEFAZ com
prazo legal. Hoje isso circula por WhatsApp, e-mail e planilhas: prazos se perdem, guias são pagas com atraso
(multa e juros) e comunicações da SEFAZ viram ciência tácita sem que ninguém tenha lido.

## A solução

| Funcionalidade | Para quem | O que resolve |
|---|---|---|
| **Painel** | Escritório e cliente | Mostra primeiro o que exige ação hoje: obrigações vencidas, prazos próximos, documentos a enviar |
| **Pendências e competência** | Ambos | Cada obrigação do mês com vencimento, competência (mês de referência), status e anexos |
| **Obrigações recorrentes** | Escritório | Modelos (DAS dia 20, FGTS dia 7…) que geram as pendências automaticamente |
| **Responsável pela obrigação** | Ambos | Separa o que o escritório entrega (guias) do que o cliente envia (documentos) |
| **Anexar guia / enviar documento** | Ambos | O arquivo é ligado à obrigação, que passa a “entregue” |
| **Confirmação de pagamento da guia** | Ambos | O cliente confirma o pagamento (com comprovante); o escritório vê quem ainda não pagou |
| **Calendário** | Ambos | Todos os vencimentos do mês |
| **Arquivos (estilo Drive)** | Ambos | Pastas por empresa, envio por arrastar e soltar, visualização, mover, lixeira |
| **Comunicações DEC** | Ambos | Mensagens da SEFAZ-TO chegam sozinhas, com urgência e contagem da ciência tácita |
| **Notificações** | Ambos | Sino com o que vence em 7 dias e o que está atrasado |
| **Empresas, sócios, usuários** | Escritório | Cadastro da carteira, quadro societário e acessos |
| **Auditoria** | Escritório | Registro de cada ação (quem, o quê, quando), com exportação CSV |
| **Leitura inteligente de guias (IA)** | Ambos | Lê o PDF da guia/documento e extrai valor, vencimento, CNPJ, competência e linha digitável; sugere a obrigação certa |
| **Assistente virtual (chatbot)** | Ambos | Responde em linguagem natural sobre prazos, guias, certidões e DEC, só com os dados que o usuário pode ver |
| **Certidões negativas** | Ambos | Validade de CND Federal, Estadual, Municipal, FGTS, Trabalhista e Falência, com aviso 15 dias antes |
| **Fechamento mensal** | Escritório | Quadro por competência (aguardando documentos → apuração → guias → concluído) com responsável e prazo interno |
| **Mensagens na obrigação** | Ambos | Conversa entre escritório e cliente dentro de cada obrigação, com aviso no sino |
| **Relatórios** | Ambos | Relatório mensal da empresa, da carteira e de pendências — impressão/PDF e CSV |
| **Minha conta** | Ambos | Perfil e troca de senha |

## Arquitetura

```
 Navegador ──▶ Front-end Angular 20 (Vercel)
                    │  REST + JWT
                    ▼
              API Quarkus 3 / Java 21 (Google Cloud Run, Docker) ──▶ PostgreSQL (Neon)
                    │  REST somente leitura, chave por escritório
                    ▼
              DEC Monitor (Next.js) ──▶ SEFAZ-TO (Domicílio Eletrônico do Contribuinte)
```

- **Autenticação**: JWT com perfis `ADMIN` (escritório) e `FUNCIONARIO` (cliente); senhas com BCrypt.
- **Isolamento por empresa (LGPD)**: toda rota verifica, a partir do token, se o usuário pode ver a empresa
  (`security/UsuarioLogado`). Registros de outra empresa respondem 404, sem revelar que existem.
- **Tarefas agendadas** (`SchedulerService`, `DecIntegracaoService`): marcação diária de vencidos, geração
  mensal de pendências e sincronização com o DEC a cada 10 minutos.
- **Integração DEC**: a API lê `GET /api/integracao/comunicacoes` do DEC Monitor e grava por *upsert*;
  as comunicações são ligadas à empresa pelo CNPJ. Somente leitura: nada é alterado no DEC.
- **Inteligência artificial** (`service/ia`, `assistente`): o texto do PDF é extraído com PDFBox e lido por
  padrões (valor, datas, CNPJ, código de barras com validação dos dígitos). Com `DEEPSEEK_API_KEY` configurada,
  a leitura e o assistente usam o modelo DeepSeek; sem a chave, tudo continua funcionando no modo local.
  O assistente recebe somente o contexto da empresa do usuário (mesmo isolamento da API) e tem limite de uso.

## Rodando localmente

Pré-requisitos: **Java 21** (o Maven vem pelo `./mvnw`) e um **PostgreSQL** (local ou Neon gratuito).

1. Crie um arquivo `.env` na raiz deste projeto (ele não vai para o git):

   ```env
   DB_URL=jdbc:postgresql://localhost:5432/diniz?sslmode=disable
   DB_USER=postgres
   DB_PASSWORD=sua-senha
   # opcional: integração com o DEC Monitor
   # DEC_URL=https://...vercel.app
   # DEC_TOKEN=...
   # ou, sem DEC real, comunicações fictícias:
   # DEC_DEMO=true
   ```

2. Suba a API em modo de desenvolvimento:

   ```bash
   ./mvnw quarkus:dev
   ```

   Em desenvolvimento o banco é **recriado a cada início** com os dados de demonstração (`import.sql`),
   com datas relativas ao mês atual.

3. Suba o front-end (veja o README do [GerenciadorDinizFront](https://github.com/rafaelsdiniz/GerenciadorDinizFront))
   e acesse http://localhost:4200.

Testes automatizados:

```bash
./mvnw test
```

## Variáveis de ambiente

| Variável | Uso | Padrão |
|---|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Conexão com o PostgreSQL | obrigatórias |
| `PORT` | Porta HTTP | `8080` |
| `CORS_ORIGINS` | Endereço do front-end | `http://localhost:4200` |
| `DB_GENERATION` | (produção) `update` preserva os dados; `drop-and-create` recria e carrega a demonstração | `update` |
| `JWT_PRIVATE_KEY_LOCATION`, `JWT_PUBLIC_KEY_LOCATION` | Chaves do JWT | chaves de desenvolvimento |
| `DEC_URL`, `DEC_TOKEN` | Integração com o DEC Monitor | desligada |
| `DEC_DEMO` | Comunicações fictícias (demonstração) | `false` |
| `DEEPSEEK_API_KEY` | IA na leitura de guias e no assistente (opcional) | modo local |

## Publicação (gratuita)

- **API**: Google Cloud Run (`southamerica-east1`, mesma região do banco), a partir do `Dockerfile`
  (build Maven + JRE 21): `gcloud run deploy --source .`. As chaves do JWT são geradas na imagem e nunca
  ficam no repositório. O `render.yaml` continua disponível como alternativa.
  Na 1ª publicação use `DB_GENERATION=drop-and-create` (cria as tabelas e carrega a demonstração);
  depois troque para `update`.
- **Front-end**: Vercel.
- **Banco**: Neon (PostgreSQL gratuito).

## Estrutura

```
src/main/java/diniz/contabilidade/arquivos/
  model/        entidades JPA, enums e value objects (CNPJ, CPF, e-mail, telefone)
  dto/          contratos da API (request/response)
  repository/   acesso a dados (Panache)
  service/      regras de negócio, agendamentos e integração DEC
  service/ia/   leitura inteligente de documentos (PDFBox + padrões + DeepSeek)
  assistente/   assistente virtual (chatbot)
  resource/     endpoints REST
  security/     usuário logado e isolamento por empresa
src/main/resources/
  application.properties   configuração (perfis dev/test/prod)
  import.sql, seed-*.sql   dados de demonstração
  exemplos/                guias fictícias para testar a leitura por IA
```

## Créditos e referências

- [Quarkus](https://quarkus.io), Hibernate ORM / Panache, SmallRye JWT, Quarkus Scheduler, PostgreSQL.
- Front-end: Angular, Chart.js / ng2-charts, JSZip, FileSaver, ícones [Lucide](https://lucide.dev) (licença ISC),
  fonte [Inter](https://rsms.me/inter/).
- Referência de design: coleção [awesome-claude-design](https://github.com/VoltAgent/awesome-claude-design) (MIT),
  adaptada às cores da marca Diniz.
- Integração com o DEC Monitor, sistema próprio do escritório para o Domicílio Eletrônico do Contribuinte (SEFAZ-TO).
- Desenvolvimento com apoio do assistente de IA Claude Code (Anthropic), registrado nos commits.
