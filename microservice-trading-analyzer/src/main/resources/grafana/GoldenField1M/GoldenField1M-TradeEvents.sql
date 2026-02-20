-- Trade Events for GoldenField1M strategy
-- This query fetches trade events to display them on the chart
-- Each event represents an entry or exit point in the trading strategy

SELECT
    timestamp AS time,
    event_type,
    direction,
    instrument,
    event_price AS price,
    stop_loss_percentage,
    take_profit_percentage,
    is_test,
    uuid
FROM trade_events
WHERE
    timeframe = '1m' AND
    tag = 'GoldenField1M-lifetime' AND
    timestamp BETWEEN $__timeFrom() AND $__timeTo()
ORDER BY timestamp;
