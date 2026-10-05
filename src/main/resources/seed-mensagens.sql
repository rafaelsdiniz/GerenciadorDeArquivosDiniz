-- dados de demonstração do módulo (seed-mensagens)
------------------------------------------------
-- MENSAGENS POR OBRIGAÇÃO (conversa escritório x cliente)
-- As obrigações são localizadas por (empresa, obrigação recorrente), sem ids fixos.
-- INSERT ... SELECT: se a obrigação não existir, a mensagem simplesmente não é criada.
-- Usuários: 1 Rafael Diniz e 3 Patrícia Diniz (ADMIN, escritório),
--           7 Maria Souza (Padaria Pão Quente), 8 Carlos Lima (Auto Peças), 9 Ana Costa (Mercado Bom Preço).
-- Ficam não lidas: 1 para a Maria (resposta do escritório) e 3 para o escritório.
-- Horários no fuso da aplicação (America/Araguaina): a sessão do banco no carregamento pode estar em UTC.
------------------------------------------------

-- 1) Padaria — DAS do mês corrente
INSERT INTO mensagemobrigacao (datacriacao, dataatualizacao, obrigacaopendente_id, autor_id, doescritorio, texto, arquivo_id, lidapeloescritorioem, lidapeloclienteem)
SELECT (NOW() AT TIME ZONE 'America/Araguaina') - INTERVAL '2 days 3 hours', NULL, o.id, 7, FALSE,
       'Olá! A guia do DAS deste mês já está disponível? Quero programar o pagamento junto com os fornecedores.',
       NULL, (NOW() AT TIME ZONE 'America/Araguaina') - INTERVAL '2 days 2 hours 30 minutes', (NOW() AT TIME ZONE 'America/Araguaina') - INTERVAL '2 days 3 hours'
  FROM (SELECT id FROM obrigacaopendente WHERE empresa_id = 2 AND obrigacaorecorrente_id = 7
         AND datavencimento >= date_trunc('month', CURRENT_DATE) ORDER BY datavencimento LIMIT 1) o;

INSERT INTO mensagemobrigacao (datacriacao, dataatualizacao, obrigacaopendente_id, autor_id, doescritorio, texto, arquivo_id, lidapeloescritorioem, lidapeloclienteem)
SELECT (NOW() AT TIME ZONE 'America/Araguaina') - INTERVAL '2 days 2 hours 20 minutes', NULL, o.id, 1, TRUE,
       'Oi, Maria! Estamos fechando o faturamento do mês passado. A guia sai até amanhã e fica anexada aqui mesmo, na obrigação.',
       NULL, (NOW() AT TIME ZONE 'America/Araguaina') - INTERVAL '2 days 2 hours 20 minutes', (NOW() AT TIME ZONE 'America/Araguaina') - INTERVAL '2 days 1 hour'
  FROM (SELECT id FROM obrigacaopendente WHERE empresa_id = 2 AND obrigacaorecorrente_id = 7
         AND datavencimento >= date_trunc('month', CURRENT_DATE) ORDER BY datavencimento LIMIT 1) o;

INSERT INTO mensagemobrigacao (datacriacao, dataatualizacao, obrigacaopendente_id, autor_id, doescritorio, texto, arquivo_id, lidapeloescritorioem, lidapeloclienteem)
SELECT (NOW() AT TIME ZONE 'America/Araguaina') - INTERVAL '1 day 5 hours', NULL, o.id, 7, FALSE,
       'Perfeito, obrigada! O faturamento cresceu bastante com as encomendas de festa. Isso pode mudar a faixa do Simples?',
       NULL, (NOW() AT TIME ZONE 'America/Araguaina') - INTERVAL '1 day 4 hours', (NOW() AT TIME ZONE 'America/Araguaina') - INTERVAL '1 day 5 hours'
  FROM (SELECT id FROM obrigacaopendente WHERE empresa_id = 2 AND obrigacaorecorrente_id = 7
         AND datavencimento >= date_trunc('month', CURRENT_DATE) ORDER BY datavencimento LIMIT 1) o;

