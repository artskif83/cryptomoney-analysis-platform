SELECT
    ts AS time,
    open,
    high,
    low,
    close,
    index_candle_1m,
    metric_short_trend_1m,
    metric_short_stop_los_1m,
    metric_long_trend_1m,
    metric_long_stop_los_1m,
    short_high_level_top_border_1m_on_1h,
    short_high_level_bottom_border_1m_on_1h,
    metric_adx_angle_1m_on_1h,
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