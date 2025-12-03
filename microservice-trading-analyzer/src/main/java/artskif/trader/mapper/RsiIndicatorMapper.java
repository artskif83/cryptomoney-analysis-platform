package artskif.trader.mapper;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.entity.RsiIndicator;
import artskif.trader.entity.RsiIndicatorId;
import artskif.trader.dto.RsiPointDto;
import org.jboss.logging.Logger;

public class RsiIndicatorMapper {

    private static final Logger LOG = Logger.getLogger(RsiIndicatorMapper.class);

    /**
     * Маппинг RsiPoint -> сущность RsiIndicator
     */
    public static RsiIndicator mapDtoToEntity(RsiPointDto point, String symbol) {
        if (point == null || point.getBucket() == null) {
            return null;
        }

        RsiIndicatorId id = new RsiIndicatorId(
                symbol,
                point.getTimeframe().name(),
                point.getBucket()
        );

        return new RsiIndicator(id, point.getRsi());
    }

    /**
     * Конвертирует сущность RsiIndicator в RsiPoint
     */
    public static RsiPointDto mapEntityToDto(RsiIndicator indicator) {
        if (indicator == null || indicator.id == null) {
            return null;
        }

        try {
            CandleTimeframe timeframe = CandleTimeframe.valueOf(indicator.id.tf);
            return new RsiPointDto(
                    indicator.id.ts,
                    indicator.rsi14,
                    timeframe,
                    indicator.id.symbol
            );
        } catch (IllegalArgumentException e) {
            LOG.warnf("Неизвестный таймфрейм: %s", indicator.id.tf);
            return null;
        } catch (Exception ex) {
            LOG.warnf(ex, "Не удалось сконвертировать RsiIndicator в RsiPoint: %s", indicator);
            return null;
        }
    }
}