INSERT INTO mensagemobrigacao (datacriacao, dataatualizacao, obrigacaopendente_id, autor_id, doescritorio, texto, arquivo_id, lidapeloescritorioem, lidapeloclienteem)
SELECT (NOW() AT TIME ZONE 'America/Araguaina') - INTERVAL '3 hours', NULL, o.id, 1, TRUE,
       'Pode sim: a receita dos últimos 12 meses passou para a 3ª faixa e a alíquota efetiva subiu de 6,1% para 6,4%. Nada fora do esperado. Assim que a guia estiver anexada eu aviso por aqui.',
       NULL, (NOW() AT TIME ZONE 'America/Araguaina') - INTERVAL '3 hours', NULL
  FROM (SELECT id FROM obrigacaopendente WHERE empresa_id = 2 AND obrigacaorecorrente_id = 7
         AND datavencimento >= date_trunc('month', CURRENT_DATE) ORDER BY datavencimento LIMIT 1) o;

-- 2) Padaria — Extrato bancário (documento que o cliente envia)
INSERT INTO mensagemobrigacao (datacriacao, dataatualizacao, obrigacaopendente_id, autor_id, doescritorio, texto, arquivo_id, lidapeloescritorioem, lidapeloclienteem)
SELECT (NOW() AT TIME ZONE 'America/Araguaina') - INTERVAL '3 days 6 hours', NULL, o.id, 3, TRUE,
       'Oi, Maria! Ainda não recebemos o extrato do Banco do Brasil do mês passado. Consegue enviar até sexta para fecharmos a conciliação?',
       NULL, (NOW() AT TIME ZONE 'America/Araguaina') - INTERVAL '3 days 6 hours', (NOW() AT TIME ZONE 'America/Araguaina') - INTERVAL '3 days 2 hours'
  FROM (SELECT id FROM obrigacaopendente WHERE empresa_id = 2 AND obrigacaorecorrente_id = 26
         ORDER BY datavencimento DESC LIMIT 1) o;

INSERT INTO mensagemobrigacao (datacriacao, dataatualizacao, obrigacaopendente_id, autor_id, doescritorio, texto, arquivo_id, lidapeloescritorioem, lidapeloclienteem)
SELECT (NOW() AT TIME ZONE 'America/Araguaina') - INTERVAL '50 minutes', NULL, o.id, 7, FALSE,
       'Oi, Patrícia! O gerente só me mandou hoje, anexo ainda hoje. O extrato da conta do Sicoob também precisa?',
       NULL, NULL, (NOW() AT TIME ZONE 'America/Araguaina') - INTERVAL '50 minutes'
  FROM (SELECT id FROM obrigacaopendente WHERE empresa_id = 2 AND obrigacaorecorrente_id = 26
         ORDER BY datavencimento DESC LIMIT 1) o;

-- 3) Auto Peças — ICMS (conversa já resolvida, histórico)
INSERT INTO mensagemobrigacao (datacriacao, dataatualizacao, obrigacaopendente_id, autor_id, doescritorio, texto, arquivo_id, lidapeloescritorioem, lidapeloclienteem)
SELECT (NOW() AT TIME ZONE 'America/Araguaina') - INTERVAL '6 days 4 hours', NULL, o.id, 8, FALSE,
       'O ICMS deste mês ficou bem mais alto que o normal. Vocês podem conferir antes de eu pagar?',
       NULL, (NOW() AT TIME ZONE 'America/Araguaina') - INTERVAL '6 days 3 hours', (NOW() AT TIME ZONE 'America/Araguaina') - INTERVAL '6 days 4 hours'
  FROM (SELECT id FROM obrigacaopendente WHERE empresa_id = 3 AND obrigacaorecorrente_id = 9
         ORDER BY datavencimento DESC LIMIT 1) o;

