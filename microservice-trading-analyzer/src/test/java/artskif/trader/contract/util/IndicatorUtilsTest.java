package artskif.trader.contract.util;

import artskif.trader.strategy.indicators.util.IndicatorUtils;
import org.junit.jupiter.api.Test;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.num.DecimalNum;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IndicatorUtilsTest {

    @Test
    void mapToHigherTfIndex_shouldReturnCorrectIndex_whenLowerTfBarMapsToHigherTfBar() {
        // Given: создаем серию свечей на старшем таймфрейме (например, 1 час)
        BarSeries higherSeries = new BaseBarSeriesBuilder()
                .withName("higher")
                .build();
        ZonedDateTime baseTime = ZonedDateTime.parse("2025-12-23T10:00:00Z");

        // Добавляем 5 часовых свечей
        for (int i = 0; i < 5; i++) {
            Instant startTime = baseTime.plusHours(i).toInstant();
            Instant endTime = baseTime.plusHours(i + 1).toInstant();
            higherSeries.addBar(createBar(startTime, endTime, Duration.ofHours(1)));
        }

        // Создаем свечу на нижнем таймфрейме (15 минут), которая заканчивается в 11:15
        // Она должна мапиться на часовую свечу, закончившуюся в 11:00 (индекс 0)
        Instant lowerBarStartTime = baseTime.plusHours(1).toInstant();
        Instant lowerBarEndTime = baseTime.plusHours(1).plusMinutes(15).toInstant();
        Bar lowerTfBar = createBar(lowerBarStartTime, lowerBarEndTime, Duration.ofMinutes(15));

        // When: маппим свечу нижнего таймфрейма на индекс старшего
        int result = IndicatorUtils.mapToHigherTfIndex(lowerTfBar, higherSeries);

        // Then: должны получить индекс 0 (первая часовая свеча, которая заканчивается в 11:00)
        assertEquals(0, result);
    }

    @Test
    void mapToHigherTfIndex_shouldReturnCorrectIndex_whenLowerTfBarAtEndOfHigherTfBar() {
        // Given: создаем серию свечей на старшем таймфрейме
        BarSeries higherSeries = new BaseBarSeriesBuilder()
                .withName("higher")
                .build();
        ZonedDateTime baseTime = ZonedDateTime.parse("2025-12-23T10:00:00Z");

        for (int i = 0; i < 5; i++) {
            Instant startTime = baseTime.plusHours(i).toInstant();
            Instant endTime = baseTime.plusHours(i + 1).toInstant();
            higherSeries.addBar(createBar(startTime, endTime, Duration.ofHours(1)));
        }

        // Создаем свечу нижнего таймфрейма, которая заканчивается ровно в конце второй часовой свечи
        Instant lowerBarEndTime = baseTime.plusHours(2).toInstant();
        Instant lowerBarStartTime = baseTime.plusHours(2).minusMinutes(15).toInstant();
        Bar lowerTfBar = createBar(lowerBarStartTime, lowerBarEndTime, Duration.ofMinutes(15));

        // When
        int result = IndicatorUtils.mapToHigherTfIndex(lowerTfBar, higherSeries);

        // Then: должны получить индекс 1 (вторая часовая свеча)
        assertEquals(1, result);
    }

    @Test
    void mapToHigherTfIndex_shouldReturnLastIndex_whenLowerTfBarAtEndOfSeries() {
        // Given: создаем серию свечей на старшем таймфрейме
        BarSeries higherSeries = new BaseBarSeriesBuilder()
                .withName("higher")
                .build();
        ZonedDateTime baseTime = ZonedDateTime.parse("2025-12-23T10:00:00Z");

        for (int i = 0; i < 3; i++) {
            Instant startTime = baseTime.plusHours(i).toInstant();
            Instant endTime = baseTime.plusHours(i + 1).toInstant();
            higherSeries.addBar(createBar(startTime, endTime, Duration.ofHours(1)));
        }

        // Создаем свечу нижнего таймфрейма, которая заканчивается после последней часовой свечи
        // Последняя часовая свеча заканчивается в 13:00, создаем 15-минутную свечу 13:00-13:15
        Instant lowerBarStartTime = baseTime.plusHours(3).toInstant();
        Instant lowerBarEndTime = baseTime.plusHours(3).plusMinutes(15).toInstant();
        Bar lowerTfBar = createBar(lowerBarStartTime, lowerBarEndTime, Duration.ofMinutes(15));

        // When
        int result = IndicatorUtils.mapToHigherTfIndex(lowerTfBar, higherSeries);

        // Then: должны получить индекс последней свечи (2), которая закончилась в 13:00
        assertEquals(2, result);
    }

    @Test
    void mapToHigherTfIndex_shouldReturnMinusOne_whenNoMatchingHigherTfBarExists() {
        // Given: создаем серию свечей на старшем таймфрейме
        BarSeries higherSeries = new BaseBarSeriesBuilder()
                .withName("higher")
                .build();
        ZonedDateTime baseTime = ZonedDateTime.parse("2025-12-23T10:00:00Z");

        for (int i = 0; i < 3; i++) {
            Instant startTime = baseTime.plusHours(i).toInstant();
            Instant endTime = baseTime.plusHours(i + 1).toInstant();
            higherSeries.addBar(createBar(startTime, endTime, Duration.ofHours(1)));
        }

        // Создаем свечу нижнего таймфрейма, которая заканчивается ДО начала всех свечей старшего таймфрейма
        Instant lowerBarEndTime = baseTime.minusHours(1).toInstant();
        Instant lowerBarStartTime = baseTime.minusHours(1).minusMinutes(15).toInstant();
        Bar lowerTfBar = createBar(lowerBarStartTime, lowerBarEndTime, Duration.ofMinutes(15));

        // When
        int result = IndicatorUtils.mapToHigherTfIndex(lowerTfBar, higherSeries);

        // Then: должно быть возвращено -1
        assertEquals(-1, result);
    }

    @Test
    void mapToHigherTfIndex_shouldReturnCorrectIndex_withMultipleLowerTfBarsInSameHigherTfBar() {
        // Given: создаем серию свечей на старшем таймфрейме
        BarSeries higherSeries = new BaseBarSeriesBuilder()
                .withName("higher")
                .build();
        ZonedDateTime baseTime = ZonedDateTime.parse("2025-12-23T10:00:00Z");

        for (int i = 0; i < 3; i++) {
            Instant startTime = baseTime.plusHours(i).toInstant();
            Instant endTime = baseTime.plusHours(i + 1).toInstant();
            higherSeries.addBar(createBar(startTime, endTime, Duration.ofHours(1)));
        }

        // Создаем несколько 15-минутных свечей после первой часовой свечи (которая закончилась в 11:00)
        // Все они должны мапиться на первую часовую свечу (индекс 0)
        int[] minutesOffsets = {15, 30, 45, 60};

        for (int minutes : minutesOffsets) {
            // Свечи начинаются с 11:00 и заканчиваются в 11:15, 11:30, 11:45, 12:00
            Instant startTime = baseTime.plusHours(1).plusMinutes(minutes - 15).toInstant();
            Instant endTime = baseTime.plusHours(1).plusMinutes(minutes).toInstant();
            Bar lowerTfBar = createBar(startTime, endTime, Duration.ofMinutes(15));

            // When
            int result = IndicatorUtils.mapToHigherTfIndex(lowerTfBar, higherSeries);

            // Then: для свечей 11:00-11:15, 11:15-11:30, 11:30-11:45, 11:45-12:00
            // должны мапиться на часовую свечу, закончившуюся в 11:00 (индекс 0)
            // кроме последней (12:00), которая мапится на свечу, закончившуюся в 12:00 (индекс 1)
            int expectedIndex = (minutes == 60) ? 1 : 0;
            assertEquals(expectedIndex, result,
                    "Bar ending at " + endTime + " should map to index " + expectedIndex);
        }
    }

    @Test
    void mapToHigherTfIndex_shouldReturnCorrectIndex_whenBarsInMiddleOfSeries() {
        // Given: создаем серию свечей на старшем таймфрейме
        BarSeries higherSeries = new BaseBarSeriesBuilder()
                .withName("higher")
                .build();
        ZonedDateTime baseTime = ZonedDateTime.parse("2025-12-23T10:00:00Z");

        for (int i = 0; i < 5; i++) {
            Instant startTime = baseTime.plusHours(i).toInstant();
            Instant endTime = baseTime.plusHours(i + 1).toInstant();
            higherSeries.addBar(createBar(startTime, endTime, Duration.ofHours(1)));
        }

        // Создаем свечу нижнего таймфрейма в середине серии: 12:30-12:45
        // Должна мапиться на свечу, закончившуюся в 12:00 (индекс 1)
        Instant lowerBarStartTime = baseTime.plusHours(2).plusMinutes(30).toInstant();
        Instant lowerBarEndTime = baseTime.plusHours(2).plusMinutes(45).toInstant();
        Bar lowerTfBar = createBar(lowerBarStartTime, lowerBarEndTime, Duration.ofMinutes(15));

        // When
        int result = IndicatorUtils.mapToHigherTfIndex(lowerTfBar, higherSeries);

        // Then: должна мапиться на вторую часовую свечу (индекс 1)
        assertEquals(1, result);
    }

    /**
     * Вспомогательный метод для создания свечи с заданными временными границами
     */
    private Bar createBar(Instant startTime, Instant endTime, Duration duration) {
        return new BaseBar(
                duration,
                startTime,
                endTime,
                DecimalNum.valueOf(100),  // open
                DecimalNum.valueOf(110),  // high
                DecimalNum.valueOf(90),   // low
                DecimalNum.valueOf(105),  // close
                DecimalNum.valueOf(1000), // volume
                DecimalNum.valueOf(0),    // amount
                0L                        // trades
        );
    }
}

