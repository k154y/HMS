package com.hotelmanagement.hms;

import com.hotelmanagement.hms.customer.dto.CustomerRequest;
import com.hotelmanagement.hms.customer.dto.CustomerResponse;
import com.hotelmanagement.hms.customer.model.CustomerKind;
import com.hotelmanagement.hms.customer.service.CustomerService;

import com.hotelmanagement.hms.payment.dto.CashierShiftRequest;
import com.hotelmanagement.hms.payment.model.PaymentMethod;
import com.hotelmanagement.hms.payment.service.CashierShiftService;
import com.hotelmanagement.hms.payment.service.PaymentService;
import com.hotelmanagement.hms.payment.service.PaymentWorkflow;

import com.hotelmanagement.hms.platform.dto.CreateBranchRequest;
import com.hotelmanagement.hms.platform.dto.CreateHotelRequest;
import com.hotelmanagement.hms.platform.onboarding.dto.OwnerSignupRequest;
import com.hotelmanagement.hms.platform.onboarding.service.OwnerOnboardingService;
import com.hotelmanagement.hms.platform.service.HotelAdministrationService;

import com.hotelmanagement.hms.reservation.dto.ReservationRequest;
import com.hotelmanagement.hms.reservation.service.ReservationService;

import com.hotelmanagement.hms.room.dto.RoomRequest;
import com.hotelmanagement.hms.room.dto.RoomResponse;
import com.hotelmanagement.hms.room.dto.RoomTypeRequest;
import com.hotelmanagement.hms.room.service.RoomService;

import com.hotelmanagement.hms.shared.web.ApiException;
import com.hotelmanagement.hms.stay.service.StayService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


@SpringBootTest(
        properties = {
                "hms.security.jwt.secret=reservation-advance-integration-test-secret-at-least-32-bytes",
                "logging.level.root=WARN",
                "logging.level.com.hotelmanagement.hms=WARN",
                "management.otlp.metrics.export.enabled=false"
        }
)
class ReservationAdvancePaymentTest {

    @Autowired
    OwnerOnboardingService onboarding;

    @Autowired
    HotelAdministrationService hotels;

    @Autowired
    CustomerService customers;

    @Autowired
    RoomService rooms;

    @Autowired
    ReservationService reservations;

    @Autowired
    PaymentWorkflow paymentWorkflow;

    @Autowired
    PaymentService paymentService;

    @Autowired
    CashierShiftService shifts;

    @Autowired
    StayService stays;

    @Autowired
    JdbcTemplate jdbc;


    record Tenant(
            UUID actor,
            UUID hotel,
            UUID branch) {
    }


    @DynamicPropertySource
    static void database(
            DynamicPropertyRegistry registry) {

        IntegrationServices.configure(
                registry
        );
    }


    @AfterEach
    void clearSecurityContext() {

        SecurityContextHolder
                .clearContext();
    }


    /*
     * A 500,000 RWF reservation can receive multiple advances.
     *
     * 100,000 MOBILE_MONEY
     * 200,000 BANK_TRANSFER
     *
     * Total advance = 300,000
     * Remaining     = 200,000
     */
    @Test
    void reservationAdvancesAccumulateAndSummaryTracksPostedPayments() {

        Tenant tenant =
                tenant();

        CustomerResponse customer =
                customer(
                        tenant
                );

        RoomResponse room =
                room(
                        tenant,
                        "ADV-101"
                );

        LocalDate start =
                LocalDate.now()
                        .plusDays(
                                3
                        );

        var reservation =
                reservations.create(
                        tenant.hotel(),
                        tenant.branch(),
                        booking(
                                customer.id(),
                                room.id(),
                                start,
                                start.plusDays(
                                        2
                                ),
                                new BigDecimal(
                                        "250000"
                                )
                        )
                );


        var before =
                reservations.paymentSummary(
                        tenant.hotel(),
                        tenant.branch(),
                        reservation.id()
                );


        assertMoney(
                "500000",
                before.reservationTotal()
        );

        assertMoney(
                "0",
                before.advancePaid()
        );

        assertMoney(
                "500000",
                before.remaining()
        );

        assertEquals(
                "NO_PAYMENT",
                before.paymentStatus()
        );


        openShift(
                tenant
        );


        approveAdvance(
                tenant,
                reservation.id(),
                PaymentMethod.MOBILE_MONEY,
                "100000"
        );


        approveAdvance(
                tenant,
                reservation.id(),
                PaymentMethod.BANK_TRANSFER,
                "200000"
        );


        /*
         * Prepayments appear as negative folio balance until the
         * accommodation charge is posted at check-in.
         */
        assertMoney(
                "-300000",
                folioBalance(
                        tenant,
                        reservation.folioId()
                )
        );


        var summary =
                reservations.paymentSummary(
                        tenant.hotel(),
                        tenant.branch(),
                        reservation.id()
                );


        assertMoney(
                "500000",
                summary.reservationTotal()
        );

        assertMoney(
                "300000",
                summary.advancePaid()
        );

        assertMoney(
                "200000",
                summary.remaining()
        );

        assertEquals(
                "PARTIALLY_PREPAID",
                summary.paymentStatus()
        );


        Integer count =
                jdbc.queryForObject(
                        """
                        select count(*)
                        from payments
                        where hotel_id = ?
                          and branch_id = ?
                          and folio_id = ?
                          and collection_scope = 'ROOM'
                          and payment_purpose = 'RESERVATION_ADVANCE'
                          and status = 'POSTED'
                        """,
                        Integer.class,
                        tenant.hotel(),
                        tenant.branch(),
                        reservation.folioId()
                );


        assertEquals(
                2,
                count
        );


        assertMoney(
                "100000",
                paymentTotal(
                        reservation.folioId(),
                        "MOBILE_MONEY"
                )
        );


        assertMoney(
                "200000",
                paymentTotal(
                        reservation.folioId(),
                        "BANK_TRANSFER"
                )
        );
    }


