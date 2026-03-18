-- Positions for GoldenField1M strategy
-- This query fetches open/closed positions to display them on the chart
-- Positions are displayed as solid lines from c_time to updated_at
-- Line color depends on pos_side: long → green, short → red, net → yellow
-- Stop-loss lines are displayed as red dashed lines

SELECT
    c_time    AS time,
    u_time AS end_time,
    pos_id,
    cl_ord_id,
    inst_id,
    px        AS position_price,
    sz        AS size,
    sl_trigger_px AS stop_loss_price,
    realized_pnl,
    pos_side,
    lever,
    state
FROM positions
WHERE
    inst_id LIKE '%BTC%' AND  -- adjust based on your instrument filter
    c_time BETWEEN $__timeFrom() AND $__timeTo()
ORDER BY c_time;

