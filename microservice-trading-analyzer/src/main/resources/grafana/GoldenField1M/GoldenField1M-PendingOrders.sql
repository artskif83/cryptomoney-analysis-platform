-- Pending Orders for GoldenField1M strategy
-- Matches per candle by tf (timeframe) and ts (timestamp), same approach as positions.
-- Multiple orders can exist for the same candle timestamp.
-- Line color depends on pos_side: long → green, short → red, net → yellow

SELECT
    ts              AS time,
    sl_trigger_px              AS order_price,
    pos_side,
    sz              AS size,
    ord_id,
    ord_type
FROM pending_orders
WHERE
    tf = '1m' AND
    ts BETWEEN $__timeFrom() AND $__timeTo()
ORDER BY ts;
