CREATE EXTENSION IF NOT EXISTS btree_gist;
CREATE TABLE reservations (
 id UUID PRIMARY KEY,hotel_id UUID NOT NULL,branch_id UUID NOT NULL,created_at TIMESTAMPTZ NOT NULL,version BIGINT NOT NULL DEFAULT 0,
 reference VARCHAR(100) NOT NULL,customer_id UUID NOT NULL,folio_id UUID NOT NULL,check_in DATE NOT NULL,check_out DATE NOT NULL,
 status VARCHAR(30) NOT NULL CHECK(status IN ('CONFIRMED','CANCELLED','CHECKED_IN','CHECKED_OUT','NO_SHOW')),
 notes VARCHAR(2000),actor_id UUID NOT NULL REFERENCES users(id),
 CHECK(check_out>check_in),UNIQUE(hotel_id,reference),UNIQUE(id,hotel_id,branch_id),
 FOREIGN KEY(branch_id,hotel_id) REFERENCES branches(id,hotel_id),
 FOREIGN KEY(customer_id,hotel_id) REFERENCES customers(id,hotel_id),
 FOREIGN KEY(folio_id,hotel_id,branch_id) REFERENCES folios(id,hotel_id,branch_id)
);
CREATE INDEX idx_reservations_dates ON reservations(hotel_id,branch_id,check_in,check_out);
CREATE INDEX idx_reservations_customer ON reservations(customer_id);
CREATE INDEX idx_reservations_folio ON reservations(folio_id);
CREATE TABLE reservation_rooms (
 id UUID PRIMARY KEY,hotel_id UUID NOT NULL,branch_id UUID NOT NULL,created_at TIMESTAMPTZ NOT NULL,version BIGINT NOT NULL DEFAULT 0,
 reservation_id UUID NOT NULL,room_id UUID NOT NULL,check_in DATE NOT NULL,check_out DATE NOT NULL,active BOOLEAN NOT NULL,
 adults INTEGER NOT NULL CHECK(adults>0),children INTEGER NOT NULL CHECK(children>=0),nightly_rate NUMERIC(19,4) NOT NULL CHECK(nightly_rate>=0),
 CHECK(check_out>check_in),UNIQUE(id,hotel_id,branch_id),
 FOREIGN KEY(reservation_id,hotel_id,branch_id) REFERENCES reservations(id,hotel_id,branch_id),
 FOREIGN KEY(room_id,hotel_id,branch_id) REFERENCES rooms(id,hotel_id,branch_id),
 CONSTRAINT no_overlapping_room_bookings EXCLUDE USING gist(room_id WITH =,daterange(check_in,check_out,'[)') WITH &&) WHERE(active)
);
CREATE INDEX idx_reservation_rooms_reservation ON reservation_rooms(reservation_id,active);
CREATE TABLE reservation_guests (
 id UUID PRIMARY KEY,hotel_id UUID NOT NULL,branch_id UUID NOT NULL,created_at TIMESTAMPTZ NOT NULL,version BIGINT NOT NULL DEFAULT 0,
 allocation_id UUID NOT NULL,guest_id UUID NOT NULL,
 FOREIGN KEY(allocation_id,hotel_id,branch_id) REFERENCES reservation_rooms(id,hotel_id,branch_id),
 FOREIGN KEY(guest_id,hotel_id) REFERENCES guests(id,hotel_id),
 UNIQUE(allocation_id,guest_id)
);
CREATE INDEX idx_reservation_guests_guest ON reservation_guests(guest_id);
CREATE TABLE stays (
 id UUID PRIMARY KEY,hotel_id UUID NOT NULL,branch_id UUID NOT NULL,created_at TIMESTAMPTZ NOT NULL,version BIGINT NOT NULL DEFAULT 0,
 reservation_id UUID NOT NULL,room_id UUID NOT NULL,folio_id UUID NOT NULL,
 checked_in_at TIMESTAMPTZ NOT NULL,checked_out_at TIMESTAMPTZ,
 checked_in_by UUID NOT NULL REFERENCES users(id),checked_out_by UUID REFERENCES users(id),
 FOREIGN KEY(reservation_id,hotel_id,branch_id) REFERENCES reservations(id,hotel_id,branch_id),
 FOREIGN KEY(room_id,hotel_id,branch_id) REFERENCES rooms(id,hotel_id,branch_id),
 FOREIGN KEY(folio_id,hotel_id,branch_id) REFERENCES folios(id,hotel_id,branch_id),
 CHECK(checked_out_at IS NULL OR checked_out_at>=checked_in_at),
 UNIQUE(reservation_id,room_id)
);
CREATE UNIQUE INDEX uq_active_stay_room ON stays(room_id) WHERE checked_out_at IS NULL;
CREATE INDEX idx_stays_scope ON stays(hotel_id,branch_id,checked_in_at);
CREATE INDEX idx_stays_folio ON stays(folio_id);
CREATE TRIGGER reservations_no_delete BEFORE DELETE ON reservations FOR EACH ROW EXECUTE FUNCTION hms_reject_history_mutation();
CREATE TRIGGER reservation_rooms_no_delete BEFORE DELETE ON reservation_rooms FOR EACH ROW EXECUTE FUNCTION hms_reject_history_mutation();
CREATE TRIGGER reservation_guests_immutable BEFORE UPDATE OR DELETE ON reservation_guests FOR EACH ROW EXECUTE FUNCTION hms_reject_history_mutation();
CREATE TRIGGER stays_no_delete BEFORE DELETE ON stays FOR EACH ROW EXECUTE FUNCTION hms_reject_history_mutation();
