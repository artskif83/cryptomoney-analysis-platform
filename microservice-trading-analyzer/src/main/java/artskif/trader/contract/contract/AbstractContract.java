package artskif.trader.contract.contract;

import artskif.trader.contract.ContractDataService;
import artskif.trader.contract.ContractFeatureRegistry;
import artskif.trader.entity.Contract;
import artskif.trader.entity.ContractFeatureMetadata;
import io.quarkus.logging.Log;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;

/**
 * Абстрактный базовый класс для всех контрактов
 * Содержит общую логику для генерации хэша контракта
 */
public abstract class AbstractContract {

    protected final ContractDataService dataService;
    protected final ContractFeatureRegistry featureRegistry;

    public AbstractContract(ContractDataService dataService, ContractFeatureRegistry featureRegistry) {
        this.dataService = dataService;
        this.featureRegistry = featureRegistry;
    }

    public abstract String getName();
    public abstract void generateHistoricalFeatures();

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

            // Добавляем все фичи в порядке sequence_order
            List<ContractFeatureMetadata> sortedFeatures = contract.features.stream()
                    .sorted(Comparator.comparing(f -> f.sequenceOrder))
                    .toList();

            for (ContractFeatureMetadata feature : sortedFeatures) {
                sb.append(feature.featureName).append(":")
                  .append(feature.dataType).append(":")
                  .append(feature.sequenceOrder).append("|");
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

