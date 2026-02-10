// ===== Получение данных =====
const frame = context.panel.data.series[0];
if (!frame) return {};

function col(name) {
    const f = frame.fields.find(x => x.name === name);
    return f ? Array.from(f.values) : [];
}

const times = col("time");
const candleIndexRaw = col("index_candle_5m");
const opens = col("open");
const highs = col("high");
const lows = col("low");
const closes = col("close");
let basePrice = closes[closes.length - 1]; // например, последний close

const resistanceStrengthRaw = col("metric_candle_resistance_strength_5m");
const resistanceLevelRaw = col("metric_resistance_level_5m");

const posPrice = col("additional_position_price_5m");
const tpPrice = col("additional_takeprofit_5m");
const slPrice = col("additional_stoploss_5m");

if (!times.length) return {};

// ===== Свечи =====
const candles = times.map((t, i) => [
    t,
    opens[i],
    closes[i],
    lows[i],
    highs[i]
]);

const resistanceStrength = times.map((t, i) => [
    t,
    resistanceStrengthRaw[i] == null ? null : resistanceStrengthRaw[i]
]);

const resistanceLevel = times.map((t, i) => [
    t,
    resistanceLevelRaw[i] == null ? null : resistanceLevelRaw[i]
]);

// Дополнительные графики (для примера - дубликаты resistanceLevel)
const metric1 = times.map((t, i) => [t, resistanceLevelRaw[i] == null ? null : resistanceLevelRaw[i] * 0.95]);
const metric2 = times.map((t, i) => [t, resistanceLevelRaw[i] == null ? null : resistanceLevelRaw[i] * 1.05]);
const metric3 = times.map((t, i) => [t, resistanceLevelRaw[i] == null ? null : resistanceLevelRaw[i] * 0.9]);
const metric4 = times.map((t, i) => [t, resistanceLevelRaw[i] == null ? null : resistanceLevelRaw[i] * 1.1]);
const metric5 = times.map((t, i) => [t, resistanceLevelRaw[i] == null ? null : resistanceLevelRaw[i] * 0.98]);
const metric6 = times.map((t, i) => [t, resistanceLevelRaw[i] == null ? null : resistanceLevelRaw[i] * 1.02]);

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

    const pos = posPrice[i];
    const tp = tpPrice[i];
    const sl = slPrice[i];

    const prevPos = i > 0 ? posPrice[i - 1] : null;

    // --- entry только на первом баре позиции ---
    if (pos != null && prevPos == null) {
        entryPoints.push([t, pos]);
    }

    // --- линии с разрывами через null ---
    posLine.push([t, pos == null ? null : pos]);
    tpLine.push([t, tp == null ? null : tp]);
    slLine.push([t, sl == null ? null : sl]);
}

