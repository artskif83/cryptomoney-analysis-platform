package artskif.trader.microserviceokxtelegrambot.orders.signal;

import my.signals.v1.StrategyKind;

import java.math.BigDecimal;
import java.time.Instant;

public record Signal2(
        String id,
        Symbol symbol,
        StrategyKind strategyKind,
        Level level,
        Side side,
        BigDecimal price,     // текущая рыночная цена в котируемой валюте
        Instant timestamp
) {}
