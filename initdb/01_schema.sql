-- 01_schema.sql

-- 0) Включаем расширение TimescaleDB
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- 1) Основные таблицы
CREATE TABLE IF NOT EXISTS candles
(
    symbol    varchar(32)    NOT NULL,
    tf        varchar(10)    NOT NULL,
    ts        timestamp      NOT NULL,
    open      numeric(18, 8) NOT NULL,
    high      numeric(18, 8) NOT NULL,
    low       numeric(18, 8) NOT NULL,
    close     numeric(18, 8) NOT NULL,
    volume    numeric(30, 8),
    confirmed boolean        NOT NULL DEFAULT false,
    PRIMARY KEY (symbol, tf, ts)
);

-- Классический синтаксис без by_range(...)
SELECT create_hypertable('candles', 'ts', if_not_exists => TRUE);
CREATE INDEX candles_symbol_tf_ts_idx ON candles (symbol, tf, ts DESC);


-- 2) Staging для COPY (можно UNLOGGED для скорости)
CREATE UNLOGGED TABLE IF NOT EXISTS stage_candles
(
    symbol    varchar(32),
    tf        varchar(10),
    ts        timestamp,
    open      numeric(18, 8),
    high      numeric(18, 8),
    low       numeric(18, 8),
    close     numeric(18, 8),
    volume    numeric(30, 8),
    confirmed boolean DEFAULT false
);

-- 3) Таблица широких свечей (Wide Table для ML, тестирования и отладки)
CREATE TABLE IF NOT EXISTS wide_candles
(
    tf            varchar(10)    NOT NULL,
    ts            timestamp      NOT NULL,
    open          numeric(18, 8) NOT NULL,
    high          numeric(18, 8) NOT NULL,
    low           numeric(18, 8) NOT NULL,
    tag           varchar(255)   NOT NULL,
    close         numeric(18, 8) NOT NULL,
    volume        numeric(30, 8),
    contract_hash varchar(64),
    confirmed     boolean        NOT NULL DEFAULT true,
    PRIMARY KEY (tf, tag, ts)
);

SELECT create_hypertable('wide_candles', 'ts', if_not_exists => TRUE);

-- PRIMARY KEY (tf, tag, ts) автоматически создаст индекс и покроет запросы вида:
-- WHERE tf = '1m' AND tag = 'positions' ORDER BY ts

-- Индекс для запросов только по tf (если используется без tag)
CREATE INDEX IF NOT EXISTS wide_candles_tf_ts_idx ON wide_candles (tf, ts DESC);

-- Индекс для быстрого поиска по contract_hash (если используется для JOIN)
CREATE INDEX IF NOT EXISTS wide_candles_contract_hash_idx ON wide_candles (contract_hash) WHERE contract_hash IS NOT NULL;

-- Staging таблица для COPY wide_candles (UNLOGGED для скорости)
CREATE UNLOGGED TABLE IF NOT EXISTS stage_wide_candles
(
    tf            varchar(10),
    ts            timestamp,
    open          numeric(18, 8),
    high          numeric(18, 8),
    low           numeric(18, 8),
    tag           varchar(255),
    close         numeric(18, 8),
    volume        numeric(30, 8),
    contract_hash varchar(64),
    confirmed     boolean DEFAULT true
);

-- 4) Таблица контрактов (набор фич для ML)
CREATE TABLE IF NOT EXISTS contracts
(
    id             bigserial PRIMARY KEY,
    contract_hash  varchar(64)  NOT NULL,
    name           varchar(255) NOT NULL,
    description    text         NOT NULL,
    feature_set_id varchar(50)  NOT NULL,
    created_at     timestamp    NOT NULL DEFAULT NOW(),
    updated_at     timestamp    NOT NULL DEFAULT NOW()
);

-- Создаем индекс для быстрого поиска по хешу контракта
CREATE INDEX IF NOT EXISTS contracts_contract_hash_idx ON contracts (contract_hash);

-- 5) Таблица метаданных для фич (параметров ML)
CREATE TABLE IF NOT EXISTS contract_metadata
(
    id             bigserial PRIMARY KEY,
    name           varchar(255) NOT NULL,
    description    text         NOT NULL,
    sequence_order integer      NOT NULL,
    data_type      varchar(50)  NOT NULL,
    metadata_type  varchar(20)  NOT NULL CHECK (metadata_type IN ('FEATURE', 'LABEL', 'METRIC', 'ADDITIONAL')),
    contract_id    bigint       NOT NULL,
    created_at     timestamp    NOT NULL DEFAULT NOW(),
    updated_at     timestamp    NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_contract FOREIGN KEY (contract_id) REFERENCES contracts (id) ON DELETE CASCADE
);

-- 6) Таблица торговых событий (для отображения в Grafana)
CREATE TABLE IF NOT EXISTS trade_events
(
    timeframe              varchar(20)    NOT NULL,
    tag                    varchar(255)   NOT NULL,
    timestamp              timestamp      NOT NULL,
    uuid                   uuid           NOT NULL DEFAULT gen_random_uuid(),
    event_type             varchar(50)    NOT NULL,
    direction              varchar(10)    NOT NULL CHECK (direction IN ('LONG', 'SHORT')),
    instrument             varchar(50)    NOT NULL,
    event_price            numeric(18, 8) NOT NULL,
    stop_loss_percentage   numeric(10, 4),
    take_profit_percentage numeric(10, 4),
    is_test                boolean        NOT NULL DEFAULT false,
    created_at             timestamp      NOT NULL DEFAULT NOW(),
    PRIMARY KEY (timeframe, tag, timestamp)
);

