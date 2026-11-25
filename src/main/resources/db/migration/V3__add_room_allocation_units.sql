ALTER TABLE room_allocation
    ADD COLUMN IF NOT EXISTS allocated_units INTEGER NOT NULL DEFAULT 1
        CHECK (allocated_units > 0);