    /*
     * Two pending approvals may both have been valid when requested.
     *
     * The approval-time reservation lock/revalidation must prevent
     * their combined value from exceeding the reservation.
     */
    @Test
    void approvalTimeRevalidationPreventsPendingAdvancesFromOverpaying() {

        Tenant tenant =
                tenant();

        CustomerResponse customer =
                customer(
                        tenant
                );

        RoomResponse room =
                room(
                        tenant,
                        "ADV-201"
                );

        LocalDate start =
                LocalDate.now()
                        .plusDays(
                                4
                        );


        var reservation =
                reservations.create(
                        tenant.hotel(),
                        tenant.branch(),
                        booking(
                                customer.id(),
                                room.id(),
                                start,
                                start.plusDays(
                                        2
                                ),
                                new BigDecimal(
                                        "250000"
                                )
                        )
                );


        openShift(
                tenant
        );


        Map<String, Object> first =
                requestAdvance(
                        tenant,
                        reservation.id(),
                        PaymentMethod.MOBILE_MONEY,
                        "300000"
                );


        Map<String, Object> second =
                requestAdvance(
                        tenant,
                        reservation.id(),
                        PaymentMethod.BANK_TRANSFER,
                        "300000"
                );


        approve(
                tenant,
                first,
                "Owner verified first reservation advance"
        );


        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () ->
                                approve(
                                        tenant,
                                        second,
                                        "Owner verified second reservation advance"
                                )
                );


        assertEquals(
                "RESERVATION_ADVANCE_EXCEEDS_BALANCE",
                exception.code()
        );


        var summary =
                reservations.paymentSummary(
                        tenant.hotel(),
                        tenant.branch(),
                        reservation.id()
                );


        assertMoney(
                "300000",
                summary.advancePaid()
        );

        assertMoney(
                "200000",
                summary.remaining()
        );


        /*
         * Failed approval must remain pending because the approval
         * transaction is rolled back.
         */
        assertEquals(
                "PENDING",
                jdbc.queryForObject(
                        """
                        select status
                        from payment_approvals
                        where id = ?
                        """,
                        String.class,
                        second.get(
                                "id"
                        )
                )
        );


