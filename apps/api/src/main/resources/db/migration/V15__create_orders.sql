CREATE TABLE orders (
 id UUID PRIMARY KEY,hotel_id UUID NOT NULL REFERENCES hotels(id),branch_id UUID NOT NULL,created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,version BIGINT NOT NULL DEFAULT 0,
 customer_id UUID,folio_id UUID,server_user_id UUID NOT NULL REFERENCES users(id),destination VARCHAR(20) NOT NULL CHECK(destination IN ('KITCHEN','BAR','SERVICE')),
 status VARCHAR(20) NOT NULL CHECK(status IN ('DRAFT','SENT','PREPARING','READY','SERVED','VOIDED')),subtotal NUMERIC(19,4) NOT NULL CHECK(subtotal>=0),tax NUMERIC(19,4) NOT NULL CHECK(tax>=0),discount NUMERIC(19,4) NOT NULL CHECK(discount>=0),total NUMERIC(19,4) NOT NULL CHECK(total>=0),
 FOREIGN KEY(branch_id,hotel_id) REFERENCES branches(id,hotel_id),FOREIGN KEY(customer_id,hotel_id) REFERENCES customers(id,hotel_id),FOREIGN KEY(folio_id,hotel_id,branch_id) REFERENCES folios(id,hotel_id,branch_id),UNIQUE(id,hotel_id,branch_id)
);
CREATE TABLE order_items (
 id UUID PRIMARY KEY,hotel_id UUID NOT NULL,branch_id UUID NOT NULL,order_id UUID NOT NULL,product_id UUID NOT NULL,product_name VARCHAR(200) NOT NULL,quantity NUMERIC(19,4) NOT NULL CHECK(quantity>0),unit_price NUMERIC(19,4) NOT NULL CHECK(unit_price>=0),tax_rate NUMERIC(19,4) NOT NULL CHECK(tax_rate>=0),destination VARCHAR(20) NOT NULL CHECK(destination IN ('KITCHEN','BAR','SERVICE')),
 FOREIGN KEY(order_id,hotel_id,branch_id) REFERENCES orders(id,hotel_id,branch_id),FOREIGN KEY(product_id,hotel_id) REFERENCES products(id,hotel_id),UNIQUE(id,hotel_id,branch_id)
);
CREATE INDEX idx_orders_scope ON orders(hotel_id,branch_id,status,created_at);
