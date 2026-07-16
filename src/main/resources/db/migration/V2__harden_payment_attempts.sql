ALTER TABLE payments
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    ADD COLUMN provider_order_id VARCHAR(100) NULL,
    ADD COLUMN provider_request_id VARCHAR(100) NULL,
    ADD COLUMN payment_channel VARCHAR(30) NULL,
    ADD COLUMN response_code VARCHAR(30) NULL,
    ADD COLUMN transaction_status VARCHAR(30) NULL,
    ADD COLUMN response_message VARCHAR(500) NULL,
    ADD COLUMN bank_code VARCHAR(50) NULL,
    ADD COLUMN card_type VARCHAR(50) NULL,
    ADD COLUMN deeplink TEXT NULL,
    ADD COLUMN qr_code_url TEXT NULL,
    ADD COLUMN signature_verified BIT NOT NULL DEFAULT 0,
    ADD COLUMN failure_reason VARCHAR(500) NULL,
    ADD COLUMN provider_pay_date VARCHAR(30) NULL,
    ADD COLUMN paid_at DATETIME(6) NULL,
    ADD COLUMN expired_at DATETIME(6) NULL;

UPDATE payments
SET provider_order_id = CONCAT('LEGACY', id)
WHERE provider_order_id IS NULL;

UPDATE payments
SET expired_at = DATE_ADD(COALESCE(created_at, NOW()), INTERVAL 15 MINUTE)
WHERE expired_at IS NULL;

ALTER TABLE payments
    MODIFY COLUMN provider_order_id VARCHAR(100) NOT NULL,
    MODIFY COLUMN status VARCHAR(20) NOT NULL;

CREATE UNIQUE INDEX uk_payments_provider_order_id ON payments (provider_order_id);
CREATE INDEX idx_payments_order_status_created ON payments (order_id, status, created_at);
