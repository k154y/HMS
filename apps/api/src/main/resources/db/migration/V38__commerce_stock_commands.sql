CREATE TABLE commerce_stock_commands (
 id UUID PRIMARY KEY, business_id UUID NOT NULL REFERENCES commerce_businesses(id),
 request_id UUID NOT NULL, created_by UUID NOT NULL REFERENCES users(id),
 kind VARCHAR(20) NOT NULL CHECK(kind IN ('TRANSFER','ADJUSTMENT')),
 source_location UUID NOT NULL, destination_location UUID, product_id UUID NOT NULL,
 quantity NUMERIC(19,6) NOT NULL CHECK(quantity <> 0), reason VARCHAR(500) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(business_id,request_id), UNIQUE(business_id,id),
 FOREIGN KEY(business_id,source_location) REFERENCES commerce_stock_locations(business_id,id),
 FOREIGN KEY(business_id,destination_location) REFERENCES commerce_stock_locations(business_id,id),
 FOREIGN KEY(business_id,product_id) REFERENCES commerce_products(business_id,id),
 CHECK((kind='TRANSFER' AND quantity>0 AND destination_location IS NOT NULL AND destination_location<>source_location)
    OR (kind='ADJUSTMENT' AND destination_location IS NULL)),
 CHECK(length(trim(reason)) > 0)
);
CREATE TRIGGER commerce_stock_commands_immutable BEFORE UPDATE OR DELETE ON commerce_stock_commands
 FOR EACH ROW EXECUTE FUNCTION commerce_prevent_stock_movement_mutation();
ALTER TABLE commerce_stock_movements DROP CONSTRAINT commerce_stock_movements_movement_type_check;
ALTER TABLE commerce_stock_movements DROP CONSTRAINT commerce_stock_movements_quantity_delta_check;
ALTER TABLE commerce_stock_movements ADD COLUMN command_id UUID;
ALTER TABLE commerce_stock_movements ADD CONSTRAINT commerce_movement_command_fk
 FOREIGN KEY(business_id,command_id) REFERENCES commerce_stock_commands(business_id,id);
ALTER TABLE commerce_stock_movements ADD CONSTRAINT commerce_movement_type_check
 CHECK(movement_type IN ('OPENING_STOCK','TRANSFER_OUT','TRANSFER_IN','ADJUSTMENT'));
ALTER TABLE commerce_stock_movements ADD CONSTRAINT commerce_movement_quantity_check
 CHECK((movement_type IN ('OPENING_STOCK','TRANSFER_IN') AND quantity_delta>0)
 OR (movement_type='TRANSFER_OUT' AND quantity_delta<0) OR (movement_type='ADJUSTMENT' AND quantity_delta<>0));
