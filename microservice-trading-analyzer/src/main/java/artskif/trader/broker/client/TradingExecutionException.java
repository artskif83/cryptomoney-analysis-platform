package artskif.trader.broker.client;

/**
 * Исключение, выбрасываемое при ошибке выполнения торговой операции
 */
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

    public String getErrorCode() {
        return errorCode;
    }
}

