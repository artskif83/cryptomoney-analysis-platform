// ===== Получение данных =====
const frame = context.panel.data.series[0];
if (!frame) return {};

function col(name) {
    const f = frame.fields.find(x => x.name === name);
    return f ? Array.from(f.values) : [];
}

const times = col("time");
const opens = col("open");
const highs = col("high");
const lows = col("low");
const closes = col("close");

const rsiRaw = col("metric_rsi_14_5m");

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

const rsi = times.map((t, i) => [t, rsiRaw[i]]);

// ===== Цвета =====
const upColor = '#00C176';
const upBorderColor = '#00A563';
const downColor = '#FF4D4D';
const downBorderColor = '#CC3C3C';

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
        { left: '5%', right: '5%', top: 10, height: '55%' },   // свечи
        { left: '5%', right: '5%', top: '65%', height: '25%' }  // RSI
    ],

    xAxis: [
        {
            type: 'time',
            boundaryGap: false,
            axisPointer: { show: true }
        },
        {
            type: 'time',
            gridIndex: 1,
            boundaryGap: false,
            axisLabel: { show: true },
            axisPointer: { show: true }
        }
    ],

    yAxis: [
        { scale: true },
        { scale: true, gridIndex: 1, min: 0, max: 100 }
    ],

    dataZoom: [
        { type: 'inside', xAxisIndex: [0, 1], start: 80, end: 100 },
        { show: true, type: 'slider', bottom: 0, xAxisIndex: [0, 1], start: 80, end: 100 }
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
            lineStyle: { width: 2, color: '#4DA3FF', type: 'dashed' },
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
            lineStyle: { width: 2, color: '#4CAF50', type: 'dashed' }
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
            lineStyle: { width: 2, color: '#FF5252', type: 'dashed' }
        },

        // --- RSI ---
        {
            name: 'RSI 14 (5m)',
            type: 'line',
            data: rsi,
            xAxisIndex: 1,
            yAxisIndex: 1,
            symbol: 'none',
            lineStyle: { width: 1, color: '#00E676' }
        },

        // --- RSI 70 ---
        {
            name: 'RSI 70',
            type: 'line',
            xAxisIndex: 1,
            yAxisIndex: 1,
            data: times.map(t => [t, 70]),
            symbol: 'none',
            tooltip: { show: false },
            lineStyle: { width: 1, color: '#FF4D4D', type: 'dashed' }
        },

        // --- RSI 30 ---
        {
            name: 'RSI 30',
            type: 'line',
            xAxisIndex: 1,
            yAxisIndex: 1,
            data: times.map(t => [t, 30]),
            symbol: 'none',
            tooltip: { show: false },
            lineStyle: { width: 1, color: '#4DA3FF', type: 'dashed' }
        }
    ],

    tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(20,20,20,0.85)',
        borderWidth: 0,
        padding: [6, 8],
        textStyle: {
            color: '#ccc',
            fontSize: 8
        },
        axisPointer: {
            link: [{ xAxisIndex: [0, 1] }],
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
