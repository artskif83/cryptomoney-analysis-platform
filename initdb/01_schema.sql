-- 01_schema.sql

-- 0) Включаем расширение TimescaleDB
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- 1) Основные таблицы
CREATE TABLE IF NOT EXISTS candles (
                                       symbol  text NOT NULL,
                                       tf      text NOT NULL,
                                       ts      timestamptz NOT NULL,
                                       open    double precision NOT NULL,
                                       high    double precision NOT NULL,
                                       low     double precision NOT NULL,
                                       close   double precision NOT NULL,
                                       volume  double precision NOT NULL,
                                       PRIMARY KEY (symbol, tf, ts)
    );

-- Классический синтаксис без by_range(...)
SELECT create_hypertable('candles', 'ts', if_not_exists => TRUE);

CREATE TABLE IF NOT EXISTS indicators_rsi (
                                              symbol  text NOT NULL,
                                              tf      text NOT NULL,
                                              ts      timestamptz NOT NULL,
                                              rsi_14  double precision,
                                              PRIMARY KEY (symbol, tf, ts)
    );

SELECT create_hypertable('indicators_rsi', 'ts', if_not_exists => TRUE);

-- 2) Staging для COPY (можно UNLOGGED для скорости)
CREATE UNLOGGED TABLE IF NOT EXISTS stage_candles (
  symbol  text,
  tf      text,
  ts      timestamptz,
  open    double precision,
  high    double precision,
  low     double precision,
  close   double precision,
  volume  double precision
);