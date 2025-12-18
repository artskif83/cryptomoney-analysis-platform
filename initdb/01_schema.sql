-- 01_schema.sql

-- 0) Включаем расширение TimescaleDB
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- 1) Основные таблицы
CREATE TABLE IF NOT EXISTS candles
(
    symbol         varchar(32)    NOT NULL,
    tf             varchar(10)    NOT NULL,
    ts             timestamp      NOT NULL,
    open           numeric(18, 8) NOT NULL,
    high           numeric(18, 8) NOT NULL,
    low            numeric(18, 8) NOT NULL,
    close          numeric(18, 8) NOT NULL,
    volume         numeric(30, 8),
    confirmed      boolean        NOT NULL DEFAULT false,
    PRIMARY KEY (symbol, tf, ts)
);

-- Классический синтаксис без by_range(...)
SELECT create_hypertable('candles', 'ts', if_not_exists => TRUE);
CREATE INDEX candles_symbol_tf_ts_idx ON candles (symbol, tf, ts DESC);


-- 2) Staging для COPY (можно UNLOGGED для скорости)
CREATE UNLOGGED TABLE IF NOT EXISTS stage_candles
(
    symbol varchar(32),
    tf     varchar(10),
    ts     timestamp,
    open   numeric(18, 8),
    high   numeric(18, 8),
    low    numeric(18, 8),
    close  numeric(18, 8),
    volume numeric(30, 8),
    confirmed      boolean DEFAULT false
);

-- 3) Таблица контрактов (Wide Table для ML)
CREATE TABLE IF NOT EXISTS features
(
    symbol         varchar(32)    NOT NULL,
    tf             varchar(10)    NOT NULL,
    ts             timestamp      NOT NULL,
    open           numeric(18, 8) NOT NULL,
    high           numeric(18, 8) NOT NULL,
    low            numeric(18, 8) NOT NULL,
    close          numeric(18, 8) NOT NULL,
    volume         numeric(30, 8),
    contract_hash         varchar(64),
    confirmed      boolean        NOT NULL DEFAULT false,
    PRIMARY KEY (symbol, tf, ts)
);

SELECT create_hypertable('features', 'ts', if_not_exists => TRUE);
CREATE INDEX features_symbol_tf_ts_idx ON features (symbol, tf, ts DESC);

-- 4) Таблица контрактов (набор фич для ML)
CREATE TABLE IF NOT EXISTS contracts
(
    id             bigserial      PRIMARY KEY,
    contract_hash  varchar(64)    NOT NULL,
    name           varchar(255)   NOT NULL,
    description    text           NOT NULL,
    feature_set_id varchar(50)    NOT NULL,
    created_at     timestamp      NOT NULL DEFAULT NOW(),
    updated_at     timestamp      NOT NULL DEFAULT NOW()
);

-- Создаем индекс для быстрого поиска по хешу контракта
CREATE INDEX IF NOT EXISTS contracts_contract_hash_idx ON contracts(contract_hash);

-- 5) Таблица метаданных для фич (параметров ML)
CREATE TABLE IF NOT EXISTS contract_metadata
(
    name           varchar(255)   NOT NULL PRIMARY KEY,
    description    text           NOT NULL,
    sequence_order integer        NOT NULL UNIQUE,
    data_type      varchar(50)    NOT NULL,
    metadata_type  varchar(20)    NOT NULL CHECK (metadata_type IN ('FEATURE', 'LABEL')),
    contract_id    bigint         NOT NULL,
    created_at     timestamp      NOT NULL DEFAULT NOW(),
    updated_at     timestamp      NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_contract FOREIGN KEY (contract_id) REFERENCES contracts(id) ON DELETE CASCADE
);

