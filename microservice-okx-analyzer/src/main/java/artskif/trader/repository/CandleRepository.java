package artskif.trader.repository;

import artskif.trader.dto.CandlestickDto;
import artskif.trader.entity.Candle;
import artskif.trader.entity.CandleId;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import artskif.trader.mapper.CandlestickMapper;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import org.hibernate.Session;
import org.jboss.logging.Logger;

import javax.sql.DataSource;
import java.io.StringReader;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;

@ApplicationScoped
public class CandleRepository implements PanacheRepositoryBase<Candle, CandleId>, BufferRepository<CandlestickDto> {

    private static final Logger LOG = Logger.getLogger(CandleRepository.class);

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Inject
    DataSource dataSource;

    public Candle saveFromDto(CandlestickDto dto) {
        if (dto == null) return null;

        // делегируем мапинг
        Candle candle = CandlestickMapper.mapDtoToEntity(dto);

        // Сохраняем сущность через Panache
        persist(candle);
        return candle;
    }

    @Transactional
    @Override
    public boolean saveFromMap(Map<Instant, CandlestickDto> buffer) {
        if (buffer == null || buffer.isEmpty()) return true;

        ManagedContext requestContext = Arc.container().requestContext();
        if (!requestContext.isActive()) {
            requestContext.activate();
            try {
                return performSave(buffer);
            } finally {
                requestContext.terminate();
            }
        } else {
            return performSave(buffer);
        }
    }

    private boolean performSave(Map<Instant, CandlestickDto> buffer) {
        String csv = buildCsv(buffer);

        if (csv.isEmpty()) return true;

        Session session = getEntityManager().unwrap(Session.class);
        try {
            session.doWork(conn -> {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("TRUNCATE TABLE stage_candles");

                    PGConnection pgConn = conn.unwrap(PGConnection.class);
                    CopyManager cm = pgConn.getCopyAPI();
                    String copySql = "COPY stage_candles(symbol, tf, ts, open, high, low, close, volume, confirmed) " +
                            "FROM STDIN WITH (FORMAT csv, DELIMITER ',', NULL '', HEADER false)";
                    long copied = cm.copyIn(copySql, new StringReader(csv));
                    LOG.infof("В staging загружено строк: %d", copied);

                    String upsert = """
                        INSERT INTO candles(symbol, tf, ts, open, high, low, close, volume, confirmed)
                        SELECT symbol, tf, ts, open, high, low, close,
                               COALESCE(volume, 0), COALESCE(confirmed, false)
                        FROM stage_candles
                        ON CONFLICT (symbol, tf, ts) DO UPDATE SET
                            open = EXCLUDED.open,
                            high = EXCLUDED.high,
                            low = EXCLUDED.low,
                            close = EXCLUDED.close,
                            volume = EXCLUDED.volume,
                            confirmed = EXCLUDED.confirmed;
                        """;
                    int affected = stmt.executeUpdate(upsert);
                    LOG.debugf("Upsert затронул строк: %d", affected);

                    stmt.execute("TRUNCATE TABLE stage_candles");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            return true;
        } catch (RuntimeException ex) {
            LOG.error("Ошибка при сохранении свечей через COPY -> stage_candles", ex);
            throw ex;
        }
    }

    private String buildCsv(Map<Instant, CandlestickDto> buffer) {
        return buffer.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .sorted(Map.Entry.comparingByKey())
                .map(e -> dtoToCsvLine(e.getValue()))
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n"));
    }

    private String dtoToCsvLine(CandlestickDto dto) {
        try {
            String symbol = safe(dto.getInstrument());
            String tf = dto.getPeriod() != null ? dto.getPeriod().name() : "";
            String ts = dto.getTimestamp() != null
                    ? TS_FMT.format(LocalDateTime.ofInstant(dto.getTimestamp(), ZoneOffset.UTC))
                    : "";

            String open = number(dto.getOpen());
            String high = number(dto.getHigh());
            String low = number(dto.getLow());
            String close = number(dto.getClose());
            String volume = numberOrZero(dto.getVolume());
            String confirmed = String.valueOf(Boolean.TRUE.equals(dto.getConfirmed()));

            // CSV со стандартным разделителем ',' и без кавычек (значения не содержат запятых)
            return String.join(",",
                    symbol, tf, ts,
                    open, high, low, close,
                    volume,
                    confirmed
            );
        } catch (Exception ex) {
            LOG.warn("Не удалось сформировать CSV-строку для свечи", ex);
            return null;
        }
    }

    private String safe(String s) { return s == null ? "" : s; }
    private String number(java.math.BigDecimal n) { return n == null ? "" : n.toPlainString(); }
    private String numberOrZero(java.math.BigDecimal n) { return n == null ? "0" : n.toPlainString(); }

    @Override
    public Map<Instant, CandlestickDto> restoreFromStorage() {
        return Map.of();
    }
}
