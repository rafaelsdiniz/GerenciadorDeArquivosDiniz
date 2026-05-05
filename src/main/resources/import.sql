BEGIN;

------------------------------------------------
-- EMPRESAS (com dados cadastrais completos)
------------------------------------------------

INSERT INTO empresa (
  datacriacao, dataatualizacao,
  nomefantasia, razaosocial, cnpj, telefone, email,
  dataabertura, situacaocadastral, naturezajuridica, site,
  logradouro, numero_endereco, complemento, bairro, cidade, uf, cep,
  inscricao_estadual, inscricao_municipal, regimetributario,
  cnaeprincipal, cnaessecundarios
) VALUES
(NOW(), NOW(),
 'Diniz Contabilidade',      'Diniz Contabilidade LTDA',       '11111111000101', '63999990001', 'contato@diniz.com.br',
 '2015-03-12', 'ATIVA', 'LTDA', 'https://diniz.com.br',
 'Avenida Joaquim Teotônio Segurado', '1200', 'Sala 305', 'Plano Diretor Sul', 'Palmas', 'TO', '77020100',
 '290012345', '12345678', 'SIMPLES_NACIONAL',
 '6920-6/01', '6920-6/02, 8219-9/99'),

(NOW(), NOW(),
 'Padaria Pão Quente',       'Pão Quente Comércio LTDA',       '22222222000102', '63999990002', 'financeiro@paoquente.com.br',
 '2018-07-05', 'ATIVA', 'LTDA', NULL,
 'Quadra 104 Norte Rua NE 01', '25', NULL, 'Plano Diretor Norte', 'Palmas', 'TO', '77006012',
 '290023456', '22233344', 'SIMPLES_NACIONAL',
 '1091-1/02', '4721-1/02, 5611-2/03'),

(NOW(), NOW(),
 'Auto Peças Tocantins',     'TO Peças LTDA',                  '33333333000103', '63999990003', 'fiscal@topecas.com.br',
 '2012-01-20', 'ATIVA', 'LTDA', 'https://topecas.com.br',
 'Avenida Teotônio Segurado', '2500', 'Loja 2', 'Plano Diretor Sul', 'Palmas', 'TO', '77022000',
 '290034567', '33344455', 'LUCRO_PRESUMIDO',
 '4530-7/01', '4530-7/04, 4520-0/01'),

(NOW(), NOW(),
 'Mercado Bom Preço',        'Bom Preço Supermercados LTDA',   '44444444000104', '63999990004', 'admin@bompreco.com.br',
 '2016-11-08', 'ATIVA', 'LTDA', NULL,
 'Quadra 306 Sul Alameda 15', '42', NULL, 'Plano Diretor Sul', 'Palmas', 'TO', '77021112',
 '290045678', '44455566', 'LUCRO_PRESUMIDO',
 '4711-3/02', '4721-1/04, 4729-6/99'),

(NOW(), NOW(),
 'Construtora Aurora',       'Aurora Engenharia LTDA',         '55555555000105', '63999990005', 'rh@aurora.com.br',
 '2008-04-22', 'ATIVA', 'LTDA', 'https://auroraeng.com.br',
 'Avenida LO-09', '750', 'Galpão B', 'Setor Industrial', 'Palmas', 'TO', '77016030',
 '290056789', '55566677', 'LUCRO_REAL',
 '4120-4/00', '4211-1/01, 7112-0/00, 4399-1/03'),

(NOW(), NOW(),
 'Farmácia Saúde+',          'Saúde Mais Farmacêutica LTDA',   '66666666000106', '63999990006', 'contabil@saudemais.com.br',
 '2019-09-15', 'ATIVA', 'EIRELI', NULL,
 'Quadra 204 Sul Alameda 5', '88', 'Loja 01', 'Plano Diretor Sul', 'Palmas', 'TO', '77020500',
 '290067890', '66677788', 'SIMPLES_NACIONAL',
 '4771-7/01', '4771-7/02'),

(NOW(), NOW(),
 'Restaurante Sabor Mineiro','Sabor Mineiro Alimentos LTDA',   '77777777000107', '63999990007', 'gerente@sabormineiro.com.br',
 '2020-06-01', 'ATIVA', 'LTDA', NULL,
 'Quadra 103 Norte Avenida JK', '15', NULL, 'Plano Diretor Norte', 'Palmas', 'TO', '77001020',
 '290078901', '77788899', 'SIMPLES_NACIONAL',
 '5611-2/01', '5612-1/00, 1094-5/00'),

(NOW(), NOW(),
 'Clínica Bem Estar',        'Bem Estar Serviços Médicos LTDA','88888888000108', '63999990008', 'contato@bemestar.com.br',
 '2017-02-14', 'ATIVA', 'SLU', 'https://clinicabemestar.com.br',
 'Avenida NS-02', '1340', 'Sala 12', 'Plano Diretor Norte', 'Palmas', 'TO', '77006400',
 '290089012', '88899900', 'LUCRO_PRESUMIDO',
 '8630-5/03', '8640-2/01, 8650-0/02'),

(NOW(), NOW(),
 'TransLog Cargas',          'TransLog Transportes LTDA',      '99999999000109', '63999990009', 'fiscal@translog.com.br',
 '2014-08-30', 'ATIVA', 'LTDA', NULL,
 'Rodovia TO-050 Km 5', 'S/N', 'Galpão 4', 'Zona Rural', 'Porto Nacional', 'TO', '77500000',
 '290090123', '99900011', 'LUCRO_REAL',
 '4930-2/02', '4930-2/01, 5229-0/02'),

(NOW(), NOW(),
 'Escola Aprender+',         'Aprender Mais Educação LTDA',    '10101010000110', '63999990010', 'diretoria@aprendermais.com.br',
 '2013-02-05', 'ATIVA', 'LTDA', 'https://aprendermais.edu.br',
 'Quadra 405 Sul Alameda 12', '200', 'Bloco A', 'Plano Diretor Sul', 'Palmas', 'TO', '77022090',
 '290001234', '10111213', 'SIMPLES_NACIONAL',
 '8513-9/00', '8520-1/00, 8599-6/04');

------------------------------------------------
-- USUARIOS
-- senha: 123456 (texto puro, apenas p/ teste)
------------------------------------------------

INSERT INTO usuario (datacriacao, dataatualizacao, nome, email, senha, perfilusuario, empresa_id) VALUES
-- Admins do escritório Diniz
(NOW(), NOW(), 'Rafael Diniz',     'rafael@diniz.com.br',        '123456', 'ADMIN',       1),
(NOW(), NOW(), 'Sr. Diniz',        'sr.diniz@diniz.com.br',      '123456', 'ADMIN',       1),
(NOW(), NOW(), 'Patrícia Diniz',   'patricia@diniz.com.br',      '123456', 'ADMIN',       1),
-- Funcionários Diniz
(NOW(), NOW(), 'João Silva',       'joao@diniz.com.br',          '123456', 'FUNCIONARIO', 1),
(NOW(), NOW(), 'Mariana Costa',    'mariana@diniz.com.br',       '123456', 'FUNCIONARIO', 1),
(NOW(), NOW(), 'Bruno Oliveira',   'bruno@diniz.com.br',         '123456', 'FUNCIONARIO', 1),
-- Funcionários dos clientes
(NOW(), NOW(), 'Maria Souza',      'maria@paoquente.com.br',     '123456', 'FUNCIONARIO', 2),
(NOW(), NOW(), 'Carlos Lima',      'carlos@topecas.com.br',      '123456', 'FUNCIONARIO', 3),
(NOW(), NOW(), 'Ana Costa',        'ana@bompreco.com.br',        '123456', 'FUNCIONARIO', 4),
(NOW(), NOW(), 'Pedro Rocha',      'pedro@aurora.com.br',        '123456', 'FUNCIONARIO', 5),
(NOW(), NOW(), 'Luiza Mendes',     'luiza@saudemais.com.br',     '123456', 'FUNCIONARIO', 6),
(NOW(), NOW(), 'Felipe Azevedo',   'felipe@sabormineiro.com.br', '123456', 'FUNCIONARIO', 7),
(NOW(), NOW(), 'Juliana Ramos',    'juliana@bemestar.com.br',    '123456', 'FUNCIONARIO', 8),
(NOW(), NOW(), 'Ricardo Torres',   'ricardo@translog.com.br',    '123456', 'FUNCIONARIO', 9),
(NOW(), NOW(), 'Camila Pereira',   'camila@aprendermais.com.br', '123456', 'FUNCIONARIO', 10);