INSERT INTO mensagemobrigacao (datacriacao, dataatualizacao, obrigacaopendente_id, autor_id, doescritorio, texto, arquivo_id, lidapeloescritorioem, lidapeloclienteem)
SELECT (NOW() AT TIME ZONE 'America/Araguaina') - INTERVAL '6 days 2 hours', NULL, o.id, 1, TRUE,
       'Conferimos, Carlos. Entrou a diferença de alíquota (DIFAL) das peças compradas de São Paulo. O valor está correto e o detalhamento está no relatório de apuração.',
       NULL, (NOW() AT TIME ZONE 'America/Araguaina') - INTERVAL '6 days 2 hours', (NOW() AT TIME ZONE 'America/Araguaina') - INTERVAL '6 days 1 hour'
  FROM (SELECT id FROM obrigacaopendente WHERE empresa_id = 3 AND obrigacaorecorrente_id = 9
         ORDER BY datavencimento DESC LIMIT 1) o;

INSERT INTO mensagemobrigacao (datacriacao, dataatualizacao, obrigacaopendente_id, autor_id, doescritorio, texto, arquivo_id, lidapeloescritorioem, lidapeloclienteem)
SELECT (NOW() AT TIME ZONE 'America/Araguaina') - INTERVAL '6 days 1 hour', NULL, o.id, 8, FALSE,
       'Entendi, obrigado pela explicação! Vou pagar hoje.',
       NULL, (NOW() AT TIME ZONE 'America/Araguaina') - INTERVAL '6 days', (NOW() AT TIME ZONE 'America/Araguaina') - INTERVAL '6 days 1 hour'
  FROM (SELECT id FROM obrigacaopendente WHERE empresa_id = 3 AND obrigacaorecorrente_id = 9
         ORDER BY datavencimento DESC LIMIT 1) o;

-- 4) Auto Peças — Folha de ponto (documento que o cliente envia)
INSERT INTO mensagemobrigacao (datacriacao, dataatualizacao, obrigacaopendente_id, autor_id, doescritorio, texto, arquivo_id, lidapeloescritorioem, lidapeloclienteem)
SELECT (NOW() AT TIME ZONE 'America/Araguaina') - INTERVAL '4 days 5 hours', NULL, o.id, 3, TRUE,
       'Carlos, lembrete: a folha de ponto é a base do fechamento da folha de pagamento. Precisamos do espelho até o vencimento para não atrasar os salários.',
       NULL, (NOW() AT TIME ZONE 'America/Araguaina') - INTERVAL '4 days 5 hours', (NOW() AT TIME ZONE 'America/Araguaina') - INTERVAL '4 days 1 hour'
  FROM (SELECT id FROM obrigacaopendente WHERE empresa_id = 3 AND obrigacaorecorrente_id = 28
         ORDER BY datavencimento DESC LIMIT 1) o;

INSERT INTO mensagemobrigacao (datacriacao, dataatualizacao, obrigacaopendente_id, autor_id, doescritorio, texto, arquivo_id, lidapeloescritorioem, lidapeloclienteem)
SELECT (NOW() AT TIME ZONE 'America/Araguaina') - INTERVAL '5 hours', NULL, o.id, 8, FALSE,
       'Oi, Patrícia! O relógio de ponto deu defeito na semana do dia 15. Posso mandar a folha desses dias preenchida à mão e assinada pelos funcionários?',
       NULL, NULL, (NOW() AT TIME ZONE 'America/Araguaina') - INTERVAL '5 hours'
  FROM (SELECT id FROM obrigacaopendente WHERE empresa_id = 3 AND obrigacaorecorrente_id = 28
         ORDER BY datavencimento DESC LIMIT 1) o;

-- 5) Mercado Bom Preço — DAS
INSERT INTO mensagemobrigacao (datacriacao, dataatualizacao, obrigacaopendente_id, autor_id, doescritorio, texto, arquivo_id, lidapeloescritorioem, lidapeloclienteem)
SELECT (NOW() AT TIME ZONE 'America/Araguaina') - INTERVAL '25 minutes', NULL, o.id, 9, FALSE,
       'Olá! O caixa está apertado este mês. Se eu pagar o DAS uns dias depois do vencimento, quanto fica de juros e multa? Ou é melhor parcelar?',
       NULL, NULL, (NOW() AT TIME ZONE 'America/Araguaina') - INTERVAL '25 minutes'
  FROM (SELECT id FROM obrigacaopendente WHERE empresa_id = 4 AND obrigacaorecorrente_id = 13
         ORDER BY datavencimento DESC LIMIT 1) o;
