package com.hotelmanagement.hms.nonresident.service;

import com.hotelmanagement.hms.audit.service.AuditService;
import com.hotelmanagement.hms.customer.model.Customer;
import com.hotelmanagement.hms.customer.model.CustomerKind;
import com.hotelmanagement.hms.customer.repository.CustomerRepository;
import com.hotelmanagement.hms.folio.model.FolioStatus;
import com.hotelmanagement.hms.folio.service.FolioService;
import com.hotelmanagement.hms.nonresident.dto.NonResidentBillActionRequest;
import com.hotelmanagement.hms.nonresident.dto.NonResidentBillRequest;
import com.hotelmanagement.hms.nonresident.dto.NonResidentBillResponse;
import com.hotelmanagement.hms.nonresident.model.NonResidentBillStatus;
import com.hotelmanagement.hms.nonresident.model.NonResidentBillType;
import com.hotelmanagement.hms.order.service.OrderService;
import com.hotelmanagement.hms.shared.service.OperationScope;
import com.hotelmanagement.hms.shared.web.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.hotelmanagement.hms.identity.authorization.model.PermissionCode.NON_RESIDENT_BILL_CANCEL;
import static com.hotelmanagement.hms.identity.authorization.model.PermissionCode.NON_RESIDENT_BILL_CREATE;
import static com.hotelmanagement.hms.identity.authorization.model.PermissionCode.NON_RESIDENT_BILL_VIEW;
import static com.hotelmanagement.hms.identity.authorization.model.PermissionCode.NON_RESIDENT_BILL_VOID;

@Service
@Transactional
public class NonResidentBillService {

    private static final String WALK_IN_CODE =
            "SYSTEM-WALK-IN";

    private static final String WALK_IN_NAME =
            "Walk-in customer";

    private final JdbcTemplate db;
    private final OperationScope scope;
    private final FolioService folios;
    private final CustomerRepository customers;
    private final OrderService orders;
    private final AuditService audit;

    public NonResidentBillService(
            JdbcTemplate db,
            OperationScope scope,
            FolioService folios,
            CustomerRepository customers,
            OrderService orders,
            AuditService audit) {

        this.db = db;
        this.scope = scope;
        this.folios = folios;
        this.customers = customers;
        this.orders = orders;
        this.audit = audit;
    }

    /**
     * Opens a new non-resident bill.
     *
     * Every bill gets its own dedicated folio.
     *
     * We intentionally use FolioService.open(...)
     * instead of FolioService.forCustomer(...).
     *
     * This prevents a restaurant, bar, laundry or other
     * non-resident bill from being attached accidentally
     * to a hotel guest's room folio.
     */
    public NonResidentBillResponse create(
            UUID hotel,
            UUID branch,
            NonResidentBillRequest request) {

        UUID actor =
                scope.branch(
                        hotel,
                        branch,
                        NON_RESIDENT_BILL_CREATE
                );

        UUID customerId =
                request.customerId() == null
                        ? walkInCustomer(
                                hotel,
                                branch,
                                actor
                        )
                        : requireCustomer(
                                hotel,
                                request.customerId()
                        );

        /*
         * Every non-resident bill receives a dedicated
         * accounting folio.
         */
        var folio =
                folios.open(
                        hotel,
                        branch,
                        customerId,
                        actor
                );

        UUID billId =
                UUID.randomUUID();

        String reference =
                reference(
                        billId
                );

        String tableReference =
                clean(
                        request.tableReference()
                );

        String notes =
                clean(
                        request.notes()
                );

        db.update(
                """
                insert into non_resident_bills(
                    id,
                    hotel_id,
                    branch_id,
                    folio_id,
                    customer_id,
                    reference,
                    bill_type,
                    table_reference,
                    notes,
                    created_by
                )
                values(
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
                )
                """,
                billId,
                hotel,
                branch,
                folio.getId(),
                customerId,
                reference,
                request.billType().name(),
                tableReference,
                notes,
                actor
        );

        audit.record(
                hotel,
                branch,
                actor,
                "NON_RESIDENT_BILL_CREATED",
                "NON_RESIDENT_BILL",
                billId
        );

        return detailInternal(
                hotel,
                branch,
                billId
        );
    }

    /**
     * Returns the latest non-resident bills for the branch.
     */
    @Transactional(readOnly = true)
    public List<NonResidentBillResponse> list(
            UUID hotel,
            UUID branch) {

        scope.branch(
                hotel,
                branch,
                NON_RESIDENT_BILL_VIEW
        );

        return db.query(
                billQuery()
                        + """
                          where b.hotel_id = ?
                            and b.branch_id = ?
                          order by
                              b.created_at desc,
                              b.id
                          limit 500
                          """,
                this::map,
                hotel,
                branch
        );
    }

