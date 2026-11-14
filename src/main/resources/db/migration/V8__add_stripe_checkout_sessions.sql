ALTER TABLE reservation_payment
    ADD COLUMN checkout_session_id VARCHAR(255);

ALTER TABLE reservation_payment
    ADD COLUMN checkout_url VARCHAR(2048);

ALTER TABLE reservation_payment
    ADD COLUMN checkout_expires_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE reservation_payment
    ADD CONSTRAINT reservation_payment_checkout_session_id_key UNIQUE (checkout_session_id);
