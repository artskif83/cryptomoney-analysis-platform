package artskif.trader.broker.client;

import lombok.Getter;

/**
 * Исключение, выбрасываемое при ошибке выполнения торговой операции
 */
@Getter
public class TradingExecutionException extends RuntimeException {

    private final String errorCode;

    public TradingExecutionException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public TradingExecutionException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

}

