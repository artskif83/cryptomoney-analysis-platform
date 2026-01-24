package artskif.trader.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Стандартный ответ для операций, который может содержать результат любого типа или ошибку
 * @param <T> тип результата операции
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TradingResponse<T>(
        boolean success,
        T result,
        String errorCode,
        String errorMessage
) {
    /**
     * Создать успешный ответ
     */
    public static <T> TradingResponse<T> success(T result) {
        return new TradingResponse<>(true, result, null, null);
    }

    /**
     * Создать ответ с ошибкой
     */
    public static <T> TradingResponse<T> error(String errorCode, String errorMessage) {
        return new TradingResponse<>(false, null, errorCode, errorMessage);
    }
}

