CREATE TABLE maintenance_tickets (
 id UUID PRIMARY KEY,hotel_id UUID NOT NULL REFERENCES hotels(id),branch_id UUID NOT NULL,created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,version BIGINT NOT NULL DEFAULT 0,
 room_id UUID,issue VARCHAR(2000) NOT NULL,priority VARCHAR(20) NOT NULL CHECK(priority IN ('LOW','MEDIUM','HIGH','URGENT')),status VARCHAR(20) NOT NULL CHECK(status IN ('OPEN','IN_PROGRESS','RESOLVED','CANCELLED')),
 assigned_user_id UUID REFERENCES users(id),opened_by UUID NOT NULL REFERENCES users(id),resolved_at TIMESTAMPTZ,
 FOREIGN KEY(branch_id,hotel_id) REFERENCES branches(id,hotel_id),FOREIGN KEY(room_id,hotel_id,branch_id) REFERENCES rooms(id,hotel_id,branch_id),
 UNIQUE(id,hotel_id,branch_id)
);
CREATE INDEX idx_maintenance_scope ON maintenance_tickets(hotel_id,branch_id,status,created_at);
