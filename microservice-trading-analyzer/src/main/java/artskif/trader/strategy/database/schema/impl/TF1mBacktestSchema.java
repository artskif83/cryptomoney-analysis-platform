package artskif.trader.strategy.database.schema.impl;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.StrategyDataService;
import artskif.trader.strategy.database.ColumnsRegistry;
import artskif.trader.strategy.database.columns.AbstractColumn;
import artskif.trader.strategy.database.columns.impl.*;
import artskif.trader.strategy.database.schema.AbstractSchema;
import artskif.trader.entity.Contract;
import artskif.trader.entity.ContractMetadata;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;


/**
 * Схема для визуализации базовой стратегии на таймфрейме 1m
 */
@ApplicationScoped
public class TF1mBacktestSchema extends AbstractSchema {

    private static final String NAME = "TF1mBacktestVisualization";


    // Конструктор без параметров для CDI proxy
    public TF1mBacktestSchema() {
        super(null, null);
    }

    @Inject
    public TF1mBacktestSchema(StrategyDataService dataService, ColumnsRegistry registry) {
        super(dataService, registry);
    }

    /**
     * Создание метаданных для схемы Waterfall
     */
    @Override
    protected List<ContractMetadata> createMetadata(Contract contract) {
        return new ArrayList<>(AbstractColumn.getColumnMetadata(
                List.of(DoubleMAIndicatorColumn.DoubleMAColumnType.DOUBLE_MA_VALUE_1M_ON_1H,
                        DoubleMAIndicatorColumn.DoubleMAColumnType.DOUBLE_MA_VALUE_1M_ON_5M,
                        ShortLevelColumn.ShortLevelColumnType.SHORT_LEVEL_1M,
                        ShortLevelColumn.ShortLevelColumnType.SHORT_STOP_LOS_1M,
                        LongLevelColumn.LongLevelColumnType.LONG_LEVEL_1M,
                        LongLevelColumn.LongLevelColumnType.LONG_STOP_LOS_1M,
                        CandleResistanceStrengthColumn.CandleResistanceStrengthColumnType.INDEX_1M,
                        PositionColumn.PositionColumnType.POSITION_PRICE_1M,
                        PositionColumn.PositionColumnType.STOPLOSS_1M,
                        PositionColumn.PositionColumnType.TAKEPROFIT_1M),
                contract
        ));
    }

    @Override
    protected String getContractDescription() {
        return "Визуализация базовой стратегии";
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
        return CandleTimeframe.CANDLE_1M;
    }

}