------------------------------------------------
-- SOCIOS
------------------------------------------------

INSERT INTO socio (datacriacao, dataatualizacao, nome, cpf, empresa_id, participacao, administrador) VALUES
(NOW(), NOW(), 'Rafael Diniz',     '12345678901', 1,  60.00, TRUE),
(NOW(), NOW(), 'Sr. Diniz',        '98765432100', 1,  40.00, FALSE),
(NOW(), NOW(), 'João Pereira',     '23456789012', 2,  70.00, TRUE),
(NOW(), NOW(), 'Renata Pereira',   '32145678901', 2,  30.00, FALSE),
(NOW(), NOW(), 'Maria Alves',      '34567890123', 3, 100.00, TRUE),
(NOW(), NOW(), 'Carlos Mendes',    '45678901234', 4, 100.00, TRUE),
(NOW(), NOW(), 'Ana Paula Souza',  '56789012345', 5,  55.00, TRUE),
(NOW(), NOW(), 'Bruno Aurora',     '65432198700', 5,  45.00, FALSE),
(NOW(), NOW(), 'Luiza Santos',     '76543219800', 6, 100.00, TRUE),
(NOW(), NOW(), 'Felipe Costa',     '87654321900', 7, 100.00, TRUE),
(NOW(), NOW(), 'Roberto Machado',  '11122233344', 8,  50.00, TRUE),
(NOW(), NOW(), 'Sandra Machado',   '22233344455', 8,  50.00, FALSE),
(NOW(), NOW(), 'Marcos Ribeiro',   '33344455566', 9, 100.00, TRUE),
(NOW(), NOW(), 'Eliane Cardoso',   '44455566677', 10,100.00, TRUE);

------------------------------------------------
-- PASTAS (hierarquia: pai -> filhas)
------------------------------------------------

-- pastas raiz (1..18)
INSERT INTO pasta (datacriacao, dataatualizacao, nome, descricao, empresa_id, pastapai_id) VALUES
(NOW(), NOW(), 'Notas Fiscais',       'Documentos fiscais',          1, NULL),  -- 1
(NOW(), NOW(), 'Contratos',           'Contratos empresariais',      1, NULL),  -- 2
(NOW(), NOW(), 'Impostos',            'Guias e DARFs',               1, NULL),  -- 3
(NOW(), NOW(), 'Folha de Pagamento',  'Departamento pessoal',        1, NULL),  -- 4
(NOW(), NOW(), 'Notas Fiscais',       'NFs Padaria',                 2, NULL),  -- 5
(NOW(), NOW(), 'Impostos',            'DAS / DARF',                  2, NULL),  -- 6
(NOW(), NOW(), 'Notas Fiscais',       'NFs Auto Peças',              3, NULL),  -- 7
(NOW(), NOW(), 'Folha de Pagamento',  'Folha mensal',                3, NULL),  -- 8
(NOW(), NOW(), 'Impostos',            'Tributos Mercado',            4, NULL),  -- 9
(NOW(), NOW(), 'Documentos Gerais',   'Diversos',                    4, NULL),  -- 10
(NOW(), NOW(), 'Folha de Pagamento',  'DP Construtora',              5, NULL),  -- 11
(NOW(), NOW(), 'Contratos',           'Obras e prestações',          5, NULL),  -- 12
(NOW(), NOW(), 'Notas Fiscais',       'NFs Farmácia',                6, NULL),  -- 13
(NOW(), NOW(), 'Notas Fiscais',       'NFs Restaurante',             7, NULL),  -- 14
(NOW(), NOW(), 'Documentos Gerais',   'Docs Clínica',                8, NULL),  -- 15
(NOW(), NOW(), 'Folha de Pagamento',  'DP Transportes',              9, NULL),  -- 16
(NOW(), NOW(), 'Impostos',            'Tributos Transportes',        9, NULL),  -- 17
(NOW(), NOW(), 'Notas Fiscais',       'NFs Escola',                 10, NULL);  -- 18

-- subpastas (id 19+)
INSERT INTO pasta (datacriacao, dataatualizacao, nome, descricao, empresa_id, pastapai_id) VALUES
(NOW(), NOW(), '2026',     'Ano corrente',           1, 1),   -- 19
(NOW(), NOW(), '2025',     'Histórico',              1, 1),   -- 20
(NOW(), NOW(), 'Entrada',  'NFs de entrada',         1, 1),   -- 21
(NOW(), NOW(), 'Saída',    'NFs de saída',           1, 1),   -- 22
(NOW(), NOW(), 'DARF',     'DARFs federais',         1, 3),   -- 23
(NOW(), NOW(), 'DAS',      'Simples Nacional',       1, 3),   -- 24
(NOW(), NOW(), '2026',     'Folhas 2026',            1, 4),   -- 25
(NOW(), NOW(), '2025',     'Folhas 2025',            1, 4),   -- 26
(NOW(), NOW(), '2026',     'Ano corrente',           2, 5),   -- 27
(NOW(), NOW(), 'DAS',      'Simples Nacional',       2, 6),   -- 28
(NOW(), NOW(), 'Entrada',  'NFs entrada peças',      3, 7),   -- 29
(NOW(), NOW(), 'Saída',    'NFs saída peças',        3, 7),   -- 30
(NOW(), NOW(), '2026',     'Folhas 2026',            3, 8),   -- 31
(NOW(), NOW(), 'Certidões','Negativas',              1, 2),   -- 32
(NOW(), NOW(), 'Balancetes','Mensais',               1, 2);   -- 33

------------------------------------------------
-- OBRIGACOES RECORRENTES
------------------------------------------------

