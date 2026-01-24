package artskif.trader.executor.orders;

import artskif.trader.api.dto.OrderExecutionResult;
import artskif.trader.executor.common.Symbol;

import java.math.BigDecimal;

public interface OrdersClient {
    /**
     * Размещает рыночный ордер на покупку на спотовом рынке
     * @param symbol Торговая пара
     * @param percentOfDeposit Процент от депозита в квотируемой валюте (от 0 до 100)
     * @return Результат исполнения ордера
     */
    OrderExecutionResult placeSpotMarketBuy(Symbol symbol, BigDecimal percentOfDeposit);

    /**
     * Размещает рыночный ордер на продажу на спотовом рынке
     * @param symbol Торговая пара
     * @param percentOfDeposit Процент от депозита в базовой валюте (от 0 до 100)
     * @return Результат исполнения ордера
     */
    OrderExecutionResult placeSpotMarketSell(Symbol symbol, BigDecimal percentOfDeposit);
}
