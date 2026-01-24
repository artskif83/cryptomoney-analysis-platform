package artskif.trader.executor.orders;

import artskif.trader.api.dto.OrderExecutionResult;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Результат выполнения операции, который может содержать либо успешный результат, либо ошибку
 */
public sealed interface OperationResult permits OperationResult.Success, OperationResult.Error {

    boolean isSuccess();

    Optional<OrderExecutionResult> getResult();

    Optional<ErrorDetails> getError();

    /**
     * Выполнить действие в зависимости от результата
     */
    default void handle(Consumer<OrderExecutionResult> onSuccess, Consumer<ErrorDetails> onError) {
        if (this instanceof Success success) {
            onSuccess.accept(success.result());
        } else if (this instanceof Error error) {
            onError.accept(error.errorDetails());
        }
    }

    /**
     * Преобразовать результат
     */
    default <R> R map(Function<OrderExecutionResult, R> onSuccess, Function<ErrorDetails, R> onError) {
        if (this instanceof Success success) {
            return onSuccess.apply(success.result());
        } else if (this instanceof Error error) {
            return onError.apply(error.errorDetails());
        }
        throw new IllegalStateException("Unexpected OperationResult type");
    }

    record Success(OrderExecutionResult result) implements OperationResult {
        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public Optional<OrderExecutionResult> getResult() {
            return Optional.of(result);
        }

        @Override
        public Optional<ErrorDetails> getError() {
            return Optional.empty();
        }
    }

    record Error(ErrorDetails errorDetails) implements OperationResult {
        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public Optional<OrderExecutionResult> getResult() {
            return Optional.empty();
        }

        @Override
        public Optional<ErrorDetails> getError() {
            return Optional.of(errorDetails);
        }
    }

    record ErrorDetails(String code, String message) {}

    static OperationResult success(OrderExecutionResult result) {
        return new Success(result);
    }

    static OperationResult error(String code, String message) {
        return new Error(new ErrorDetails(code, message));
    }
}

