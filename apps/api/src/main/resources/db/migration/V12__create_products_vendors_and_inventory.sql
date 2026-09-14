CREATE TABLE products (
 id UUID PRIMARY KEY,hotel_id UUID NOT NULL REFERENCES hotels(id),created_at TIMESTAMPTZ NOT NULL,version BIGINT NOT NULL DEFAULT 0,
 sku VARCHAR(100) NOT NULL,name VARCHAR(200) NOT NULL,category VARCHAR(100) NOT NULL,
 purchase_unit VARCHAR(30) NOT NULL,selling_unit VARCHAR(30) NOT NULL,stock_unit VARCHAR(30) NOT NULL,
 purchase_factor NUMERIC(19,4) NOT NULL CHECK(purchase_factor>0),selling_factor NUMERIC(19,4) NOT NULL CHECK(selling_factor>0),
 purchase_price NUMERIC(19,4) NOT NULL CHECK(purchase_price>=0),selling_price NUMERIC(19,4) NOT NULL CHECK(selling_price>=0),
 tax_rate NUMERIC(19,4) NOT NULL CHECK(tax_rate>=0 AND tax_rate<=1),
 stock_tracked BOOLEAN NOT NULL,sellable BOOLEAN NOT NULL,purchasable BOOLEAN NOT NULL,active BOOLEAN NOT NULL,
 reorder_level NUMERIC(19,4) NOT NULL CHECK(reorder_level>=0),
 destination VARCHAR(30) NOT NULL CHECK(destination IN ('KITCHEN','BAR','SERVICE')),
 UNIQUE(hotel_id,sku),UNIQUE(id,hotel_id)
);
CREATE TABLE vendors (
 id UUID PRIMARY KEY,hotel_id UUID NOT NULL REFERENCES hotels(id),created_at TIMESTAMPTZ NOT NULL,version BIGINT NOT NULL DEFAULT 0,
 code VARCHAR(100) NOT NULL,name VARCHAR(200) NOT NULL,email VARCHAR(255),phone VARCHAR(50),address TEXT,
 payment_terms_days INTEGER NOT NULL CHECK(payment_terms_days>=0),active BOOLEAN NOT NULL,
 UNIQUE(hotel_id,code),UNIQUE(id,hotel_id)
);
CREATE TABLE stock_movements (
 id UUID PRIMARY KEY,hotel_id UUID NOT NULL,branch_id UUID NOT NULL,created_at TIMESTAMPTZ NOT NULL,version BIGINT NOT NULL DEFAULT 0,
 product_id UUID NOT NULL,quantity NUMERIC(19,4) NOT NULL CHECK(quantity<>0),unit VARCHAR(30) NOT NULL,
 kind VARCHAR(30) NOT NULL CHECK(kind IN ('OPENING','RECEIPT','SALE','TRANSFER_IN','TRANSFER_OUT','ADJUSTMENT','WASTE','REVERSAL')),
 source_id UUID NOT NULL,actor_id UUID NOT NULL REFERENCES users(id),reason VARCHAR(1000) NOT NULL,
 FOREIGN KEY(product_id,hotel_id) REFERENCES products(id,hotel_id),
 FOREIGN KEY(branch_id,hotel_id) REFERENCES branches(id,hotel_id),
 UNIQUE(id,hotel_id,branch_id),UNIQUE(hotel_id,branch_id,product_id,kind,source_id)
);
CREATE INDEX idx_stock_balance ON stock_movements(hotel_id,branch_id,product_id);
CREATE INDEX idx_stock_actor ON stock_movements(actor_id);
CREATE TRIGGER stock_movements_immutable BEFORE UPDATE OR DELETE ON stock_movements FOR EACH ROW EXECUTE FUNCTION hms_reject_history_mutation();
