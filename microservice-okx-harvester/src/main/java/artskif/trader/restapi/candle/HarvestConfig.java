package artskif.trader.restapi.candle;

/**
 * Конфигурация для сбора исторических данных
 */
public class HarvestConfig {
    private final String instId;
    private final int limit;
    private final long startEpochMs;
    private final long requestPauseMs;
    private final int pagesLimit;

    private HarvestConfig(Builder builder) {
        this.instId = builder.instId;
        this.limit = builder.limit;
        this.startEpochMs = builder.startEpochMs;
        this.requestPauseMs = builder.requestPauseMs;
        this.pagesLimit = builder.pagesLimit;
    }

    public String getInstId() {
        return instId;
    }

    public int getLimit() {
        return limit;
    }

    public long getStartEpochMs() {
        return startEpochMs;
    }

    public long getRequestPauseMs() {
        return requestPauseMs;
    }

    public int getPagesLimit() {
        return pagesLimit;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String instId;
        private int limit = 300;
        private long startEpochMs;
        private long requestPauseMs = 250;
        private int pagesLimit = 1;

        public Builder instId(String instId) {
            this.instId = instId;
            return this;
        }

        public Builder limit(int limit) {
            this.limit = limit;
            return this;
        }

        public Builder startEpochMs(long startEpochMs) {
            this.startEpochMs = startEpochMs;
            return this;
        }

        public Builder requestPauseMs(long requestPauseMs) {
            this.requestPauseMs = requestPauseMs;
            return this;
        }

        public Builder pagesLimit(int pagesLimit) {
            this.pagesLimit = pagesLimit;
            return this;
        }

        public HarvestConfig build() {
            return new HarvestConfig(this);
        }
    }
}

