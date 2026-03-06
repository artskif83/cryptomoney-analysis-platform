SELECT
    ts AS time,
    open,
    high,
    low,
    close,
    index_candle_1m,
    metric_resistance_level_1m_on_5m,
    metric_resistance_level_1m_on_4h,
    metric_resistance_level_1m_on_1h,
    metric_double_ma_value_1m_on_1h
FROM wide_candles
WHERE
    tf = '1m' AND
    tag = 'GoldenField1M-lifetime' AND
    ts BETWEEN $__timeFrom() AND $__timeTo()
ORDER BY ts;