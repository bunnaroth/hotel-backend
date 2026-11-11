CREATE TABLE hotel_settings (
    settings_id SMALLINT PRIMARY KEY CHECK (settings_id = 1),
    hotel_name VARCHAR(160) NOT NULL,
    street_address VARCHAR(300) NOT NULL,
    phone_number VARCHAR(40),
    contact_email VARCHAR(254),
    time_zone VARCHAR(100) NOT NULL,
    currency_code VARCHAR(3) NOT NULL CHECK (currency_code ~ '^[A-Z]{3}$'),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
