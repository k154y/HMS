ALTER TABLE payments ADD COLUMN collection_scope VARCHAR(10) NOT NULL DEFAULT 'FOOD' CHECK(collection_scope IN ('FOOD','ROOM'));
UPDATE payments p SET collection_scope='ROOM' WHERE EXISTS(SELECT 1 FROM folio_entries e WHERE e.folio_id=p.folio_id AND e.kind='ACCOMMODATION');
