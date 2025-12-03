package artskif.trader.executor.orders;

import artskif.trader.executor.orders.strategy.list.Symbol;
import my.signals.v1.SignalLevel;
import my.signals.v1.StrategyKind;
import org.springframework.stereotype.Component;

@Component
public class OrderManagerRunnerTest {

    private final OrderManagerService orderManagerService;

    public OrderManagerRunnerTest(OrderManagerService orderManagerService) {
        this.orderManagerService = orderManagerService;
    }

    // Сугубо для тестовых целей раскомментируйте что бы выполнить ордер
    //@EventListener(ApplicationReadyEvent.class)
    public void runAfterStartup() {
        // здесь создаёшь тестовый сигнал
        my.signals.v1.Signal testSignal = my.signals.v1.Signal.newBuilder()
                .setOperation(my.signals.v1.OperationType.BUY)
                .setPrice(111812.0)
                .setSymbol(new Symbol("BTC", "USDT").toProto())
                .setStrategy(StrategyKind.RSI_DUAL_TF)
                .setLevel(SignalLevel.MIDDLE)
                .build();

        orderManagerService.onSignal(testSignal);
    }

}
