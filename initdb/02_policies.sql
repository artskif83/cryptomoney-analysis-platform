-- Retention на сырые свечи
SELECT add_retention_policy('candles', INTERVAL '1825 days');

-- Компрессия старых чанков
ALTER TABLE candles
    SET (
    timescaledb.compress,
    timescaledb.compress_orderby = 'ts DESC',
    timescaledb.compress_segmentby = 'symbol, tf'
    );
SELECT add_compression_policy('candles', INTERVAL '7 days');