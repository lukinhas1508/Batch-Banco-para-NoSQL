CREATE TABLE IF NOT EXISTS clientes (
    id              BIGINT       PRIMARY KEY,
    nome            VARCHAR(120) NOT NULL,
    email           VARCHAR(160) NOT NULL UNIQUE,
    telefone        VARCHAR(20)  NOT NULL,
    data_nascimento DATE         NOT NULL,
    logradouro      VARCHAR(120) NOT NULL,
    numero          VARCHAR(10)  NOT NULL,
    cidade          VARCHAR(80)  NOT NULL,
    estado          CHAR(2)      NOT NULL,
    cep             CHAR(9)      NOT NULL
);
