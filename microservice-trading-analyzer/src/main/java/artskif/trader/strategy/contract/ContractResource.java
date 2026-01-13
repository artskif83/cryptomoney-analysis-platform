package artskif.trader.strategy.contract;

import artskif.trader.candle.CandleTimeframe;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

/**
 * REST API для управления контрактами
 */
@Path("/api/contracts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ContractResource {


    @Inject
    ContractService contractService;

    @Inject
    ContractDataService contractDataService;

    /**
     * Сгенерировать исторические фичи для всех контрактов
     */
    @POST
    @Path("/generate-historical")
    public Response generateHistoricalFeatures() {
        try {
            Log.infof("🚀 Запуск генерации исторических фич");

            // Генерируем исторические данные
            contractService.generateHistoricalFeaturesForAll();

            return Response.ok()
                    .entity(Map.of(
                            "status", "success",
                            "message", "Исторические фичи сгенерированы"
                    ))
                    .build();
        } catch (Exception e) {
            Log.errorf(e, "Ошибка при генерации исторических фич");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "status", "error",
                            "message", e.getMessage()
                    ))
                    .build();
        }
    }

    /**
     * Сгенерировать исторические фичи для одного контракта по его ID
     * @param contractId ID контракта
     */
    @POST
    @Path("/{contractId}/generate-historical")
    public Response generateHistoricalFeaturesForContract(
            @PathParam("contractId") Long contractId) {
        try {
            Log.infof("🚀 Запуск генерации исторических фич для контракта ID=%d",
                      contractId);

            // Получаем имя контракта по ID
            String contractName = contractService.getContractNameById(contractId);
            if (contractName == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of(
                                "status", "error",
                                "message", "Контракт с указанным ID не найден",
                                "contractId", contractId
                        ))
                        .build();
            }

            // Генерируем исторические данные для контракта
            boolean success = contractService.generateHistoricalFeaturesForContract(contractName);

            if (!success) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of(
                                "status", "error",
                                "message", "Контракт не найден в реестре",
                                "contractId", contractId,
                                "contractName", contractName
                        ))
                        .build();
            }

            return Response.ok()
                    .entity(Map.of(
                            "status", "success",
                            "message", "Исторические фичи сгенерированы для контракта",
                            "contractId", contractId,
                            "contractName", contractName
                    ))
                    .build();
        } catch (Exception e) {
            Log.errorf(e, "Ошибка при генерации исторических фич для контракта ID=%d", contractId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "status", "error",
                            "message", e.getMessage(),
                            "contractId", contractId
                    ))
                    .build();
        }
    }

    /**
     * Вспомогательный метод для получения доступных значений таймфреймов
     */
    private String[] getCandleTimeframeValues() {
        CandleTimeframe[] values = CandleTimeframe.values();
        String[] result = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = values[i].name();
        }
        return result;
    }

    /**
     * Сгенерировать live фичи для всех контрактов
     */
    @POST
    @Path("/current-predict")
    public Response generatePredict() {
        try {
            contractService.generatePredict();

            return Response.ok()
                    .entity(Map.of(
                            "status", "success",
                            "message", "Live фичи сгенерированы"
                    ))
                    .build();
        } catch (Exception e) {
            Log.errorf(e, "Ошибка при генерации live фич");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "status", "error",
                            "message", e.getMessage()
                    ))
                    .build();
        }
    }

    /**
     * Удалить контракт со всеми его метаданными и зависимыми фичами по ID
     * @param contractId ID контракта для удаления
     * @return ответ с результатом удаления
     */
    @DELETE
    @Path("/{contractId}")
    public Response deleteContractById(@PathParam("contractId") Long contractId) {
        try {
            Log.infof("🗑️ Получен запрос на удаление контракта с ID: %d", contractId);

            boolean deleted = contractDataService.deleteContractById(contractId);

            if (deleted) {
                return Response.ok()
                        .entity(Map.of(
                                "status", "success",
                                "message", "Контракт успешно удален",
                                "contractId", contractId
                        ))
                        .build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of(
                                "status", "error",
                                "message", "Контракт с указанным ID не найден",
                                "contractId", contractId
                        ))
                        .build();
            }
        } catch (Exception e) {
            Log.errorf(e, "❌ Ошибка при удалении контракта с ID: %d", contractId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "status", "error",
                            "message", e.getMessage(),
                            "contractId", contractId
                    ))
                    .build();
        }
    }
}

