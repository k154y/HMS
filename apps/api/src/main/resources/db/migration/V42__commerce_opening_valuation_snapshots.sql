CREATE TABLE commerce_opening_values (
 business_id UUID NOT NULL,activation_id UUID NOT NULL,location_id UUID NOT NULL,product_id UUID NOT NULL,
 quantity NUMERIC(19,6) NOT NULL CHECK(quantity>=0),unit_cost NUMERIC(19,4) NOT NULL CHECK(unit_cost>=0),
 inventory_value NUMERIC(24,6) NOT NULL CHECK(inventory_value>=0),
 PRIMARY KEY(business_id,location_id,product_id),
 FOREIGN KEY(business_id,activation_id) REFERENCES commerce_financial_commands(business_id,id),
 FOREIGN KEY(business_id,location_id) REFERENCES commerce_stock_locations(business_id,id),
 FOREIGN KEY(business_id,product_id) REFERENCES commerce_products(business_id,id)
);
CREATE TRIGGER commerce_opening_values_immutable BEFORE UPDATE OR DELETE ON commerce_opening_values
 FOR EACH ROW EXECUTE FUNCTION commerce_prevent_stock_movement_mutation();
