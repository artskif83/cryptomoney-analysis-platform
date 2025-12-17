package artskif.trader.contract;

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

    /**
     * Сгенерировать исторические фичи для всех контрактов
     */
    @POST
    @Path("/generate-historical")
    public Response generateHistoricalFeatures() {
        try {
            Log.info("🚀 Запуск генерации исторических фич");

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
}

