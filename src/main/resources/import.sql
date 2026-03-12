BEGIN;

------------------------------------------------
-- EMPRESAS
------------------------------------------------

INSERT INTO empresa (datacriacao, dataatualizacao, nomefantasia, razaosocial, cnpj, telefone, email) VALUES
(NOW(), NOW(), 'Diniz Contabilidade', 'Diniz Contabilidade LTDA', '11111111000101', '63999990001', 'empresa1@email.com'),
(NOW(), NOW(), 'Contábil Palmas', 'Contábil Palmas LTDA', '22222222000102', '63999990002', 'empresa2@email.com'),
(NOW(), NOW(), 'Fiscal Tocantins', 'Fiscal Tocantins LTDA', '33333333000103', '63999990003', 'empresa3@email.com'),
(NOW(), NOW(), 'Assessoria Norte', 'Assessoria Norte LTDA', '44444444000104', '63999990004', 'empresa4@email.com'),
(NOW(), NOW(), 'Grupo Contábil Brasil', 'Grupo Contábil Brasil LTDA', '55555555000105', '63999990005', 'empresa5@email.com');

------------------------------------------------
-- USUARIOS
------------------------------------------------

INSERT INTO usuario (datacriacao, dataatualizacao, nome, endereco, senha, perfilusuario, empresa_id) VALUES
(NOW(), NOW(), 'Rafael Diniz', 'rafael@email.com', '123456', 'ADMIN', 1),
(NOW(), NOW(), 'João Silva', 'joao@email.com', '123456', 'FUNCIONARIO', 1),
(NOW(), NOW(), 'Maria Souza', 'maria@email.com', '123456', 'FUNCIONARIO', 2),
(NOW(), NOW(), 'Carlos Lima', 'carlos@email.com', '123456', 'FUNCIONARIO', 3),
(NOW(), NOW(), 'Ana Costa', 'ana@email.com', '123456', 'FUNCIONARIO', 4);

------------------------------------------------
-- SOCIOS
------------------------------------------------

INSERT INTO socio (datacriacao, dataatualizacao, nome, numero, empresa_id) VALUES
(NOW(), NOW(), 'Rafael Diniz', '12345678901', 1),
(NOW(), NOW(), 'João Pereira', '23456789012', 2),
(NOW(), NOW(), 'Maria Alves', '34567890123', 3),
(NOW(), NOW(), 'Carlos Mendes', '45678901234', 4),
(NOW(), NOW(), 'Ana Paula', '56789012345', 5);

------------------------------------------------
-- PASTAS
------------------------------------------------

INSERT INTO pasta (datacriacao, dataatualizacao, nome, descricao, empresa_id, pastapai_id) VALUES
(NOW(), NOW(), 'Notas Fiscais', 'Documentos fiscais', 1, NULL),
(NOW(), NOW(), 'Contratos', 'Contratos empresariais', 1, NULL),
(NOW(), NOW(), 'Impostos', 'Arquivos de impostos', 2, NULL),
(NOW(), NOW(), 'Folha de Pagamento', 'Departamento pessoal', 3, NULL),
(NOW(), NOW(), 'Documentos Gerais', 'Arquivos diversos', 4, NULL);

------------------------------------------------
-- ARQUIVOS (simulando uploads)
------------------------------------------------

INSERT INTO arquivo (datacriacao, dataatualizacao, nome, nomeoriginal, tamanho, tipoarquivo, caminho, empresa_id, usuario_id, pasta_id) VALUES
(NOW(), NOW(), 'uuid1.pdf', 'nota_janeiro.pdf', 120000, 'PDF', 'empresa-1/pasta-1/uuid1.pdf', 1, 1, 1),
(NOW(), NOW(), 'uuid2.xml', 'nf_123.xml', 50000, 'XML', 'empresa-1/pasta-1/uuid2.xml', 1, 2, 1),
(NOW(), NOW(), 'uuid3.xlsx', 'planilha.xlsx', 80000, 'XLSX', 'empresa-2/pasta-3/uuid3.xlsx', 2, 3, 3),
(NOW(), NOW(), 'uuid4.pdf', 'contrato.pdf', 150000, 'PDF', 'empresa-1/pasta-2/uuid4.pdf', 1, 2, 2),
(NOW(), NOW(), 'uuid5.csv', 'dados.csv', 30000, 'CSV', 'empresa-4/pasta-5/uuid5.csv', 4, 5, 5);

COMMIT;