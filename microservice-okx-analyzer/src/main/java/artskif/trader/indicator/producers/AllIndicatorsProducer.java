package artskif.trader.indicator.producers;

import artskif.trader.candle.Candle1m;
import artskif.trader.events.CandleEventBus;
import artskif.trader.indicator.IndicatorPoint;
import artskif.trader.indicator.adx.AdxIndicator1m;
import artskif.trader.indicator.rsi.RsiIndicator1m;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

@Startup
@ApplicationScoped
public class AllIndicatorsProducer {

    @Inject
    ObjectMapper objectMapper;
    @Inject
    Candle1m candle1m;
    @Inject
    CandleEventBus bus;

    /**
     * Отдаём один CDI-бин типа List<IndicatorPoint>, который уже собран на старте
     */
    @Produces
    @ApplicationScoped
    public List<IndicatorPoint> allIndicators() {
        System.out.println("🔌 Создаем все возможные индикаторы");

        List<IndicatorPoint> list = new ArrayList<>(2);

        RsiIndicator1m ind = new RsiIndicator1m(
                14,
                objectMapper,
                candle1m,
                bus
        );
        ind.init();

        AdxIndicator1m indAdx = new AdxIndicator1m(
                14,
                objectMapper,
                candle1m,
                bus
        );
        indAdx.init();

        // подпишется на bus/прочитает состояние/подогреет буфер
        list.add(ind);
        list.add(indAdx);

        return list;
    }
}
