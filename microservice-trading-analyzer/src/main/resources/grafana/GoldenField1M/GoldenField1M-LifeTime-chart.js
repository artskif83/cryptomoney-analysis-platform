// ===== Получение данных =====
const frame = context.panel.data.series[0];
if (!frame) return {};

function col(name, frameIndex = 0) {
    const targetFrame = context.panel.data.series[frameIndex];
    if (!targetFrame) return [];
    const f = targetFrame.fields.find(x => x.name === name);
    return f ? Array.from(f.values) : [];
}

const times = col("time");
const candleIndexRaw = col("index_candle_1m");
const opens = col("open");
const highs = col("high");
const lows = col("low");
const closes = col("close");
let basePrice = closes[closes.length - 1]; // например, последний close

const resistanceLevel5mRaw = col("metric_resistance_level_1m_on_5m", 0);
const resistanceLevel4hRaw = col("metric_resistance_level_1m_on_4h", 0);
const resistanceLevel1hRaw = col("metric_resistance_level_1m_on_1h", 0);
const doubleMaValue1hRaw = col("metric_double_ma_value_1m_on_1h", 0);

// ===== Торговые события (из второго query) =====
const eventTimes = col("time", 1);
const eventTypes = col("event_type", 1);
const eventDirections = col("direction", 1);
const eventPrices = col("price", 1);
const eventStopLoss = col("stop_loss_percentage", 1);
const eventTakeProfit = col("take_profit_percentage", 1);
const eventIsTest = col("is_test", 1);

// ===== Pending Orders (из третьего query) =====
const orderTimes = col("time", 2);
const orderEndTimes = col("end_time", 2);
const orderIds = col("ord_id", 2);
const orderPrices = col("order_price", 2);
const stopLossPrices = col("stop_loss_price", 2);
const orderPosSides = col("pos_side", 2);
const orderStates = col("state", 2);

// ===== Positions (из четвёртого query) =====
const posTimes = col("time", 3);
const posEndTimes = col("end_time", 3);
const posIds = col("pos_id", 3);
const posPrices = col("position_price", 3);
const posSlPrices = col("stop_loss_price", 3);
const posPosSides = col("pos_side", 3);
const posStates = col("state", 3);

if (!times.length) return {};

// ===== Свечи =====
const candles = times.map((t, i) => [
    t,
    opens[i],
    closes[i],
    lows[i],
    highs[i]
]);

const resistanceLevel5m = times.map((t, i) => [
    t,
    resistanceLevel5mRaw[i] == null ? null : resistanceLevel5mRaw[i]
]);

const resistanceLevel4h = times.map((t, i) => [
    t,
    resistanceLevel4hRaw[i] == null ? null : resistanceLevel4hRaw[i]
]);

const resistanceLevel1h = times.map((t, i) => [
    t,
    resistanceLevel1hRaw[i] == null ? null : resistanceLevel1hRaw[i]
]);

const doubleMaValue1h = times.map((t, i) => [
    t,
    doubleMaValue1hRaw[i] == null ? null : doubleMaValue1hRaw[i]
]);

// ===== Торговые события - подготовка данных =====
const tradeEventsLong = [];
const tradeEventsShort = [];