// ===== Конфигурация =====
return {
    animation: false,

    grid: [
        { left: '5%', right: '5%', top: 10, height: '50%' },      // свечи (grid 0)
        { left: '5%', right: '5%', top: '52%', height: '5.5%' },  // Resistance strength (grid 1)
        { left: '5%', right: '5%', top: '58.5%', height: '5.5%' },// Resistance level (grid 2)
        { left: '5%', right: '5%', top: '65%', height: '5.5%' },  // Metric 1 (grid 3)
        { left: '5%', right: '5%', top: '71.5%', height: '5.5%' },// Metric 2 (grid 4)
        { left: '5%', right: '5%', top: '78%', height: '5.5%' },  // Metric 3 (grid 5)
        { left: '5%', right: '5%', top: '84.5%', height: '5.5%' },// Metric 4 (grid 6)
        { left: '5%', right: '5%', top: '91%', height: '4%' },    // Metric 5 (grid 7)
        { left: '5%', right: '5%', top: '96%', height: '4%' }     // Metric 6 (grid 8)
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
            axisLabel: { show: false },
            axisPointer: {
                show: true,
                label: { show: false }
            }
        },
        {
            type: 'time',
            gridIndex: 3,
            boundaryGap: false,
            axisLabel: { show: false },
            axisPointer: {
                show: true,
                label: { show: false }
            }
        },
        {
            type: 'time',
            gridIndex: 4,
            boundaryGap: false,
            axisLabel: { show: false },
            axisPointer: {
                show: true,
                label: { show: false }
            }
        },
        {
            type: 'time',
            gridIndex: 5,
            boundaryGap: false,
            axisLabel: { show: false },
            axisPointer: {
                show: true,
                label: { show: false }
            }
        },
        {
            type: 'time',
            gridIndex: 6,
            boundaryGap: false,
            axisLabel: { show: false },
            axisPointer: {
                show: true,
                label: { show: false }
            }
        },
        {
            type: 'time',
            gridIndex: 7,
            boundaryGap: false,
            axisLabel: { show: false },
            axisPointer: {
                show: true,
                label: { show: false }
            }
        },
        {
            type: 'time',
            gridIndex: 8,
            boundaryGap: false,
            axisLabel: { show: false },
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
                formatter: (v) => `${v}`
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
            gridIndex: 3,
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
            gridIndex: 4,
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
            gridIndex: 5,
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
            gridIndex: 6,
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
            gridIndex: 7,
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
            gridIndex: 8,
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
        }
    ],

    axisPointer: {
        link: [{ xAxisIndex: [0, 1, 2, 3, 4, 5, 6, 7, 8] }]
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
            xAxisIndex: [0, 1, 2, 3, 4, 5, 6, 7, 8],
            start: 80,
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

        // --- Resistance strength ---
        {
            name: 'Resistance strength (5m)',
            type: 'line',
            data: resistanceStrength,
            xAxisIndex: 1,
            yAxisIndex: 1,
            symbol: 'none',
            connectNulls: false,
            lineStyle: { width: 1, color: '#FFA726' }
        },

        // --- Resistance level ---
        {
            name: 'Resistance level (5m)',
            type: 'line',
            data: resistanceLevel,
            xAxisIndex: 2,
            yAxisIndex: 2,
            symbol: 'none',
            connectNulls: false,
            lineStyle: { width: 1, color: '#AB47BC' }
        },

        // --- Metric 1 ---
        {
            name: 'Metric 1',
            type: 'line',
            data: metric1,
            xAxisIndex: 3,
            yAxisIndex: 3,
            symbol: 'none',
            connectNulls: false,
            lineStyle: { width: 1, color: '#26C6DA' }
        },

        // --- Metric 2 ---
        {
            name: 'Metric 2',
            type: 'line',
            data: metric2,
            xAxisIndex: 4,
            yAxisIndex: 4,
            symbol: 'none',
            connectNulls: false,
            lineStyle: { width: 1, color: '#66BB6A' }
        },

        // --- Metric 3 ---
        {
            name: 'Metric 3',
            type: 'line',
            data: metric3,
            xAxisIndex: 5,
            yAxisIndex: 5,
            symbol: 'none',
            connectNulls: false,
            lineStyle: { width: 1, color: '#EF5350' }
        },

        // --- Metric 4 ---
        {
            name: 'Metric 4',
            type: 'line',
            data: metric4,
            xAxisIndex: 6,
            yAxisIndex: 6,
            symbol: 'none',
            connectNulls: false,
            lineStyle: { width: 1, color: '#FFCA28' }
        },

        // --- Metric 5 ---
        {
            name: 'Metric 5',
            type: 'line',
            data: metric5,
            xAxisIndex: 7,
            yAxisIndex: 7,
            symbol: 'none',
            connectNulls: false,
            lineStyle: { width: 1, color: '#42A5F5' }
        },

        // --- Metric 6 ---
        {
            name: 'Metric 6',
            type: 'line',
            data: metric6,
            xAxisIndex: 8,
            yAxisIndex: 8,
            symbol: 'none',
            connectNulls: false,
            lineStyle: { width: 1, color: '#8D6E63' }
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

            const strengthPoint = list.find(p => p.seriesName === 'Resistance strength (5m)');
            const sVal = strengthPoint && Array.isArray(strengthPoint.data) ? strengthPoint.data[1] : null;

            const levelPoint = list.find(p => p.seriesName === 'Resistance level (5m)');
            const lVal = levelPoint && Array.isArray(levelPoint.data) ? levelPoint.data[1] : null;

            // Расчет теней и изменений
            let upperShadowPct = null;
            let lowerShadowPct = null;
            let candleChangePct = null;
            let bodyChangePct = null;
            let highChangePct = null;

            if (o != null && c != null && h != null && l != null) {
                const bodyTop = Math.max(o, c);
                const bodyBottom = Math.min(o, c);
                const bodySize = Math.abs(c - o);

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
            if (sVal != null) lines.push(`Resistance strength: ${sVal}`);
            if (lVal != null) lines.push(`Resistance level: ${Math.round(lVal)}`);

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