INSERT INTO obrigacaorecorrente (datacriacao, dataatualizacao, nome, descricao, periodicidade, diavencimento, tipoarquivoesperado, ativo, empresa_id) VALUES
-- empresa 1 (Diniz)
(NOW(), NOW(), 'DAS — Simples Nacional', 'Documento de Arrecadação do Simples',          'MENSAL',     20, 'PDF',  TRUE,  1),   -- 1
(NOW(), NOW(), 'DARF — IRPJ',            'Imposto de Renda Pessoa Jurídica',             'TRIMESTRAL', 25, 'PDF',  TRUE,  1),   -- 2
(NOW(), NOW(), 'FGTS',                   'Recolhimento mensal FGTS',                     'MENSAL',     7,  'PDF',  TRUE,  1),   -- 3
(NOW(), NOW(), 'INSS',                   'Contribuição previdenciária',                  'MENSAL',     20, 'PDF',  TRUE,  1),   -- 4
(NOW(), NOW(), 'Balancete Mensal',       'Balancete contábil',                           'MENSAL',     30, 'PDF',  TRUE,  1),   -- 5
(NOW(), NOW(), 'DEFIS',                  'Declaração anual do Simples',                  'ANUAL',      31, 'PDF',  TRUE,  1),   -- 6
-- empresa 2 (Padaria)
(NOW(), NOW(), 'DAS — Simples Nacional', 'DAS Padaria',                                  'MENSAL',     20, 'PDF',  TRUE,  2),   -- 7
(NOW(), NOW(), 'FGTS',                   'FGTS Padaria',                                 'MENSAL',     7,  'PDF',  TRUE,  2),   -- 8
-- empresa 3 (Auto Peças)
(NOW(), NOW(), 'ICMS',                   'Imposto sobre Circulação de Mercadorias',      'MENSAL',     15, 'PDF',  TRUE,  3),   -- 9
(NOW(), NOW(), 'DAS — Simples Nacional', 'DAS Auto Peças',                               'MENSAL',     20, 'PDF',  TRUE,  3),   -- 10
(NOW(), NOW(), 'INSS',                   'INSS Auto Peças',                              'MENSAL',     20, 'PDF',  TRUE,  3),   -- 11
-- empresa 4 (Mercado)
(NOW(), NOW(), 'ICMS',                   'ICMS Mercado',                                 'MENSAL',     15, 'PDF',  TRUE,  4),   -- 12
(NOW(), NOW(), 'DAS — Simples Nacional', 'DAS Mercado',                                  'MENSAL',     20, 'PDF',  TRUE,  4),   -- 13
-- empresa 5 (Construtora)
(NOW(), NOW(), 'INSS Obra',              'INSS de obra (CEI)',                           'MENSAL',     20, 'PDF',  TRUE,  5),   -- 14
(NOW(), NOW(), 'IRPJ Lucro Presumido',   'IRPJ trimestral',                              'TRIMESTRAL', 30, 'PDF',  TRUE,  5),   -- 15
-- empresa 6 (Farmácia)
(NOW(), NOW(), 'DAS — Simples Nacional', 'DAS Farmácia',                                 'MENSAL',     20, 'PDF',  TRUE,  6),   -- 16
-- empresa 7 (Restaurante)
(NOW(), NOW(), 'DAS — Simples Nacional', 'DAS Restaurante',                              'MENSAL',     20, 'PDF',  TRUE,  7),   -- 17
(NOW(), NOW(), 'FGTS',                   'FGTS Restaurante',                             'MENSAL',     7,  'PDF',  FALSE, 7),   -- 18
-- empresa 8 (Clínica)
(NOW(), NOW(), 'DAS — Simples Nacional', 'DAS Clínica',                                  'MENSAL',     20, 'PDF',  TRUE,  8),   -- 19
(NOW(), NOW(), 'ISS',                    'Imposto Sobre Serviços',                       'MENSAL',     10, 'PDF',  TRUE,  8),   -- 20
-- empresa 9 (Transportes)
(NOW(), NOW(), 'ICMS',                   'ICMS Transportes',                             'MENSAL',     15, 'PDF',  TRUE,  9),   -- 21
(NOW(), NOW(), 'FGTS',                   'FGTS Transportes',                             'MENSAL',     7,  'PDF',  TRUE,  9),   -- 22
(NOW(), NOW(), 'INSS',                   'INSS Transportes',                             'MENSAL',     20, 'PDF',  TRUE,  9),   -- 23
-- empresa 10 (Escola)
(NOW(), NOW(), 'DAS — Simples Nacional', 'DAS Escola',                                   'MENSAL',     20, 'PDF',  TRUE,  10),  -- 24
(NOW(), NOW(), 'ISS',                    'ISS Escola',                                   'MENSAL',     10, 'PDF',  TRUE,  10);  -- 25

------------------------------------------------
-- OBRIGACOES PENDENTES
-- hoje ~ 2026-04-22
-- mistura: ENTREGUE (passado entregue), VENCIDA (passado sem entrega), PENDENTE (atual/futuro)
------------------------------------------------

