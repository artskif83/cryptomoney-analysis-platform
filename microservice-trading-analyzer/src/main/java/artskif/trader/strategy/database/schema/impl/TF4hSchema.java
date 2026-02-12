package artskif.trader.strategy.database.schema.impl;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.StrategyDataService;
import artskif.trader.strategy.database.ColumnsRegistry;
import artskif.trader.strategy.database.columns.impl.*;
import artskif.trader.strategy.database.schema.AbstractSchema;
import artskif.trader.strategy.database.columns.AbstractColumn;
import artskif.trader.entity.Contract;
import artskif.trader.entity.ContractMetadata;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class TF4hSchema extends AbstractSchema {

    private static final String NAME = "TF4hVisualization";


    // Конструктор без параметров для CDI proxy
    public TF4hSchema() {
        super(null, null);
    }

    @Inject
    public TF4hSchema(StrategyDataService dataService, ColumnsRegistry registry) {
        super(dataService, registry);
    }

    /**
     * Создание метаданных для схемы Regime
     */
    @Override
    protected List<ContractMetadata> createMetadata(Contract contract) {
        return new ArrayList<>(AbstractColumn.getColumnMetadata(
                List.of(CandleResistanceStrengthColumn.CandleResistanceStrengthColumnType.RESISTANCE_4H,
                        ResistanceLevelColumn.ResistanceLevelColumnType.RESISTANCE_LEVEL_4H,
                        ResistanceLevelColumn.ResistanceLevelColumnType.RESISTANCE_POWER_ABOVE_4H,
                        CandleResistanceStrengthColumn.CandleResistanceStrengthColumnType.INDEX_4H),
                contract
        ));
    }

    @Override
    protected String getContractDescription() {
        return "First testing contract 4h timeframe";
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
        return CandleTimeframe.CANDLE_4H;
    }

}