-- Создаем гипертаблицу для trade_events
SELECT create_hypertable('trade_events', 'timestamp', if_not_exists => TRUE);

-- 7) Таблица активных (ожидающих) ордеров
-- Несколько ордеров могут существовать в один период времени (tf, ts не уникальны).
-- ord_id может дублироваться в разные метки времени.
CREATE TABLE IF NOT EXISTS pending_orders
(
    id            bigserial PRIMARY KEY,
    ord_id        varchar(128)   NOT NULL,
    cl_ord_id     varchar(128),
    inst_id       varchar(50)    NOT NULL,
    inst_type     varchar(20)    NOT NULL,
    px            numeric(18, 8),
    sz            numeric(18, 8) NOT NULL,
    pos_side      varchar(10)    NOT NULL CHECK (pos_side IN ('long', 'short', 'net')),
    td_mode       varchar(20)    NOT NULL,
    lever         numeric(5, 2),
    state         varchar(20)    NOT NULL DEFAULT 'LIVE' CHECK (state IN ('LIVE', 'PARTIALLY_FILLED', 'CLOSED')),
    ord_type      varchar(20),
    sl_trigger_px numeric(18, 8),
    tf            varchar(10)    NOT NULL,
    ts            timestamp      NOT NULL,
    c_time        timestamp,
    u_time        timestamp,
    created_at    timestamp      NOT NULL DEFAULT NOW(),
    updated_at    timestamp      NOT NULL DEFAULT NOW()
);

-- Индекс для JOIN-ов со свечами по (ts, tf)
CREATE INDEX IF NOT EXISTS pending_orders_ts_tf_idx ON pending_orders (tf, ts DESC);

-- 8) Таблица открытых позиций
-- Уникальность позиции определяется комбинацией (pos_id, c_time),
-- так как pos_id на OKX не является глобально уникальным идентификатором.
CREATE TABLE IF NOT EXISTS positions
(
    id            bigserial PRIMARY KEY,
    pos_id        varchar(128) NOT NULL,
    cl_ord_id     varchar(128),
    inst_id       varchar(50)  NOT NULL,
    inst_type     varchar(20)  NOT NULL,
    px            numeric(18, 8),
    sz            numeric(18, 8),
    pos_side      varchar(10) CHECK (pos_side IN ('long', 'short', 'net')),
    td_mode       varchar(20),
    lever         numeric(5, 2),
    state         varchar(20)  NOT NULL DEFAULT 'LIVE' CHECK (state IN ('LIVE', 'PARTIALLY_FILLED', 'CLOSED')),
    sl_trigger_px numeric(18, 8),
    realized_pnl  numeric(24, 8),
    notional_usd  numeric(18, 8),
    tf            varchar(10)  NOT NULL,
    ts            timestamp    NOT NULL,
    c_time        timestamp,
    u_time        timestamp,
    created_at    timestamp    NOT NULL DEFAULT NOW(),
    updated_at    timestamp    NOT NULL DEFAULT NOW()
);

-- Индекс для JOIN-ов со свечами по (ts, tf)
CREATE INDEX IF NOT EXISTS positions_ts_tf_idx ON positions (tf, ts DESC);

-- 9) Таблица параметров создания ордера (торговые настройки стратегии)
CREATE TABLE IF NOT EXISTS order_creation_params
(
    id                            bigserial      PRIMARY KEY,
    trend_strength                integer        NOT NULL CHECK (trend_strength BETWEEN -100 AND 100),
    long_deposit_risk_percent     numeric(10, 4) NOT NULL,
    long_only_close               boolean        NOT NULL DEFAULT false,
    short_deposit_risk_percent    numeric(10, 4) NOT NULL,
    short_only_close              boolean        NOT NULL DEFAULT false,
    stop_loss_deviation_percent   numeric(10, 4) NOT NULL,
    wait_minutes                  integer        NOT NULL,
    max_position_size_percent     numeric(10, 4) NOT NULL,
    close_opposite_long                boolean        NOT NULL DEFAULT false,
    close_opposite_short               boolean        NOT NULL DEFAULT false,
    created_at                    timestamp      NOT NULL DEFAULT NOW()
);

-- Дефолтные значения для order_creation_params
-- long/short_deposit_risk_percent: чем ближе trend_strength к 0, тем выше риск (±1→10, ±3→6, ±5→2)
INSERT INTO order_creation_params (trend_strength, long_deposit_risk_percent, long_only_close,
                                   short_deposit_risk_percent, short_only_close,
                                   stop_loss_deviation_percent, wait_minutes, max_position_size_percent,
                                   close_opposite_long, close_opposite_short)
VALUES ( 5, 5,  false, 2,  true, 3, 30, 100, false, true),
       ( 3, 10,  false, 5,  true, 3, 30, 100, false, true),
       ( 1, 20, false, 10, true, 3, 30, 100, false, true),
       (-1, 10, true, 20, false, 3, 30, 100, true, false),
       (-3, 5,  true, 10,  false, 3, 30, 100, true, false),
       (-5, 2,  true, 5,  false, 3, 30, 100, true, false);

