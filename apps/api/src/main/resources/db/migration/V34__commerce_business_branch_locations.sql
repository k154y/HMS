-- Additive standalone-commerce foundation. Existing hotel tables and stock remain authoritative and unchanged.
CREATE TABLE commerce_businesses (
 id UUID PRIMARY KEY,
 name VARCHAR(200) NOT NULL,
 base_currency VARCHAR(3) NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','SUSPENDED')),
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT commerce_business_currency_format CHECK (base_currency ~ '^[A-Z]{3}$')
);
CREATE TABLE commerce_memberships (
 business_id UUID NOT NULL REFERENCES commerce_businesses(id),
 user_id UUID NOT NULL REFERENCES users(id),
 role VARCHAR(20) NOT NULL CHECK (role IN ('OWNER','MANAGER','VIEWER')),
 status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','DISABLED')),
 PRIMARY KEY (business_id,user_id)
);
CREATE INDEX commerce_memberships_by_user ON commerce_memberships(user_id,business_id);
CREATE TABLE commerce_branches (
 id UUID PRIMARY KEY,
 business_id UUID NOT NULL REFERENCES commerce_businesses(id),
 name VARCHAR(200) NOT NULL,
 active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE (business_id,id), UNIQUE (business_id,name)
);
CREATE TABLE commerce_stock_locations (
 id UUID PRIMARY KEY,
 business_id UUID NOT NULL,
 branch_id UUID NOT NULL,
 name VARCHAR(200) NOT NULL,
 location_type VARCHAR(24) NOT NULL CHECK(location_type IN ('SHOP_FLOOR','WAREHOUSE','STOREROOM','RETURNS','DAMAGED')),
 active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY (business_id,branch_id) REFERENCES commerce_branches(business_id,id),
 UNIQUE (business_id,branch_id,id), UNIQUE (branch_id,name)
);
-- Explicit entitlements default to no access; they must be provisioned deliberately.
CREATE TABLE commerce_entitlements (
 business_id UUID NOT NULL REFERENCES commerce_businesses(id),
 module VARCHAR(30) NOT NULL CHECK(module IN ('CORE','INVENTORY','RETAIL','WHOLESALE','DISTRIBUTION','ACCOUNTING')),
 enabled BOOLEAN NOT NULL DEFAULT FALSE,
 PRIMARY KEY(business_id,module)
);
