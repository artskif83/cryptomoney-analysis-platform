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

const doubleMaValue1hRaw = col("metric_double_ma_value_1m_on_1h", 0);
const rsi14Raw = col("metric_rsi_14_1m", 0);

// ===== Торговые события (из второго query) =====
const eventTimes = col("time", 1);
const eventTypes = col("event_type", 1);
const eventDirections = col("direction", 1);
const eventPrices = col("price", 1);
const eventStopLoss = col("stop_loss_percentage", 1);
const eventTakeProfit = col("take_profit_percentage", 1);
const eventIsTest = col("is_test", 1);

// ===== Positions (из третьего query) =====
const posTimes = col("time", 2);
const posPrices = col("position_price", 2);
const posNotionalUsd = col("notional_usd", 2);
const posPosSides = col("pos_side", 2);

// ===== Pending Orders (из четвёртого query) =====
const ordTimes = col("time", 3);
const ordPrices = col("order_price", 3);
const ordPosSides = col("pos_side", 3);
const ordSizes = col("size", 3);
const ordIds = col("ord_id", 3);
const ordTypes = col("ord_type", 3);

if (!times.length) return {};

// ===== Привязка временных меток к ближайшей свечной (snap to candle) =====
// Решает проблему рассинхронизации вертикальной линии курсора между графиками:
// произвольные timestamps ордеров/позиций создают новые точки на оси X,
// из-за чего xAxis разных grid'ов перестают быть синхронными.
function snapToCandle(ts) {
    if (ts == null || !times.length) return ts;
    let lo = 0, hi = times.length - 1;
    while (lo < hi) {
        const mid = (lo + hi) >> 1;
        if (times[mid] < ts) lo = mid + 1;
        else hi = mid;
    }
    if (lo > 0 && Math.abs(times[lo - 1] - ts) < Math.abs(times[lo] - ts)) lo--;
    return times[lo];
}

// ===== Свечи =====
const candles = times.map((t, i) => [
    t,
    opens[i],
    closes[i],
    lows[i],
    highs[i]
]);


const doubleMaValue1h = times.map((t, i) => [
    t,
    doubleMaValue1hRaw[i] == null ? null : doubleMaValue1hRaw[i]
]);

const rsi14 = times.map((t, i) => [
    t,
    rsi14Raw[i] == null ? null : rsi14Raw[i]
]);


// ===== Торговые события - подготовка данных =====
const tradeEventsLong = [];
const tradeEventsShort = [];

