-- Additive commerce configuration. No hotel data or applied migrations are changed.
ALTER TABLE commerce_memberships DROP CONSTRAINT commerce_memberships_role_check;
ALTER TABLE commerce_memberships ADD CONSTRAINT commerce_memberships_role_check
 CHECK(role IN ('OWNER','MANAGER','CASHIER','STOREKEEPER','ACCOUNTANT','VIEWER'));
CREATE TABLE commerce_branch_access (
 business_id UUID NOT NULL, user_id UUID NOT NULL, branch_id UUID NOT NULL,
 PRIMARY KEY(business_id,user_id,branch_id),
 FOREIGN KEY(business_id,user_id) REFERENCES commerce_memberships(business_id,user_id),
 FOREIGN KEY(business_id,branch_id) REFERENCES commerce_branches(business_id,id)
);
-- Preserve existing manager/viewer access to existing branches. New staff default to no branches.
INSERT INTO commerce_branch_access(business_id,user_id,branch_id)
 SELECT m.business_id,m.user_id,b.id FROM commerce_memberships m
 JOIN commerce_branches b ON b.business_id=m.business_id WHERE m.role <> 'OWNER';
ALTER TABLE commerce_branches ADD COLUMN contact VARCHAR(300);
CREATE TABLE commerce_configuration_audit (
 id UUID PRIMARY KEY, business_id UUID NOT NULL REFERENCES commerce_businesses(id),
 actor_id UUID NOT NULL REFERENCES users(id), action VARCHAR(80) NOT NULL,
 entity_id UUID, detail JSONB NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TRIGGER commerce_configuration_audit_immutable BEFORE UPDATE OR DELETE ON commerce_configuration_audit
 FOR EACH ROW EXECUTE FUNCTION commerce_prevent_stock_movement_mutation();
CREATE TABLE commerce_currencies (
 business_id UUID NOT NULL REFERENCES commerce_businesses(id), currency VARCHAR(3) NOT NULL,
 decimal_places INTEGER NOT NULL CHECK(decimal_places BETWEEN 0 AND 4), active BOOLEAN NOT NULL DEFAULT TRUE,
 PRIMARY KEY(business_id,currency), CHECK(currency ~ '^[A-Z]{3}$')
);
-- Base currency is supplied from commerce_businesses by the service with ISO precision.
-- Do not guess decimal precision for previously provisioned currencies in a data backfill.
CREATE TABLE commerce_exchange_rates (
 id UUID PRIMARY KEY, business_id UUID NOT NULL, currency VARCHAR(3) NOT NULL,
 effective_at TIMESTAMPTZ NOT NULL, rate_to_base NUMERIC(24,10) NOT NULL CHECK(rate_to_base > 0),
 source VARCHAR(200) NOT NULL CHECK(length(trim(source)) > 0),
 created_by UUID NOT NULL REFERENCES users(id), created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(business_id,currency) REFERENCES commerce_currencies(business_id,currency),
 UNIQUE(business_id,currency,effective_at)
);
CREATE TRIGGER commerce_exchange_rates_immutable BEFORE UPDATE OR DELETE ON commerce_exchange_rates
 FOR EACH ROW EXECUTE FUNCTION commerce_prevent_stock_movement_mutation();
