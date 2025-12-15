package artskif.trader.contract;

import artskif.trader.entity.ContractFeatureMetadata;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API для управления контрактами
 */
@Path("/api/contracts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ContractResource {

    @Inject
    ContractProcessor contractProcessor;

    @Inject
    ContractService contractService;

    @Inject
    ContractFeatureRegistry featureRegistry;

    /**
     * Обработать свечи и создать контракты
     */
    @POST
    @Path("/process")
    public Response processCandles(
            @QueryParam("symbol") @DefaultValue("BTC-USDT") String symbol,
            @QueryParam("tf") @DefaultValue("5m") String tf,
            @QueryParam("from") String fromStr,
            @QueryParam("to") String toStr) {

        try {
            Instant from = fromStr != null ? Instant.parse(fromStr) : Instant.now().minusSeconds(3600);
            Instant to = toStr != null ? Instant.parse(toStr) : Instant.now();

            Log.infof("Начинаем обработку свечей %s %s с %s по %s", symbol, tf, from, to);

            contractProcessor.processConfirmedCandles(symbol, tf, from, to);

            return Response.ok()
                    .entity(Map.of(
                            "status", "success",
                            "message", "Свечи обработаны"
                    ))
                    .build();

        } catch (Exception e) {
            Log.errorf(e, "Ошибка при обработке свечей");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "status", "error",
                            "message", e.getMessage()
                    ))
                    .build();
        }
    }

    /**
     * Получить метаданные всех зарегистрированных фич
     */
    @GET
    @Path("/features")
    public Response getFeatures() {
        try {
            List<ContractFeatureMetadata> metadata = contractService.getAllFeatureMetadata();

            return Response.ok(metadata).build();

        } catch (Exception e) {
            Log.errorf(e, "Ошибка при получении метаданных фич");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "status", "error",
                            "message", e.getMessage()
                    ))
                    .build();
        }
    }

    /**
     * Получить информацию о зарегистрированных создателях фич
     */
    @GET
    @Path("/features/creators")
    public Response getFeatureCreators() {
        try {
            List<Map<String, Object>> creators = featureRegistry.getAllCreators().stream()
                    .map(creator -> {
                        Map<String, Object> info = new HashMap<>();
                        info.put("featureName", creator.getFeatureName());
                        info.put("dataType", creator.getDataType());
                        ContractFeatureMetadata metadata = creator.getFeatureMetadata();
                        info.put("description", metadata.description);
                        info.put("sequenceOrder", metadata.sequenceOrder);
                        return info;
                    })
                    .toList();

            return Response.ok(creators).build();

        } catch (Exception e) {
            Log.errorf(e, "Ошибка при получении создателей фич");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "status", "error",
                            "message", e.getMessage()
                    ))
                    .build();
        }
    }

    /**
     * Создать колонку для новой фичи
     */
    @POST
    @Path("/features/{featureName}/column")
    public Response createFeatureColumn(@PathParam("featureName") String featureName) {
        try {
            contractService.ensureColumnExists(featureName);

            return Response.ok()
                    .entity(Map.of(
                            "status", "success",
                            "message", "Колонка создана или уже существует",
                            "featureName", featureName
                    ))
                    .build();

        } catch (Exception e) {
            Log.errorf(e, "Ошибка при создании колонки для фичи %s", featureName);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "status", "error",
                            "message", e.getMessage()
                    ))
                    .build();
        }
    }
}

