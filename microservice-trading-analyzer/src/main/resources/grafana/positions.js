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

const adxRaw = col("feature_adx_14_4h");
const rsiRaw = col("feature_rsi_14_4h");

if (!times.length) return {};

// ===== Свечи =====
const candles = times.map((t, i) => [
    t,
    opens[i],
    closes[i],
    lows[i],
    highs[i]
]);

// ===== ADX / RSI =====
const adx = times.map((t, i) => [t, adxRaw[i]]);
const rsi = times.map((t, i) => [t, rsiRaw[i]]);

// ===== Цвета =====
const upColor = '#00C176';
const upBorderColor = '#00A563';
const downColor = '#FF4D4D';
const downBorderColor = '#CC3C3C';

// ===== Конфигурация =====
return {
    animation: false,

    grid: [
        { left: '5%', right: '5%', top: 10, height: '55%' },   // свечи
        { left: '5%', right: '5%', top: '62%', height: '18%' }, // ADX
        { left: '5%', right: '5%', top: '82%', height: '15%' }  // RSI
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
            axisLabel: { show: false },
            axisPointer: { show: true }
        },
        {
            type: 'time',
            gridIndex: 2,
            boundaryGap: false,
            axisLabel: { show: false },
            axisPointer: { show: true }
        }
    ],

    yAxis: [
        { scale: true },
        { scale: true, gridIndex: 1, min: 0 },
        { scale: true, gridIndex: 2, min: 0, max: 100 }
    ],

    dataZoom: [
        { type: 'inside', xAxisIndex: [0, 1, 2], start: 80, end: 100 },
        { show: true, type: 'slider', bottom: 0, xAxisIndex: [0, 1, 2], start: 80, end: 100 }
    ],

    axisPointer: {
        link: [{ xAxisIndex: [0, 1, 2] }],   // связать все 3 панели
        triggerTooltip: true,
        label: {
            backgroundColor: '#333'
        }
    },

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

        // --- ADX ---
        {
            name: 'ADX 14 (4H)',
            type: 'line',
            data: adx,
            xAxisIndex: 1,
            yAxisIndex: 1,
            symbol: 'none',
            lineStyle: { width: 1, color: '#FFD166' }
        },

        // --- RSI ---
        {
            name: 'RSI 14 (4H)',
            type: 'line',
            data: rsi,
            xAxisIndex: 2,
            yAxisIndex: 2,
            symbol: 'none',
            lineStyle: { width: 1, color: '#00E676' }
        },

        // --- RSI 70 ---
        {
            name: 'RSI 70',
            type: 'line',
            xAxisIndex: 2,
            yAxisIndex: 2,
            data: times.map(t => [t, 70]),
            symbol: 'none',
            tooltip: { show: false },
            lineStyle: { width: 1, color: '#FF4D4D', type: 'dashed' }
        },

        // --- RSI 30 ---
        {
            name: 'RSI 30',
            type: 'line',
            xAxisIndex: 2,
            yAxisIndex: 2,
            data: times.map(t => [t, 30]),
            symbol: 'none',
            tooltip: { show: false },
            lineStyle: { width: 1, color: '#4DA3FF', type: 'dashed' }
        }
    ],

    tooltip: {
        trigger: 'axis',
        axisPointer: {
            type: 'line' // вертикальная линия
        }
    },
};
