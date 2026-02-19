SELECT
    ts AS time,
    open,
    high,
    low,
    close,
    index_candle_1m,
    metric_resistance_level_1m,
    metric_triple_ma_value_1m
FROM wide_candles
WHERE
    tf = '1m' AND
    tag = 'GoldenField1M-lifetime'
ORDER BY ts;