    /**
     * Returns one non-resident bill.
     */
    @Transactional(readOnly = true)
    public NonResidentBillResponse get(
            UUID hotel,
            UUID branch,
            UUID id) {

        scope.branch(
                hotel,
                branch,
                NON_RESIDENT_BILL_VIEW
        );

        return detailInternal(
                hotel,
                branch,
                id
        );
    }

    /**
     * Cancels an unused non-resident bill.
     *
     * A cancellation is allowed only before financial
     * activity has occurred.
     *
     * Draft orders are voided through OrderService so
     * we preserve an audit trail instead of deleting them.
     */
    public NonResidentBillResponse cancel(
            UUID hotel,
            UUID branch,
            UUID id,
            NonResidentBillActionRequest request) {

        UUID actor =
                scope.branch(
                        hotel,
                        branch,
                        NON_RESIDENT_BILL_CANCEL
                );

        Map<String, Object> bill =
                lockBill(
                        hotel,
                        branch,
                        id
                );

        ensureActive(
                bill
        );

        UUID folioId =
                (UUID) bill.get(
                        "folio_id"
                );

        List<Map<String, Object>> existingOrders =
                db.queryForList(
                        """
                        select
                            id,
                            status
                        from orders
                        where hotel_id = ?
                          and branch_id = ?
                          and folio_id = ?
                        order by
                            created_at,
                            id
                        """,
                        hotel,
                        branch,
                        folioId
                );

        /*
         * Only DRAFT or already VOIDED orders can exist
         * on a bill that is being cancelled.
         */
        for (Map<String, Object> order
                : existingOrders) {

            String status =
                    String.valueOf(
                            order.get(
                                    "status"
                            )
                    );

            if (!"DRAFT".equals(
                    status
            )
                    && !"VOIDED".equals(
                    status
            )) {

                throw new ApiException(
                        409,
                        "NON_RESIDENT_BILL_HAS_ACTIVITY",
                        "A bill with sent or prepared orders cannot be cancelled. Void the bill instead."
                );
            }
        }

        Integer entries =
                db.queryForObject(
                        """
                        select count(*)
                        from folio_entries
                        where hotel_id = ?
                          and branch_id = ?
                          and folio_id = ?
                        """,
                        Integer.class,
                        hotel,
                        branch,
                        folioId
                );

        if (entries != null
                && entries > 0) {

            throw new ApiException(
                    409,
                    "NON_RESIDENT_BILL_HAS_FINANCIAL_ACTIVITY",
                    "A bill with financial activity cannot be cancelled. Void the bill instead."
            );
        }

        /*
         * Preserve draft-order history by voiding instead
         * of deleting.
         */
        for (Map<String, Object> order
                : existingOrders) {

            if ("DRAFT".equals(
                    String.valueOf(
                            order.get(
                                    "status"
                            )
                    )
            )) {

                orders.voidOrder(
                        hotel,
                        branch,
                        (UUID) order.get(
                                "id"
                        )
                );
            }
        }

        OffsetDateTime now =
                OffsetDateTime.now(
                        ZoneOffset.UTC
                );

        db.update(
                """
                update non_resident_bills
                set
                    cancelled_at = ?,
                    cancelled_by = ?,
                    cancellation_reason = ?,
                    updated_at = current_timestamp
                where id = ?
                  and hotel_id = ?
                  and branch_id = ?
                """,
                now,
                actor,
                request.reason().trim(),
                id,
                hotel,
                branch
        );

        /*
         * An unused cancelled bill has a zero balance.
         * Close its folio so future financial entries
         * cannot accidentally be posted to it.
         */
        folios.closeSettled(
                hotel,
                branch,
                folioId
        );

        audit.record(
                hotel,
                branch,
                actor,
                "NON_RESIDENT_BILL_CANCELLED",
                "NON_RESIDENT_BILL",
                id
        );

        return detailInternal(
                hotel,
                branch,
                id
        );
    }

