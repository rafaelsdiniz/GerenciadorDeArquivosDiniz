-- dados de demonstração do módulo (seed-fechamento)
------------------------------------------------
-- FECHAMENTO MENSAL (quadro kanban do escritório)
-- competência padrão = mês anterior a hoje (obrigações que vencem neste mês);
-- prazo interno = dia 20 do mês seguinte à competência.
-- usuários 1-3 são os ADMIN do escritório: 1 Rafael, 2 Sr. Diniz, 3 Patrícia
------------------------------------------------

-- competência atual: empresas espalhadas pelas 4 etapas
INSERT INTO fechamentomensal (datacriacao, dataatualizacao, empresa_id, competencia, etapa, responsavel_id, prazo, observacao, concluidoem) VALUES
(NOW(), NOW() - INTERVAL '1 day',  1,  to_char(CURRENT_DATE - INTERVAL '1 month', 'MM/YYYY'), 'GUIAS_EMITIDAS',        2,    (date_trunc('month', CURRENT_DATE) + INTERVAL '19 days')::date, NULL, NULL),
(NOW(), NOW() - INTERVAL '3 hours', 2, to_char(CURRENT_DATE - INTERVAL '1 month', 'MM/YYYY'), 'AGUARDANDO_DOCUMENTOS', 3,    (date_trunc('month', CURRENT_DATE) + INTERVAL '19 days')::date, 'Faltam o extrato do Banco do Brasil e as notas de entrada. Cliente avisado pelo WhatsApp, prometeu enviar até sexta.', NULL),
(NOW(), NOW() - INTERVAL '2 days', 3,  to_char(CURRENT_DATE - INTERVAL '1 month', 'MM/YYYY'), 'AGUARDANDO_DOCUMENTOS', 1,    (date_trunc('month', CURRENT_DATE) + INTERVAL '19 days')::date, 'Sem a folha de ponto não dá para fechar a folha. Ligar para o Carlos.', NULL),
(NOW(), NOW() - INTERVAL '5 hours', 4, to_char(CURRENT_DATE - INTERVAL '1 month', 'MM/YYYY'), 'EM_APURACAO',           1,    (date_trunc('month', CURRENT_DATE) + INTERVAL '19 days')::date, NULL, NULL),
(NOW(), NOW() - INTERVAL '1 day',  5,  to_char(CURRENT_DATE - INTERVAL '1 month', 'MM/YYYY'), 'EM_APURACAO',           2,    (date_trunc('month', CURRENT_DATE) + INTERVAL '19 days')::date, 'Lucro Real: conferir os créditos de PIS/COFINS das notas de insumo antes de apurar.', NULL),
(NOW(), NOW() - INTERVAL '6 hours', 6, to_char(CURRENT_DATE - INTERVAL '1 month', 'MM/YYYY'), 'GUIAS_EMITIDAS',        3,    (date_trunc('month', CURRENT_DATE) + INTERVAL '19 days')::date, NULL, NULL),
(NOW(), NOW() - INTERVAL '4 days', 7,  to_char(CURRENT_DATE - INTERVAL '1 month', 'MM/YYYY'), 'AGUARDANDO_DOCUMENTOS', NULL, (date_trunc('month', CURRENT_DATE) + INTERVAL '19 days')::date, NULL, NULL),
(NOW(), NOW() - INTERVAL '2 hours', 8, to_char(CURRENT_DATE - INTERVAL '1 month', 'MM/YYYY'), 'EM_APURACAO',           3,    (date_trunc('month', CURRENT_DATE) + INTERVAL '19 days')::date, NULL, NULL),
(NOW(), NOW() - INTERVAL '2 days', 9,  to_char(CURRENT_DATE - INTERVAL '1 month', 'MM/YYYY'), 'CONCLUIDO',             1,    (date_trunc('month', CURRENT_DATE) + INTERVAL '19 days')::date, 'Tudo conferido e enviado ao cliente.', NOW() - INTERVAL '2 days'),
(NOW(), NOW() - INTERVAL '1 day',  10, to_char(CURRENT_DATE - INTERVAL '1 month', 'MM/YYYY'), 'CONCLUIDO',             2,    (date_trunc('month', CURRENT_DATE) + INTERVAL '19 days')::date, NULL, NOW() - INTERVAL '1 day');

-- competência anterior: tudo concluído (histórico)
INSERT INTO fechamentomensal (datacriacao, dataatualizacao, empresa_id, competencia, etapa, responsavel_id, prazo, observacao, concluidoem)
SELECT NOW() - INTERVAL '1 month', NOW() - INTERVAL '1 month',
       f.empresa_id,
       to_char(CURRENT_DATE - INTERVAL '2 months', 'MM/YYYY'),
       'CONCLUIDO',
       COALESCE(f.responsavel_id, 1),
       (date_trunc('month', CURRENT_DATE) - INTERVAL '1 month' + INTERVAL '19 days')::date,
       NULL,
       date_trunc('month', CURRENT_DATE) - INTERVAL '1 month' + (8 + f.empresa_id) * INTERVAL '1 day' + INTERVAL '16 hours'
  FROM fechamentomensal f
 WHERE f.competencia = to_char(CURRENT_DATE - INTERVAL '1 month', 'MM/YYYY');

-- coerência com o quadro: quem já está em "Guias emitidas" ou "Concluído" teve as obrigações
-- da competência entregues (as do "Concluído" também pagas); quem está "Em apuração" já
-- recebeu os documentos do cliente
UPDATE obrigacaopendente p SET status = 'ENTREGUE', dataentrega = GREATEST(CURRENT_DATE - (p.id % 3 + 1)::int, date_trunc('month', CURRENT_DATE)::date), datapagamento = NULL
  FROM obrigacaorecorrente r
 WHERE p.obrigacaorecorrente_id = r.id AND r.periodicidade <> 'ANUAL' AND p.status <> 'ENTREGUE'
   AND p.datavencimento >= date_trunc('month', CURRENT_DATE) AND p.datavencimento < date_trunc('month', CURRENT_DATE) + INTERVAL '1 month'
   AND (p.empresa_id IN (SELECT empresa_id FROM fechamentomensal WHERE competencia = to_char(CURRENT_DATE - INTERVAL '1 month', 'MM/YYYY') AND etapa IN ('GUIAS_EMITIDAS', 'CONCLUIDO'))
        OR (COALESCE(r.responsavel, 'ESCRITORIO') = 'CLIENTE'
            AND p.empresa_id IN (SELECT empresa_id FROM fechamentomensal WHERE competencia = to_char(CURRENT_DATE - INTERVAL '1 month', 'MM/YYYY') AND etapa = 'EM_APURACAO')));
UPDATE obrigacaopendente p SET datapagamento = p.dataentrega
  FROM obrigacaorecorrente r
 WHERE p.obrigacaorecorrente_id = r.id AND p.status = 'ENTREGUE' AND COALESCE(r.responsavel, 'ESCRITORIO') = 'ESCRITORIO'
   AND p.datavencimento >= date_trunc('month', CURRENT_DATE) AND p.datavencimento < date_trunc('month', CURRENT_DATE) + INTERVAL '1 month'
   AND p.empresa_id IN (SELECT empresa_id FROM fechamentomensal WHERE competencia = to_char(CURRENT_DATE - INTERVAL '1 month', 'MM/YYYY') AND etapa = 'CONCLUIDO');
