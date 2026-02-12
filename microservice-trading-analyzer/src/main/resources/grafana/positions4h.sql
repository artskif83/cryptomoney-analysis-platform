SELECT
    ts AS time,
    open,
    high,
    low,
    close,
    index_candle_4h,
    metric_candle_resistance_strength_4h,
    metric_resistance_level_4h,
    metric_resistance_power_above_4h
FROM wide_candles
WHERE
    tf = '4h'
  AND confirmed = true
ORDER BY ts;