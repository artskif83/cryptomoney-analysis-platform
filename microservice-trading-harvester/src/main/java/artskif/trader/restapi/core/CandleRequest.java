package artskif.trader.restapi.core;

/**
 * Запрос на получение свечных данных
 */
public class CandleRequest {
    private final String instId;
    private final String timeframe;
    private final int limit;
    private final Long before;
    private final Long after;

    private CandleRequest(Builder builder) {
        this.instId = builder.instId;
        this.timeframe = builder.timeframe;
        this.limit = builder.limit;
        this.before = builder.before;
        this.after = builder.after;
    }

    public String getInstId() {
        return instId;
    }

    public String getTimeframe() {
        return timeframe;
    }

    public int getLimit() {
        return limit;
    }

    public Long getBefore() {
        return before;
    }

    public Long getAfter() {
        return after;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String instId;
        private String timeframe;
        private int limit = 300;
        private Long before;
        private Long after;

        public Builder instId(String instId) {
            this.instId = instId;
            return this;
        }

        public Builder timeframe(String timeframe) {
            this.timeframe = timeframe;
            return this;
        }

        public Builder limit(int limit) {
            this.limit = limit;
            return this;
        }

        public Builder before(Long before) {
            this.before = before;
            return this;
        }

        public Builder after(Long after) {
            this.after = after;
            return this;
        }

        public CandleRequest build() {
            return new CandleRequest(this);
        }
    }
}

