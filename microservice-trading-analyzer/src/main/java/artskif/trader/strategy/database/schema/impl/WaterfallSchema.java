package artskif.trader.strategy.database.schema.impl;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.strategy.StrategyDataService;
import artskif.trader.strategy.database.ColumnsRegistry;
import artskif.trader.strategy.database.schema.AbstractSchema;
import artskif.trader.strategy.database.columns.impl.ADXColumn;
import artskif.trader.strategy.database.columns.impl.RSIColumn;
import artskif.trader.entity.Contract;
import artskif.trader.entity.ContractMetadata;
import io.quarkus.logging.Log;
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
     * Создать и инициализировать контракт с метаданными
     */
    @PostConstruct
    public void initContract() {

        // Сначала проверяем, существует ли контракт
        Contract existingContract = dataService.findContractByName(NAME);
        if (existingContract != null) {
            Log.infof("📋 Схема '%s' уже существует в БД (id: %d), используем существующую", NAME, existingContract.id);
            this.contract = existingContract;
            this.contractHash = existingContract.contractHash;
            return;
        }

        // Создаем контракт с метаданными
        Contract newContract = new Contract(NAME, "Визуализация стратегии водопад", "V1");

        // Добавляем все фичи к контракту одним вызовом
        List<ContractMetadata> allMetadata = new ArrayList<>();
        allMetadata.addAll(RSIColumn.getColumnMetadata(
                Map.of(1, RSIColumn.RSIColumnType.RSI_5M
                        , 2, RSIColumn.RSIColumnType.RSI_5M_ON_4H),
                newContract
        ));
        allMetadata.addAll(ADXColumn.getColumnMetadata(
                Map.of(3, ADXColumn.ADXColumnType.ADX_5M
                        , 4, ADXColumn.ADXColumnType.ADX_5M_ON_4H),
                newContract
        ));

        // Генерируем и сохраняем hash
        newContract.contractHash = generateContractHash(newContract);
        this.contract = dataService.saveNewContract(newContract);
        this.contractHash = this.contract.contractHash;
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

