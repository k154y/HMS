-- Reservation advance / prepayment support.
--
-- collection_scope keeps its existing accounting meaning:
--   FOOD
--   ROOM
--
-- A reservation advance is money collected toward future accommodation,
-- therefore its collection_scope remains ROOM.
--
-- payment_purpose describes WHY the payment was received:
--   SETTLEMENT
--   RESERVATION_ADVANCE
--
-- reservation_id provides an explicit, tenant-safe link between an
-- advance payment and the reservation it belongs to.

ALTER TABLE payments
    ADD COLUMN payment_purpose VARCHAR(30)
        NOT NULL
        DEFAULT 'SETTLEMENT';

ALTER TABLE payments
    ADD COLUMN reservation_id UUID;


ALTER TABLE payments
    ADD CONSTRAINT payments_payment_purpose_check
    CHECK (
        payment_purpose IN (
            'SETTLEMENT',
            'RESERVATION_ADVANCE'
        )
    );


-- Normal settlement payments are not reservation advances.
--
-- Reservation advances must:
--   1. reference a reservation;
--   2. be allocated to ROOM.
ALTER TABLE payments
    ADD CONSTRAINT payments_reservation_advance_shape_check
    CHECK (
        (
            payment_purpose = 'SETTLEMENT'
            AND reservation_id IS NULL
        )
        OR
        (
            payment_purpose = 'RESERVATION_ADVANCE'
            AND reservation_id IS NOT NULL
            AND collection_scope = 'ROOM'
        )
    );


ALTER TABLE payments
    ADD CONSTRAINT fk_payments_reservation
    FOREIGN KEY (
        reservation_id,
        hotel_id,
        branch_id
    )
    REFERENCES reservations (
        id,
        hotel_id,
        branch_id
    );


CREATE INDEX idx_payments_reservation_advance
    ON payments (
        hotel_id,
        branch_id,
        reservation_id,
        status,
        created_at
    )
    WHERE payment_purpose = 'RESERVATION_ADVANCE';
