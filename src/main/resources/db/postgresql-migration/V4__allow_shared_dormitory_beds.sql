UPDATE room_allocation allocation
SET allocated_units = CASE
    WHEN room_type.category = 'DORMITORY' THEN room_type.max_occupancy
    ELSE 1
END
FROM hotel_room
JOIN room_type ON room_type.room_type_id = hotel_room.room_type_id
WHERE allocation.room_id = hotel_room.room_id;

ALTER TABLE room_allocation
    DROP CONSTRAINT IF EXISTS room_allocation_no_overlapping_active;