INSERT INTO obrigacaopendente (datacriacao, dataatualizacao, obrigacaorecorrente_id, empresa_id, datavencimento, dataentrega, status) VALUES
-- empresa 1 (Diniz) — histórico + ciclo atual
(NOW(), NOW(), 1, 1, '2026-01-20', '2026-01-19', 'ENTREGUE'),   -- 1
(NOW(), NOW(), 1, 1, '2026-02-20', '2026-02-19', 'ENTREGUE'),   -- 2
(NOW(), NOW(), 1, 1, '2026-03-20', '2026-03-21', 'ENTREGUE'),   -- 3
(NOW(), NOW(), 1, 1, '2026-04-20', NULL,         'VENCIDA'),    -- 4  (passou de hoje sem entrega)
(NOW(), NOW(), 1, 1, '2026-05-20', NULL,         'PENDENTE'),   -- 5
(NOW(), NOW(), 2, 1, '2026-04-25', NULL,         'PENDENTE'),   -- 6  (DARF IRPJ — vence em dias)
(NOW(), NOW(), 3, 1, '2026-02-07', '2026-02-06', 'ENTREGUE'),   -- 7  FGTS fev
(NOW(), NOW(), 3, 1, '2026-03-07', '2026-03-07', 'ENTREGUE'),   -- 8  FGTS mar
(NOW(), NOW(), 3, 1, '2026-04-07', NULL,         'VENCIDA'),    -- 9  FGTS abr atrasado
(NOW(), NOW(), 3, 1, '2026-05-07', NULL,         'PENDENTE'),   -- 10
(NOW(), NOW(), 4, 1, '2026-02-20', '2026-02-20', 'ENTREGUE'),   -- 11
(NOW(), NOW(), 4, 1, '2026-03-20', '2026-03-18', 'ENTREGUE'),   -- 12
(NOW(), NOW(), 4, 1, '2026-04-20', NULL,         'VENCIDA'),    -- 13 INSS abr atrasado
(NOW(), NOW(), 5, 1, '2026-02-28', '2026-02-28', 'ENTREGUE'),   -- 14 Balancete fev
(NOW(), NOW(), 5, 1, '2026-03-30', '2026-04-02', 'ENTREGUE'),   -- 15
(NOW(), NOW(), 5, 1, '2026-04-30', NULL,         'PENDENTE'),   -- 16
-- empresa 2 (Padaria)
(NOW(), NOW(), 7, 2, '2026-02-20', '2026-02-20', 'ENTREGUE'),   -- 17
(NOW(), NOW(), 7, 2, '2026-03-20', '2026-03-19', 'ENTREGUE'),   -- 18
(NOW(), NOW(), 7, 2, '2026-04-20', NULL,         'VENCIDA'),    -- 19
(NOW(), NOW(), 7, 2, '2026-05-20', NULL,         'PENDENTE'),   -- 20
(NOW(), NOW(), 8, 2, '2026-03-07', '2026-03-07', 'ENTREGUE'),   -- 21
(NOW(), NOW(), 8, 2, '2026-04-07', NULL,         'VENCIDA'),    -- 22
-- empresa 3 (Auto Peças)
(NOW(), NOW(), 9, 3, '2026-02-15', '2026-02-15', 'ENTREGUE'),   -- 23
(NOW(), NOW(), 9, 3, '2026-03-15', '2026-03-15', 'ENTREGUE'),   -- 24
(NOW(), NOW(), 9, 3, '2026-04-15', NULL,         'VENCIDA'),    -- 25
(NOW(), NOW(), 10, 3, '2026-03-20', '2026-03-20', 'ENTREGUE'),  -- 26
(NOW(), NOW(), 10, 3, '2026-04-20', NULL,         'VENCIDA'),   -- 27
(NOW(), NOW(), 11, 3, '2026-04-20', NULL,         'VENCIDA'),   -- 28
-- empresa 4 (Mercado)
(NOW(), NOW(), 12, 4, '2026-03-15', '2026-03-14', 'ENTREGUE'),  -- 29
(NOW(), NOW(), 12, 4, '2026-04-15', NULL,         'VENCIDA'),   -- 30
(NOW(), NOW(), 13, 4, '2026-04-20', NULL,         'VENCIDA'),   -- 31
-- empresa 5 (Construtora)
(NOW(), NOW(), 14, 5, '2026-03-20', '2026-03-20', 'ENTREGUE'),  -- 32
(NOW(), NOW(), 14, 5, '2026-04-20', NULL,         'VENCIDA'),   -- 33
(NOW(), NOW(), 15, 5, '2026-04-30', NULL,         'PENDENTE'),  -- 34
-- empresa 6 (Farmácia)
(NOW(), NOW(), 16, 6, '2026-02-20', '2026-02-21', 'ENTREGUE'),  -- 35
(NOW(), NOW(), 16, 6, '2026-03-20', '2026-03-21', 'ENTREGUE'),  -- 36
(NOW(), NOW(), 16, 6, '2026-04-20', NULL,         'VENCIDA'),   -- 37
-- empresa 7 (Restaurante)
(NOW(), NOW(), 17, 7, '2026-03-20', '2026-03-20', 'ENTREGUE'),  -- 38
(NOW(), NOW(), 17, 7, '2026-04-20', NULL,         'PENDENTE'),  -- 39
-- empresa 8 (Clínica)
(NOW(), NOW(), 19, 8, '2026-03-20', '2026-03-19', 'ENTREGUE'),  -- 40
(NOW(), NOW(), 19, 8, '2026-04-20', NULL,         'PENDENTE'),  -- 41
(NOW(), NOW(), 20, 8, '2026-03-10', '2026-03-10', 'ENTREGUE'),  -- 42
(NOW(), NOW(), 20, 8, '2026-04-10', NULL,         'VENCIDA'),   -- 43
(NOW(), NOW(), 20, 8, '2026-05-10', NULL,         'PENDENTE'),  -- 44
-- empresa 9 (Transportes)
(NOW(), NOW(), 21, 9, '2026-03-15', '2026-03-14', 'ENTREGUE'),  -- 45
(NOW(), NOW(), 21, 9, '2026-04-15', NULL,         'VENCIDA'),   -- 46
(NOW(), NOW(), 22, 9, '2026-04-07', NULL,         'VENCIDA'),   -- 47
(NOW(), NOW(), 23, 9, '2026-04-20', NULL,         'PENDENTE'),  -- 48
-- empresa 10 (Escola)
(NOW(), NOW(), 24, 10, '2026-03-20', '2026-03-20', 'ENTREGUE'), -- 49
(NOW(), NOW(), 24, 10, '2026-04-20', NULL,         'PENDENTE'), -- 50
(NOW(), NOW(), 25, 10, '2026-04-10', NULL,         'VENCIDA'),  -- 51
(NOW(), NOW(), 25, 10, '2026-05-10', NULL,         'PENDENTE'); -- 52

------------------------------------------------
-- ARQUIVOS (entregues, pendentes, vencidos, arquivados, lixeira)
-- Campos: nome (interno/UUID), nomeoriginal, tamanho, tipoarquivo, caminho, hash,
--         descricao, datavencimento, status, categoriafiscal, excluidoem, FKs, obrigacaopendente_id
-- Vencimentos ao redor de hoje (2026-04-22)
------------------------------------------------

INSERT INTO arquivo
  (datacriacao, dataatualizacao, nome, nomeoriginal, tamanho, tipoarquivo, caminho, hash,
   descricao, datavencimento, status, categoriafiscal, excluidoem,
   empresa_id, usuario_id, pasta_id, obrigacaopendente_id)
VALUES
-- ====== ENTREGUES (vinculados a obrigações entregues) ======
(NOW(), NOW(), 'a01.pdf', 'das-jan-2026.pdf',         138000, 'PDF', 'empresa-1/pasta-24/a01.pdf', 'a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4',
 'DAS de janeiro/2026',       '2026-01-20', 'ENTREGUE', 'DAS',           NULL, 1, 1, 24, 1),
(NOW(), NOW(), 'a02.pdf', 'das-fev-2026.pdf',         142000, 'PDF', 'empresa-1/pasta-24/a02.pdf', 'b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5',
 'DAS de fevereiro/2026',     '2026-02-20', 'ENTREGUE', 'DAS',           NULL, 1, 1, 24, 2),
(NOW(), NOW(), 'a03.pdf', 'das-mar-2026.pdf',         145000, 'PDF', 'empresa-1/pasta-24/a03.pdf', 'c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6',
 'DAS de março/2026',         '2026-03-20', 'ENTREGUE', 'DAS',           NULL, 1, 4, 24, 3),
(NOW(), NOW(), 'a04.pdf', 'inss-fev-2026.pdf',        86000,  'PDF', 'empresa-1/pasta-23/a04.pdf', 'd4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1',
 'Guia INSS fevereiro',       '2026-02-20', 'ENTREGUE', 'GUIA_IMPOSTO',  NULL, 1, 4, 23, 11),
(NOW(), NOW(), 'a05.pdf', 'inss-mar-2026.pdf',        88000,  'PDF', 'empresa-1/pasta-23/a05.pdf', 'e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2',
 'Guia INSS março',           '2026-03-20', 'ENTREGUE', 'GUIA_IMPOSTO',  NULL, 1, 5, 23, 12),
(NOW(), NOW(), 'a06.pdf', 'fgts-fev-2026.pdf',        90000,  'PDF', 'empresa-1/pasta-23/a06.pdf', 'f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3',
 'FGTS fevereiro',            '2026-02-07', 'ENTREGUE', 'GUIA_IMPOSTO',  NULL, 1, 5, 23, 7),
(NOW(), NOW(), 'a07.pdf', 'fgts-mar-2026.pdf',        92000,  'PDF', 'empresa-1/pasta-23/a07.pdf', '1a2b3c4d5e6f1a2b3c4d5e6f1a2b3c4d',
 'FGTS março',                '2026-03-07', 'ENTREGUE', 'GUIA_IMPOSTO',  NULL, 1, 6, 23, 8),
(NOW(), NOW(), 'a08.pdf', 'balancete-fev-2026.pdf',   280000, 'PDF', 'empresa-1/pasta-33/a08.pdf', '2b3c4d5e6f1a2b3c4d5e6f1a2b3c4d5e',
 'Balancete fev/2026',        '2026-02-28', 'ENTREGUE', 'BALANCETE',     NULL, 1, 4, 33, 14),
