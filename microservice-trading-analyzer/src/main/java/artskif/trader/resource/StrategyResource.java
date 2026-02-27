package artskif.trader.resource;

import artskif.trader.strategy.StrategyService;
import artskif.trader.strategy.StrategyDataService;
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
    StrategyDataService strategyDataService;

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
     * Запустить бэктест для стратегии
     * @param strategyName имя стратегии для запуска бэктеста
     */
    @POST
    @Path("/backtest/{strategyName}")
    public Response runBacktest(@PathParam("strategyName") String strategyName) {
        try {
            Log.infof("📊 Запрос на запуск бэктеста для стратегии: %s", strategyName);

            boolean success = strategyService.runBacktest(strategyName);

            if (success) {
                return Response.ok()
                        .entity(Map.of(
                                "status", "success",
                                "message", "Бэктест успешно выполнен",
                                "strategyName", strategyName
                        ))
                        .build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of(
                                "status", "error",
                                "message", "Не удалось запустить бэктест (стратегия не найдена)",
                                "strategyName", strategyName
                        ))
                        .build();
            }
        } catch (Exception e) {
            Log.errorf(e, "❌ Ошибка при запуске бэктеста для стратегии: %s", strategyName);
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
     * Удалить контракт со всеми его метаданными и зависимыми фичами по ID
     * @param contractId ID контракта для удаления
     * @return ответ с результатом удаления
     */
    @DELETE
    @Path("/{contractId}")
    public Response deleteContractById(@PathParam("contractId") Long contractId) {
        try {
            Log.infof("🗑️ Получен запрос на удаление контракта с ID: %d", contractId);

            boolean deleted = strategyDataService.deleteContractById(contractId);

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