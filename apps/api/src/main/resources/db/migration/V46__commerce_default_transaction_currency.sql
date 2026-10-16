-- Existing catalog and ledger amounts retain their accounting-base denomination.
ALTER TABLE commerce_businesses ADD COLUMN default_transaction_currency VARCHAR(3)
 CHECK (default_transaction_currency ~ '^[A-Z]{3}$');
