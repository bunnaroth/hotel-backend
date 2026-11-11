CREATE TABLE room_allocation (
    allocation_id UUID PRIMARY KEY,
    reservation_id UUID NOT NULL REFERENCES reservation (reservation_id),
    room_id BIGINT NOT NULL REFERENCES hotel_room (room_id),
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (check_in_date < check_out_date)
);

CREATE INDEX idx_room_allocation_reservation_id ON room_allocation (reservation_id);
CREATE INDEX idx_room_allocation_room_dates_active
    ON room_allocation (room_id, check_in_date, check_out_date, active);
