package artskif.trader.repository;

import artskif.trader.candle.CandleTimeframe;
import artskif.trader.dto.CandlestickDto;
import artskif.trader.entity.RsiIndicator;
import artskif.trader.entity.RsiIndicatorId;
import artskif.trader.dto.RsiPointDto;
import artskif.trader.mapper.RsiIndicatorMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.hibernate.Session;
import org.jboss.logging.Logger;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;

import java.io.StringReader;
import java.math.BigDecimal;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class RsiIndicatorRepository implements PanacheRepositoryBase<RsiIndicator, RsiIndicatorId>, BufferRepository<RsiPointDto> {

    private static final Logger LOG = Logger.getLogger(RsiIndicatorRepository.class);

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static final int DEFAULT_RESTORE_LIMIT = 300; // Максимальное количество точек для восстановления

    @Override
    @Transactional
    public int saveFromMap(Map<Instant, RsiPointDto> buffer) {
        if (buffer == null || buffer.isEmpty()) return 0;

        Map<Instant, RsiPointDto> unsavedBuffer = buffer.entrySet().stream()
                .filter(e -> !Boolean.TRUE.equals(e.getValue().getSaved()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        if (unsavedBuffer.isEmpty()) {
            LOG.warn("RSI нет данных для сохранения");
            return 0;
        }
        String csv = buildCsv(unsavedBuffer);

        if (csv.isEmpty()) return 0;
        final int[] affected = new int[1];

        Session session = getEntityManager().unwrap(Session.class);
        try {
            session.doWork(conn -> {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("TRUNCATE TABLE stage_indicators_rsi");

                    PGConnection pgConn = conn.unwrap(PGConnection.class);
                    CopyManager cm = pgConn.getCopyAPI();
                    String copySql = "COPY stage_indicators_rsi(symbol, tf, ts, rsi_14) " +
                            "FROM STDIN WITH (FORMAT csv, DELIMITER ',', NULL '', HEADER false)";
                    Long copied = cm.copyIn(copySql, new StringReader(csv));
                    LOG.infof("В staging RSI загружено: %s строк", copied);

                    String upsert = """
                            INSERT INTO indicators_rsi(symbol, tf, ts, rsi_14)
                            SELECT symbol, tf, ts, rsi_14
                            FROM stage_indicators_rsi
                            ON CONFLICT (symbol, tf, ts) DO UPDATE SET
                                rsi_14 = EXCLUDED.rsi_14;
                            """;
                    affected[0] = stmt.executeUpdate(upsert);
                    LOG.debugf("Upsert RSI затронул: %d строк", affected[0]);

                    stmt.execute("TRUNCATE TABLE stage_indicators_rsi");
                    unsavedBuffer.values().forEach(dto -> dto.setSaved(true));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            return affected[0];
        } catch (RuntimeException ex) {
            LOG.error("Ошибка при сохранении RSI индикаторов через COPY -> stage_indicators_rsi", ex);
            return 0;
        }
    }

    private String buildCsv(Map<Instant, RsiPointDto> buffer) {
        return buffer.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .map(e -> pointToCsvLine(e.getValue()))
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n"));
    }

    private String pointToCsvLine(RsiPointDto point) {
        try {
            String symbol = point.getInstrument();
            String tf = point.getTimeframe() != null ? point.getTimeframe().name() : "";
            String ts = point.getBucket() != null
                    ? TS_FMT.format(LocalDateTime.ofInstant(point.getBucket(), ZoneOffset.UTC))
                    : "";
            String rsi = number(point.getRsi());

            return String.join(",", symbol, tf, ts, rsi);
        } catch (Exception ex) {
            LOG.warn("Не удалось сформировать CSV-строку для RSI точки", ex);
            return null;
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String number(BigDecimal n) {
        return n == null ? "" : n.toPlainString();
    }

    @Override
    public Map<Instant, RsiPointDto> restoreFromStorage(Integer size, CandleTimeframe timeframe, String symbol) {
        if (timeframe == null || symbol == null || symbol.isEmpty()) {
            LOG.warn("Неверные параметры для восстановления RSI индикаторов из базы данных");
            return new LinkedHashMap<>();
        }
        return performRestore(size, timeframe, symbol);
    }

    @Transactional
    protected Map<Instant, RsiPointDto> performRestore(Integer size, CandleTimeframe timeframe, String symbol) {
        try {
            // Получаем последние точки RSI для конкретного таймфрейма и символа
            int limit = size != null ? size : DEFAULT_RESTORE_LIMIT;
            List<RsiIndicator> indicators = find(
                    "id.symbol = ?1 AND id.tf = ?2 ORDER BY id.ts DESC", symbol, timeframe.name()
            ).page(0, limit).list();

            LOG.infof("Восстановили последние %d точек RSI из базы данных для таймфрейма %s и символа %s",
                    indicators.size(), timeframe, symbol);

            if (indicators.isEmpty()) {
                LOG.infof("Точки RSI для восстановления не найдены для таймфрейма %s и символа %s", timeframe, symbol);
                return new LinkedHashMap<>();
            }

            // Конвертируем Entity в RsiPoint и собираем в LinkedHashMap
            Map<Instant, RsiPointDto> result = new LinkedHashMap<>();
            for (RsiIndicator indicator : indicators) {
                RsiPointDto point = RsiIndicatorMapper.mapEntityToDto(indicator);
                if (point != null) {
                    point.setSaved(true);
                    result.put(point.getBucket(), point);
                }
            }

            LOG.infof("Восстановлено %d точек RSI из базы данных для таймфрейма %s и символа %s",
                    result.size(), timeframe, symbol);
            return result;
        } catch (Exception ex) {
            LOG.errorf(ex, "Ошибка при восстановлении RSI индикаторов из базы данных для таймфрейма %s и символа %s",
                    timeframe, symbol);
            return new LinkedHashMap<>();
        }
    }
}
