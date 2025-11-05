package artskif.trader.repository;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.entity.Candle;
import artskif.trader.entity.CandleId;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import artskif.trader.mapper.CandlestickMapper;

import org.hibernate.Session;
import org.jboss.logging.Logger;

import javax.sql.DataSource;
import java.io.StringReader;
import java.math.BigDecimal;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;

@ApplicationScoped
public class CandleRepository implements PanacheRepositoryBase<Candle, CandleId>, BufferRepository<CandlestickDto> {

    private static final Logger LOG = Logger.getLogger(CandleRepository.class);

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static final int DEFAULT_RESTORE_LIMIT = 100;

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

    @ActivateRequestContext
    @Override
    public boolean saveFromMap(Map<Instant, CandlestickDto> buffer) {
        if (buffer == null || buffer.isEmpty()) return true;
        return performSave(buffer);
    }

    @Transactional
    protected boolean performSave(Map<Instant, CandlestickDto> buffer) {
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

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String number(BigDecimal n) {
        return n == null ? "" : n.toPlainString();
    }

    private String numberOrZero(BigDecimal n) {
        return n == null ? "0" : n.toPlainString();
    }


    @Override
    public Map<Instant, CandlestickDto> restoreFromStorage(CandleTimeframe timeframe, String symbol) {
        if (timeframe == null || symbol == null || symbol.isEmpty()) {
            LOG.warn("Неверные параметры для восстановления свечей из базы данных");
            return new LinkedHashMap<>();
        }
        return performRestore(timeframe, symbol);
    }

    @Transactional
    protected Map<Instant, CandlestickDto> performRestore(CandleTimeframe timeframe, String symbol) {

        try {
            LOG.infof("Восстанавливаем последние %d свечей из базы данных для таймфрейма %s и символа %s",
                    DEFAULT_RESTORE_LIMIT, timeframe, symbol);

            // Получаем последние свечи для конкретного таймфрейма и символа, отсортированные по timestamp по убыванию
            List<Candle> candles = find(
                    "id.symbol = ?1 AND id.tf = ?2 ORDER BY id.ts DESC", symbol, timeframe.name()
            )
                    .page(0, DEFAULT_RESTORE_LIMIT)
                    .list();

            if (candles.isEmpty()) {
                LOG.infof("Свечи для восстановления не найдены для таймфрейма %s и символа %s", timeframe, symbol);
                return new LinkedHashMap<>();
            }

            // Переворачиваем порядок, чтобы получить свечи от старых к новым (по возрастанию timestamp)
            Collections.reverse(candles);

            // Конвертируем Entity в DTO и собираем в LinkedHashMap для сохранения порядка
            Map<Instant, CandlestickDto> result = new LinkedHashMap<>();
            for (Candle candle : candles) {
                CandlestickDto dto = CandlestickMapper.mapEntityToDto(candle);
                if (dto != null) {
                    result.put(dto.getTimestamp(), dto);
                }
            }

            LOG.infof("Восстановлено %d свечей из базы данных для таймфрейма %s и символа %s",
                    result.size(), timeframe, symbol);
            return result;
        } catch (Exception ex) {
            LOG.errorf(ex, "Ошибка при восстановлении свечей из базы данных для таймфрейма %s и символа %s",
                    timeframe, symbol);
            return new LinkedHashMap<>();
        }
    }
}
