package artskif.trader.indicator.rsi;

import artskif.trader.common.BufferRepository;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.LinkedHashMap;

@ApplicationScoped
public class RsiRepository extends BufferRepository<RsiPoint> {
    @Inject
    ObjectMapper objectMapper;

    @Override
    protected ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    protected JavaType getMapType() {
        return getObjectMapper().getTypeFactory()
                .constructMapType(LinkedHashMap.class, Instant.class, RsiPoint.class);
    }
}
