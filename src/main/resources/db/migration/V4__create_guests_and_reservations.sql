CREATE TABLE guest (
    guest_id UUID PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(254),
    phone_number VARCHAR(40),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (email IS NOT NULL OR phone_number IS NOT NULL)
);

CREATE INDEX idx_guest_email ON guest (email);
CREATE INDEX idx_guest_name ON guest (last_name, first_name);

CREATE TABLE reservation (
    reservation_id UUID PRIMARY KEY,
    booking_reference VARCHAR(24) NOT NULL UNIQUE,
    primary_guest_id UUID NOT NULL REFERENCES guest (guest_id),
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    status VARCHAR(24) NOT NULL
        CHECK (status IN ('PENDING', 'CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED', 'NO_SHOW')),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CHECK (check_in_date < check_out_date)
);

CREATE INDEX idx_reservation_primary_guest_id ON reservation (primary_guest_id);
CREATE INDEX idx_reservation_status_expires_at ON reservation (status, expires_at);

CREATE TABLE reservation_guest (
    reservation_id UUID NOT NULL REFERENCES reservation (reservation_id),
    guest_id UUID NOT NULL REFERENCES guest (guest_id),
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (reservation_id, guest_id)
);

CREATE INDEX idx_reservation_guest_guest_id ON reservation_guest (guest_id);
