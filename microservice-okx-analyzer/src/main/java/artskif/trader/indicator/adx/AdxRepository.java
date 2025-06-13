package artskif.trader.indicator.adx;

import artskif.trader.common.BufferRepository;
import artskif.trader.dto.CandlestickDto;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.LinkedHashMap;

@ApplicationScoped
public class AdxRepository extends BufferRepository<AdxPoint> {

    @Inject
    ObjectMapper objectMapper;

    @Override
    protected ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    protected JavaType getMapType() {
        return getObjectMapper().getTypeFactory()
                .constructMapType(LinkedHashMap.class, Instant.class, AdxPoint.class);
    }
}
