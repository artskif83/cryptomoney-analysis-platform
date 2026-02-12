// ===== Получение данных =====
const frame = context.panel.data.series[0];
if (!frame) return {};

function col(name) {
    const f = frame.fields.find(x => x.name === name);
    return f ? Array.from(f.values) : [];
}

const times = col("time");
const candleIndexRaw = col("index_candle_4h");
const opens = col("open");
const highs = col("high");
const lows = col("low");
const closes = col("close");
let basePrice = closes[closes.length - 1]; // например, последний close

const resistanceLevelRaw = col("metric_resistance_level_4h");

const tripleMaFastRaw = col("metric_triple_ma_fast_sma_4h");
const tripleMaMediumRaw = col("metric_triple_ma_medium_sma_4h");
const tripleMaSlowRaw = col("metric_triple_ma_slow_sma_4h");

const tripleMaFastAngleRaw = col("metric_triple_ma_fast_angle_4h");
const tripleMaMediumAngleRaw = col("metric_triple_ma_medium_angle_4h");
const tripleMaSlowAngleRaw = col("metric_triple_ma_slow_angle_4h");

const tripleMaValueRaw = col("metric_triple_ma_value_4h");

if (!times.length) return {};

// ===== Свечи =====
const candles = times.map((t, i) => [
    t,
    opens[i],
    closes[i],
    lows[i],
    highs[i]
]);

const tripleMaFast = times.map((t, i) => [t, tripleMaFastRaw[i] == null ? null : tripleMaFastRaw[i]]);
const tripleMaMedium = times.map((t, i) => [t, tripleMaMediumRaw[i] == null ? null : tripleMaMediumRaw[i]]);
const tripleMaSlow = times.map((t, i) => [t, tripleMaSlowRaw[i] == null ? null : tripleMaSlowRaw[i]]);

const resistanceLevel = times.map((t, i) => [
    t,
    resistanceLevelRaw[i] == null ? null : resistanceLevelRaw[i]
]);

const tripleMaFastAngle = times.map((t, i) => [t, tripleMaFastAngleRaw[i] == null ? null : tripleMaFastAngleRaw[i]]);
const tripleMaMediumAngle = times.map((t, i) => [t, tripleMaMediumAngleRaw[i] == null ? null : tripleMaMediumAngleRaw[i]]);
const tripleMaSlowAngle = times.map((t, i) => [t, tripleMaSlowAngleRaw[i] == null ? null : tripleMaSlowAngleRaw[i]]);

const tripleMaValue = times.map((t, i) => [t, tripleMaValueRaw[i] == null ? null : tripleMaValueRaw[i]]);



// ===== Цвета =====
const upColor = '#4CAF50';
const upBorderColor = '#4CAF50';
const downColor = '#FF4D4D';
const downBorderColor = '#FF4D4D';


