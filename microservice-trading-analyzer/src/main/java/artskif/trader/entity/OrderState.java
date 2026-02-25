package artskif.trader.entity;

/**
 * Состояние ордера
 */
public enum OrderState {
    /**
     * Активный ордер (ожидает исполнения)
     */
    LIVE("live"),

    /**
     * Частично исполненный ордер
     */
    PARTIALLY_FILLED("partially_filled"),

    /**
     * Закрытый ордер (исполнен или отменен)
     */
    CLOSED("closed");

    private final String value;

    OrderState(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * Преобразует строковое значение из API в enum
     */
    public static OrderState fromString(String value) {
        if (value == null) {
            return LIVE; // По умолчанию
        }

        for (OrderState state : OrderState.values()) {
            if (state.value.equalsIgnoreCase(value)) {
                return state;
            }
        }

        return LIVE; // По умолчанию для неизвестных значений
    }

    @Override
    public String toString() {
        return value;
    }
}
