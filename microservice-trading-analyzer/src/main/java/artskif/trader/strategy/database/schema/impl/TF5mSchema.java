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
 * Экземпляр контракта - описывает один отдельный контракт (одна запись в таблице contracts)
 * Отвечает за:
 * - Генерацию исторического набора фич и сохранение в таблицу wide_candles
 * - Генерацию фич для текущей свечи из liveBuffer
 * - Подписывание каждой строки фич специальным хешкодом контракта
 */
@ApplicationScoped
public class TF5mSchema extends AbstractSchema {

    private static final String NAME = "TF5mVisualization";


    // Конструктор без параметров для CDI proxy
    public TF5mSchema() {
        super(null, null);
    }

    @Inject
    public TF5mSchema(StrategyDataService dataService, ColumnsRegistry registry) {
        super(dataService, registry);
    }

    /**
     * Создание метаданных для схемы Waterfall
     */
    @Override
    protected List<ContractMetadata> createMetadata(Contract contract) {
        return new ArrayList<>(AbstractColumn.getColumnMetadata(
                List.of(CandleResistanceStrengthColumn.CandleResistanceStrengthColumnType.RESISTANCE_5M,
                        ResistanceLevelColumn.ResistanceLevelColumnType.RESISTANCE_LEVEL_5M,
                        ResistanceLevelColumn.ResistanceLevelColumnType.RESISTANCE_POWER_ABOVE_5M,
                        CandleResistanceStrengthColumn.CandleResistanceStrengthColumnType.INDEX_5M,
                        PositionColumn.PositionColumnType.POSITION_PRICE_5M,
                        PositionColumn.PositionColumnType.STOPLOSS_5M,
                        PositionColumn.PositionColumnType.TAKEPROFIT_5M),
                contract
        ));
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

