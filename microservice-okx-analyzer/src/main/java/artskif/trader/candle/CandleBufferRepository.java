package artskif.trader.candle;

import artskif.trader.common.BufferRepository;
import artskif.trader.dto.CandlestickDto;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@ApplicationScoped
public class CandleBufferRepository extends BufferRepository<CandlestickDto> {

    @Inject
    ObjectMapper objectMapper;

    @Override
    protected ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    protected JavaType getMapType() {
        return getObjectMapper().getTypeFactory()
                .constructMapType(LinkedHashMap.class, Instant.class, CandlestickDto.class);
    }
}
