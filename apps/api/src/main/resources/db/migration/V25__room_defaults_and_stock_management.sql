ALTER TABLE room_types ADD COLUMN bed_type VARCHAR(100) NOT NULL DEFAULT 'DOUBLE';
ALTER TABLE room_types ADD COLUMN bed_dimensions VARCHAR(100) NOT NULL DEFAULT '160 x 200 cm';
ALTER TABLE room_types ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE rooms ADD COLUMN nightly_rate NUMERIC(19,4) CHECK(nightly_rate>=0);
INSERT INTO role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM roles r CROSS JOIN permissions p
WHERE r.system_defined AND r.code='ACCOUNTANT' AND p.code IN ('PRODUCT_MANAGE','INVENTORY_ADJUST') ON CONFLICT DO NOTHING;