for (let i = 0; i < eventTimes.length; i++) {
    const eventPrice = eventPrices[i];
    const stopLossPercent = eventStopLoss[i];
    const direction = eventDirections[i];
    let displayPrice = eventPrice;

    if (stopLossPercent != null && stopLossPercent > 0) {
        const stopLossOffset = eventPrice * (stopLossPercent / 100);

        if (direction === 'SHORT') {
            // Для SHORT позиции: маркер ВЫШЕ на величину stopLoss
            // (стоп-лосс для шорта находится выше цены входа)
            displayPrice = eventPrice + stopLossOffset;
        } else if (direction === 'LONG') {
            // Для LONG позиции: маркер НИЖЕ на величину stopLoss
            // (стоп-лосс для лонга находится ниже цены входа)
            displayPrice = eventPrice - stopLossOffset;
        }
    } else {
        // Если stopLoss не задан, отступаем на 0.2% от цены
        if (direction === 'SHORT') {
            displayPrice = eventPrice * 1.002;
        } else if (direction === 'LONG') {
            displayPrice = eventPrice * 0.998;
        }
    }

    const eventData = {
        value: [eventTimes[i], displayPrice],
        eventType: eventTypes[i],
        direction: eventDirections[i],
        stopLoss: eventStopLoss[i],
        takeProfit: eventTakeProfit[i],
        isTest: eventIsTest[i],
        actualPrice: eventPrice // сохраняем реальную цену для tooltip
    };

    if (direction === 'LONG') {
        tradeEventsLong.push(eventData);
    } else if (direction === 'SHORT') {
        tradeEventsShort.push(eventData);
    }
}


// ===== Pending Orders - подготовка линий =====
const orderLines = [];
const stopLossLines = [];

for (let i = 0; i < orderTimes.length; i++) {
    const startTime = orderTimes[i];
    const endTime = orderEndTimes[i];
    const orderPrice = orderPrices[i];
    const stopLossPrice = stopLossPrices[i];
    const orderId = orderIds[i];
    const posSide = orderPosSides[i];
    const state = orderStates[i];

    // Цвет линии ордера зависит от pos_side: long → зелёный, short → красный, net → жёлтый
    const orderLineColor = posSide === 'long' ? '#00FF00'
        : posSide === 'short' ? '#FF4D4D'
        : '#FFD700'; // net

    // Создаем линию для ордера (пунктирная, цвет по pos_side)
    if (orderPrice != null && startTime != null && endTime != null) {
        orderLines.push({
            id: `order_${orderId}`,
            data: [
                [startTime, orderPrice],
                [endTime, orderPrice]
            ],
            orderId: orderId,
            posSide: posSide,
            state: state,
            price: orderPrice,
            lineColor: orderLineColor
        });
    }

    // Создаем линию для стоп-лосса (красная пунктирная)
    if (stopLossPrice != null && startTime != null && endTime != null) {
        stopLossLines.push({
            id: `sl_${orderId}`,
            data: [
                [startTime, stopLossPrice],
                [endTime, stopLossPrice]
            ],
            orderId: orderId,
            posSide: posSide,
            state: state,
            price: stopLossPrice
        });
    }
}


// ===== Positions - подготовка линий =====
const positionLines = [];
const positionSlLines = [];

for (let i = 0; i < posTimes.length; i++) {
    const startTime = posTimes[i];
    const endTime = posEndTimes[i];
    const posPrice = posPrices[i];
    const posSlPrice = posSlPrices[i];
    const posId = posIds[i];
    const posSide = posPosSides[i];
    const posState = posStates[i];

    // Цвет линии позиции: long → зелёный, short → красный, net → жёлтый
    const posLineColor = posSide === 'long' ? '#00FF00'
        : posSide === 'short' ? '#FF4D4D'
        : '#FFD700'; // net

    // Сплошная линия для позиции
    if (posPrice != null && startTime != null && endTime != null) {
        positionLines.push({
            id: `pos_${posId}`,
            data: [
                [startTime, posPrice],
                [endTime, posPrice]
            ],
            posId: posId,
            posSide: posSide,
            state: posState,
            price: posPrice,
            lineColor: posLineColor
        });
    }

    // Пунктирная линия для стоп-лосса позиции
    if (posSlPrice != null && startTime != null && endTime != null) {
        positionSlLines.push({
            id: `pos_sl_${posId}`,
            data: [
                [startTime, posSlPrice],
                [endTime, posSlPrice]
            ],
            posId: posId,
            posSide: posSide,
            state: posState,
            price: posSlPrice
        });
    }
}


// ===== Цвета =====
const upColor = '#4CAF50';
const upBorderColor = '#4CAF50';
const downColor = '#FF4D4D';
const downBorderColor = '#FF4D4D';