    /**
     * Voids an active bill whose financial activity
     * comes from orders.
     *
     * OrderService performs:
     *
     * - stock reversal
     * - folio reversal
     * - order status update
     *
     * Payments must first be resolved through the payment
     * workflow.
     *
     * Credit bills must first be resolved through the
     * credit workflow.
     */
    public NonResidentBillResponse voidBill(
            UUID hotel,
            UUID branch,
            UUID id,
            NonResidentBillActionRequest request) {

        UUID actor =
                scope.branch(
                        hotel,
                        branch,
                        NON_RESIDENT_BILL_VOID
                );

        Map<String, Object> bill =
                lockBill(
                        hotel,
                        branch,
                        id
                );

        ensureActive(
                bill
        );

        UUID folioId =
                (UUID) bill.get(
                        "folio_id"
                );

        String folioStatus =
                db.queryForObject(
                        """
                        select status
                        from folios
                        where id = ?
                          and hotel_id = ?
                          and branch_id = ?
                        """,
                        String.class,
                        folioId,
                        hotel,
                        branch
                );

        if (FolioStatus.CREDIT
                .name()
                .equals(
                        folioStatus
                )) {

            throw new ApiException(
                    409,
                    "NON_RESIDENT_CREDIT_BILL",
                    "Resolve the credit balance before voiding this bill."
            );
        }

        Integer paymentActivity =
                db.queryForObject(
                        """
                        select count(*)
                        from folio_entries
                        where hotel_id = ?
                          and branch_id = ?
                          and folio_id = ?
                          and kind in (
                              'PAYMENT',
                              'REFUND'
                          )
                        """,
                        Integer.class,
                        hotel,
                        branch,
                        folioId
                );

        if (paymentActivity != null
                && paymentActivity > 0) {

            throw new ApiException(
                    409,
                    "NON_RESIDENT_BILL_HAS_PAYMENTS",
                    "Refund or resolve recorded payments before voiding this bill."
            );
        }

        /*
         * At this stage the automatic void workflow
         * supports charges generated by orders.
         *
         * Manual charges must be reversed through their
         * originating workflow first.
         */
        Integer unsupportedEntries =
                db.queryForObject(
                        """
                        select count(*)
                        from folio_entries
                        where hotel_id = ?
                          and branch_id = ?
                          and folio_id = ?
                          and kind not in (
                              'ORDER',
                              'REVERSAL'
                          )
                        """,
                        Integer.class,
                        hotel,
                        branch,
                        folioId
                );

        if (unsupportedEntries != null
                && unsupportedEntries > 0) {

            throw new ApiException(
                    409,
                    "NON_RESIDENT_BILL_HAS_OTHER_CHARGES",
                    "Reverse non-order charges before voiding this bill."
            );
        }

        List<UUID> orderIds =
                db.query(
                        """
                        select id
                        from orders
                        where hotel_id = ?
                          and branch_id = ?
                          and folio_id = ?
                          and status <> 'VOIDED'
                        order by
                            created_at,
                            id
                        """,
                        (
                                result,
                                rowNumber
                        ) ->
                                result.getObject(
                                        "id",
                                        UUID.class
                                ),
                        hotel,
                        branch,
                        folioId
                );

        for (UUID orderId
                : orderIds) {

            orders.voidOrder(
                    hotel,
                    branch,
                    orderId
            );
        }

        BigDecimal remaining =
                folios.balance(
                        hotel,
                        branch,
                        folioId
                );

        if (remaining.signum()
                != 0) {

            throw new ApiException(
                    409,
                    "NON_RESIDENT_BILL_NOT_SETTLED",
                    "The bill still has an outstanding balance after its orders were reversed."
            );
        }

        OffsetDateTime now =
                OffsetDateTime.now(
                        ZoneOffset.UTC
                );

        db.update(
                """
                update non_resident_bills
                set
                    voided_at = ?,
                    voided_by = ?,
                    void_reason = ?,
                    updated_at = current_timestamp
                where id = ?
                  and hotel_id = ?
                  and branch_id = ?
                """,
                now,
                actor,
                request.reason().trim(),
                id,
                hotel,
                branch
        );

        folios.closeSettled(
                hotel,
                branch,
                folioId
        );

        audit.record(
                hotel,
                branch,
                actor,
                "NON_RESIDENT_BILL_VOIDED",
                "NON_RESIDENT_BILL",
                id
        );

        return detailInternal(
                hotel,
                branch,
                id
        );
    }

