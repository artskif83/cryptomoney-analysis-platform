SELECT
    ts AS time,
    open,
    high,
    low,
    close,
    index_candle_1m,
    metric_resistance_level_1m,
    metric_resistance_stop_los_1m,
    metric_double_ma_value_1m_on_5m,
    metric_double_ma_value_1m_on_1h,
    additional_position_price_1m,
    additional_takeprofit_1m,
    additional_stoploss_1m
FROM wide_candles
WHERE
    tf = '1m' AND
    tag = 'GoldenField1M-backtest' AND
    ts BETWEEN $__timeFrom() AND $__timeTo()
ORDER BY ts;