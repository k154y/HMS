package com.hotelmanagement.hms;

import com.hotelmanagement.hms.customer.dto.CustomerRequest;
import com.hotelmanagement.hms.customer.dto.CustomerResponse;
import com.hotelmanagement.hms.customer.model.CustomerKind;
import com.hotelmanagement.hms.customer.service.CustomerService;
import com.hotelmanagement.hms.folio.service.FolioService;
import com.hotelmanagement.hms.payment.dto.CashierShiftRequest;
import com.hotelmanagement.hms.payment.model.PaymentMethod;
import com.hotelmanagement.hms.payment.service.CashierShiftService;
import com.hotelmanagement.hms.payment.service.PaymentWorkflow;
import com.hotelmanagement.hms.platform.currency.dto.ExchangeRateRequest;
import com.hotelmanagement.hms.platform.dto.CreateBranchRequest;
import com.hotelmanagement.hms.platform.dto.CreateHotelRequest;
import com.hotelmanagement.hms.platform.onboarding.dto.OwnerSignupRequest;
import com.hotelmanagement.hms.platform.onboarding.service.OwnerOnboardingService;
import com.hotelmanagement.hms.platform.service.HotelAdministrationService;
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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        properties = {
                "hms.security.jwt.secret=payment-fx-integration-test-secret-at-least-32-bytes",
                "logging.level.root=WARN",
                "logging.level.com.hotelmanagement.hms=WARN",
                "management.otlp.metrics.export.enabled=false"
        }
)
class PaymentFxWorkflowTest {

    @Autowired
    OwnerOnboardingService onboarding;

    @Autowired
    HotelAdministrationService hotels;

    @Autowired
    CustomerService customers;

    @Autowired
    FolioService folios;

    @Autowired
    PaymentWorkflow paymentWorkflow;

    @Autowired
    CashierShiftService shifts;

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

    /**
     * A payment made directly in the hotel's base currency must use
     * an exchange rate of exactly 1 and preserve the same amount as
     * its base amount.
     */
    @Test
    void baseCurrencyPaymentUsesRateOne() {

        Tenant tenant =
                tenant();

        CustomerResponse customer =
                customer(
                        tenant
                );

        var folio =
                folios.create(
                        tenant.hotel(),
                        tenant.branch(),
                        customer.id()
                );

        folios.charge(
                tenant.hotel(),
                tenant.branch(),
                folio.id(),
                new BigDecimal(
                        "100000"
                ),
                "Restaurant service",
                UUID.randomUUID()
        );

        UUID requestId =
                UUID.randomUUID();

        var pending =
                paymentWorkflow.request(
                        tenant.hotel(),
                        tenant.branch(),
                        new PaymentWorkflow.Request(
                                folio.id(),
                                requestId,
                                List.of(
                                        new PaymentWorkflow.Part(
                                                PaymentMethod.CASH,
                                                "RWF",
                                                new BigDecimal(
                                                        "50000"
                                                )
                                        )
                                ),
                                "FOOD"
                        )
                );

        assertMoney(
                "50000",
                (BigDecimal) pending.get(
                        "baseAmount"
                )
        );

        openShift(
                tenant
        );

        UUID approvalId =
                (UUID) pending.get(
                        "id"
                );

        paymentWorkflow.approve(
                tenant.hotel(),
                tenant.branch(),
                approvalId,
                true,
                "Owner verified base-currency payment"
        );

        assertEquals(
                "RWF",
                jdbc.queryForObject(
                        """
                        select currency
                        from payments
                        where folio_id = ?
                        """,
                        String.class,
                        folio.id()
                )
        );

        assertMoney(
                "50000",
                jdbc.queryForObject(
                        """
                        select original_amount
                        from payments
                        where folio_id = ?
                        """,
                        BigDecimal.class,
                        folio.id()
                )
        );

        assertMoney(
                "1",
                jdbc.queryForObject(
                        """
                        select fx_rate
                        from payments
                        where folio_id = ?
                        """,
                        BigDecimal.class,
                        folio.id()
                )
        );

        assertMoney(
                "50000",
                jdbc.queryForObject(
                        """
                        select base_amount
                        from payments
                        where folio_id = ?
                        """,
                        BigDecimal.class,
                        folio.id()
                )
        );

        assertMoney(
                "50000",
                folios.get(
                        tenant.hotel(),
                        tenant.branch(),
                        folio.id()
                ).balance()
        );
    }

