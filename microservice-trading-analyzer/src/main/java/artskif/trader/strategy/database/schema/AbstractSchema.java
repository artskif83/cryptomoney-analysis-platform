package artskif.trader.strategy.database.schema;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.entity.Contract;
import artskif.trader.entity.ContractMetadata;
import artskif.trader.strategy.StrategyDataService;
import artskif.trader.strategy.database.ColumnsRegistry;
import io.quarkus.logging.Log;
import lombok.Getter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;

/**
 * Абстрактный базовый класс для всех контрактов
 * Содержит общую логику для генерации хэша контракта
 */
public abstract class AbstractSchema {

    protected final StrategyDataService dataService;

    @Getter
    protected Contract contract;
    @Getter
    protected String contractHash;

    public AbstractSchema(StrategyDataService dataService, ColumnsRegistry registry) {
        this.dataService = dataService;
    }

    public abstract String getName();


    public abstract CandleTimeframe getTimeframe();

    /**
     * Сгенерировать хешкод контракта на основе всех его метаданных
     * Этот хеш будет подписывать каждую строку фич
     */
    protected String generateContractHash(Contract contract) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Формируем строку из метаданных контракта
            StringBuilder sb = new StringBuilder();
            sb.append(contract.name).append("|");
            sb.append(contract.featureSetId).append("|");

            // Добавляем все метаданные в порядке sequence_order
            List<ContractMetadata> sortedMetadata = contract.metadata.stream()
                    .sorted(Comparator.comparing(f -> f.sequenceOrder))
                    .toList();

            for (ContractMetadata metadata : sortedMetadata) {
                sb.append(metadata.name).append(":")
                  .append(metadata.dataType).append(":")
                  .append(metadata.metadataType).append(":")
                  .append(metadata.sequenceOrder).append("|");
            }

            byte[] hashBytes = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));

            // Конвертируем в hex строку
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.error("Ошибка при генерации хеша контракта", e);
            throw new RuntimeException("Не удалось создать хеш контракта", e);
        }
    }
}