(NOW(), NOW(), 'a09.pdf', 'balancete-mar-2026.pdf',   290000, 'PDF', 'empresa-1/pasta-33/a09.pdf', '3c4d5e6f1a2b3c4d5e6f1a2b3c4d5e6f',
 'Balancete mar/2026',        '2026-03-30', 'ENTREGUE', 'BALANCETE',     NULL, 1, 5, 33, 15),
(NOW(), NOW(), 'a10.pdf', 'das-padaria-fev.pdf',      115000, 'PDF', 'empresa-2/pasta-28/a10.pdf', '4d5e6f1a2b3c4d5e6f1a2b3c4d5e6f1a',
 'DAS Padaria fev',           '2026-02-20', 'ENTREGUE', 'DAS',           NULL, 2, 7, 28, 17),
(NOW(), NOW(), 'a11.pdf', 'das-padaria-mar.pdf',      120000, 'PDF', 'empresa-2/pasta-28/a11.pdf', '5e6f1a2b3c4d5e6f1a2b3c4d5e6f1a2b',
 'DAS Padaria mar',           '2026-03-20', 'ENTREGUE', 'DAS',           NULL, 2, 7, 28, 18),
(NOW(), NOW(), 'a12.pdf', 'fgts-padaria-mar.pdf',     80000,  'PDF', 'empresa-2/pasta-6/a12.pdf',  '6f1a2b3c4d5e6f1a2b3c4d5e6f1a2b3c',
 'FGTS Padaria mar',          '2026-03-07', 'ENTREGUE', 'GUIA_IMPOSTO',  NULL, 2, 7, 6,  21),
(NOW(), NOW(), 'a13.pdf', 'icms-autopecas-fev.pdf',   170000, 'PDF', 'empresa-3/pasta-30/a13.pdf', '7a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d',
 'ICMS Auto Peças fev',       '2026-02-15', 'ENTREGUE', 'GUIA_IMPOSTO',  NULL, 3, 8, 30, 23),
(NOW(), NOW(), 'a14.pdf', 'icms-autopecas-mar.pdf',   175000, 'PDF', 'empresa-3/pasta-30/a14.pdf', '8b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e',
 'ICMS Auto Peças mar',       '2026-03-15', 'ENTREGUE', 'GUIA_IMPOSTO',  NULL, 3, 8, 30, 24),
(NOW(), NOW(), 'a15.pdf', 'das-autopecas-mar.pdf',    125000, 'PDF', 'empresa-3/pasta-7/a15.pdf',  '9c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f',
 'DAS Auto Peças mar',        '2026-03-20', 'ENTREGUE', 'DAS',           NULL, 3, 8, 7,  26),
(NOW(), NOW(), 'a16.pdf', 'icms-mercado-mar.pdf',     160000, 'PDF', 'empresa-4/pasta-9/a16.pdf',  '0d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a',
 'ICMS Mercado mar',          '2026-03-15', 'ENTREGUE', 'GUIA_IMPOSTO',  NULL, 4, 9, 9,  29),
(NOW(), NOW(), 'a17.pdf', 'inss-obra-mar.pdf',        98000,  'PDF', 'empresa-5/pasta-11/a17.pdf', '1e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b',
 'INSS Obra mar',             '2026-03-20', 'ENTREGUE', 'GUIA_IMPOSTO',  NULL, 5, 10, 11, 32),
(NOW(), NOW(), 'a18.pdf', 'das-farmacia-fev.pdf',     116000, 'PDF', 'empresa-6/pasta-13/a18.pdf', '2f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c',
 'DAS Farmácia fev',          '2026-02-20', 'ENTREGUE', 'DAS',           NULL, 6, 11, 13, 35),
(NOW(), NOW(), 'a19.pdf', 'das-farmacia-mar.pdf',     118000, 'PDF', 'empresa-6/pasta-13/a19.pdf', '3a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d',
 'DAS Farmácia mar',          '2026-03-20', 'ENTREGUE', 'DAS',           NULL, 6, 11, 13, 36),
(NOW(), NOW(), 'a20.pdf', 'das-restaurante-mar.pdf',  122000, 'PDF', 'empresa-7/pasta-14/a20.pdf', '4b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e',
 'DAS Restaurante mar',       '2026-03-20', 'ENTREGUE', 'DAS',           NULL, 7, 12, 14, 38),
(NOW(), NOW(), 'a21.pdf', 'das-clinica-mar.pdf',      119000, 'PDF', 'empresa-8/pasta-15/a21.pdf', '5c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f',
 'DAS Clínica mar',           '2026-03-20', 'ENTREGUE', 'DAS',           NULL, 8, 13, 15, 40),
(NOW(), NOW(), 'a22.pdf', 'iss-clinica-mar.pdf',      75000,  'PDF', 'empresa-8/pasta-15/a22.pdf', '6d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a',
 'ISS Clínica mar',           '2026-03-10', 'ENTREGUE', 'GUIA_IMPOSTO',  NULL, 8, 13, 15, 42),
(NOW(), NOW(), 'a23.pdf', 'icms-translog-mar.pdf',    180000, 'PDF', 'empresa-9/pasta-17/a23.pdf', '7e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b',
 'ICMS Transportes mar',      '2026-03-15', 'ENTREGUE', 'GUIA_IMPOSTO',  NULL, 9, 14, 17, 45),
(NOW(), NOW(), 'a24.pdf', 'das-escola-mar.pdf',       117000, 'PDF', 'empresa-10/pasta-18/a24.pdf','8f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c',
 'DAS Escola mar',            '2026-03-20', 'ENTREGUE', 'DAS',           NULL, 10, 15, 18, 49),

-- ====== PENDENTES (vencimento atual / futuro próximo) ======
(NOW(), NOW(), 'a25.pdf', 'das-abr-2026.pdf',         140000, 'PDF', 'empresa-1/pasta-24/a25.pdf', 'a9b8c7d6e5f4a9b8c7d6e5f4a9b8c7d6',
 'DAS abril (rascunho)',      '2026-05-20', 'PENDENTE', 'DAS',           NULL, 1, 4, 24, 5),
(NOW(), NOW(), 'a26.pdf', 'darf-irpj-2026Q1.pdf',     160000, 'PDF', 'empresa-1/pasta-23/a26.pdf', 'b8c7d6e5f4a9b8c7d6e5f4a9b8c7d6e5',
 'DARF IRPJ 1º trimestre',    '2026-04-25', 'PENDENTE', 'DARF',          NULL, 1, 1, 23, 6),
(NOW(), NOW(), 'a27.xml', 'nfe-saida-001.xml',        45000,  'XML', 'empresa-1/pasta-22/a27.xml', 'c7d6e5f4a9b8c7d6e5f4a9b8c7d6e5f4',
 'NF-e de saída 001',         '2026-04-25', 'PENDENTE', 'NFE_SAIDA',     NULL, 1, 4, 22, NULL),
(NOW(), NOW(), 'a28.xml', 'nfe-saida-002.xml',        47000,  'XML', 'empresa-1/pasta-22/a28.xml', 'd6e5f4a9b8c7d6e5f4a9b8c7d6e5f4a9',
 'NF-e de saída 002',         '2026-04-30', 'PENDENTE', 'NFE_SAIDA',     NULL, 1, 4, 22, NULL),