// ===== Конфигурация =====
return {
    animation: false,

    grid: [
        { left: '5%', right: '5%', top: 10, height: '58%' },      // свечи и MA (grid 0)
        { left: '5%', right: '5%', top: '70%', height: '6%' },    // Resistance level (grid 1)
        { left: '5%', right: '5%', top: '78%', height: '5%' },    // Triple MA value (grid 2)
        { left: '5%', right: '5%', top: '84%', height: '4%' },    // Fast MA angle (grid 3)
        { left: '5%', right: '5%', top: '89%', height: '4%' },    // Medium MA angle (grid 4)
        { left: '5%', right: '5%', top: '94%', height: '4%' }     // Slow MA angle (grid 5)
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
        }
    ],

    axisPointer: {
        link: [{ xAxisIndex: [0, 1, 2, 3, 4, 5] }]
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
            xAxisIndex: [0, 1, 2, 3, 4, 5],
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

        // --- Скользящие средние на основном графике ---
        {
            name: 'Triple MA Fast (4h)',
            type: 'line',
            data: tripleMaFast,
            xAxisIndex: 0,
            yAxisIndex: 0,
            symbol: 'none',
            connectNulls: false,
            lineStyle: { width: 1, color: '#42A5F5' }
        },
        {
            name: 'Triple MA Medium (4h)',
            type: 'line',
            data: tripleMaMedium,
            xAxisIndex: 0,
            yAxisIndex: 0,
            symbol: 'none',
            connectNulls: false,
            lineStyle: { width: 1, color: '#66BB6A' }
        },
        {
            name: 'Triple MA Slow (4h)',
            type: 'line',
            data: tripleMaSlow,
            xAxisIndex: 0,
            yAxisIndex: 0,
            symbol: 'none',
            connectNulls: false,
            lineStyle: { width: 1, color: '#FFA726' }
        },

        // --- Resistance level ---
        {
            name: 'Resistance level (4h)',
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
            name: 'Triple MA value (4h)',
            type: 'line',
            data: tripleMaValue,
            xAxisIndex: 2,
            yAxisIndex: 2,
            symbol: 'none',
            connectNulls: false,
            lineStyle: { width: 1, color: '#EC407A' }
        },

        // --- Fast MA angle ---
        {
            name: 'Triple MA Fast angle (4h)',
            type: 'line',
            data: tripleMaFastAngle,
            xAxisIndex: 3,
            yAxisIndex: 3,
            symbol: 'none',
            connectNulls: false,
            lineStyle: { width: 1, color: '#42A5F5' }
        },

        // --- Medium MA angle ---
        {
            name: 'Triple MA Medium angle (4h)',
            type: 'line',
            data: tripleMaMediumAngle,
            xAxisIndex: 4,
            yAxisIndex: 4,
            symbol: 'none',
            connectNulls: false,
            lineStyle: { width: 1, color: '#66BB6A' }
        },

        // --- Slow MA angle ---
        {
            name: 'Triple MA Slow angle (4h)',
            type: 'line',
            data: tripleMaSlowAngle,
            xAxisIndex: 5,
            yAxisIndex: 5,
            symbol: 'none',
            connectNulls: false,
            lineStyle: { width: 1, color: '#FFA726' }
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

            const maFastPoint = list.find(p => p.seriesName === 'Triple MA Fast (4h)');
            const maFastVal = maFastPoint && Array.isArray(maFastPoint.data) ? maFastPoint.data[1] : null;

            const maMediumPoint = list.find(p => p.seriesName === 'Triple MA Medium (4h)');
            const maMediumVal = maMediumPoint && Array.isArray(maMediumPoint.data) ? maMediumPoint.data[1] : null;

            const maSlowPoint = list.find(p => p.seriesName === 'Triple MA Slow (4h)');
            const maSlowVal = maSlowPoint && Array.isArray(maSlowPoint.data) ? maSlowPoint.data[1] : null;

            const levelPoint = list.find(p => p.seriesName === 'Resistance level (4h)');
            const lVal = levelPoint && Array.isArray(levelPoint.data) ? levelPoint.data[1] : null;

            const maValuePoint = list.find(p => p.seriesName === 'Triple MA value (4h)');
            const maValueVal = maValuePoint && Array.isArray(maValuePoint.data) ? maValuePoint.data[1] : null;

            const maFastAnglePoint = list.find(p => p.seriesName === 'Triple MA Fast angle (4h)');
            const maFastAngleVal = maFastAnglePoint && Array.isArray(maFastAnglePoint.data) ? maFastAnglePoint.data[1] : null;

            const maMediumAnglePoint = list.find(p => p.seriesName === 'Triple MA Medium angle (4h)');
            const maMediumAngleVal = maMediumAnglePoint && Array.isArray(maMediumAnglePoint.data) ? maMediumAnglePoint.data[1] : null;

            const maSlowAnglePoint = list.find(p => p.seriesName === 'Triple MA Slow angle (4h)');
            const maSlowAngleVal = maSlowAnglePoint && Array.isArray(maSlowAnglePoint.data) ? maSlowAnglePoint.data[1] : null;

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
            if (maFastVal != null) lines.push(`MA Fast: ${Number(maFastVal).toFixed(2)}`);
            if (maMediumVal != null) lines.push(`MA Medium: ${Number(maMediumVal).toFixed(2)}`);
            if (maSlowVal != null) lines.push(`MA Slow: ${Number(maSlowVal).toFixed(2)}`);
            if (lVal != null) lines.push(`Resistance level: ${Math.round(lVal)}`);
            if (maValueVal != null) lines.push(`Triple MA value: ${Math.round(maValueVal)}`);
            if (maFastAngleVal != null) lines.push(`MA Fast angle: ${Math.round(maFastAngleVal)}`);
            if (maMediumAngleVal != null) lines.push(`MA Medium angle: ${Math.round(maMediumAngleVal)}`);
            if (maSlowAngleVal != null) lines.push(`MA Slow angle: ${Math.round(maSlowAngleVal)}`);

            return lines.join('<br/>');
        },

        axisPointer: {
            link: [{ xAxisIndex: [0, 1, 2, 3, 4, 5] }],
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