    /**
     * The quote shown to the cashier must be calculated by the server
     * using the hotel's configured exchange rate.
     *
     * 100 USD x 1450 = 145,000 RWF.
     */
    @Test
    void foreignCurrencyQuoteUsesConfiguredHotelRate() {

        Tenant tenant =
                tenant();

        configureRate(
                tenant,
                "USD",
                "1450",
                OffsetDateTime
                        .now(
                                ZoneOffset.UTC
                        )
                        .minusMinutes(
                                5
                        )
        );

        CustomerResponse customer =
                customer(
                        tenant
                );

        var folio =
                folios.create(
                        tenant.hotel(),
                        tenant.branch(),
                        customer.id()
                );

        folios.charge(
                tenant.hotel(),
                tenant.branch(),
                folio.id(),
                new BigDecimal(
                        "200000"
                ),
                "Accommodation and services",
                UUID.randomUUID()
        );

        PaymentWorkflow.QuoteResponse quote =
                paymentWorkflow.quote(
                        tenant.hotel(),
                        tenant.branch(),
                        new PaymentWorkflow.QuoteRequest(
                                folio.id(),
                                "USD",
                                new BigDecimal(
                                        "100"
                                )
                        )
                );

        assertEquals(
                "RWF",
                quote.folioCurrency()
        );

        assertEquals(
                "USD",
                quote.paymentCurrency()
        );

        assertMoney(
                "100",
                quote.originalAmount()
        );

        assertMoney(
                "1450",
                quote.fxRate()
        );

        assertMoney(
                "145000",
                quote.baseAmount()
        );

        assertTrue(
                quote.exchangeRateId()
                        != null
        );

        assertTrue(
                quote.rateEffectiveFrom()
                        != null
        );
    }

    /**
     * Once a payment request has been created, its exchange rate is frozen.
     *
     * Changing the hotel's current rate must not alter:
     *
     * - an idempotent retry of the same request;
     * - the rate eventually posted when that approval is accepted.
     */
    @Test
    void exchangeRateSnapshotSurvivesRateChangeAndRetry() {

        Tenant tenant =
                tenant();

        OffsetDateTime initialTime =
                OffsetDateTime
                        .now(
                                ZoneOffset.UTC
                        )
                        .minusMinutes(
                                10
                        );

        configureRate(
                tenant,
                "USD",
                "1450",
                initialTime
        );

        CustomerResponse customer =
                customer(
                        tenant
                );

        var folio =
                folios.create(
                        tenant.hotel(),
                        tenant.branch(),
                        customer.id()
                );

        folios.charge(
                tenant.hotel(),
                tenant.branch(),
                folio.id(),
                new BigDecimal(
                        "200000"
                ),
                "Guest services",
                UUID.randomUUID()
        );

        UUID requestId =
                UUID.randomUUID();

        PaymentWorkflow.Request request =
                new PaymentWorkflow.Request(
                        folio.id(),
                        requestId,
                        List.of(
                                new PaymentWorkflow.Part(
                                        PaymentMethod.CASH,
                                        "USD",
                                        new BigDecimal(
                                                "100"
                                        )
                                )
                        ),
                        "FOOD"
                );

        var first =
                paymentWorkflow.request(
                        tenant.hotel(),
                        tenant.branch(),
                        request
                );

        UUID firstApprovalId =
                (UUID) first.get(
                        "id"
                );

        assertMoney(
                "145000",
                (BigDecimal) first.get(
                        "baseAmount"
                )
        );

        /*
         * The hotel changes its current rate after the approval request
         * has already been created.
         */
        configureRate(
                tenant,
                "USD",
                "1500",
                OffsetDateTime
                        .now(
                                ZoneOffset.UTC
                        )
                        .minusSeconds(
                                1
                        )
        );

        /*
         * A fresh quote should now use the NEW rate.
         */
        PaymentWorkflow.QuoteResponse currentQuote =
                paymentWorkflow.quote(
                        tenant.hotel(),
                        tenant.branch(),
                        new PaymentWorkflow.QuoteRequest(
                                folio.id(),
                                "USD",
                                new BigDecimal(
                                        "100"
                                )
                        )
                );

        assertMoney(
                "1500",
                currentQuote.fxRate()
        );

        assertMoney(
                "150000",
                currentQuote.baseAmount()
        );

        /*
         * Retrying the original requestId must return the old approval,
         * not recalculate it at 1500.
         */
        var retry =
                paymentWorkflow.request(
                        tenant.hotel(),
                        tenant.branch(),
                        request
                );

        assertEquals(
                firstApprovalId,
                retry.get(
                        "id"
                )
        );

        openShift(
                tenant
        );

        paymentWorkflow.approve(
                tenant.hotel(),
                tenant.branch(),
                firstApprovalId,
                true,
                "Owner verified frozen FX payment"
        );

        /*
         * The final payment must still contain the original 1450 rate.
         */
        assertMoney(
                "1450",
                jdbc.queryForObject(
                        """
                        select fx_rate
                        from payments
                        where folio_id = ?
                        """,
                        BigDecimal.class,
                        folio.id()
                )
        );

        assertMoney(
                "100",
                jdbc.queryForObject(
                        """
                        select original_amount
                        from payments
                        where folio_id = ?
                        """,
                        BigDecimal.class,
                        folio.id()
                )
        );

        assertMoney(
                "145000",
                jdbc.queryForObject(
                        """
                        select base_amount
                        from payments
                        where folio_id = ?
                        """,
                        BigDecimal.class,
                        folio.id()
                )
        );

        assertMoney(
                "55000",
                folios.get(
                        tenant.hotel(),
                        tenant.branch(),
                        folio.id()
                ).balance()
        );
    }