(NOW(), NOW(), 'a29.xml', 'nfe-entrada-014.xml',      48000,  'XML', 'empresa-1/pasta-21/a29.xml', 'e5f4a9b8c7d6e5f4a9b8c7d6e5f4a9b8',
 'NF-e de entrada 014',       '2026-04-30', 'PENDENTE', 'NFE_ENTRADA',   NULL, 1, 5, 21, NULL),
(NOW(), NOW(), 'a30.pdf', 'folha-abr-2026.pdf',       210000, 'PDF', 'empresa-1/pasta-25/a30.pdf', 'f4a9b8c7d6e5f4a9b8c7d6e5f4a9b8c7',
 'Folha abril (rascunho)',    '2026-05-05', 'PENDENTE', 'FOLHA_PAGAMENTO', NULL, 1, 6, 25, NULL),
(NOW(), NOW(), 'a31.pdf', 'das-padaria-mai.pdf',      123000, 'PDF', 'empresa-2/pasta-28/a31.pdf', '11223344556677881122334455667788',
 'DAS Padaria mai (rascunho)','2026-05-20', 'PENDENTE', 'DAS',           NULL, 2, 7, 28, 20),
(NOW(), NOW(), 'a32.pdf', 'das-restaurante-abr.pdf',  124000, 'PDF', 'empresa-7/pasta-14/a32.pdf', '22334455667788992233445566778899',
 'DAS Restaurante abr',       '2026-04-20', 'PENDENTE', 'DAS',           NULL, 7, 12, 14, 39),
(NOW(), NOW(), 'a33.pdf', 'das-clinica-abr.pdf',      120000, 'PDF', 'empresa-8/pasta-15/a33.pdf', '3344556677889900aabbccdd11223344',
 'DAS Clínica abr',           '2026-04-20', 'PENDENTE', 'DAS',           NULL, 8, 13, 15, 41),
(NOW(), NOW(), 'a34.pdf', 'inss-translog-abr.pdf',    99000,  'PDF', 'empresa-9/pasta-16/a34.pdf', '44556677889900aabbccddee11223344',
 'INSS Transportes abr',      '2026-04-20', 'PENDENTE', 'GUIA_IMPOSTO',  NULL, 9, 14, 16, 48),
(NOW(), NOW(), 'a35.pdf', 'das-escola-abr.pdf',       119000, 'PDF', 'empresa-10/pasta-18/a35.pdf','55667788990011223344556677889900',
 'DAS Escola abr',            '2026-04-20', 'PENDENTE', 'DAS',           NULL, 10, 15, 18, 50),

-- ====== VENCIDOS (sem entrega, já passou o prazo) ======
(NOW(), NOW(), 'a36.pdf', 'fgts-padaria-abr.pdf',     81000,  'PDF', 'empresa-2/pasta-6/a36.pdf',  '66778899aabbccdd6677889900aabbcc',
 'FGTS Padaria abril (atrasado)', '2026-04-07', 'VENCIDO', 'GUIA_IMPOSTO', NULL, 2, 7, 6,  22),
(NOW(), NOW(), 'a37.pdf', 'icms-autopecas-abr.pdf',   168000, 'PDF', 'empresa-3/pasta-7/a37.pdf',  '778899aabbccddee778899aabbccddee',
 'ICMS Auto Peças abr',       '2026-04-15', 'VENCIDO', 'GUIA_IMPOSTO',  NULL, 3, 8, 7,  25),
(NOW(), NOW(), 'a38.pdf', 'das-autopecas-abr.pdf',    126000, 'PDF', 'empresa-3/pasta-7/a38.pdf',  '8899aabbccddeeff8899aabbccddeeff',
 'DAS Auto Peças abr',        '2026-04-20', 'VENCIDO', 'DAS',           NULL, 3, 8, 7,  27),
(NOW(), NOW(), 'a39.pdf', 'inss-autopecas-abr.pdf',   89000,  'PDF', 'empresa-3/pasta-8/a39.pdf',  '99aabbccddeeff0099aabbccddeeff00',
 'INSS Auto Peças abr',       '2026-04-20', 'VENCIDO', 'GUIA_IMPOSTO',  NULL, 3, 8, 8,  28),
(NOW(), NOW(), 'a40.pdf', 'icms-mercado-abr.pdf',     155000, 'PDF', 'empresa-4/pasta-9/a40.pdf',  'aabbccddeeff0011aabbccddeeff0011',
 'ICMS Mercado abr',          '2026-04-15', 'VENCIDO', 'GUIA_IMPOSTO',  NULL, 4, 9, 9,  30),
(NOW(), NOW(), 'a41.pdf', 'das-mercado-abr.pdf',      121000, 'PDF', 'empresa-4/pasta-9/a41.pdf',  'bbccddeeff001122bbccddeeff001122',
 'DAS Mercado abr',           '2026-04-20', 'VENCIDO', 'DAS',           NULL, 4, 9, 9,  31),
(NOW(), NOW(), 'a42.pdf', 'inss-obra-abr.pdf',        99500,  'PDF', 'empresa-5/pasta-11/a42.pdf', 'ccddeeff00112233ccddeeff00112233',
 'INSS Obra abr',             '2026-04-20', 'VENCIDO', 'GUIA_IMPOSTO',  NULL, 5, 10, 11, 33),
(NOW(), NOW(), 'a43.pdf', 'das-farmacia-abr.pdf',     117000, 'PDF', 'empresa-6/pasta-13/a43.pdf', 'ddeeff0011223344ddeeff0011223344',
 'DAS Farmácia abr',          '2026-04-20', 'VENCIDO', 'DAS',           NULL, 6, 11, 13, 37),
(NOW(), NOW(), 'a44.pdf', 'iss-clinica-abr.pdf',      76000,  'PDF', 'empresa-8/pasta-15/a44.pdf', 'eeff001122334455eeff001122334455',
 'ISS Clínica abr',           '2026-04-10', 'VENCIDO', 'GUIA_IMPOSTO',  NULL, 8, 13, 15, 43),
(NOW(), NOW(), 'a45.pdf', 'icms-translog-abr.pdf',    181000, 'PDF', 'empresa-9/pasta-17/a45.pdf', 'ff00112233445566ff00112233445566',
 'ICMS Transportes abr',      '2026-04-15', 'VENCIDO', 'GUIA_IMPOSTO',  NULL, 9, 14, 17, 46),
(NOW(), NOW(), 'a46.pdf', 'fgts-translog-abr.pdf',    83000,  'PDF', 'empresa-9/pasta-16/a46.pdf', '00112233445566770011223344556677',
 'FGTS Transportes abr',      '2026-04-07', 'VENCIDO', 'GUIA_IMPOSTO',  NULL, 9, 14, 16, 47),
(NOW(), NOW(), 'a47.pdf', 'iss-escola-abr.pdf',       77000,  'PDF', 'empresa-10/pasta-18/a47.pdf','11223344556677881122334455667788',
 'ISS Escola abr',            '2026-04-10', 'VENCIDO', 'GUIA_IMPOSTO',  NULL, 10, 15, 18, 51),

-- ====== ARQUIVADOS (sem vencimento — documentos de referência) ======
(NOW(), NOW(), 'a48.pdf', 'contrato-social-diniz.pdf',  320000, 'PDF',  'empresa-1/pasta-2/a48.pdf',   '223344556677889900aabbccddeeff00',
 'Contrato social',           NULL, 'ARQUIVADO', 'CONTRATO_SOCIAL', NULL, 1, 2, 2,  NULL),