        assertEquals(
                1,
                jdbc.queryForObject(
                        """
                        select count(*)
                        from payments
                        where folio_id = ?
                          and collection_scope = 'ROOM'
                          and payment_purpose = 'RESERVATION_ADVANCE'
                          and status = 'POSTED'
                        """,
                        Integer.class,
                        reservation.folioId()
                )
        );
    }


    /*
     * Advance:
     *     300,000
     *
     * Check-in accommodation:
     *     500,000
     *
     * Remaining room balance:
     *     200,000
     *
     * A later ROOM payment must only be allowed up to 200,000.
     */
    @Test
    void reservationAdvanceOffsetsAccommodationAfterCheckIn() {

        Tenant tenant =
                tenant();

        CustomerResponse customer =
                customer(
                        tenant
                );

        RoomResponse room =
                room(
                        tenant,
                        "ADV-301"
                );

        LocalDate start =
                LocalDate.now();


        var reservation =
                reservations.create(
                        tenant.hotel(),
                        tenant.branch(),
                        booking(
                                customer.id(),
                                room.id(),
                                start,
                                start.plusDays(
                                        2
                                ),
                                new BigDecimal(
                                        "250000"
                                )
                        )
                );


        openShift(
                tenant
        );


        approveAdvance(
                tenant,
                reservation.id(),
                PaymentMethod.MOBILE_MONEY,
                "300000"
        );


        assertMoney(
                "-300000",
                folioBalance(
                        tenant,
                        reservation.folioId()
                )
        );


        assertEquals(
                1,
                stays.checkIn(
                        tenant.hotel(),
                        tenant.branch(),
                        reservation.id()
                ).size()
        );


        /*
         * Accommodation +500,000 minus advance 300,000.
         */
        assertMoney(
                "200000",
                folioBalance(
                        tenant,
                        reservation.folioId()
                )
        );


        /*
         * 250,000 is greater than the remaining ROOM balance.
         */
        assertThrows(
                IllegalStateException.class,
                () ->
                        requestPayment(
                                tenant,
                                reservation.folioId(),
                                PaymentMethod.CASH,
                                "250000",
                                "ROOM"
                        )
        );


        /*
         * Exact remaining amount is permitted.
         */
        approvePayment(
                tenant,
                reservation.folioId(),
                PaymentMethod.CASH,
                "200000",
                "ROOM"
        );


        assertMoney(
                "0",
                folioBalance(
                        tenant,
                        reservation.folioId()
                )
        );
    }


    /*
     * Adding RESERVATION support must not weaken ordinary ROOM/FOOD
     * payment protections.
     *
     * A zero-balance folio cannot receive a normal payment.
     *
     * A cancelled reservation also cannot receive a new reservation
     * advance.
     */
    @Test
    void ordinaryPrepaymentAndCancelledReservationAdvanceRemainBlocked() {

        Tenant tenant =
                tenant();

        CustomerResponse customer =
                customer(
                        tenant
                );

        RoomResponse room =
                room(
                        tenant,
                        "ADV-401"
                );

        LocalDate start =
                LocalDate.now()
                        .plusDays(
                                5
                        );


        var reservation =
                reservations.create(
                        tenant.hotel(),
                        tenant.branch(),
                        booking(
                                customer.id(),
                                room.id(),
                                start,
                                start.plusDays(
                                        2
                                ),
                                new BigDecimal(
                                        "250000"
                                )
                        )
                );


        assertThrows(
                IllegalStateException.class,
                () ->
                        requestPayment(
                                tenant,
                                reservation.folioId(),
                                PaymentMethod.CASH,
                                "1000",
                                "FOOD"
                        )
        );


        assertThrows(
                IllegalStateException.class,
                () ->
                        requestPayment(
                                tenant,
                                reservation.folioId(),
                                PaymentMethod.CASH,
                                "1000",
                                "ROOM"
                        )
        );


        reservations.cancel(
                tenant.hotel(),
                tenant.branch(),
                reservation.id()
        );


        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () ->
                                requestAdvance(
                                        tenant,
                                        reservation.id(),
                                        PaymentMethod.MOBILE_MONEY,
                                        "100000"
                                )
                );


        assertEquals(
                "RESERVATION_ADVANCE_NOT_ALLOWED",
                exception.code()
        );
    }


    /*
     * CASH reservation advances must participate in the existing
     * cashier-shift calculation exactly once.
     */
    @Test
    void cashReservationAdvanceIsCountedOnceInCashierShift() {

        Tenant tenant =
                tenant();

        CustomerResponse customer =
                customer(
                        tenant
                );

        RoomResponse room =
                room(
                        tenant,
                        "ADV-501"
                );

        LocalDate start =
                LocalDate.now()
                        .plusDays(
                                6
                        );


        var reservation =
                reservations.create(
                        tenant.hotel(),
                        tenant.branch(),
                        booking(
                                customer.id(),
                                room.id(),
                                start,
                                start.plusDays(
                                        2
                                ),
                                new BigDecimal(
                                        "250000"
                                )
                        )
                );


        var shift =
                openShift(
                        tenant
                );


        approveAdvance(
                tenant,
                reservation.id(),
                PaymentMethod.CASH,
                "100000"
        );


        var closed =
                shifts.close(
                        tenant.hotel(),
                        tenant.branch(),
                        shift.id(),
                        BigDecimal.ZERO,
                        new BigDecimal(
                                "100000"
                        )
                );


        assertMoney(
                "100000",
                closed.expectedAmount()
        );

        assertMoney(
                "100000",
                closed.countedAmount()
        );

        assertMoney(
                "0",
                closed.difference()
        );
    }


    /*
     * A refunded reservation advance must stop counting as an active
     * advance and must restore the reservation's remaining amount.
     */
    @Test
    void refundedAdvanceNoLongerCountsTowardReservationSummary() {

        Tenant tenant =
                tenant();

        CustomerResponse customer =
                customer(
                        tenant
                );

        RoomResponse room =
                room(
                        tenant,
                        "ADV-601"
                );

        LocalDate start =
                LocalDate.now()
                        .plusDays(
                                7
                        );


        var reservation =
                reservations.create(
                        tenant.hotel(),
                        tenant.branch(),
                        booking(
                                customer.id(),
                                room.id(),
                                start,
                                start.plusDays(
                                        2
                                ),
                                new BigDecimal(
                                        "250000"
                                )
                        )
                );


        openShift(
                tenant
        );


        approveAdvance(
                tenant,
                reservation.id(),
                PaymentMethod.MOBILE_MONEY,
                "100000"
        );


        UUID paymentId =
                jdbc.queryForObject(
                        """
                        select id
                        from payments
                        where hotel_id = ?
                          and branch_id = ?
                          and folio_id = ?
                          and collection_scope = 'ROOM'
                          and payment_purpose = 'RESERVATION_ADVANCE'
                          and status = 'POSTED'
                        order by created_at desc
                        limit 1
                        """,
                        UUID.class,
                        tenant.hotel(),
                        tenant.branch(),
                        reservation.folioId()
                );


        paymentService.refund(
                tenant.hotel(),
                tenant.branch(),
                paymentId
        );


        assertEquals(
                "REFUNDED",
                jdbc.queryForObject(
                        """
                        select status
                        from payments
                        where id = ?
                        """,
                        String.class,
                        paymentId
                )
        );


        var summary =
                reservations.paymentSummary(
                        tenant.hotel(),
                        tenant.branch(),
                        reservation.id()
                );


        assertMoney(
                "0",
                summary.advancePaid()
        );

        assertMoney(
                "500000",
                summary.remaining()
        );

        assertEquals(
                "NO_PAYMENT",
                summary.paymentStatus()
        );


        /*
         * Original payment -100,000 plus refund +100,000.
         */
        assertMoney(
                "0",
                folioBalance(
                        tenant,
                        reservation.folioId()
                )
        );
    }


    private Tenant tenant() {

        String code =
                UUID.randomUUID()
                        .toString();


        var owner =
                onboarding.signup(
                        new OwnerSignupRequest(
                                code
                                        + "@example.test",
                                "long operational passphrase",
                                "Owner",
                                null,
                                "en",
                                new CreateHotelRequest(
                                        code,
                                        "Hotel",
                                        "Hotel",
                                        null,
                                        null,
                                        null,
                                        null,
                                        "RWF",
                                        "Africa/Kigali",
                                        "en"
                                )
                        )
                );


        var branch =
                hotels.createBranch(
                        owner.ownerUserId(),
                        owner.hotel().id(),
                        new CreateBranchRequest(
                                "MAIN",
                                "Main",
                                null,
                                null,
                                null,
                                null
                        )
                );


        Tenant result =
                new Tenant(
                        owner.ownerUserId(),
                        owner.hotel().id(),
                        branch.id()
                );


        as(
                result
        );


        return result;
    }


    private void as(
            Tenant tenant) {

        var jwt =
                Jwt.withTokenValue(
                                "test"
                        )
                        .header(
                                "alg",
                                "HS256"
                        )
                        .subject(
                                tenant.actor()
                                        .toString()
                        )
                        .build();


        SecurityContextHolder
                .getContext()
                .setAuthentication(
                        new JwtAuthenticationToken(
                                jwt,
                                List.of()
                        )
                );
    }


    private CustomerResponse customer(
            Tenant tenant) {

        as(
                tenant
        );


        return customers.create(
                tenant.hotel(),
                new CustomerRequest(
                        "CUSTOMER",
                        CustomerKind.COMPANY,
                        "Company",
                        null,
                        null,
                        null,
                        null,
                        true
                )
        );
    }


    private RoomResponse room(
            Tenant tenant,
            String code) {

        as(
                tenant
        );


        var type =
                rooms.type(
                        tenant.hotel(),
                        tenant.branch(),
                        new RoomTypeRequest(
                                code,
                                "Double",
                                null,
                                2,
                                2,
                                1,
                                new BigDecimal(
                                        "10000"
                                )
                        )
                );


        return rooms.create(
                tenant.hotel(),
                tenant.branch(),
                new RoomRequest(
                        type.id(),
                        code,
                        null,
                        1,
                        "King",
                        "180 x 200 cm",
                        2,
                        1
                )
        );
    }


    private ReservationRequest booking(
            UUID customer,
            UUID room,
            LocalDate start,
            LocalDate end,
            BigDecimal nightlyRate) {

        return new ReservationRequest(
                customer,
                start,
                end,
                List.of(
                        new ReservationRequest.RoomBooking(
                                room,
                                1,
                                0,
                                nightlyRate,
                                List.of()
                        )
                ),
                null
        );
    }


    private com.hotelmanagement.hms.payment.dto.CashierShiftResponse openShift(
            Tenant tenant) {

        as(
                tenant
        );


        return shifts.open(
                tenant.hotel(),
                tenant.branch(),
                new CashierShiftRequest(
                        BigDecimal.ZERO,
                        "Reservation advance integration test",
                        null
                )
        );
    }


    private Map<String, Object> requestAdvance(
            Tenant tenant,
            UUID reservationId,
            PaymentMethod method,
            String amount) {

        as(tenant);

        return paymentWorkflow.requestReservationAdvance(
                tenant.hotel(),
                tenant.branch(),
                reservationId,
                new PaymentWorkflow.AdvanceRequest(
                        UUID.randomUUID(),
                        List.of(
                                new PaymentWorkflow.Part(
                                        method,
                                        "RWF",
                                        new BigDecimal(amount)
                                )
                        )
                )
        );
    }

    private UUID approveAdvance(
            Tenant tenant,
            UUID reservationId,
            PaymentMethod method,
            String amount) {

        Map<String, Object> pending =
                requestAdvance(
                        tenant,
                        reservationId,
                        method,
                        amount
                );

        return approve(
                tenant,
                pending,
                "Owner verified reservation advance"
        );
    }


    private Map<String, Object> requestPayment(
            Tenant tenant,
            UUID folioId,
            PaymentMethod method,
            String amount,
            String collectionScope) {

        as(
                tenant
        );


        return paymentWorkflow.request(
                tenant.hotel(),
                tenant.branch(),
                new PaymentWorkflow.Request(
                        folioId,
                        UUID.randomUUID(),
                        List.of(
                                new PaymentWorkflow.Part(
                                        method,
                                        "RWF",
                                        new BigDecimal(
                                                amount
                                        )
                                )
                        ),
                        collectionScope
                )
        );
    }


    private UUID approvePayment(
            Tenant tenant,
            UUID folioId,
            PaymentMethod method,
            String amount,
            String collectionScope) {

        Map<String, Object> pending =
                requestPayment(
                        tenant,
                        folioId,
                        method,
                        amount,
                        collectionScope
                );


        return approve(
                tenant,
                pending,
                "Owner verified payment"
        );
    }


    private UUID approve(
            Tenant tenant,
            Map<String, Object> pending,
            String reason) {

        UUID approvalId =
                (UUID) pending.get(
                        "id"
                );


        as(
                tenant
        );


        paymentWorkflow.approve(
                tenant.hotel(),
                tenant.branch(),
                approvalId,
                true,
                reason
        );


        return approvalId;
    }


    private BigDecimal folioBalance(
            Tenant tenant,
            UUID folioId) {

        as(
                tenant
        );


        return jdbc.queryForObject(
                """
                select coalesce(
                    sum(amount),
                    0
                )
                from folio_entries
                where hotel_id = ?
                  and branch_id = ?
                  and folio_id = ?
                """,
                BigDecimal.class,
                tenant.hotel(),
                tenant.branch(),
                folioId
        );
    }


    private BigDecimal paymentTotal(
            UUID folioId,
            String method) {

        return jdbc.queryForObject(
                """
                select coalesce(
                    sum(base_amount),
                    0
                )
                from payments
                where folio_id = ?
                  and method = ?
                  and status = 'POSTED'
                """,
                BigDecimal.class,
                folioId,
                method
        );
    }


    private void assertMoney(
            String expected,
            BigDecimal actual) {

        assertEquals(
                0,
                new BigDecimal(
                        expected
                ).compareTo(
                        actual
                )
        );
    }
}