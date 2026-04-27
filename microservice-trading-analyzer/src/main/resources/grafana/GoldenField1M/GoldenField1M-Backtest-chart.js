// ===== Получение данных =====
const frame = context.panel.data.series[0];
if (!frame) return {};

function col(name) {
    const f = frame.fields.find(x => x.name === name);
    return f ? Array.from(f.values) : [];
}

const times = col("time");
const candleIndexRaw = col("index_candle_1m");
const opens = col("open");
const highs = col("high");
const lows = col("low");
const closes = col("close");
let basePrice = closes[closes.length - 1]; // например, последний close

const doubleMaValue1hOn1hRaw = col("metric_double_ma_value_1m_on_1h");
const rsi14Raw = col("metric_rsi_14_1m_on_5m");
const shortLevelRaw = col("metric_short_trend_1m");
const shortStopLossRaw = col("metric_short_stop_los_1m");
const longLevelRaw = col("metric_long_trend_1m");
const longStopLossRaw = col("metric_long_stop_los_1m");

const posPrice = col("additional_position_price_1m");
const tpPrice = col("additional_takeprofit_1m");
const slPrice = col("additional_stoploss_1m");

if (!times.length) return {};

// ===== Свечи =====
const candles = times.map((t, i) => [
    t,
    opens[i],
    closes[i],
    lows[i],
    highs[i]
]);


const doubleMaValue1hOn1h = times.map((t, i) => [
    t,
    doubleMaValue1hOn1hRaw[i] == null ? null : doubleMaValue1hOn1hRaw[i]
]);

const rsi14 = times.map((t, i) => {
    const v = rsi14Raw[i];
    return { value: [t, v == null ? null : v] };
});



// ===== Зона сопротивления (short level + stop loss band) =====
// Рисуем отдельный прямоугольник для каждой свечи, чтобы зона точно
// следовала значениям short_trend (верх) и stop_loss (низ).
const shortBandSegments = [];

for (let i = 0; i < times.length; i++) {
    const rl = shortLevelRaw[i];
    const sl = shortStopLossRaw[i];
    if (rl == null || sl == null) continue;

    // Определяем конец прямоугольника: до начала следующей свечи или текущий бар
    const tEnd = i + 1 < times.length ? times[i + 1] : times[i];

    shortBandSegments.push([
        { xAxis: times[i], yAxis: Math.min(rl, sl) },
        { xAxis: tEnd,     yAxis: Math.max(rl, sl) }
    ]);
}

// ===== Зона поддержки (long level + stop loss band) =====
const longBandSegments = [];

for (let i = 0; i < times.length; i++) {
    const rl = longLevelRaw[i];
    const sl = longStopLossRaw[i];
    if (rl == null || sl == null) continue;

    const tEnd = i + 1 < times.length ? times[i + 1] : times[i];

    longBandSegments.push([
        { xAxis: times[i], yAxis: Math.min(rl, sl) },
        { xAxis: tEnd,     yAxis: Math.max(rl, sl) }
    ]);
}



// ===== Цвета =====
const upColor = '#4CAF50';
const upBorderColor = '#4CAF50';
const downColor = '#FF4D4D';
const downBorderColor = '#FF4D4D';

// Формирование позиции
const entryPoints = [];
const posLine = [];
const tpLine = [];
const slLine = [];

for (let i = 0; i < times.length; i++) {
    const t = times[i];

    // Считаем 0 как отсутствующее значение (из БД null может приходить как 0)
    const pos = (posPrice[i] == null || posPrice[i] === 0) ? null : posPrice[i];
    const tp = (tpPrice[i] == null || tpPrice[i] === 0) ? null : tpPrice[i];
    const sl = (slPrice[i] == null || slPrice[i] === 0) ? null : slPrice[i];

    const prevPos = i > 0 ? ((posPrice[i - 1] == null || posPrice[i - 1] === 0) ? null : posPrice[i - 1]) : null;

    // --- entry только на первом баре позиции ---
    if (pos != null && prevPos == null) {
        entryPoints.push([t, pos]);
    }

    // --- линии с разрывами через null ---
    posLine.push([t, pos]);
    tpLine.push([t, tp]);
    slLine.push([t, sl]);
}

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

        // --- Зона сопротивления (short level ↔ stop loss) ---
        {
            name: 'Short Band',
            type: 'line',
            data: [],
            xAxisIndex: 0,
            yAxisIndex: 0,
            silent: false,
            z: 2,
            markArea: {
                silent: true,
                itemStyle: {
                    color: 'rgba(255, 77, 77, 0.18)',
                    borderWidth: 0
                },
                data: shortBandSegments
            }
        },

        // --- Зона поддержки (long level ↔ stop loss) ---
        {
            name: 'Long Band',
            type: 'line',
            data: [],
            xAxisIndex: 0,
            yAxisIndex: 0,
            silent: false,
            z: 2,
            markArea: {
                silent: true,
                itemStyle: {
                    color: 'rgba(76, 175, 80, 0.18)',
                    borderWidth: 0
                },
                data: longBandSegments
            }
        },


        // Позиции
        // --- Position (blue dashed) ---
        {
            name: 'Position',
            type: 'line',
            data: posLine,
            xAxisIndex: 0,
            yAxisIndex: 0,
            symbol: 'none',
            connectNulls: false,
            lineStyle: { width: 1, color: '#4DA3FF', type: 'dashed' },
            tooltip: { show: false }
        },

        // --- Entry (only first bar) ---
        {
            name: 'Entry',
            type: 'scatter',
            data: entryPoints,
            xAxisIndex: 0,
            yAxisIndex: 0,
            symbol: 'triangle',
            tooltip: { show: false },
            symbolSize: 8,
            itemStyle: { color: '#00E676' }
        },

        // --- Take Profit ---
        {
            name: 'Take Profit',
            type: 'line',
            data: tpLine,
            xAxisIndex: 0,
            yAxisIndex: 0,
            symbol: 'none',
            tooltip: { show: false },
            connectNulls: false,
            lineStyle: { width: 1, color: '#4CAF50', type: 'dashed' }
        },

        // --- Stop Loss ---
        {
            name: 'Stop Loss',
            type: 'line',
            data: slLine,
            xAxisIndex: 0,
            yAxisIndex: 0,
            symbol: 'none',
            tooltip: { show: false },
            connectNulls: false,
            lineStyle: { width: 1, color: '#FF5252', type: 'dashed' }
        },

        // --- Double MA value 1m on 1h ---
        {
            name: 'Double MA value (1h on 1h)',
            type: 'line',
            data: doubleMaValue1hOn1h,
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
                const v = p.value[1];
                let color = '#90CAF9'; // neutral
                if (v != null && v >= 70) color = '#FF5252';
                if (v != null && v <= 30) color = '#69F0AE';
                return { value: p.value, itemStyle: { color } };
            }),
            xAxisIndex: 2,
            yAxisIndex: 2,
            symbol: 'none',
            connectNulls: false,
            lineStyle: { width: 1.5, color: '#90CAF9' },
            visualMap: false,
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


            const doubleMa1hOn1hPoint = list.find(p => p.seriesName === 'Double MA value (1h on 1h)');
            const doubleMa1hOn1hVal = doubleMa1hOn1hPoint && Array.isArray(doubleMa1hOn1hPoint.data) ? doubleMa1hOn1hPoint.data[1] : null;

            const rsi14Point = list.find(p => p.seriesName === 'RSI 14');
            const rsi14Val = rsi14Point && rsi14Point.data && rsi14Point.data.value ? rsi14Point.data.value[1] : null;



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
