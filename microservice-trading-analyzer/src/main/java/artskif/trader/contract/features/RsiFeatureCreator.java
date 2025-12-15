package artskif.trader.contract.features;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.entity.ContractFeatureMetadata;
import artskif.trader.contract.FeatureCreator;
import artskif.trader.entity.Candle;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DecimalNum;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@ApplicationScoped
public class RsiFeatureCreator implements FeatureCreator {

    private static final String FEATURE_NAME = "rsi_14";
    private static final String DESCRIPTION = "RSI индикатор с периодом 14 на основе ta4j";
    private static final Integer SEQUENCE_ORDER = 1;
    private static final String DATA_TYPE = "numeric(5, 2)";
    private static final int RSI_PERIOD = 14;

    @Override
    public ContractFeatureMetadata getFeatureMetadata() {
        return new ContractFeatureMetadata(FEATURE_NAME, DESCRIPTION, SEQUENCE_ORDER, DATA_TYPE);
    }

    @Override
    public Object calculateFeature(Object context) {
        if (!(context instanceof RsiFeatureContext)) {
            Log.warnf("Неверный тип контекста для RSI: %s", context.getClass().getName());
            return null;
        }

        RsiFeatureContext rsiContext = (RsiFeatureContext) context;
        List<Candle> candles = rsiContext.getCandles();

        if (candles == null || candles.size() < RSI_PERIOD) {
            Log.debugf("Недостаточно свечей для расчета RSI: %d (требуется %d)",
                    candles != null ? candles.size() : 0, RSI_PERIOD);
            return null;
        }

        try {
            BarSeries series = new BaseBarSeriesBuilder()
                    .withName(candles.get(0).id.symbol + "_" + candles.get(0).id.tf)
                    .build();

            for (Candle candle : candles) {
                Bar bar = new BaseBar(
                        CandleTimeframe.fromString(candle.id.tf).getDuration(), // период свечи
                        candle.id.ts,  // beginTime (Instant)
                        candle.id.ts.plus(CandleTimeframe.fromString(candle.id.tf).getDuration()),           // endTime (Instant)
                        DecimalNum.valueOf(candle.open),   // openPrice
                        DecimalNum.valueOf(candle.high),   // highPrice
                        DecimalNum.valueOf(candle.low),    // lowPrice
                        DecimalNum.valueOf(candle.close),  // closePrice
                        DecimalNum.valueOf(candle.volume != null ? candle.volume : BigDecimal.ZERO), // volume
                        DecimalNum.valueOf(0),              // amount
                        0L                                  // trades
                );

                series.addBar(bar);
            }

            ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
            RSIIndicator rsiIndicator = new RSIIndicator(closePrice, RSI_PERIOD);

            int lastIndex = series.getEndIndex();
            double rsiValue = rsiIndicator.getValue(lastIndex).doubleValue();

            Log.debugf("Рассчитан RSI для %s: %.2f", candles.get(candles.size() - 1).id, rsiValue);

            return BigDecimal.valueOf(rsiValue).setScale(2, RoundingMode.HALF_UP);

        } catch (Exception e) {
            Log.errorf(e, "Ошибка при расчете RSI");
            return null;
        }
    }

    @Override
    public String getFeatureName() {
        return FEATURE_NAME;
    }

    @Override
    public String getDataType() {
        return DATA_TYPE;
    }
}