    /**
     * Finds or creates the hotel's shared anonymous
     * Walk-in customer.
     *
     * The customer master may be reused, but every
     * non-resident bill still receives a separate folio.
     *
     * The hotel row is locked before checking/creating
     * SYSTEM-WALK-IN. This prevents two simultaneous
     * first walk-in requests from creating duplicate
     * customer records.
     */
    private UUID walkInCustomer(
            UUID hotel,
            UUID branch,
            UUID actor) {

        List<UUID> lockedHotel =
                db.query(
                        """
                        select id
                        from hotels
                        where id = ?
                        for update
                        """,
                        (
                                result,
                                rowNumber
                        ) ->
                                result.getObject(
                                        "id",
                                        UUID.class
                                ),
                        hotel
                );

        if (lockedHotel.isEmpty()) {

            throw ApiException.notFound();
        }

        List<UUID> existing =
                db.query(
                        """
                        select id
                        from customers
                        where hotel_id = ?
                          and code = ?
                        limit 1
                        """,
                        (
                                result,
                                rowNumber
                        ) ->
                                result.getObject(
                                        "id",
                                        UUID.class
                                ),
                        hotel,
                        WALK_IN_CODE
                );

        if (!existing.isEmpty()) {

            Customer customer =
                    customers
                            .findByIdAndHotelId(
                                    existing.getFirst(),
                                    hotel
                            )
                            .orElseThrow(
                                    ApiException::notFound
                            );

            if (!customer.getActive()) {

                throw new ApiException(
                        409,
                        "WALK_IN_CUSTOMER_INACTIVE",
                        "The system walk-in customer is inactive."
                );
            }

            if (customer.getKind()
                    != CustomerKind.WALK_IN) {

                throw new ApiException(
                        409,
                        "WALK_IN_CUSTOMER_INVALID",
                        "The reserved system walk-in customer code is already used by another customer type."
                );
            }

            return customer.getId();
        }

        Customer created =
                customers.saveAndFlush(
                        Customer.create(
                                hotel,
                                WALK_IN_CODE,
                                CustomerKind.WALK_IN,
                                WALK_IN_NAME,
                                null,
                                null,
                                null,
                                null,
                                true
                        )
                );

        audit.record(
                hotel,
                branch,
                actor,
                "SYSTEM_WALK_IN_CUSTOMER_CREATED",
                "CUSTOMER",
                created.getId()
        );

        return created.getId();
    }

    /**
     * Verifies that a selected existing customer belongs
     * to this hotel and is active.
     */
    private UUID requireCustomer(
            UUID hotel,
            UUID customerId) {

        Customer customer =
                customers
                        .findByIdAndHotelId(
                                customerId,
                                hotel
                        )
                        .filter(
                                Customer::getActive
                        )
                        .orElseThrow(
                                ApiException::notFound
                        );

        return customer.getId();
    }

    /**
     * Locks one bill before a state-changing operation.
     */
    private Map<String, Object> lockBill(
            UUID hotel,
            UUID branch,
            UUID id) {

        List<Map<String, Object>> rows =
                db.queryForList(
                        """
                        select *
                        from non_resident_bills
                        where id = ?
                          and hotel_id = ?
                          and branch_id = ?
                        for update
                        """,
                        id,
                        hotel,
                        branch
                );

        if (rows.isEmpty()) {

            throw ApiException.notFound();
        }

        return rows.getFirst();
    }

    /**
     * Prevents operations on already cancelled/voided bills.
     */
    private void ensureActive(
            Map<String, Object> bill) {

        if (bill.get(
                "cancelled_at"
        ) != null) {

            throw new ApiException(
                    409,
                    "NON_RESIDENT_BILL_CANCELLED",
                    "The bill is already cancelled."
            );
        }

        if (bill.get(
                "voided_at"
        ) != null) {

            throw new ApiException(
                    409,
                    "NON_RESIDENT_BILL_VOIDED",
                    "The bill is already voided."
            );
        }
    }

    /**
     * Internal bill lookup used after state-changing
     * operations where permission has already been checked.
     */
    private NonResidentBillResponse detailInternal(
            UUID hotel,
            UUID branch,
            UUID id) {

        List<NonResidentBillResponse> rows =
                db.query(
                        billQuery()
                                + """
                                  where b.id = ?
                                    and b.hotel_id = ?
                                    and b.branch_id = ?
                                  """,
                        this::map,
                        id,
                        hotel,
                        branch
                );

        if (rows.isEmpty()) {

            throw ApiException.notFound();
        }

        return rows.getFirst();
    }

