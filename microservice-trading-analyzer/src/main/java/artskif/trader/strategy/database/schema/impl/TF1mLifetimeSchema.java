package artskif.trader.strategy.database.schema.impl;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.entity.Contract;
import artskif.trader.entity.ContractMetadata;
import artskif.trader.strategy.StrategyDataService;
import artskif.trader.strategy.database.ColumnsRegistry;
import artskif.trader.strategy.database.columns.AbstractColumn;
import artskif.trader.strategy.database.columns.impl.CandleResistanceStrengthColumn;
import artskif.trader.strategy.database.columns.impl.PositionColumn;
import artskif.trader.strategy.database.columns.impl.ResistanceLevelColumn;
import artskif.trader.strategy.database.columns.impl.TripleMAColumn;
import artskif.trader.strategy.database.schema.AbstractSchema;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

/**
 * Схема для визуализации базовой стратегии на таймфрейме 1m
 */
@ApplicationScoped
public class TF1mLifetimeSchema extends AbstractSchema {

    private static final String NAME = "TF1mLifetimeVisualization";


    // Конструктор без параметров для CDI proxy
    public TF1mLifetimeSchema() {
        super(null, null);
    }

    @Inject
    public TF1mLifetimeSchema(StrategyDataService dataService, ColumnsRegistry registry) {
        super(dataService, registry);
    }

    /**
     * Создание метаданных для схемы Waterfall
     */
    @Override
    protected List<ContractMetadata> createMetadata(Contract contract) {
        return new ArrayList<>(AbstractColumn.getColumnMetadata(
                List.of(TripleMAColumn.TripleMAColumnType.TRIPLE_MA_VALUE_1M,
                        ResistanceLevelColumn.ResistanceLevelColumnType.RESISTANCE_LEVEL_1M,
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

