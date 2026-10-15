-- V39 has already been exercised by Flyway. Correct its deferred trigger additively.
CREATE OR REPLACE FUNCTION commerce_check_balanced_journal() RETURNS trigger AS $$
DECLARE target UUID;
BEGIN
 -- PL/pgSQL resolves every field in a CASE expression against the current record.
 -- Separate statements keep each table's distinct record shape valid.
 IF TG_TABLE_NAME='commerce_journals' THEN
  target := NEW.id;
 ELSE
  target := NEW.journal_id;
 END IF;
 IF EXISTS(SELECT 1 FROM commerce_journal_lines WHERE journal_id=target GROUP BY branch_id HAVING SUM(debit)<>SUM(credit)) THEN
  RAISE EXCEPTION 'Commerce journal must balance within every branch';
 END IF;
 RETURN NULL;
END;
$$ LANGUAGE plpgsql;
