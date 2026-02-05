SELECT
    ts AS time,
    open,
    high,
    low,
    close,
    metric_rsi_14_5m,
    additional_position_price_5m,
    additional_takeprofit_5m,
    additional_stoploss_5m
FROM wide_candles
WHERE
    tf = '5m'
  AND contract_hash = '4ff4cf87a222e7a7e14aea3cc81e1468edb6d95279c919909cb840a8b68b057e'
  AND confirmed = true
ORDER BY ts;