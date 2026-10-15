-- Additional staff roles. Keep commerce_memberships.role as the existing primary role.
-- OWNER is never delegated through staff role assignments.
CREATE TABLE commerce_membership_roles (
  business_id UUID NOT NULL,
  user_id UUID NOT NULL,
  role VARCHAR(20) NOT NULL CHECK (role IN ('MANAGER','CASHIER','STOREKEEPER','ACCOUNTANT','VIEWER')),
  PRIMARY KEY(business_id,user_id,role),
  FOREIGN KEY (business_id,user_id) REFERENCES commerce_memberships(business_id,user_id) ON DELETE CASCADE
);
INSERT INTO commerce_membership_roles(business_id,user_id,role)
 SELECT business_id,user_id,role FROM commerce_memberships WHERE role <> 'OWNER';
