package artskif.trader.repository;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.entity.Candle;
import artskif.trader.entity.CandleId;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import artskif.trader.mapper.CandlestickMapper;

import org.hibernate.Session;
import org.jboss.logging.Logger;
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

    private static final int DEFAULT_RESTORE_LIMIT = 300; // Максимальное количество свечей для восстановления

    @Override
    @Transactional
    public int saveFromMap(Map<Instant, CandlestickDto> buffer) {
        if (buffer == null || buffer.isEmpty()) return 0;
        Map<Instant, CandlestickDto> unsavedBuffer = buffer.entrySet().stream()
                .filter(e -> !Boolean.TRUE.equals(e.getValue().getSaved()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        if (unsavedBuffer.isEmpty()) {
            LOG.warn("Нет данных для сохранения");
            return 0;
        }

        String csv = buildCsv(unsavedBuffer);

        if (csv.isEmpty()) return 0;
        final int[] affected = new int[1];
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
                    LOG.debugf("В staging загружено строк: %d", copied);

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
                    affected[0] = stmt.executeUpdate(upsert);
                    LOG.debugf("Upsert затронул строк: %d", affected[0]);

                    stmt.execute("TRUNCATE TABLE stage_candles");
                    unsavedBuffer.values().forEach(dto -> dto.setSaved(true));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            return affected[0];
        } catch (RuntimeException ex) {
            LOG.error("Ошибка при сохранении свечей через COPY -> stage_candles", ex);
            return 0;
        }
    }

    private String buildCsv(Map<Instant, CandlestickDto> buffer) {
        return buffer.entrySet().stream()
                .filter(e -> e.getValue() != null)
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
    @Transactional
    public Map<Instant, CandlestickDto> restoreFromStorage(Integer maxBufferSize, CandleTimeframe timeframe, String symbol, boolean isLive) {
        if (timeframe == null || symbol == null || symbol.isEmpty()) {
            LOG.warn("Неверные параметры для восстановления свечей из базы данных");
            return new LinkedHashMap<>();
        }

        try {
            int limit = maxBufferSize != null ? maxBufferSize : DEFAULT_RESTORE_LIMIT;

            // Для live-режима вычисляем временную границу актуальности данных
            Instant cutoffTime = null;
            if (isLive) {
                // Актуальные данные = текущее время минус (период таймфрейма * количество свечей)
                long secondsToSubtract = timeframe.getDuration().toSeconds() * (limit+1);
                cutoffTime = Instant.now().minusSeconds(secondsToSubtract);
                LOG.infof("💾 Live-режим: загружаем данные не старее %s для таймфрейма %s и символа %s",
                        cutoffTime, timeframe, symbol);
            }

            // Получаем последние свечи для конкретного таймфрейма и символа, отсортированные по timestamp по убыванию
            List<Candle> candles;
            if (cutoffTime != null) {
                candles = find(
                        "id.symbol = ?1 AND id.tf = ?2 AND id.ts >= ?3 ORDER BY id.ts DESC",
                        symbol, timeframe.name(), cutoffTime
                ).page(0, limit).list();
            } else {
                candles = find(
                        "id.symbol = ?1 AND id.tf = ?2 ORDER BY id.ts DESC",
                        symbol, timeframe.name()
                ).page(0, limit).list();
            }

            LOG.infof("💾 Восстановили последние %d свечей из базы данных для таймфрейма %s и символа %s (isLive=%s)",
                    candles.size(), timeframe, symbol, isLive);

            if (candles.isEmpty()) {
                LOG.infof("💾 Свечи для восстановления не найдены для таймфрейма %s и символа %s", timeframe, symbol);
                return new LinkedHashMap<>();
            }

            // Конвертируем Entity в DTO и собираем в LinkedHashMap для сохранения порядка
            Map<Instant, CandlestickDto> result = new LinkedHashMap<>();
            for (Candle candle : candles) {
                CandlestickDto dto = CandlestickMapper.mapEntityToDto(candle);
                if (dto != null && dto.getConfirmed()) {
                    dto.setSaved(true);
                    result.put(dto.getTimestamp(), dto);
                }
            }

            LOG.infof("💾 Восстановлено %d свечей из базы данных для таймфрейма %s и символа %s",
                    result.size(), timeframe, symbol);
            return result;
        } catch (Exception ex) {
            LOG.errorf(ex, "Ошибка при восстановлении свечей из базы данных для таймфрейма %s и символа %s",
                    timeframe, symbol);
            return new LinkedHashMap<>();
        }
    }
}
