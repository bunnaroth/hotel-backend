CREATE TABLE reservation_payment (
    payment_id UUID PRIMARY KEY,
    reservation_id UUID NOT NULL REFERENCES reservation (reservation_id),
    provider VARCHAR(32) NOT NULL,
    provider_reference VARCHAR(128),
    status VARCHAR(16) NOT NULL
        CHECK (status IN ('PENDING', 'AUTHORIZED', 'CAPTURED', 'REFUNDED', 'FAILED')),
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_reservation_payment_reservation_id ON reservation_payment (reservation_id);
CREATE INDEX idx_reservation_payment_status ON reservation_payment (status);