    /**
     * Financial status comes from the folio ledger.
     *
     * The lateral query calculates one financial summary
     * independently for every non-resident bill.
     */
    private String billQuery() {

        return """
               select
                   b.id,
                   b.folio_id,
                   b.customer_id,
                   c.name as customer_name,
                   c.kind as customer_kind,
                   b.reference,
                   b.bill_type,
                   b.table_reference,
                   b.notes,
                   b.created_at,
                   b.cancelled_at,
                   b.voided_at,
                   f.status as folio_status,

                   ledger.bill_total,
                   ledger.paid,
                   ledger.balance

               from non_resident_bills b

               join customers c
                 on c.id = b.customer_id
                and c.hotel_id = b.hotel_id

               join folios f
                 on f.id = b.folio_id
                and f.hotel_id = b.hotel_id
                and f.branch_id = b.branch_id

               left join lateral (
                   select
                       coalesce(
                           sum(e.amount)
                               filter (
                                   where e.kind not in (
                                       'PAYMENT',
                                       'REFUND'
                                   )
                               ),
                           0
                       ) as bill_total,

                       (
                           coalesce(
                               -sum(e.amount)
                                   filter (
                                       where e.kind = 'PAYMENT'
                                   ),
                               0
                           )
                           -
                           coalesce(
                               sum(e.amount)
                                   filter (
                                       where e.kind = 'REFUND'
                                   ),
                               0
                           )
                       ) as paid,

                       coalesce(
                           sum(e.amount),
                           0
                       ) as balance

                   from folio_entries e

                   where e.folio_id = b.folio_id
                     and e.hotel_id = b.hotel_id
                     and e.branch_id = b.branch_id
               ) ledger
                 on true

               """;
    }

    /**
     * Maps the database projection into the API response.
     */
    private NonResidentBillResponse map(
            ResultSet result,
            int rowNumber)
            throws SQLException {

        BigDecimal total =
                result.getBigDecimal(
                        "bill_total"
                );

        BigDecimal paid =
                result.getBigDecimal(
                        "paid"
                );

        BigDecimal balance =
                result.getBigDecimal(
                        "balance"
                );

        /*
         * Defensive fallback. The SQL currently guarantees
         * these values with COALESCE, but keeping the mapper
         * safe avoids accidental NullPointerExceptions if the
         * query changes later.
         */
        if (total == null) {
            total = BigDecimal.ZERO;
        }

        if (paid == null) {
            paid = BigDecimal.ZERO;
        }

        if (balance == null) {
            balance = BigDecimal.ZERO;
        }

        NonResidentBillStatus status =
                status(
                        result,
                        total,
                        paid,
                        balance
                );

        return new NonResidentBillResponse(
                result.getObject(
                        "id",
                        UUID.class
                ),

                result.getObject(
                        "folio_id",
                        UUID.class
                ),

                result.getObject(
                        "customer_id",
                        UUID.class
                ),

                result.getString(
                        "customer_name"
                ),

                CustomerKind.valueOf(
                        result.getString(
                                "customer_kind"
                        )
                ),

                result.getString(
                        "reference"
                ),

                NonResidentBillType.valueOf(
                        result.getString(
                                "bill_type"
                        )
                ),

                result.getString(
                        "table_reference"
                ),

                result.getString(
                        "notes"
                ),

                status,

                total,

                paid,

                balance,

                result.getObject(
                        "created_at",
                        OffsetDateTime.class
                )
        );
    }

    /**
     * Calculates the business status.
     *
     * CANCELLED and VOIDED come from the bill.
     *
     * CREDIT comes from the folio.
     *
     * OPEN, PARTIAL and PAID are derived from ledger
     * amounts instead of being independently stored.
     */
    private NonResidentBillStatus status(
            ResultSet result,
            BigDecimal total,
            BigDecimal paid,
            BigDecimal balance)
            throws SQLException {

        if (result.getObject(
                "cancelled_at"
        ) != null) {

            return NonResidentBillStatus.CANCELLED;
        }

        if (result.getObject(
                "voided_at"
        ) != null) {

            return NonResidentBillStatus.VOIDED;
        }

        if (FolioStatus.CREDIT
                .name()
                .equals(
                        result.getString(
                                "folio_status"
                        )
                )) {

            return NonResidentBillStatus.CREDIT;
        }

        if (total.signum() != 0
                && balance.signum() == 0) {

            return NonResidentBillStatus.PAID;
        }

        if (paid.signum() > 0
                && balance.signum() > 0) {

            return NonResidentBillStatus.PARTIAL;
        }

        return NonResidentBillStatus.OPEN;
    }

    /**
     * Produces a human-readable bill reference such as:
     *
     * NRB-20260917-A1B2C3D4
     */
    private String reference(
            UUID id) {

        String date =
                LocalDate.now(
                        ZoneOffset.UTC
                ).format(
                        DateTimeFormatter.BASIC_ISO_DATE
                );

        return (
                "NRB-"
                        + date
                        + "-"
                        + id.toString()
                        .substring(
                                0,
                                8
                        )
                        .toUpperCase(
                                Locale.ROOT
                        )
        );
    }

    /**
     * Normalizes optional text fields.
     */
    private String clean(
            String value) {

        if (value == null) {
            return null;
        }

        String cleaned =
                value.trim();

        return cleaned.isEmpty()
                ? null
                : cleaned;
    }
}