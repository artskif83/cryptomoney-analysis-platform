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

CREATE TABLE IF NOT EXISTS indicators_rsi
(
    symbol varchar(32) NOT NULL,
    tf     varchar(10) NOT NULL,
    ts     timestamp   NOT NULL,
    rsi_14 numeric(5, 2),
    PRIMARY KEY (symbol, tf, ts)
);

SELECT create_hypertable('indicators_rsi', 'ts', if_not_exists => TRUE);
CREATE INDEX indicators_rsi_symbol_tf_ts_idx ON indicators_rsi (symbol, tf, ts DESC);

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

CREATE UNLOGGED TABLE IF NOT EXISTS stage_indicators_rsi
(
    symbol varchar(32),
    tf     varchar(10),
    ts     timestamp,
    rsi_14 numeric(5, 2)
);

-- 3) Таблица контрактов (Wide Table для ML)
CREATE TABLE IF NOT EXISTS contracts
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

SELECT create_hypertable('contracts', 'ts', if_not_exists => TRUE);
CREATE INDEX contracts_symbol_tf_ts_idx ON contracts (symbol, tf, ts DESC);

-- 4) Таблица метаданных для фич (параметров ML)
CREATE TABLE IF NOT EXISTS contract_features_metadata
(
    feature_name   varchar(255)   NOT NULL PRIMARY KEY,
    description    text           NOT NULL,
    sequence_order integer        NOT NULL UNIQUE,
    data_type      varchar(50)    NOT NULL,
    created_at     timestamp      NOT NULL DEFAULT NOW(),
    updated_at     timestamp      NOT NULL DEFAULT NOW()
);

