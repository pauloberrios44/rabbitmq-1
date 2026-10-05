-- Run once using a migration user against a NEW dedicated secure database.
CREATE TABLE orders (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT,
    customer_name VARCHAR(255),
    customer_email VARCHAR(255),
    amount DECIMAL(19,2),
    status VARCHAR(255),
    created_at DATETIME(6)
);
CREATE TABLE outbox_events (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(36) NOT NULL,
    correlation_id VARCHAR(36) NOT NULL,
    payload TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    sent_at DATETIME(6),
    attempts INT NOT NULL DEFAULT 0,
    INDEX idx_outbox_pending(sent_at, id)
);
