-- 03_add_contract_hash.sql
-- Добавление поля contract_hash в таблицу contracts

ALTER TABLE contracts
ADD COLUMN IF NOT EXISTS contract_hash varchar(64);

-- Создаем индекс для быстрого поиска по хешу контракта
CREATE INDEX IF NOT EXISTS contracts_contract_hash_idx ON contracts(contract_hash);

