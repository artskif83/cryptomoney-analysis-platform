-- 03_migrate_trade_events_order_params.sql
-- Миграция для продакшен среды: добавление полей параметров стратегии в таблицу trade_events.
-- Все колонки добавляются как nullable, затем заполняются значениями по умолчанию.

-- 1) Добавляем колонки (если ещё не существуют)
ALTER TABLE trade_events
    ADD COLUMN IF NOT EXISTS trend_strength               integer,
    ADD COLUMN IF NOT EXISTS long_deposit_risk_percent    numeric(10, 4),
    ADD COLUMN IF NOT EXISTS long_only_close              boolean,
    ADD COLUMN IF NOT EXISTS short_deposit_risk_percent   numeric(10, 4),
    ADD COLUMN IF NOT EXISTS short_only_close             boolean,
    ADD COLUMN IF NOT EXISTS max_position_size_percent    numeric(10, 4);

-- 2) Заполняем значениями по умолчанию существующие строки
UPDATE trade_events
SET long_deposit_risk_percent  = 10.0000,
    short_deposit_risk_percent = 10.0000,
    max_position_size_percent  = 300.0000
WHERE long_deposit_risk_percent  IS NULL
   OR short_deposit_risk_percent IS NULL
   OR max_position_size_percent  IS NULL;