    /**
     * One approval may contain different payment methods and currencies.
     *
     * Example:
     *
     * 100 USD cash at 1450 = 145,000 RWF
     * 50,000 RWF mobile money = 50,000 RWF
     *
     * Total = 195,000 RWF.
     */
    @Test
    void splitPaymentSupportsMultipleCurrencies() {

        Tenant tenant =
                tenant();

        configureRate(
                tenant,
                "USD",
                "1450",
                OffsetDateTime
                        .now(
                                ZoneOffset.UTC
                        )
                        .minusMinutes(
                                5
                        )
        );

        CustomerResponse customer =
                customer(
                        tenant
                );

        var folio =
                folios.create(
                        tenant.hotel(),
                        tenant.branch(),
                        customer.id()
                );

        folios.charge(
                tenant.hotel(),
                tenant.branch(),
                folio.id(),
                new BigDecimal(
                        "250000"
                ),
                "Guest consumption",
                UUID.randomUUID()
        );

        var pending =
                paymentWorkflow.request(
                        tenant.hotel(),
                        tenant.branch(),
                        new PaymentWorkflow.Request(
                                folio.id(),
                                UUID.randomUUID(),
                                List.of(
                                        new PaymentWorkflow.Part(
                                                PaymentMethod.CASH,
                                                "USD",
                                                new BigDecimal(
                                                        "100"
                                                )
                                        ),
                                        new PaymentWorkflow.Part(
                                                PaymentMethod.MOBILE_MONEY,
                                                "RWF",
                                                new BigDecimal(
                                                        "50000"
                                                )
                                        )
                                ),
                                "FOOD"
                        )
                );

        assertMoney(
                "195000",
                (BigDecimal) pending.get(
                        "baseAmount"
                )
        );

        openShift(
                tenant
        );

        paymentWorkflow.approve(
                tenant.hotel(),
                tenant.branch(),
                (UUID) pending.get(
                        "id"
                ),
                true,
                "Owner verified split payment"
        );

        Integer paymentCount =
                jdbc.queryForObject(
                        """
                        select count(*)
                        from payments
                        where folio_id = ?
                        """,
                        Integer.class,
                        folio.id()
                );

        assertEquals(
                2,
                paymentCount
        );

        assertMoney(
                "195000",
                jdbc.queryForObject(
                        """
                        select sum(base_amount)
                        from payments
                        where folio_id = ?
                        """,
                        BigDecimal.class,
                        folio.id()
                )
        );

        assertMoney(
                "145000",
                jdbc.queryForObject(
                        """
                        select base_amount
                        from payments
                        where folio_id = ?
                          and currency = 'USD'
                        """,
                        BigDecimal.class,
                        folio.id()
                )
        );

        assertMoney(
                "50000",
                jdbc.queryForObject(
                        """
                        select base_amount
                        from payments
                        where folio_id = ?
                          and currency = 'RWF'
                        """,
                        BigDecimal.class,
                        folio.id()
                )
        );

        assertMoney(
                "55000",
                folios.get(
                        tenant.hotel(),
                        tenant.branch(),
                        folio.id()
                ).balance()
        );
    }

