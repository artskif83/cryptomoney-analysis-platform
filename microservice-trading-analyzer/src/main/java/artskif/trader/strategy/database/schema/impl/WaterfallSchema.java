package artskif.trader.strategy.database.schema.impl;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.StrategyDataService;
import artskif.trader.strategy.database.ColumnsRegistry;
import artskif.trader.strategy.database.columns.impl.PositionColumn;
import artskif.trader.strategy.database.schema.AbstractSchema;
import artskif.trader.strategy.database.columns.impl.ADXColumn;
import artskif.trader.strategy.database.columns.impl.RSIColumn;
import artskif.trader.entity.Contract;
import artskif.trader.entity.ContractMetadata;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Экземпляр контракта - описывает один отдельный контракт (одна запись в таблице contracts)
 * Отвечает за:
 * - Генерацию исторического набора фич и сохранение в таблицу wide_candles
 * - Генерацию фич для текущей свечи из liveBuffer
 * - Подписывание каждой строки фич специальным хешкодом контракта
 */
@ApplicationScoped
public class WaterfallSchema extends AbstractSchema {

    private static final String NAME = "WaterfallVisualization";


    // Конструктор без параметров для CDI proxy
    public WaterfallSchema() {
        super(null, null);
    }

    @Inject
    public WaterfallSchema(StrategyDataService dataService, ColumnsRegistry registry) {
        super(dataService, registry);
    }

    /**
     * Создание метаданных для схемы Waterfall
     */
    @Override
    protected List<ContractMetadata> createMetadata(Contract contract) {
        List<ContractMetadata> allMetadata = new ArrayList<>();
        allMetadata.addAll(RSIColumn.getColumnMetadata(
                Map.of(1, RSIColumn.RSIColumnType.RSI_5M
                ),
                contract
        ));
        allMetadata.addAll(PositionColumn.getColumnMetadata(
                Map.of(
                        2, PositionColumn.PositionColumnType.POSITION_PRICE_5M,
                        3, PositionColumn.PositionColumnType.STOPLOSS_5M,
                        4, PositionColumn.PositionColumnType.TAKEPROFIT_5M
                ),
                contract
        ));
        return allMetadata;
    }

    @Override
    protected String getContractDescription() {
        return "Визуализация стратегии водопад";
    }

    /**
     * Инициализация контракта при запуске
     */
    @PostConstruct
    public void init() {
        initSchema();
    }

    @Override
    public String getName() {
        return NAME;
    }


    @Override
    public CandleTimeframe getTimeframe() {
        return CandleTimeframe.CANDLE_5M;
    }

}

