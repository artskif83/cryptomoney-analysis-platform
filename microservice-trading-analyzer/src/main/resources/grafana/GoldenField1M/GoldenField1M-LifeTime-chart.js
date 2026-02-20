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

const resistanceLevelRaw = col("metric_resistance_level_1m");
const tripleMaValueRaw = col("metric_triple_ma_value_1m");

// ===== Торговые события (из второго query) =====
const eventTimes = col("time", 1);
const eventTypes = col("event_type", 1);
const eventDirections = col("direction", 1);
const eventPrices = col("price", 1);
const eventStopLoss = col("stop_loss_percentage", 1);
const eventTakeProfit = col("take_profit_percentage", 1);
const eventIsTest = col("is_test", 1);

if (!times.length) return {};

// ===== Свечи =====
const candles = times.map((t, i) => [
    t,
    opens[i],
    closes[i],
    lows[i],
    highs[i]
]);

const resistanceLevel = times.map((t, i) => [
    t,
    resistanceLevelRaw[i] == null ? null : resistanceLevelRaw[i]
]);

const tripleMaValue = times.map((t, i) => [
    t,
    tripleMaValueRaw[i] == null ? null : tripleMaValueRaw[i]
]);

// ===== Торговые события - подготовка данных =====
const tradeEventsLong = [];
const tradeEventsShort = [];

for (let i = 0; i < eventTimes.length; i++) {
    const eventData = {
        value: [eventTimes[i], eventPrices[i]],
        eventType: eventTypes[i],
        direction: eventDirections[i],
        stopLoss: eventStopLoss[i],
        takeProfit: eventTakeProfit[i],
        isTest: eventIsTest[i]
    };

    if (eventDirections[i] === 'LONG') {
        tradeEventsLong.push(eventData);
    } else if (eventDirections[i] === 'SHORT') {
        tradeEventsShort.push(eventData);
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
        { left: '5%', right: '5%', top: '72%', height: '12%' },   // Resistance level (grid 1)
        { left: '5%', right: '5%', top: '86%', height: '12%' }    // Triple MA value (grid 2)
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
                        return v == null ? '' : `${v}`;
                    }
                }
            }
        },
        {
            scale: true,
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
            start: 98,
            end: 100,
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


        // --- Resistance level ---
        {
            name: 'Resistance level (1m)',
            type: 'line',
            data: resistanceLevel,
            xAxisIndex: 1,
            yAxisIndex: 1,
            symbol: 'none',
            connectNulls: false,
            lineStyle: { width: 1, color: '#AB47BC' }
        },

        // --- Triple MA value ---
        {
            name: 'Triple MA value (1m)',
            type: 'line',
            data: tripleMaValue,
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
            symbolSize: 15,
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
            symbolSize: 15,
            symbolRotate: 180,
            itemStyle: {
                color: '#FF0000',
                borderColor: '#AA0000',
                borderWidth: 2
            },
            z: 10
        }
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

            const levelPoint = list.find(p => p.seriesName === 'Resistance level (1m)');
            const lVal = levelPoint && Array.isArray(levelPoint.data) ? levelPoint.data[1] : null;

            const tripleMaPoint = list.find(p => p.seriesName === 'Triple MA value (1m)');
            const tripleMaVal = tripleMaPoint && Array.isArray(tripleMaPoint.data) ? tripleMaPoint.data[1] : null;

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
            if (lVal != null) lines.push(`Resistance level: ${Math.round(lVal)}`);
            if (tripleMaVal != null) lines.push(`Triple MA value: ${tripleMaVal.toFixed(2)}`);

            // Информация о торговых событиях
            if (tradeEventPoint) {
                const eventData = tradeEventPoint.data;
                if (eventData && typeof eventData === 'object') {
                    lines.push(''); // пустая строка для разделения
                    lines.push(`<b style="color: #FFD700;">⚡ ${eventData.eventType || 'Trade Event'}</b>`);
                    lines.push(`Direction: <b>${eventData.direction || 'N/A'}</b>`);
                    lines.push(`Price: ${eventData.value ? Number(eventData.value[1]).toFixed(4) : 'N/A'}`);
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