    /**
     * Overpayment protection must compare the converted base amount,
     * not the raw foreign-currency amount.
     *
     * Balance = 200,000 RWF.
     *
     * 150 USD x 1450 = 217,500 RWF.
     *
     * The request must therefore be rejected.
     */
    @Test
    void foreignCurrencyOverpaymentIsRejectedUsingBaseAmount() {

        Tenant tenant =
                tenant();

        configureRate(
                tenant,
                "USD",
                "1450",
                OffsetDateTime
                        .now(
                                ZoneOffset.UTC
                        )
                        .minusMinutes(
                                5
                        )
        );

        CustomerResponse customer =
                customer(
                        tenant
                );

        var folio =
                folios.create(
                        tenant.hotel(),
                        tenant.branch(),
                        customer.id()
                );

        folios.charge(
                tenant.hotel(),
                tenant.branch(),
                folio.id(),
                new BigDecimal(
                        "200000"
                ),
                "Hotel services",
                UUID.randomUUID()
        );

        IllegalStateException error =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                paymentWorkflow.request(
                                        tenant.hotel(),
                                        tenant.branch(),
                                        new PaymentWorkflow.Request(
                                                folio.id(),
                                                UUID.randomUUID(),
                                                List.of(
                                                        new PaymentWorkflow.Part(
                                                                PaymentMethod.CASH,
                                                                "USD",
                                                                new BigDecimal(
                                                                        "150"
                                                                )
                                                        )
                                                ),
                                                "FOOD"
                                        )
                                )
                );

        assertEquals(
                "Payment exceeds folio balance.",
                error.getMessage()
        );

        Integer approvals =
                jdbc.queryForObject(
                        """
                        select count(*)
                        from payment_approvals
                        where folio_id = ?
                        """,
                        Integer.class,
                        folio.id()
                );

