# Система Feature Contracts для обучения ML моделей

## Описание

Система контрактов (feature contracts) предназначена для создания wide table с фичами (параметрами) для обучения ML модели на базе XGBoost. Система использует библиотеку ta4j для расчета технических индикаторов.

## Архитектура

### Основные компоненты

1. **Contract** - Entity, представляющая wide table для ML. Содержит базовые поля из свечей и динамически добавляемые фичи.

2. **ContractId** - Составной ключ контракта (symbol, tf, ts).

3. **ContractFeatureMetadata** - Метаданные для фич:
   - `featureName` - имя фичи в БД
   - `description` - описание фичи
   - `sequenceOrder` - порядок следования для ML
   - `dataType` - тип данных SQL

4. **FeatureCreator** - Интерфейс для создателей фич. Каждый индикатор реализует этот интерфейс.

5. **ContractService** - Сервис для управления контрактами:
   - Создание/обновление контрактов из свечей
   - Добавление фич в контракты
   - Автоматическое создание колонок для новых фич

6. **ContractProcessor** - Обработчик свечей для создания контрактов с фичами.

7. **ContractFeatureRegistry** - Реестр всех создателей фич.

### Структура БД

#### Таблица `contracts`
```sql
CREATE TABLE IF NOT EXISTS contracts
(
    symbol         varchar(32)    NOT NULL,
    tf             varchar(10)    NOT NULL,
    ts             timestamp      NOT NULL,
    open           numeric(18, 8) NOT NULL,
    high           numeric(18, 8) NOT NULL,
    low            numeric(18, 8) NOT NULL,
    close          numeric(18, 8) NOT NULL,
    volume         numeric(30, 8),
    confirmed      boolean        NOT NULL DEFAULT false,
    -- Динамические колонки для фич добавляются автоматически
    PRIMARY KEY (symbol, tf, ts)
);
```

#### Таблица `contract_features_metadata`
```sql
CREATE TABLE IF NOT EXISTS contract_features_metadata
(
    feature_name   varchar(255)   NOT NULL PRIMARY KEY,
    description    text           NOT NULL,
    sequence_order integer        NOT NULL UNIQUE,
    data_type      varchar(50)    NOT NULL,
    created_at     timestamp      NOT NULL DEFAULT NOW(),
    updated_at     timestamp      NOT NULL DEFAULT NOW()
);
```

## Использование

### 1. Создание нового индикатора/фичи

Для добавления новой фичи необходимо:

1. Создать класс, реализующий интерфейс `FeatureCreator`:

```java
@ApplicationScoped
public class MyFeatureCreator implements FeatureCreator {
    
    private static final String FEATURE_NAME = "my_feature";
    private static final String DESCRIPTION = "Описание фичи";
    private static final Integer SEQUENCE_ORDER = 2; // Порядок следования
    private static final String DATA_TYPE = "numeric(10, 2)"; // Тип данных SQL
    
    @Override
    public ContractFeatureMetadata getFeatureMetadata() {
        return new ContractFeatureMetadata(
            FEATURE_NAME, DESCRIPTION, SEQUENCE_ORDER, DATA_TYPE
        );
    }
    
    @Override
    public Object calculateFeature(Object context) {
        // Логика расчета фичи
        // Возвращаем вычисленное значение
        return null;
    }
    
    @Override
    public String getFeatureName() {
        return FEATURE_NAME;
    }
    
    @Override
    public String getDataType() {
        return DATA_TYPE;
    }
}
```

2. При первом запуске система автоматически:
   - Зарегистрирует фичу в реестре
   - Создаст колонку в таблице `contracts`
   - Добавит метаданные в `contract_features_metadata`

### 2. Обработка свечей

```java
@Inject
ContractProcessor contractProcessor;

// Обработать одну свечу
contractProcessor.processCandle(candle);

// Обработать пакет подтвержденных свечей
contractProcessor.processConfirmedCandles("BTC-USDT", "5m", from, to);
```

### 3. REST API

#### Обработать свечи
```http
POST /api/contracts/process?symbol=BTC-USDT&tf=5m&from=2024-01-01T00:00:00Z&to=2024-01-02T00:00:00Z
```

#### Получить метаданные фич
```http
GET /api/contracts/features
```

#### Получить информацию о создателях фич
```http
GET /api/contracts/features/creators
```

#### Создать колонку для новой фичи
```http
POST /api/contracts/features/{featureName}/column
```

## Пример: RSI индикатор

Система включает готовую реализацию RSI индикатора на базе ta4j:

```java
@ApplicationScoped
public class RsiFeatureCreator implements FeatureCreator {
    
    private static final int RSI_PERIOD = 14;
    
    @Override
    public Object calculateFeature(Object context) {
        RsiFeatureContext rsiContext = (RsiFeatureContext) context;
        List<Candle> candles = rsiContext.getCandles();
        
        // Создаем BarSeries для ta4j
        BarSeries series = new BaseBarSeriesBuilder()
                .withName(candles.get(0).id.symbol + "_" + candles.get(0).id.tf)
                .build();

        // Добавляем свечи
        for (Candle candle : candles) {
            series.addBar(/*...*/);
        }

        // Вычисляем RSI
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        RSIIndicator rsiIndicator = new RSIIndicator(closePrice, RSI_PERIOD);
        
        return BigDecimal.valueOf(rsiIndicator.getValue(series.getEndIndex()).doubleValue())
                .setScale(2, RoundingMode.HALF_UP);
    }
}
```

## Расширение системы

### Добавление новых индикаторов

1. Создайте новый пакет под индикатор: `artskif.trader.contract.features.{indicator_name}`
2. Создайте контекст для индикатора: `{Indicator}FeatureContext`
3. Создайте создателя фичи: `{Indicator}FeatureCreator`
4. Добавьте логику создания контекста в `ContractProcessor.createContext()`

### Пример: ADX индикатор

```java
// 1. Контекст
public class AdxFeatureContext {
    private final List<Candle> candles;
    // ...
}

// 2. Создатель фичи
@ApplicationScoped
public class AdxFeatureCreator implements FeatureCreator {
    private static final String FEATURE_NAME = "adx_14";
    private static final Integer SEQUENCE_ORDER = 2;
    // ...
}

// 3. Обновить ContractProcessor
private Object createContext(FeatureCreator creator, List<Candle> historicalCandles, Candle currentCandle) {
    String featureName = creator.getFeatureName();

    if (featureName.startsWith("rsi")) {
        return new RsiFeatureContext(historicalCandles, currentCandle);
    } else if (featureName.startsWith("adx")) {
        return new AdxFeatureContext(historicalCandles, currentCandle);
    }
    
    return null;
}
```

## Настройки

В `application.properties` можно настроить параметры:

```properties
# Включить/выключить обработку контрактов
contracts.processing.enabled=true

# Минимальное количество свечей для расчета индикаторов
contracts.min.candles=20
```

## Логирование

Система логирует:
- Регистрацию новых фич
- Создание колонок
- Обработку свечей
- Вычисление фич
- Ошибки при расчете

Уровень логирования можно настроить в `application.properties`:

```properties
quarkus.log.category."artskif.trader.contract".level=DEBUG
```

## Производительность

- Контракты сохраняются в TimescaleDB для эффективной работы с временными рядами
- Колонки создаются автоматически только при первом использовании фичи
- Индексы создаются автоматически для оптимизации запросов
- Обработка свечей выполняется пакетами для повышения производительности

