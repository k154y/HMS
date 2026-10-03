CREATE TABLE hotel_currencies (
 hotel_id UUID NOT NULL REFERENCES hotels(id), code VARCHAR(3) NOT NULL CHECK(code ~ '^[A-Z]{3}$'),
 name VARCHAR(100) NOT NULL, enabled BOOLEAN NOT NULL DEFAULT TRUE, PRIMARY KEY(hotel_id,code)
);
INSERT INTO hotel_currencies(hotel_id,code,name) SELECT id,currency_code,currency_code FROM hotels;
INSERT INTO hotel_currencies(hotel_id,code,name) SELECT DISTINCT hotel_id,currency_code,currency_code FROM hotel_exchange_rates ON CONFLICT DO NOTHING;
CREATE TABLE payment_accounts (
 id UUID PRIMARY KEY, hotel_id UUID NOT NULL REFERENCES hotels(id), name VARCHAR(100) NOT NULL,
 method VARCHAR(30) NOT NULL CHECK(method IN ('CASH','BANK_TRANSFER','MOBILE_MONEY','CARD')),
 currency VARCHAR(3) NOT NULL, identifier_type VARCHAR(30) NOT NULL CHECK(identifier_type IN ('NONE','ACCOUNT_NUMBER','PHONE_NUMBER','MERCHANT_CODE')),
 identifier VARCHAR(100), active BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(id,hotel_id), UNIQUE(hotel_id,name), FOREIGN KEY(hotel_id,currency) REFERENCES hotel_currencies(hotel_id,code),
 CHECK (method<>'BANK_TRANSFER' OR identifier_type='ACCOUNT_NUMBER'),
 CHECK (method<>'MOBILE_MONEY' OR identifier_type IN ('PHONE_NUMBER','MERCHANT_CODE')),
 CHECK ((identifier_type='NONE' AND identifier IS NULL) OR (identifier_type<>'NONE' AND identifier IS NOT NULL AND length(trim(identifier))>0))
);
ALTER TABLE payments ADD COLUMN payment_account_id UUID;
ALTER TABLE payments ADD COLUMN payment_account_name VARCHAR(100);
ALTER TABLE payments ADD COLUMN payment_account_identifier VARCHAR(100);
ALTER TABLE payments ADD FOREIGN KEY(payment_account_id,hotel_id) REFERENCES payment_accounts(id,hotel_id);
ALTER TABLE vendor_payments ADD COLUMN payment_account_id UUID;
ALTER TABLE vendor_payments ADD COLUMN payment_account_name VARCHAR(100);
ALTER TABLE vendor_payments ADD COLUMN payment_account_identifier VARCHAR(100);
ALTER TABLE vendor_payments ADD FOREIGN KEY(payment_account_id,hotel_id) REFERENCES payment_accounts(id,hotel_id);
ALTER TABLE expenses ADD COLUMN payment_account_id UUID;
ALTER TABLE expenses ADD COLUMN payment_account_name VARCHAR(100);
ALTER TABLE expenses ADD COLUMN payment_account_identifier VARCHAR(100);
ALTER TABLE expenses ADD FOREIGN KEY(payment_account_id,hotel_id) REFERENCES payment_accounts(id,hotel_id);