        assertEquals(
                0,
                approvals
        );
    }

    /**
     * A foreign-currency payment is impossible until the hotel has
     * configured an applicable rate for that currency.
     */
    @Test
    void foreignCurrencyWithoutConfiguredRateIsRejected() {

        Tenant tenant =
                tenant();

        CustomerResponse customer =
                customer(
                        tenant
                );

        var folio =
                folios.create(
                        tenant.hotel(),
                        tenant.branch(),
                        customer.id()
                );

        folios.charge(
                tenant.hotel(),
                tenant.branch(),
                folio.id(),
                new BigDecimal(
                        "100000"
                ),
                "Restaurant bill",
                UUID.randomUUID()
        );

        IllegalStateException error =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                paymentWorkflow.request(
                                        tenant.hotel(),
                                        tenant.branch(),
                                        new PaymentWorkflow.Request(
                                                folio.id(),
                                                UUID.randomUUID(),
                                                List.of(
                                                        new PaymentWorkflow.Part(
                                                                PaymentMethod.CASH,
                                                                "EUR",
                                                                new BigDecimal(
                                                                        "50"
                                                                )
                                                        )
                                                ),
                                                "FOOD"
                                        )
                                )
                );

        assertTrue(
                error
                        .getMessage()
                        .contains(
                                "No exchange rate is configured for currency EUR"
                        )
        );

        Integer approvals =
                jdbc.queryForObject(
                        """
                        select count(*)
                        from payment_approvals
                        where folio_id = ?
                        """,
                        Integer.class,
                        folio.id()
                );

        assertEquals(
                0,
                approvals
        );
    }

    /**
     * Pending approvals created before the FX implementation contained
     * only method + amount.
     *
     * The old system only permitted the folio currency at rate 1,
     * so such an approval can safely be normalized into:
     *
     * currency = folio currency
     * fx rate = 1
     * base amount = original amount
     */
    @Test
    void legacyPendingApprovalCanStillBeApprovedSafely() {

        Tenant tenant =
                tenant();

        CustomerResponse customer =
                customer(
                        tenant
                );

        var folio =
                folios.create(
                        tenant.hotel(),
                        tenant.branch(),
                        customer.id()
                );

        folios.charge(
                tenant.hotel(),
                tenant.branch(),
                folio.id(),
                new BigDecimal(
                        "100"
                ),
                "Legacy service charge",
                UUID.randomUUID()
        );

        UUID approvalId =
                UUID.randomUUID();

        UUID requestId =
                UUID.randomUUID();

        String oldPayload =
                """
                {
                  "folioId": "%s",
                  "requestId": "%s",
                  "parts": [
                    {
                      "method": "CASH",
                      "amount": 50
                    }
                  ],
                  "collectionScope": "FOOD"
                }
                """
                        .formatted(
                                folio.id(),
                                requestId
                        );

        jdbc.update(
                """
                insert into payment_approvals(
                    id,
                    hotel_id,
                    branch_id,
                    folio_id,
                    request_id,
                    requested_by,
                    payload,
                    status
                )
                values(
                    ?, ?, ?, ?, ?, ?, ?, 'PENDING'
                )
                """,
                approvalId,
                tenant.hotel(),
                tenant.branch(),
                folio.id(),
                requestId,
                tenant.actor(),
                oldPayload
        );

        openShift(
                tenant
        );

        paymentWorkflow.approve(
                tenant.hotel(),
                tenant.branch(),
                approvalId,
                true,
                "Owner verified legacy payment"
        );

        assertEquals(
                "APPROVED",
                jdbc.queryForObject(
                        """
                        select status
                        from payment_approvals
                        where id = ?
                        """,
                        String.class,
                        approvalId
                )
        );

        assertEquals(
                "RWF",
                jdbc.queryForObject(
                        """
                        select currency
                        from payments
                        where folio_id = ?
                        """,
                        String.class,
                        folio.id()
                )
        );

        assertMoney(
                "50",
                jdbc.queryForObject(
                        """
                        select original_amount
                        from payments
                        where folio_id = ?
                        """,
                        BigDecimal.class,
                        folio.id()
                )
        );

        assertMoney(
                "1",
                jdbc.queryForObject(
                        """
                        select fx_rate
                        from payments
                        where folio_id = ?
                        """,
                        BigDecimal.class,
                        folio.id()
                )
        );

        assertMoney(
                "50",
                jdbc.queryForObject(
                        """
                        select base_amount
                        from payments
                        where folio_id = ?
                        """,
                        BigDecimal.class,
                        folio.id()
                )
        );

        assertMoney(
                "50",
                folios.get(
                        tenant.hotel(),
                        tenant.branch(),
                        folio.id()
                ).balance()
        );
    }

    /**
     * Creates an isolated hotel + owner + main branch.
     */
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

    /**
     * Creates a reusable customer belonging to the test hotel.
     */
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
                        "FX Test Company",
                        null,
                        null,
                        null,
                        null,
                        true
                )
        );
    }

    /**
     * Adds an immutable hotel exchange-rate record.
     */
    private void configureRate(
            Tenant tenant,
            String currency,
            String rate,
            OffsetDateTime effectiveFrom) {

        as(
                tenant
        );

        hotels.createRate(
                tenant.actor(),
                tenant.hotel(),
                new ExchangeRateRequest(
                        currency,
                        new BigDecimal(
                                rate
                        ),
                        effectiveFrom
                )
        );
    }

    /**
     * Opens the owner's cashier shift so the owner can approve
     * the payment in integration tests.
     */
    private void openShift(
            Tenant tenant) {

        as(
                tenant
        );

        shifts.open(
                tenant.hotel(),
                tenant.branch(),
                new CashierShiftRequest(
                        BigDecimal.ZERO,
                        "FX integration test shift",
                        null
                )
        );
    }

    /**
     * Makes service calls execute as the tenant owner.
     */
    private void as(
            Tenant tenant) {

        Jwt jwt =
                Jwt
                        .withTokenValue(
                                "test"
                        )
                        .header(
                                "alg",
                                "HS256"
                        )
                        .subject(
                                tenant
                                        .actor()
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

    /**
     * BigDecimal equality helper that intentionally ignores scale.
     *
     * 1450
     * 1450.0000
     * 1450.00000000
     *
     * are monetary equivalents for these tests.
     */
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