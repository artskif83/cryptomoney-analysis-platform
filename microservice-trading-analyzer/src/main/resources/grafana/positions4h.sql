SELECT ts AS time,
       open,
       high,
       low,
       close,
       index_candle_4h,
       metric_triple_ma_fast_sma_4h,
       metric_triple_ma_medium_sma_4h,
       metric_triple_ma_slow_sma_4h,
       metric_triple_ma_fast_angle_4h,
       metric_triple_ma_medium_angle_4h,
       metric_triple_ma_slow_angle_4h,
       metric_triple_ma_value_4h,
       metric_resistance_level_4h
FROM wide_candles
WHERE tf = '4h'
  AND confirmed = true
ORDER BY ts;