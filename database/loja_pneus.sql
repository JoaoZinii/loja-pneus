DROP DATABASE IF EXISTS loja_pneus;
CREATE DATABASE loja_pneus
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
USE loja_pneus;

CREATE TABLE cliente (
    id_cliente INT          NOT NULL AUTO_INCREMENT,
    nome       VARCHAR(100) NOT NULL,
    email      VARCHAR(100) NULL,
    telefone   VARCHAR(11)  NULL,   -- so digitos, com DDD (10 ou 11)
    endereco   VARCHAR(150) NULL,
    CONSTRAINT pk_cliente       PRIMARY KEY (id_cliente),
    CONSTRAINT uq_cliente_email UNIQUE (email)
) ENGINE = InnoDB;

CREATE TABLE pneu (
    id_pneu INT           NOT NULL AUTO_INCREMENT,
    marca   VARCHAR(50)   NOT NULL,
    modelo  VARCHAR(80)   NOT NULL,
    aro     INT           NOT NULL,
    preco   DECIMAL(10,2) NOT NULL,
    estoque INT           NOT NULL DEFAULT 0,
    CONSTRAINT pk_pneu         PRIMARY KEY (id_pneu),
    CONSTRAINT chk_pneu_aro    CHECK (aro BETWEEN 10 AND 24),
    CONSTRAINT chk_pneu_preco  CHECK (preco > 0),
    CONSTRAINT chk_pneu_estoque CHECK (estoque >= 0)
) ENGINE = InnoDB;

CREATE TABLE venda (
    id_venda       INT           NOT NULL AUTO_INCREMENT,
    id_cliente     INT           NOT NULL,
    id_pneu        INT           NOT NULL,
    quantidade     INT           NOT NULL,
    preco_unitario DECIMAL(10,2) NOT NULL,
    data_venda     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_venda PRIMARY KEY (id_venda),
    CONSTRAINT fk_venda_cliente FOREIGN KEY (id_cliente)
        REFERENCES cliente (id_cliente)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_venda_pneu FOREIGN KEY (id_pneu)
        REFERENCES pneu (id_pneu)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_venda_quantidade CHECK (quantidade > 0),
    CONSTRAINT chk_venda_preco      CHECK (preco_unitario > 0)
) ENGINE = InnoDB;

 
CREATE INDEX idx_cliente_nome ON cliente (nome);
CREATE INDEX idx_pneu_marca   ON pneu (marca);
CREATE INDEX idx_venda_data   ON venda (data_venda);


INSERT INTO cliente (nome, email, telefone, endereco) VALUES
('Carlos Eduardo Souza',  'carlos.souza@email.com',    '19991234567', 'Rua das Acacias, 120 - Campinas'),
('Mariana Alves Pereira', 'mariana.alves@email.com',   '19998765432', 'Av. Brasil, 845 - Campinas'),
('Roberto Nogueira',      'roberto.nog@email.com',     '1932345678',  'Rua Barao de Jaguara, 50 - Campinas'),
('Fernanda Costa Ribeiro','fernanda.ribeiro@email.com','19997654321', 'Rua Sao Paulo, 300 - Valinhos'),
('Lucas Henrique Martins','lucas.martins@email.com',   '19996543210', 'Rua das Palmeiras, 77 - Sumare'),
('Patricia Gomes',        NULL,                        '1933456789',  'Av. Norte-Sul, 1500 - Campinas'),
('Joao Pedro Almeida',    'joao.almeida@email.com',    '19995432109', 'Rua XV de Novembro, 410 - Indaiatuba'),
('Camila Rocha',          'camila.rocha@email.com',    NULL,          'Rua Doze, 23 - Paulinia'),
('Rafael Teixeira',       NULL,                        '19994321098', 'Av. Andrade Neves, 900 - Campinas'),
('Beatriz Fernandes',     'bia.fernandes@email.com',   '19993210987', 'Rua Ipe, 15 - Vinhedo');

INSERT INTO pneu (marca, modelo, aro, preco, estoque) VALUES
('Pirelli',     'Cinturato P1',     15, 289.90, 24),
('Pirelli',     'Scorpion Verde',   17, 549.90, 12),
('Pirelli',     'P400 Evo',         14, 259.90, 30),
('Michelin',    'Primacy 4',        17, 689.00,  8),
('Michelin',    'Energy XM2',       15, 412.50, 15),
('Goodyear',    'Assurance',        16, 379.90, 20),
('Goodyear',    'EfficientGrip',    17, 599.90, 10),
('Bridgestone', 'Turanza',          16, 465.00, 14),
('Bridgestone', 'Dueler',           18, 819.90,  6),
('Continental', 'PowerContact 2',   15, 349.90, 18);

INSERT INTO venda (id_cliente, id_pneu, quantidade, preco_unitario, data_venda) VALUES
(1,  1, 4, 289.90, '2026-08-05 10:15:00'),
(2,  4, 2, 679.00, '2026-08-12 14:30:00'),
(3,  3, 4, 259.90, '2026-08-19 09:40:00'),
(1,  5, 2, 412.50, '2026-08-27 16:05:00'),
(4,  2, 4, 529.90, '2026-09-02 11:20:00'),
(5,  6, 4, 379.90, '2026-09-09 15:45:00'),
(6,  1, 2, 279.90, '2026-09-14 10:00:00'),
(2,  7, 2, 599.90, '2026-09-18 13:10:00'),
(7,  8, 4, 465.00, '2026-09-23 17:30:00'),
(8,  9, 1, 799.90, '2026-09-29 09:25:00'),
(3, 10, 4, 349.90, '2026-10-01 14:50:00'),
(9,  4, 2, 689.00, '2026-10-05 12:15:00'),
(10, 2, 2, 549.90, '2026-10-08 16:40:00'),
(4,  6, 2, 379.90, '2026-10-12 10:35:00'),
(5,  3, 2, 259.90, '2026-10-14 15:20:00');

SELECT v.id_venda,
       c.nome AS cliente,
       p.marca,
       p.modelo,
       v.quantidade,
       v.preco_unitario,
       (v.quantidade * v.preco_unitario) AS total,
       v.data_venda
FROM venda v
JOIN cliente c ON c.id_cliente = v.id_cliente
JOIN pneu    p ON p.id_pneu    = v.id_pneu
ORDER BY v.data_venda DESC;

SELECT v.id_venda, c.nome, p.marca, p.modelo, v.quantidade,
       v.preco_unitario, v.data_venda
FROM venda v
JOIN cliente c ON c.id_cliente = v.id_cliente
JOIN pneu    p ON p.id_pneu    = v.id_pneu
WHERE c.nome LIKE '%Souza%'
  AND v.data_venda BETWEEN '2026-08-01 00:00:00' AND '2026-10-31 23:59:59'
ORDER BY v.data_venda;

SELECT c.nome,
       COUNT(v.id_venda)                   AS qtd_vendas,
       SUM(v.quantidade * v.preco_unitario) AS total_gasto
FROM cliente c
JOIN venda v ON v.id_cliente = c.id_cliente
GROUP BY c.id_cliente, c.nome
ORDER BY total_gasto DESC;
 