for (let i = 0; i < eventTimes.length; i++) {
    const eventPrice = eventPrices[i];
        const direction = eventDirections[i];
    let displayPrice = eventPrice;

    if (direction === 'SHORT') {
        displayPrice = eventPrice * 1.002;
    } else if (direction === 'LONG') {
        displayPrice = eventPrice * 0.998;
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



// ===== Positions - подготовка линий =====
// Группируем последовательные записи позиций в непрерывные сегменты.
// Новый сегмент начинается при разрыве > 60 секунд ИЛИ смене pos_side.
const positionLines = [];

if (posTimes.length > 0) {
    let segData = [];
    let segSide = posPosSides[0];
    let segNotional = posNotionalUsd[0];
    let segPrice = posPrices[0];

    for (let i = 0; i < posTimes.length; i++) {
        const ts = snapToCandle(posTimes[i]);
        const price = posPrices[i];
        const side = posPosSides[i];
        const notional = posNotionalUsd[i];

        if (price == null || ts == null) continue;

        const prevTs = segData.length > 0 ? segData[segData.length - 1][0] : null;
        const gap = prevTs != null ? (ts - prevTs) : 0;
        const sideChanged = side !== segSide;

        // Разрыв больше одной свечи (с запасом 1.5x) или смена стороны → закрываем сегмент
        if (segData.length > 0 && (gap > 90000 || sideChanged)) {
            // Продлеваем последнюю точку на одну свечу вперёд
            const lastPt = segData[segData.length - 1];
            segData.push([lastPt[0] + 60000, lastPt[1]]);

            const lineColor = segSide === 'long' ? '#00FF66'
                : segSide === 'short' ? '#FF1A1A'
                    : '#FFD700';
            positionLines.push({
                data: [...segData],
                posSide: segSide,
                price: segPrice,
                notionalUsd: segNotional,
                lineColor: lineColor
            });
            segData = [];
        }

        if (segData.length === 0) {
            segSide = side;
            segNotional = notional;
            segPrice = price;
        }

        segData.push([ts, price]);
    }

    // Закрываем последний сегмент
    if (segData.length > 0) {
        const lastPt = segData[segData.length - 1];
        segData.push([lastPt[0] + 60000, lastPt[1]]);
        const lineColor = segSide === 'long' ? '#00FF66'
            : segSide === 'short' ? '#FF1A1A'
                : '#FFD700';
        positionLines.push({
            data: [...segData],
            posSide: segSide,
            price: segPrice,
            notionalUsd: segNotional,
            lineColor: lineColor
        });
    }
}


// ===== Pending Orders - подготовка линий =====
// Группируем последовательные записи одного ордера (по ord_id) в непрерывные сегменты.
// Новый сегмент начинается при разрыве > 90 секунд.
const orderLines = [];

if (ordTimes.length > 0) {
    // Группируем по ord_id
    const ordGroups = {};
    for (let i = 0; i < ordTimes.length; i++) {
        const id = ordIds[i] || `__noId_${i}`;
        if (!ordGroups[id]) {
            ordGroups[id] = {
                times: [], prices: [], sides: [], sizes: [], types: []
            };
        }
        const g = ordGroups[id];
        g.times.push(ordTimes[i]);
        g.prices.push(ordPrices[i]);
        g.sides.push(ordPosSides[i]);
        g.sizes.push(ordSizes[i]);
        g.types.push(ordTypes[i]);
    }

    for (const [ordId, g] of Object.entries(ordGroups)) {
        let segData = [];
        let segSide = g.sides[0];
        let segSize = g.sizes[0];
        let segType = g.types[0];
        let segPrice = g.prices[0];

        for (let i = 0; i < g.times.length; i++) {
            const ts = snapToCandle(g.times[i]);
            const price = g.prices[i];
            if (price == null || ts == null) continue;

            const prevTs = segData.length > 0 ? segData[segData.length - 1][0] : null;
            const gap = prevTs != null ? (ts - prevTs) : 0;

            if (segData.length > 0 && gap > 90000) {
                const lastPt = segData[segData.length - 1];
                segData.push([lastPt[0] + 60000, lastPt[1]]);

                const lineColor = segSide === 'long' ? '#00BFFF'
                    : segSide === 'short' ? '#FF8C00'
                    : '#FFFF00';
                orderLines.push({
                    data: [...segData],
                    posSide: segSide,
                    price: segPrice,
                    size: segSize,
                    ordType: segType,
                    ordId: ordId,
                    lineColor
                });
                segData = [];
            }

            if (segData.length === 0) {
                segSide = g.sides[i];
                segSize = g.sizes[i];
                segType = g.types[i];
                segPrice = g.prices[i];
            }

            segData.push([ts, price]);
        }

        if (segData.length > 0) {
            const lastPt = segData[segData.length - 1];
            segData.push([lastPt[0] + 60000, lastPt[1]]);
            const lineColor = segSide === 'long' ? '#00BFFF'
                : segSide === 'short' ? '#FF8C00'
                : '#FFFF00';
            orderLines.push({
                data: [...segData],
                posSide: segSide,
                price: segPrice,
                size: segSize,
                ordType: segType,
                ordId: ordId,
                lineColor
            });
        }
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
        { left: '5%', right: '5%', top: 10, height: '55%' },      // свечи (grid 0)
        { left: '5%', right: '5%', top: '68%', height: '12%' },   // Double MA value 1m on 1h (grid 1)
        { left: '5%', right: '5%', top: '83%', height: '12%' }    // RSI 14 (grid 2)
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
            scale: false,
            min: -5,
            max: 5,
            gridIndex: 1,
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
        },
        {
            scale: false,
            min: 0,
            max: 100,
            gridIndex: 2,
            splitNumber: 4,
            axisLabel: {
                formatter: (v) => v.toFixed(0)
            },
            axisPointer: {
                label: {
                    formatter: (params) => {
                        const v = params.value;
                        return v == null ? '' : `${v.toFixed(1)}`;
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


        // --- Double MA value 1m on 1h ---
        {
            name: 'Double MA value (1m on 1h)',
            type: 'line',
            data: doubleMaValue1h,
            xAxisIndex: 1,
            yAxisIndex: 1,
            symbol: 'none',
            connectNulls: false,
            lineStyle: { width: 1, color: '#AB47BC' }
        },

        // --- RSI 14 (1m on 5m) ---
        {
            name: 'RSI 14',
            type: 'line',
            data: rsi14.map(p => {
                const v = p[1];
                let color = '#90CAF9';
                if (v != null && v >= 70) color = '#FF5252';
                if (v != null && v <= 30) color = '#69F0AE';
                return { value: p, itemStyle: { color } };
            }),
            xAxisIndex: 2,
            yAxisIndex: 2,
            symbol: 'none',
            connectNulls: false,
            lineStyle: { width: 1.5, color: '#90CAF9' },
            markLine: {
                silent: true,
                symbol: 'none',
                data: [
                    {
                        yAxis: 70,
                        lineStyle: { type: 'dashed', color: '#FF5252', width: 1 },
                        label: { formatter: '70', position: 'end', color: '#FF5252' }
                    },
                    {
                        yAxis: 30,
                        lineStyle: { type: 'dashed', color: '#69F0AE', width: 1 },
                        label: { formatter: '30', position: 'end', color: '#69F0AE' }
                    }
                ]
            },
            markArea: {
                silent: true,
                data: [
                    [
                        { yAxis: 70, itemStyle: { color: 'rgba(255, 82, 82, 0.12)' } },
                        { yAxis: 100 }
                    ],
                    [
                        { yAxis: 0, itemStyle: { color: 'rgba(105, 240, 174, 0.12)' } },
                        { yAxis: 30 }
                    ]
                ]
            }
        },

        // --- Торговые события: LONG ---
        {
            name: 'Trade Event LONG',
            type: 'scatter',
            data: tradeEventsLong,
            xAxisIndex: 0,
            yAxisIndex: 0,
            symbol: 'triangle',
            symbolSize: 5,
            symbolRotate: 0,
            itemStyle: {
                color: '#00FF00',
                borderColor: '#00AA00',
                borderWidth: 0
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
            symbolSize: 5,
            symbolRotate: 180,
            itemStyle: {
                color: '#FF0000',
                borderColor: '#AA0000',
                borderWidth: 0
            },
            z: 10
        },


        // --- Positions (сплошные линии, цвет зависит от pos_side) ---
        ...positionLines.map((pos, idx) => ({
            name: `Position_${idx}`,
            type: 'line',
            data: pos.data,
            xAxisIndex: 0,
            yAxisIndex: 0,
            symbol: 'none',
            lineStyle: {
                color: pos.lineColor,
                width: 2,
                type: 'solid',
                opacity: 1
            },
            z: 6,
            tooltip: {
                formatter: () => {
                    const sideColor = pos.posSide === 'long' ? '#00FF66' : pos.posSide === 'short' ? '#FF4D4D' : '#FFD700';
                    return `<b style="color:${sideColor};">📌 Position</b><br/>
                            Side: ${pos.posSide}<br/>
                            Price: ${Number(pos.price).toFixed(4)}<br/>
                            Size: $${pos.notionalUsd != null ? Number(pos.notionalUsd).toFixed(2) : 'N/A'}`;
                }
            }
        })),

        // --- Pending Orders (пунктирные линии цена ордера, цвет зависит от pos_side) ---
        ...orderLines.map((ord, idx) => ({
            name: `Order_${idx}`,
            type: 'line',
            data: ord.data,
            xAxisIndex: 0,
            yAxisIndex: 0,
            symbol: 'none',
            lineStyle: {
                color: ord.lineColor,
                width: 1.5,
                type: 'dashed',
                opacity: 0.9
            },
            z: 5
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


            const doubleMa1hOn1hPoint = list.find(p => p.seriesName === 'Double MA value (1m on 1h)');
            const doubleMa1hOn1hVal = doubleMa1hOn1hPoint && Array.isArray(doubleMa1hOn1hPoint.data) ? doubleMa1hOn1hPoint.data[1] : null;

            const rsi14Point = list.find(p => p.seriesName === 'RSI 14');
            const rsi14Val = rsi14Point && rsi14Point.data && rsi14Point.data.value ? rsi14Point.data.value[1] : null;

            // Торговые события
            const tradeEventPoint = list.find(p =>
                p.seriesName === 'Trade Event LONG' ||
                p.seriesName === 'Trade Event SHORT'
            );


            // Positions на данном таймстемпе
            const positionPoints = list.filter(p => p.seriesName && p.seriesName.startsWith('Position_'));

            // Pending Orders на данном таймстемпе
            const orderPoints = list.filter(p => p.seriesName && p.seriesName.startsWith('Order_'));

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
            if (doubleMa1hOn1hVal != null) lines.push(`Double MA value (1m on 1h): ${doubleMa1hOn1hVal.toFixed(2)}`);
            if (rsi14Val != null) lines.push(`RSI 14 (1m on 5m): ${rsi14Val.toFixed(2)}`);

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


            // Positions
            if (positionPoints.length > 0) {
                // Собираем мета-данные позиций по индексу серии
                positionPoints.forEach(p => {
                    const idx = parseInt(p.seriesName.replace('Position_', ''), 10);
                    const meta = positionLines[idx];
                    if (!meta) return;
                    const sideColor = meta.posSide === 'long' ? '#00FF66' : meta.posSide === 'short' ? '#FF4D4D' : '#FFD700';
                    lines.push('');
                    lines.push(`<b style="color: ${sideColor};">📌 Position</b>`);
                    lines.push(`Side: ${meta.posSide}`);
                    lines.push(`Price: ${Number(meta.price).toFixed(4)}`);
                    lines.push(`Size: $${meta.notionalUsd != null ? Number(meta.notionalUsd).toFixed(2) : 'N/A'}`);
                });
            }

            // Pending Orders
            if (orderPoints.length > 0) {
                orderPoints.forEach(p => {
                    const idx = parseInt(p.seriesName.replace('Order_', ''), 10);
                    const meta = orderLines[idx];
                    if (!meta) return;
                    const sideColor = meta.posSide === 'long' ? '#00BFFF' : meta.posSide === 'short' ? '#FF8C00' : '#FFFF00';
                    lines.push('');
                    lines.push(`<b style="color: ${sideColor};">📋 Order [${meta.ordType || ''}]</b>`);
                    lines.push(`Side: ${meta.posSide}`);
                    lines.push(`Price: ${Number(meta.price).toFixed(4)}`);
                    if (meta.size != null) lines.push(`Size: ${meta.size}`);
                });
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
