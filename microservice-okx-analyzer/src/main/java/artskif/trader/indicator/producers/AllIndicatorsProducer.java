package artskif.trader.indicator.producers;

import artskif.trader.candle.Candle1m;
import artskif.trader.events.CandleEventBus;
import artskif.trader.indicator.IndicatorPoint;
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

    /** Отдаём один CDI-бин типа List<RsiIndicator1m>, который уже собран на старте */
    @Produces
    @ApplicationScoped
    public List<IndicatorPoint> allIndicators() {
        System.out.println("🔌 Создаем все возможные индикаторы");

        List<Integer> periods = List.of(6, 12, 18);
        List<IndicatorPoint> list = new ArrayList<>(periods.size());

        for (Integer p : periods) {
            RsiIndicator1m ind = new RsiIndicator1m(
                    p,
                    objectMapper,
                    candle1m,
                    bus
            );
            ind.init(); // подпишется на bus/прочитает состояние/подогреет буфер
            list.add(ind);
        }
        return list;
    }
}
