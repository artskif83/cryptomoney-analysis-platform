SELECT
    ts AS time,
    open,
    high,
    low,
    close,
    index_candle_5m,
    metric_resistance_level_5m,
    metric_resistance_level_5m_on_4h,
    metric_triple_ma_value_5m_on_4h,
    additional_position_price_5m,
    additional_takeprofit_5m,
    additional_stoploss_5m
FROM wide_candles
WHERE
    tf = '5m'
  AND confirmed = true
ORDER BY ts;