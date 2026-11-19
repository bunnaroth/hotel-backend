CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE room_allocation
    ADD CONSTRAINT room_allocation_no_overlapping_active
    EXCLUDE USING gist (
        room_id WITH =,
        daterange(check_in_date, check_out_date, '[)') WITH &&
    )
    WHERE (active = TRUE);
