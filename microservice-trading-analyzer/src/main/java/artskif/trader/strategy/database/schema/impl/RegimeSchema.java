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

@ApplicationScoped
public class RegimeSchema extends AbstractSchema {

    private static final String NAME = "Test Contract-4h V1.0 ";


    // Конструктор без параметров для CDI proxy
    public RegimeSchema() {
        super(null, null);
    }

    @Inject
    public RegimeSchema(StrategyDataService dataService, ColumnsRegistry registry) {
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
            Log.infof("📋 Контракт '%s' уже существует в БД (id: %d), используем существующий", NAME, existingContract.id);
            this.contract = existingContract;
            this.contractHash = existingContract.contractHash;
            return;
        }

        // Создаем новый контракт с метаданными
        Contract newContract = new Contract(NAME, "First testing contract 4h timeframe", "V1");

        // Добавляем все фичи к контракту одним вызовом
        List<ContractMetadata> allMetadata = new ArrayList<>();
        allMetadata.addAll(RSIColumn.getColumnMetadata(
                Map.of(1, RSIColumn.RSIColumnType.RSI_4H),
                newContract
        ));
        allMetadata.addAll(ADXColumn.getColumnMetadata(
                Map.of(2, ADXColumn.ADXColumnType.ADX_4H),
                newContract
        ));
        newContract.addMetadata(allMetadata);

        // Добавляем лейблы к контракту
//        newContract.addMetadata(FutureReturnLabel.getLabelMetadata(100, newContract));

        // Генерируем hash
        newContract.contractHash = generateContractHash(newContract);

        // Сохраняем новый контракт через транзакционный метод сервиса
        this.contract = dataService.saveNewContract(newContract);
        this.contractHash = this.contract.contractHash;
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