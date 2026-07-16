CREATE TABLE IF NOT EXISTS payments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id VARCHAR(100) NOT NULL,
    amount BIGINT NOT NULL,
    provider VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    transaction_id VARCHAR(100) NULL,
    order_info VARCHAR(255) NULL,
    payment_url TEXT NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    PRIMARY KEY (id)
);
