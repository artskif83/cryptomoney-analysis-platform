package artskif.trader.resource;

import artskif.trader.strategy.StrategyService;
import artskif.trader.strategy.contract.ContractDataService;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

/**
 * REST API для управления контрактами
 */
@Path("/api/strategy")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StrategyResource {


    @Inject
    StrategyService strategyService;

    @Inject
    ContractDataService contractDataService;


    /**
     * Запустить стратегию по имени
     * @param strategyName имя стратегии для запуска
     */
    @POST
    @Path("/start/{strategyName}")
    public Response startStrategy(@PathParam("strategyName") String strategyName) {
        try {
            Log.infof("🚀 Запрос на запуск стратегии: %s", strategyName);

            boolean success = strategyService.startStrategy(strategyName);

            if (success) {
                return Response.ok()
                        .entity(Map.of(
                                "status", "success",
                                "message", "Стратегия успешно запущена",
                                "strategyName", strategyName,
                                "running", true
                        ))
                        .build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of(
                                "status", "error",
                                "message", "Не удалось запустить стратегию (не найдена или уже запущена)",
                                "strategyName", strategyName
                        ))
                        .build();
            }
        } catch (Exception e) {
            Log.errorf(e, "❌ Ошибка при запуске стратегии: %s", strategyName);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "status", "error",
                            "message", e.getMessage(),
                            "strategyName", strategyName
                    ))
                    .build();
        }
    }

    /**
     * Остановить стратегию по имени
     * @param strategyName имя стратегии для остановки
     */
    @POST
    @Path("/stop/{strategyName}")
    public Response stopStrategy(@PathParam("strategyName") String strategyName) {
        try {
            Log.infof("🛑 Запрос на остановку стратегии: %s", strategyName);

            boolean success = strategyService.stopStrategy(strategyName);

            if (success) {
                return Response.ok()
                        .entity(Map.of(
                                "status", "success",
                                "message", "Стратегия успешно остановлена",
                                "strategyName", strategyName,
                                "running", false
                        ))
                        .build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of(
                                "status", "error",
                                "message", "Не удалось остановить стратегию (не найдена или не запущена)",
                                "strategyName", strategyName
                        ))
                        .build();
            }
        } catch (Exception e) {
            Log.errorf(e, "❌ Ошибка при остановке стратегии: %s", strategyName);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "status", "error",
                            "message", e.getMessage(),
                            "strategyName", strategyName
                    ))
                    .build();
        }
    }

    /**
     * Получить список всех зарегистрированных стратегий и их статусы
     */
    @GET
    @Path("/list")
    public Response getAllStrategies() {
        try {
            Map<String, Boolean> strategies = strategyService.getAllStrategies();

            return Response.ok()
                    .entity(Map.of(
                            "status", "success",
                            "strategies", strategies
                    ))
                    .build();
        } catch (Exception e) {
            Log.errorf(e, "❌ Ошибка при получении списка стратегий");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "status", "error",
                            "message", e.getMessage()
                    ))
                    .build();
        }
    }

    /**
     * Получить статус конкретной стратегии
     * @param strategyName имя стратегии
     */
    @GET
    @Path("/status/{strategyName}")
    public Response getStrategyStatus(@PathParam("strategyName") String strategyName) {
        try {
            boolean isRunning = strategyService.isStrategyRunning(strategyName);

            return Response.ok()
                    .entity(Map.of(
                            "status", "success",
                            "strategyName", strategyName,
                            "running", isRunning
                    ))
                    .build();
        } catch (Exception e) {
            Log.errorf(e, "❌ Ошибка при получении статуса стратегии: %s", strategyName);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "status", "error",
                            "message", e.getMessage(),
                            "strategyName", strategyName
                    ))
                    .build();
        }
    }

    /**
     * Сгенерировать исторические фичи для всех контрактов
     */
    @POST
    @Path("/generate-historical")
    public Response generateHistoricalFeatures() {
        try {
            Log.infof("🚀 Запуск генерации исторических фич");

            // Генерируем исторические данные
            strategyService.generateHistoricalFeaturesForAll();

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
            String contractName = strategyService.getContractNameById(contractId);
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
            boolean success = strategyService.generateHistoricalFeaturesForContract(contractName);

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
     * Сгенерировать live фичи для всех контрактов
     */
    @POST
    @Path("/current-predict")
    public Response generatePredict() {
        try {
            strategyService.generatePredict();

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


