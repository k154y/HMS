CREATE TABLE customers (
 id UUID PRIMARY KEY, hotel_id UUID NOT NULL REFERENCES hotels(id), created_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL DEFAULT 0,
 code VARCHAR(100) NOT NULL, kind VARCHAR(30) NOT NULL CHECK(kind IN ('INDIVIDUAL','COMPANY','TOUR_AGENCY','NGO','GOVERNMENT','WALK_IN')),
 name VARCHAR(200) NOT NULL, email VARCHAR(255), phone VARCHAR(50), address TEXT, tax_number VARCHAR(100), active BOOLEAN NOT NULL,
 UNIQUE(hotel_id,code), UNIQUE(id,hotel_id)
);
CREATE TABLE guests (
 id UUID PRIMARY KEY, hotel_id UUID NOT NULL REFERENCES hotels(id), created_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL DEFAULT 0,
 full_name VARCHAR(200) NOT NULL, date_of_birth DATE, nationality VARCHAR(3), phone VARCHAR(50),
 UNIQUE(id,hotel_id)
);
CREATE INDEX idx_guests_hotel ON guests(hotel_id,created_at,id);
CREATE TABLE room_types (
 id UUID PRIMARY KEY, hotel_id UUID NOT NULL REFERENCES hotels(id), branch_id UUID NOT NULL,
 created_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL DEFAULT 0,
 code VARCHAR(100) NOT NULL, name VARCHAR(200) NOT NULL, description TEXT,
 standard_occupancy INTEGER NOT NULL CHECK(standard_occupancy>0),
 max_adults INTEGER NOT NULL CHECK(max_adults>0), max_children INTEGER NOT NULL CHECK(max_children>=0),
 default_rate NUMERIC(19,4) NOT NULL CHECK(default_rate>=0),
 CHECK(standard_occupancy<=max_adults+max_children),
 FOREIGN KEY(branch_id,hotel_id) REFERENCES branches(id,hotel_id),
 UNIQUE(hotel_id,branch_id,code), UNIQUE(id,hotel_id,branch_id)
);
CREATE TABLE rooms (
 id UUID PRIMARY KEY, hotel_id UUID NOT NULL REFERENCES hotels(id), branch_id UUID NOT NULL,
 created_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL DEFAULT 0,
 room_type_id UUID NOT NULL, code VARCHAR(100) NOT NULL, floor VARCHAR(50), beds INTEGER NOT NULL CHECK(beds>0),
 bed_type VARCHAR(100) NOT NULL, bed_dimensions VARCHAR(100),
 adults INTEGER NOT NULL CHECK(adults>0), children INTEGER NOT NULL CHECK(children>=0),
 active BOOLEAN NOT NULL,
 housekeeping VARCHAR(30) NOT NULL CHECK(housekeeping IN ('CLEAN','DIRTY','CLEANING','INSPECTED')),
 operational VARCHAR(30) NOT NULL CHECK(operational IN ('AVAILABLE','MAINTENANCE','OUT_OF_SERVICE')),
 FOREIGN KEY(branch_id,hotel_id) REFERENCES branches(id,hotel_id),
 FOREIGN KEY(room_type_id,hotel_id,branch_id) REFERENCES room_types(id,hotel_id,branch_id),
 UNIQUE(hotel_id,branch_id,code), UNIQUE(id,hotel_id,branch_id)
);
CREATE INDEX idx_rooms_type ON rooms(room_type_id);
CREATE INDEX idx_rooms_scope_state ON rooms(hotel_id,branch_id,active,operational);
