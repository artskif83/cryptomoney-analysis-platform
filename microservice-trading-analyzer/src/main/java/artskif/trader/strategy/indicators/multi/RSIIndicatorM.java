package artskif.trader.strategy.indicators.multi;

import artskif.trader.candle.Candle;
import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.indicators.MultiAbstractIndicator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.ta4j.core.indicators.RSIIndicator;

@ApplicationScoped
public class RSIIndicatorM extends MultiAbstractIndicator<RSIIndicator> {

    public static final int RSI_PERIOD = 14;

    private final CloseIndicatorM closeIndicator;

    // No-args constructor required by CDI
    protected RSIIndicatorM() {
        super(null);
        this.closeIndicator = null;
    }

    @Inject
    public RSIIndicatorM(Candle candle, CloseIndicatorM closeIndicator) {
        super(candle);
        this.closeIndicator = closeIndicator;
    }

    @Override
    protected RSIIndicator createIndicator(CandleTimeframe timeframe, boolean isLifeSeries) {
        return new RSIIndicator(closeIndicator.getIndicator(timeframe, isLifeSeries), RSI_PERIOD);
    }


}
