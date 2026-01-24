package artskif.trader.executor.common;

public record Symbol(String base, String quote) {
    public String asPair() {
        return base + "/" + quote;
    }

    public static Symbol fromInstrument(String instrument) {
        String[] parts = instrument.split("-");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Неверный формат инструмента: " + instrument + ". Ожидается формат: BASE-QUOTE (например, BTC-USDT)");
        }
        return new Symbol(parts[0], parts[1]);
    }
}

