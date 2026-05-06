-- Positions for GoldenField1M strategy
-- Positions are matched per candle by tf (timeframe) and ts (timestamp)
-- Line color depends on pos_side: long → green, short → red
-- One or more positions per candle, drawn as solid lines for the duration of one candle

SELECT
    ts             AS time,
    px             AS position_price,
    notional_usd,
    pos_side
FROM positions
WHERE
    tf = '1m' AND
    ts BETWEEN $__timeFrom() AND $__timeTo()
ORDER BY ts;
