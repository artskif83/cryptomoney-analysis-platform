-- Pending Orders for GoldenField1M strategy
-- This query fetches pending orders to display them on the chart
-- Orders are displayed as dashed lines from c_time to updated_at
-- Line color depends on pos_side: long → green, short → red, net → yellow
-- Red dashed lines represent stop-loss price

SELECT
    c_time AS time,
    updated_at AS end_time,
    ord_id,
    cl_ord_id,
    inst_id,
    px AS order_price,
    sl_trigger_px AS stop_loss_price,
    pos_side,
    state,
    sz AS size,
    lever
FROM pending_orders
WHERE
    inst_id LIKE '%BTC%' AND  -- adjust based on your instrument filter
    c_time BETWEEN $__timeFrom() AND $__timeTo()
ORDER BY c_time;