(NOW(), NOW(), 'a49.pdf', 'cnd-federal.pdf',            75000,  'PDF',  'empresa-1/pasta-32/a49.pdf',  '3344556677889900aabbccddeeff0011',
 'Certidão negativa federal', NULL, 'ARQUIVADO', 'CERTIDAO',        NULL, 1, 2, 32, NULL),
(NOW(), NOW(), 'a50.pdf', 'cnd-estadual.pdf',           72000,  'PDF',  'empresa-1/pasta-32/a50.pdf',  '44556677889900aabbccddeeff001122',
 'Certidão negativa estadual',NULL, 'ARQUIVADO', 'CERTIDAO',        NULL, 1, 2, 32, NULL),
(NOW(), NOW(), 'a51.pdf', 'extrato-banco-mar.pdf',      52000,  'PDF',  'empresa-1/pasta-1/a51.pdf',   '556677889900aabbccddeeff00112233',
 'Extrato bancário março',    NULL, 'ARQUIVADO', 'EXTRATO_BANCARIO',NULL, 1, 4, 1,  NULL),
(NOW(), NOW(), 'a52.xlsx','relatorio-vendas-mar.xlsx',  91000,  'XLSX', 'empresa-2/pasta-5/a52.xlsx',  '6677889900aabbccddeeff0011223344',
 'Relatório vendas Padaria',  NULL, 'ARQUIVADO', 'RELATORIO',       NULL, 2, 7, 5,  NULL),
(NOW(), NOW(), 'a53.pdf', 'contrato-social-padaria.pdf',310000, 'PDF',  'empresa-2/pasta-5/a53.pdf',   '77889900aabbccddeeff001122334455',
 'Contrato social Padaria',   NULL, 'ARQUIVADO', 'CONTRATO_SOCIAL', NULL, 2, 7, 5,  NULL),
(NOW(), NOW(), 'a54.pdf', 'cnd-trabalhista-aurora.pdf', 71000,  'PDF',  'empresa-5/pasta-12/a54.pdf',  '889900aabbccddeeff00112233445566',
 'CND trabalhista',           NULL, 'ARQUIVADO', 'CERTIDAO',        NULL, 5, 10, 12, NULL),
(NOW(), NOW(), 'a55.pdf', 'contrato-prestacao-aurora.pdf',250000,'PDF','empresa-5/pasta-12/a55.pdf',   '9900aabbccddeeff001122334455aabb',
 'Contrato prestação serviço',NULL, 'ARQUIVADO', 'CONTRATO_SOCIAL', NULL, 5, 10, 12, NULL),
(NOW(), NOW(), 'a56.pdf', 'cnd-clinica.pdf',            70000,  'PDF',  'empresa-8/pasta-15/a56.pdf',  'aabbccddeeff00112233445566778899',
 'CND Clínica',               NULL, 'ARQUIVADO', 'CERTIDAO',        NULL, 8, 13, 15, NULL),

-- ====== LIXEIRA (excluidoem preenchido) ======
(NOW(), NOW(), 'a57.pdf', 'documento-errado.pdf',     12000, 'PDF', 'empresa-1/pasta-1/a57.pdf', 'bbccddeeff001122334455667788aabb',
 'Enviado por engano',        NULL, 'ARQUIVADO', 'OUTRO',          '2026-04-15 10:00:00', 1, 4, 1,  NULL),
(NOW(), NOW(), 'a58.pdf', 'duplicata-darf.pdf',       18000, 'PDF', 'empresa-1/pasta-23/a58.pdf','ccddeeff001122334455667788aabbcc',
 'DARF duplicado',            NULL, 'ARQUIVADO', 'DARF',           '2026-04-10 16:30:00', 1, 1, 23, NULL),
(NOW(), NOW(), 'a59.xml', 'nf-cancelada.xml',         32000, 'XML', 'empresa-2/pasta-5/a59.xml', 'ddeeff00112233445566778899aabbcc',
 'NF cancelada',              NULL, 'ARQUIVADO', 'NFE_SAIDA',      '2026-04-12 09:15:00', 2, 7, 5,  NULL),
(NOW(), NOW(), 'a60.pdf', 'guia-errada.pdf',          22000, 'PDF', 'empresa-3/pasta-7/a60.pdf', 'eeff0011223344556677889900aabbcc',
 'Guia com valor errado',     NULL, 'ARQUIVADO', 'GUIA_IMPOSTO',   '2026-04-17 14:22:00', 3, 8, 7,  NULL);

------------------------------------------------
-- LOG DE ACESSO (auditoria — linha do tempo dos últimos dias)
------------------------------------------------

