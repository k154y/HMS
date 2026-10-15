-- Commerce-only catalog and initial inventory. Never touches hotel products, stock or folios.
ALTER TABLE commerce_stock_locations
ADD CONSTRAINT commerce_stock_locations_business_id_id_key
UNIQUE (business_id, id);

CREATE TABLE commerce_categories (
 id UUID PRIMARY KEY,
 business_id UUID NOT NULL REFERENCES commerce_businesses(id),
 parent_id UUID,
 name VARCHAR(160) NOT NULL,
 active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(business_id,id),
 FOREIGN KEY (business_id,parent_id) REFERENCES commerce_categories(business_id,id),
 CHECK (parent_id IS DISTINCT FROM id)
);
CREATE UNIQUE INDEX commerce_category_sibling_name ON commerce_categories
 (business_id,coalesce(parent_id,'00000000-0000-0000-0000-000000000000'::uuid),lower(name));
CREATE TABLE commerce_products (
 id UUID PRIMARY KEY,
 business_id UUID NOT NULL REFERENCES commerce_businesses(id),
 category_id UUID,
 sku VARCHAR(80) NOT NULL,
 barcode VARCHAR(120),
 name VARCHAR(200) NOT NULL,
 base_unit VARCHAR(32) NOT NULL,
 purchase_cost NUMERIC(19,4) NOT NULL DEFAULT 0 CHECK(purchase_cost >= 0),
 retail_price NUMERIC(19,4) NOT NULL DEFAULT 0 CHECK(retail_price >= 0),
 wholesale_price NUMERIC(19,4) NOT NULL DEFAULT 0 CHECK(wholesale_price >= 0),
 active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(business_id,id), UNIQUE(business_id,sku),
 FOREIGN KEY(business_id,category_id) REFERENCES commerce_categories(business_id,id),
 CHECK(length(trim(sku)) > 0), CHECK(length(trim(name)) > 0), CHECK(length(trim(base_unit)) > 0)
);
CREATE UNIQUE INDEX commerce_product_barcode ON commerce_products(business_id,barcode) WHERE barcode IS NOT NULL;
CREATE TABLE commerce_product_units (
 business_id UUID NOT NULL,
 product_id UUID NOT NULL,
 unit_name VARCHAR(32) NOT NULL,
 units_per_package NUMERIC(19,6) NOT NULL CHECK(units_per_package > 0),
 PRIMARY KEY (business_id,product_id,unit_name),
 FOREIGN KEY(business_id,product_id) REFERENCES commerce_products(business_id,id)
);
-- One balance row is locked atomically for every posted movement.
CREATE TABLE commerce_stock_balances (
 business_id UUID NOT NULL,
 location_id UUID NOT NULL,
 product_id UUID NOT NULL,
 quantity NUMERIC(19,6) NOT NULL DEFAULT 0 CHECK(quantity >= 0),
 PRIMARY KEY(business_id,location_id,product_id),
 FOREIGN KEY(business_id,location_id) REFERENCES commerce_stock_locations(business_id,id),
 FOREIGN KEY(business_id,product_id) REFERENCES commerce_products(business_id,id)
);
CREATE TABLE commerce_stock_movements (
 id UUID PRIMARY KEY,
 business_id UUID NOT NULL,
 location_id UUID NOT NULL,
 product_id UUID NOT NULL,
 movement_type VARCHAR(30) NOT NULL CHECK(movement_type IN ('OPENING_STOCK')),
 quantity_delta NUMERIC(19,6) NOT NULL CHECK(quantity_delta > 0),
 request_id UUID NOT NULL,
 created_by UUID NOT NULL REFERENCES users(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(business_id,request_id),
 FOREIGN KEY(business_id,location_id) REFERENCES commerce_stock_locations(business_id,id),
 FOREIGN KEY(business_id,product_id) REFERENCES commerce_products(business_id,id)
);
CREATE INDEX commerce_stock_movement_history ON commerce_stock_movements(business_id,location_id,product_id,created_at);
-- Opening entries are immutable. Future adjustment/transfer events need their own approved workflow.
CREATE OR REPLACE FUNCTION commerce_prevent_stock_movement_mutation() RETURNS trigger AS $$
BEGIN
 RAISE EXCEPTION 'Commerce stock movements cannot be modified or deleted';
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER commerce_immutable_stock_movements BEFORE UPDATE OR DELETE ON commerce_stock_movements
FOR EACH ROW EXECUTE FUNCTION commerce_prevent_stock_movement_mutation();