// ===== Конфигурация =====
return {
    animation: false,

    grid: [
        { left: '5%', right: '5%', top: 10, height: '70%' },      // свечи (grid 0)
        { left: '5%', right: '5%', top: '72%', height: '12%' },   // Resistance levels (grid 1)
        { left: '5%', right: '5%', top: '86%', height: '12%' }    // Double MA value 1h (grid 2)
    ],

    xAxis: [
        {
            type: 'time',
            boundaryGap: false,
            axisLabel: {
                formatter: {
                    year: '{yyyy}',
                    month: '{dd}.{MM}',
                    day: '{dd}.{MM}',
                    hour: '{dd}.{MM} {HH}:{mm}',
                    minute: '{dd}.{MM} {HH}:{mm}',
                    second: '{HH}:{mm}:{ss}',
                    millisecond: '{HH}:{mm}:{ss}'
                }
            },
            axisPointer: {
                show: true,
                label: { show: false }
            }
        },
        {
            type: 'time',
            gridIndex: 1,
            boundaryGap: false,
            axisLabel: { show: false },
            axisPointer: {
                show: true,
                label: { show: false }
            }
        },
        {
            type: 'time',
            gridIndex: 2,
            boundaryGap: false,
            axisLabel: {
                formatter: {
                    year: '{yyyy}',
                    month: '{dd}.{MM}',
                    day: '{dd}.{MM}',
                    hour: '{dd}.{MM} {HH}:{mm}',
                    minute: '{dd}.{MM} {HH}:{mm}',
                    second: '{HH}:{mm}:{ss}',
                    millisecond: '{HH}:{mm}:{ss}'
                }
            },
            axisPointer: {
                show: true,
                label: { show: false }
            }
        }
    ],

    yAxis: [
        {
            scale: true,
            axisPointer: {
                label: {
                    formatter: (params) => {
                        const price = params.value;
                        const pct = ((price - basePrice) / basePrice) * 100;

                        return `${price.toFixed(1)}  (${pct >= 0 ? '+' : ''}${pct.toFixed(2)}%)`;
                    }
                }
            }
        },
        {
            scale: true,
            gridIndex: 1,
            axisLabel: {
                formatter: (v) => Math.round(v)
            },
            axisPointer: {
                label: {
                    formatter: (params) => {
                        const v = params.value;
                        return v == null ? '' : `${Math.round(v)}`;
                    }
                }
            }
        },
        {
            scale: false,
            min: -1,
            max: 1,
            gridIndex: 2,
            axisLabel: {
                formatter: (v) => v.toFixed(2)
            },
            axisPointer: {
                label: {
                    formatter: (params) => {
                        const v = params.value;
                        return v == null ? '' : `${v.toFixed(2)}`;
                    }
                }
            }
        }
    ],

    axisPointer: {
        link: [{ xAxisIndex: [0, 1, 2] }]
    },

    toolbox: {
        feature: {
            dataZoom: {
                yAxisIndex: false
            },
            restore: {}
        }
    },

    dataZoom: [
        {
            type: 'inside',
            xAxisIndex: [0, 1, 2],
            zoomOnMouseWheel: true,
            moveOnMouseMove: true,
            moveOnMouseWheel: false,
            preventDefaultMouseMove: true
        }
    ],

    series: [

        // --- Свечи ---
        {
            name: 'Candles',
            type: 'candlestick',
            data: candles,
            xAxisIndex: 0,
            yAxisIndex: 0,
            itemStyle: {
                color: upColor,
                color0: downColor,
                borderColor: upBorderColor,
                borderColor0: downBorderColor
            }
        },


        // --- Resistance level 5m ---
        {
            name: 'Resistance level (5m)',
            type: 'line',
            data: resistanceLevel5m,
            xAxisIndex: 1,
            yAxisIndex: 1,
            symbol: 'none',
            connectNulls: false,
            lineStyle: { width: 1, color: '#FFD700' }
        },

        // --- Resistance level 4h ---
        {
            name: 'Resistance level (4h)',
            type: 'line',
            data: resistanceLevel4h,
            xAxisIndex: 1,
            yAxisIndex: 1,
            symbol: 'none',
            connectNulls: false,
            lineStyle: { width: 1, color: '#2196F3' }
        },

        // --- Resistance level 1h ---
        {
            name: 'Resistance level (1h)',
            type: 'line',
            data: resistanceLevel1h,
            xAxisIndex: 1,
            yAxisIndex: 1,
            symbol: 'none',
            connectNulls: false,
            lineStyle: { width: 1, color: '#FF4D4D' }
        },

        // --- Double MA value 1h ---
        {
            name: 'Double MA value (1h)',
            type: 'line',
            data: doubleMaValue1h,
            xAxisIndex: 2,
            yAxisIndex: 2,
            symbol: 'none',
            connectNulls: false,
            lineStyle: { width: 1, color: '#FFA726' }
        },

        // --- Торговые события: LONG ---
        {
            name: 'Trade Event LONG',
            type: 'scatter',
            data: tradeEventsLong,
            xAxisIndex: 0,
            yAxisIndex: 0,
            symbol: 'triangle',
            symbolSize: 10,
            symbolRotate: 0,
            itemStyle: {
                color: '#00FF00',
                borderColor: '#00AA00',
                borderWidth: 2
            },
            z: 10
        },

        // --- Торговые события: SHORT ---
        {
            name: 'Trade Event SHORT',
            type: 'scatter',
            data: tradeEventsShort,
            xAxisIndex: 0,
            yAxisIndex: 0,
            symbol: 'triangle',
            symbolSize: 10,
            symbolRotate: 180,
            itemStyle: {
                color: '#FF0000',
                borderColor: '#AA0000',
                borderWidth: 2
            },
            z: 10
        },

        // --- Pending Orders (пунктирные линии, цвет зависит от pos_side) ---
        ...orderLines.map(order => ({
            name: `Order ${order.orderId}`,
            type: 'line',
            data: order.data,
            xAxisIndex: 0,
            yAxisIndex: 0,
            symbol: 'none',
            lineStyle: {
                color: order.lineColor,
                width: 1,
                type: 'dashed',
                opacity: 0.8
            },
            z: 5,
            tooltip: {
                formatter: () => {
                    return `<b>Pending Order</b><br/>
                            ID: ${order.orderId}<br/>
                            Pos Side: ${order.posSide}<br/>
                            State: ${order.state}<br/>
                            Price: ${Number(order.price).toFixed(4)}`;
                }
            }
        })),

        // --- Stop Loss (красные пунктирные линии) ---
        ...stopLossLines.map(sl => ({
            name: `Stop Loss ${sl.orderId}`,
            type: 'line',
            data: sl.data,
            xAxisIndex: 0,
            yAxisIndex: 0,
            symbol: 'none',
            lineStyle: {
                color: '#FF0000',
                width: 1,
                type: 'dashed',
                opacity: 0.8
            },
            z: 5,
            tooltip: {
                formatter: () => {
                    return `<b>Stop Loss</b><br/>
                            Order ID: ${sl.orderId}<br/>
                            Pos Side: ${sl.posSide}<br/>
                            State: ${sl.state}<br/>
                            Price: ${Number(sl.price).toFixed(4)}`;
                }
            }
        })),

        // --- Positions (сплошные линии, цвет зависит от pos_side) ---
        ...positionLines.map(pos => ({
            name: `Position ${pos.posId}`,
            type: 'line',
            data: pos.data,
            xAxisIndex: 0,
            yAxisIndex: 0,
            symbol: 'none',
            lineStyle: {
                color: pos.lineColor,
                width: 2,
                type: 'solid',
                opacity: 0.9
            },
            z: 6,
            tooltip: {
                formatter: () => {
                    return `<b>Position</b><br/>
                            ID: ${pos.posId}<br/>
                            Pos Side: ${pos.posSide}<br/>
                            State: ${pos.state}<br/>
                            Price: ${Number(pos.price).toFixed(4)}`;
                }
            }
        })),

        // --- Position Stop Loss (красные пунктирные линии) ---
        ...positionSlLines.map(sl => ({
            name: `Position SL ${sl.posId}`,
            type: 'line',
            data: sl.data,
            xAxisIndex: 0,
            yAxisIndex: 0,
            symbol: 'none',
            lineStyle: {
                color: '#FF0000',
                width: 1,
                type: 'dashed',
                opacity: 0.8
            },
            z: 6,
            tooltip: {
                formatter: () => {
                    return `<b>Position Stop Loss</b><br/>
                            Pos ID: ${sl.posId}<br/>
                            Pos Side: ${sl.posSide}<br/>
                            State: ${sl.state}<br/>
                            Price: ${Number(sl.price).toFixed(4)}`;
                }
            }
        }))
    ],

    tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(20,20,20,0.85)',
        borderWidth: 0,
        padding: [6, 8],
        textStyle: {
            color: '#ccc',
            fontSize: 10
        },
        formatter: (params) => {
            const list = Array.isArray(params) ? params : [params];
            const first = list[0];
            const axisValue = first && first.axisValue;

            // Пробуем найти индекс свечи по времени (как по key), с защитой от отсутствия данных
            let candleIndex = null;
            let currentIdx = -1;
            if (axisValue != null && times && times.length) {
                const idx = times.indexOf(axisValue);
                candleIndex = idx >= 0 ? candleIndexRaw[idx] : null;
                currentIdx = idx;
            }

            // Candle series в ECharts candlestick приходит как [time, open, close, low, high]
            const candlePoint = list.find(p => p.seriesName === 'Candles');
            const cv = candlePoint && Array.isArray(candlePoint.data) ? candlePoint.data : null;

            const o = cv ? cv[1] : null;
            const c = cv ? cv[2] : null;
            const l = cv ? cv[3] : null;
            const h = cv ? cv[4] : null;

            // Получаем предыдущий high
            let prevH = null;
            if (currentIdx > 0 && highs && highs.length > currentIdx) {
                prevH = highs[currentIdx - 1];
            }

            const levelPoint5m = list.find(p => p.seriesName === 'Resistance level (5m)');
            const lVal5m = levelPoint5m && Array.isArray(levelPoint5m.data) ? levelPoint5m.data[1] : null;

            const levelPoint4h = list.find(p => p.seriesName === 'Resistance level (4h)');
            const lVal4h = levelPoint4h && Array.isArray(levelPoint4h.data) ? levelPoint4h.data[1] : null;

            const levelPoint1h = list.find(p => p.seriesName === 'Resistance level (1h)');
            const lVal1h = levelPoint1h && Array.isArray(levelPoint1h.data) ? levelPoint1h.data[1] : null;

            const doubleMaPoint = list.find(p => p.seriesName === 'Double MA value (1h)');
            const doubleMaVal = doubleMaPoint && Array.isArray(doubleMaPoint.data) ? doubleMaPoint.data[1] : null;

            // Торговые события
            const tradeEventPoint = list.find(p =>
                p.seriesName === 'Trade Event LONG' ||
                p.seriesName === 'Trade Event SHORT'
            );

            // Расчет теней и изменений
            let upperShadowPct = null;
            let lowerShadowPct = null;
            let candleChangePct = null;
            let bodyChangePct = null;
            let highChangePct = null;

            if (o != null && c != null && h != null && l != null) {
                const bodyTop = Math.max(o, c);
                const bodyBottom = Math.min(o, c);

                // Верхняя тень: (high - max(close, open)) / max(close, open) * 100
                if (h > bodyTop) {
                    upperShadowPct = ((h - bodyTop) / bodyTop) * 100;
                }

                // Нижняя тень: (min(close, open) - low) / min(close, open) * 100
                if (bodyBottom > l) {
                    lowerShadowPct = ((bodyBottom - l) / bodyBottom) * 100;
                }

                // Процентное изменение всей свечи: (high - low) / low * 100
                if (l > 0) {
                    candleChangePct = ((h - l) / l) * 100;
                }

                // Процентное изменение тела свечи: (close - open) / open * 100
                if (o > 0) {
                    bodyChangePct = ((c - o) / o) * 100;
                }

                // Процентное изменение high относительно предыдущего high
                if (prevH != null && prevH > 0) {
                    highChangePct = ((h - prevH) / prevH) * 100;
                }
            }

            const lines = [];
            if (first && first.axisValueLabel) lines.push(first.axisValueLabel);
            if (candleIndex != null) lines.push(`index: ${candleIndex}`);
            if (o != null || h != null || l != null || c != null) {
                if (o != null) lines.push(`O: ${Number(o).toFixed(4)}`);
                if (h != null) lines.push(`H: ${Number(h).toFixed(4)}`);
                if (l != null) lines.push(`L: ${Number(l).toFixed(4)}`);
                if (c != null) lines.push(`C: ${Number(c).toFixed(4)}`);
            }
            if (candleChangePct != null) lines.push(`Candle change: ${candleChangePct.toFixed(2)}%`);
            if (bodyChangePct != null) lines.push(`Body change: ${bodyChangePct >= 0 ? '+' : ''}${bodyChangePct.toFixed(2)}%`);
            if (highChangePct != null) lines.push(`High vs Prev High: ${highChangePct >= 0 ? '+' : ''}${highChangePct.toFixed(2)}%`);
            if (upperShadowPct != null) lines.push(`Upper shadow: ${upperShadowPct.toFixed(2)}%`);
            if (lowerShadowPct != null) lines.push(`Lower shadow: ${lowerShadowPct.toFixed(2)}%`);
            if (lVal5m != null) lines.push(`Resistance level (5m): ${Math.round(lVal5m)}`);
            if (lVal1h != null) lines.push(`Resistance level (1h): ${Math.round(lVal1h)}`);
            if (lVal4h != null) lines.push(`Resistance level (4h): ${Math.round(lVal4h)}`);
            if (doubleMaVal != null) lines.push(`Double MA value (1h): ${doubleMaVal.toFixed(2)}`);

            // Информация о торговых событиях
            if (tradeEventPoint) {
                const eventData = tradeEventPoint.data;
                if (eventData && typeof eventData === 'object') {
                    lines.push(''); // пустая строка для разделения
                    lines.push(`<b style="color: #FFD700;">⚡ ${eventData.eventType || 'Trade Event'}</b>`);
                    lines.push(`Direction: <b>${eventData.direction || 'N/A'}</b>`);
                    // Показываем реальную цену события, а не displayPrice
                    const priceToShow = eventData.actualPrice || (eventData.value ? eventData.value[1] : null);
                    lines.push(`Price: ${priceToShow ? Number(priceToShow).toFixed(4) : 'N/A'}`);
                    if (eventData.stopLoss != null) {
                        lines.push(`Stop Loss: ${Number(eventData.stopLoss).toFixed(2)}%`);
                    }
                    if (eventData.takeProfit != null) {
                        lines.push(`Take Profit: ${Number(eventData.takeProfit).toFixed(2)}%`);
                    }
                    if (eventData.isTest) {
                        lines.push(`<span style="color: #FFA500;">[TEST]</span>`);
                    }
                }
            }

            return lines.join('<br/>');
        },

        axisPointer: {
            link: [{ xAxisIndex: [0, 1, 2] }],
            triggerTooltip: false,
            type: 'cross',
            crossStyle: {
                color: '#888',
                width: 1,
                type: 'dashed'
            },
            label: {
                show: true,
                backgroundColor: '#222'
            }
        }
    }
};