INSERT INTO logacesso (datacriacao, dataatualizacao, usuario_id, acao, entidade, identidade, detalhes) VALUES
-- mais antigo -> mais recente
(NOW() - INTERVAL '20 days', NOW() - INTERVAL '20 days', 1, 'LOGIN',                'Usuario', 1,    'Login efetuado'),
(NOW() - INTERVAL '20 days', NOW() - INTERVAL '20 days', 1, 'UPLOAD',               'Arquivo', 1,    'Upload de "das-jan-2026.pdf" (empresa 1)'),
(NOW() - INTERVAL '19 days', NOW() - INTERVAL '19 days', 2, 'LOGIN',                'Usuario', 2,    'Login efetuado'),
(NOW() - INTERVAL '18 days', NOW() - INTERVAL '18 days', 4, 'UPLOAD',               'Arquivo', 4,    'Upload de "inss-fev-2026.pdf" (empresa 1)'),
(NOW() - INTERVAL '18 days', NOW() - INTERVAL '18 days', 4, 'UPLOAD',               'Arquivo', 6,    'Upload de "fgts-fev-2026.pdf" (empresa 1)'),
(NOW() - INTERVAL '17 days', NOW() - INTERVAL '17 days', 5, 'UPLOAD',               'Arquivo', 8,    'Upload de "balancete-fev-2026.pdf" (empresa 1)'),
(NOW() - INTERVAL '16 days', NOW() - INTERVAL '16 days', 7, 'LOGIN',                'Usuario', 7,    'Login efetuado'),
(NOW() - INTERVAL '16 days', NOW() - INTERVAL '16 days', 7, 'UPLOAD',               'Arquivo', 10,   'Upload de "das-padaria-fev.pdf" (empresa 2)'),
(NOW() - INTERVAL '15 days', NOW() - INTERVAL '15 days', 8, 'UPLOAD',               'Arquivo', 13,   'Upload de "icms-autopecas-fev.pdf" (empresa 3)'),
(NOW() - INTERVAL '14 days', NOW() - INTERVAL '14 days', 1, 'UPLOAD',               'Arquivo', 2,    'Upload de "das-fev-2026.pdf" (empresa 1)'),
(NOW() - INTERVAL '13 days', NOW() - INTERVAL '13 days', 4, 'UPLOAD',               'Arquivo', 3,    'Upload de "das-mar-2026.pdf" (empresa 1)'),
(NOW() - INTERVAL '12 days', NOW() - INTERVAL '12 days', 5, 'UPLOAD',               'Arquivo', 5,    'Upload de "inss-mar-2026.pdf" (empresa 1)'),
(NOW() - INTERVAL '11 days', NOW() - INTERVAL '11 days', 6, 'UPLOAD',               'Arquivo', 7,    'Upload de "fgts-mar-2026.pdf" (empresa 1)'),
(NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days', 1, 'DOWNLOAD',             'Arquivo', 2,    'Download: das-fev-2026.pdf'),
(NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days', 5, 'UPLOAD',               'Arquivo', 9,    'Upload de "balancete-mar-2026.pdf" (empresa 1)'),
(NOW() - INTERVAL '9 days',  NOW() - INTERVAL '9 days',  7, 'UPLOAD',               'Arquivo', 11,   'Upload de "das-padaria-mar.pdf" (empresa 2)'),
(NOW() - INTERVAL '9 days',  NOW() - INTERVAL '9 days',  7, 'UPLOAD',               'Arquivo', 12,   'Upload de "fgts-padaria-mar.pdf" (empresa 2)'),
(NOW() - INTERVAL '8 days',  NOW() - INTERVAL '8 days',  8, 'UPLOAD',               'Arquivo', 14,   'Upload de "icms-autopecas-mar.pdf" (empresa 3)'),
(NOW() - INTERVAL '8 days',  NOW() - INTERVAL '8 days',  8, 'UPLOAD',               'Arquivo', 15,   'Upload de "das-autopecas-mar.pdf" (empresa 3)'),
(NOW() - INTERVAL '7 days',  NOW() - INTERVAL '7 days',  9, 'UPLOAD',               'Arquivo', 16,   'Upload de "icms-mercado-mar.pdf" (empresa 4)'),
(NOW() - INTERVAL '7 days',  NOW() - INTERVAL '7 days',  10,'UPLOAD',               'Arquivo', 17,   'Upload de "inss-obra-mar.pdf" (empresa 5)'),
(NOW() - INTERVAL '6 days',  NOW() - INTERVAL '6 days',  11,'UPLOAD',               'Arquivo', 19,   'Upload de "das-farmacia-mar.pdf" (empresa 6)'),
(NOW() - INTERVAL '6 days',  NOW() - INTERVAL '6 days',  12,'UPLOAD',               'Arquivo', 20,   'Upload de "das-restaurante-mar.pdf" (empresa 7)'),
(NOW() - INTERVAL '5 days',  NOW() - INTERVAL '5 days',  13,'UPLOAD',               'Arquivo', 21,   'Upload de "das-clinica-mar.pdf" (empresa 8)'),
(NOW() - INTERVAL '5 days',  NOW() - INTERVAL '5 days',  13,'UPLOAD',               'Arquivo', 22,   'Upload de "iss-clinica-mar.pdf" (empresa 8)'),
(NOW() - INTERVAL '5 days',  NOW() - INTERVAL '5 days',  1, 'ATUALIZAR_STATUS',     'Arquivo', 37,   'Status: PENDENTE -> VENCIDO'),
(NOW() - INTERVAL '5 days',  NOW() - INTERVAL '5 days',  1, 'ATUALIZAR_VENCIMENTO', 'Arquivo', 26,   'Vencimento: 2026-04-20 -> 2026-04-25'),
(NOW() - INTERVAL '4 days',  NOW() - INTERVAL '4 days',  4, 'EXCLUIR',              'Arquivo', 57,   'Movido para a lixeira: documento-errado.pdf'),
(NOW() - INTERVAL '4 days',  NOW() - INTERVAL '4 days',  14,'UPLOAD',               'Arquivo', 23,   'Upload de "icms-translog-mar.pdf" (empresa 9)'),
(NOW() - INTERVAL '4 days',  NOW() - INTERVAL '4 days',  15,'UPLOAD',               'Arquivo', 24,   'Upload de "das-escola-mar.pdf" (empresa 10)'),
(NOW() - INTERVAL '3 days',  NOW() - INTERVAL '3 days',  1, 'EXCLUIR',              'Arquivo', 58,   'Movido para a lixeira: duplicata-darf.pdf'),
(NOW() - INTERVAL '3 days',  NOW() - INTERVAL '3 days',  2, 'LOGIN',                'Usuario', 2,    'Login efetuado'),
(NOW() - INTERVAL '3 days',  NOW() - INTERVAL '3 days',  2, 'DOWNLOAD',             'Arquivo', 9,    'Download: balancete-mar-2026.pdf'),
(NOW() - INTERVAL '2 days',  NOW() - INTERVAL '2 days',  7, 'EXCLUIR',              'Arquivo', 59,   'Movido para a lixeira: nf-cancelada.xml'),
(NOW() - INTERVAL '2 days',  NOW() - INTERVAL '2 days',  1, 'ATUALIZAR_STATUS',     'Arquivo', 40,   'Status: PENDENTE -> VENCIDO'),
(NOW() - INTERVAL '2 days',  NOW() - INTERVAL '2 days',  1, 'ATUALIZAR_STATUS',     'Arquivo', 43,   'Status: PENDENTE -> VENCIDO'),
(NOW() - INTERVAL '2 days',  NOW() - INTERVAL '2 days',  8, 'EXCLUIR',              'Arquivo', 60,   'Movido para a lixeira: guia-errada.pdf'),
(NOW() - INTERVAL '1 day',   NOW() - INTERVAL '1 day',   1, 'LOGIN',                'Usuario', 1,    'Login efetuado'),
(NOW() - INTERVAL '1 day',   NOW() - INTERVAL '1 day',   1, 'VISUALIZAR',           'Arquivo', 26,   'Visualizou: darf-irpj-2026Q1.pdf'),
(NOW() - INTERVAL '1 day',   NOW() - INTERVAL '1 day',   4, 'UPLOAD',               'Arquivo', 25,   'Upload de "das-abr-2026.pdf" (empresa 1)'),
(NOW() - INTERVAL '12 hours',NOW() - INTERVAL '12 hours',4, 'UPLOAD',               'Arquivo', 30,   'Upload de "folha-abr-2026.pdf" (empresa 1)'),
(NOW() - INTERVAL '8 hours', NOW() - INTERVAL '8 hours', 12,'UPLOAD',               'Arquivo', 32,   'Upload de "das-restaurante-abr.pdf" (empresa 7)'),
(NOW() - INTERVAL '6 hours', NOW() - INTERVAL '6 hours', 1, 'LOGIN',                'Usuario', 1,    'Login efetuado'),
(NOW() - INTERVAL '5 hours', NOW() - INTERVAL '5 hours', 13,'UPLOAD',               'Arquivo', 33,   'Upload de "das-clinica-abr.pdf" (empresa 8)'),
(NOW() - INTERVAL '4 hours', NOW() - INTERVAL '4 hours', 14,'UPLOAD',               'Arquivo', 34,   'Upload de "inss-translog-abr.pdf" (empresa 9)'),
(NOW() - INTERVAL '3 hours', NOW() - INTERVAL '3 hours', 15,'UPLOAD',               'Arquivo', 35,   'Upload de "das-escola-abr.pdf" (empresa 10)'),
(NOW() - INTERVAL '2 hours', NOW() - INTERVAL '2 hours', 1, 'DOWNLOAD',             'Arquivo', 25,   'Download: das-abr-2026.pdf'),
(NOW() - INTERVAL '1 hour',  NOW() - INTERVAL '1 hour',  1, 'DOWNLOAD',             'Arquivo', 26,   'Download: darf-irpj-2026Q1.pdf'),
(NOW() - INTERVAL '30 minutes',NOW() - INTERVAL '30 minutes',2,'LOGIN',             'Usuario', 2,    'Login efetuado');

COMMIT;
