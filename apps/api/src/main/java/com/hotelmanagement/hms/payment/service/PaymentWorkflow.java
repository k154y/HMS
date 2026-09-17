package com.hotelmanagement.hms.payment.service;

import com.hotelmanagement.hms.audit.service.AuditService;
import com.hotelmanagement.hms.folio.model.Folio;
import com.hotelmanagement.hms.folio.service.FolioService;
import com.hotelmanagement.hms.payment.dto.PaymentRequest;
import com.hotelmanagement.hms.payment.model.PaymentMethod;
import com.hotelmanagement.hms.platform.currency.dto.ExchangeRateResponse;
import com.hotelmanagement.hms.platform.currency.service.ExchangeRateService;
import com.hotelmanagement.hms.shared.service.OperationScope;
import com.hotelmanagement.hms.shared.web.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static com.hotelmanagement.hms.identity.authorization.model.PermissionCode.PAYMENT_RECORD;
import static com.hotelmanagement.hms.identity.authorization.model.PermissionCode.PAYMENT_VIEW;

@Service
@Transactional
public class PaymentWorkflow {

    private static final int MONEY_SCALE = 4;
    private static final int FX_SCALE = 8;

    private final JdbcTemplate db;
    private final OperationScope scope;
    private final FolioService folios;
    private final PaymentService payments;
    private final ExchangeRateService exchangeRates;
    private final JsonMapper json;
    private final AuditService audit;

    public PaymentWorkflow(
            JdbcTemplate db,
            OperationScope scope,
            FolioService folios,
            PaymentService payments,
            ExchangeRateService exchangeRates,
            JsonMapper json,
            AuditService audit) {

        this.db = db;
        this.scope = scope;
        this.folios = folios;
        this.payments = payments;
        this.exchangeRates = exchangeRates;
        this.json = json;
        this.audit = audit;
    }

    /**
     * Payment part entered by the cashier.
     *
     * Currency is optional for backward compatibility.
     * When omitted, the folio currency is used.
     */
    public record Part(
            PaymentMethod method,
            String currency,
            BigDecimal amount) {

        public Part(
                PaymentMethod method,
                BigDecimal amount) {

            this(
                    method,
                    null,
                    amount
            );
        }
    }

    /**
     * Payment approval request entered by the cashier.
     */
    public record Request(
            UUID folioId,
            UUID requestId,
            List<Part> parts,
            String collectionScope) {

        public Request(
                UUID folioId,
                UUID requestId,
                List<Part> parts) {

            this(
                    folioId,
                    requestId,
                    parts,
                    null
            );
        }
    }

    /**
     * Server-generated immutable monetary snapshot.
     *
     * fxRate and baseAmount are never trusted from the browser.
     */
    public record QuotedPart(
            PaymentMethod method,
            String currency,
            BigDecimal amount,
            BigDecimal fxRate,
            BigDecimal baseAmount,
            UUID exchangeRateId,
            OffsetDateTime rateEffectiveFrom) {
    }

    /**
     * Immutable approval payload.
     *
     * Once an approval is created, changing the hotel's exchange rate
     * does not change this payment request.
     */
    public record ApprovalPayload(
            UUID folioId,
            UUID requestId,
            List<QuotedPart> parts,
            String collectionScope,
            String folioCurrency) {
    }

    /**
     * Request used by the frontend to preview a payment conversion.
     */
    public record QuoteRequest(
            UUID folioId,
            String currency,
            BigDecimal amount) {
    }

    /**
     * Server-authoritative conversion result shown to the cashier.
     */
    public record QuoteResponse(
            UUID folioId,
            String folioCurrency,
            String paymentCurrency,
            BigDecimal originalAmount,
            BigDecimal fxRate,
            BigDecimal baseAmount,
            UUID exchangeRateId,
            OffsetDateTime rateEffectiveFrom) {
    }

    /**
     * Compatibility entry point for the existing single-payment API.
     *
     * PaymentRequest.fxRate is intentionally ignored.
     * The backend resolves the authoritative exchange rate.
     */
    public Map<String, Object> requestSingle(
            UUID hotel,
            UUID branch,
            PaymentRequest request) {

        scope.branch(
                hotel,
                branch,
                PAYMENT_RECORD
        );

        if (request.idempotencyKey() == null
                || request.idempotencyKey().isBlank()) {

            throw new IllegalArgumentException(
                    "Payment idempotency key is required."
            );
        }

        UUID requestId =
                UUID.nameUUIDFromBytes(
                        request
                                .idempotencyKey()
                                .getBytes(
                                        java.nio.charset.StandardCharsets.UTF_8
                                )
                );

        return request(
                hotel,
                branch,
                new Request(
                        request.folioId(),
                        requestId,
                        List.of(
                                new Part(
                                        request.method(),
                                        request.currency(),
                                        request.amount()
                                )
                        ),
                        null
                )
        );
    }

    /**
     * Provides a server-authoritative conversion preview.
     *
     * For example:
     *
     * 100 USD at 1450 = 145,000 RWF.
     */
    @Transactional
    public QuoteResponse quote(
            UUID hotel,
            UUID branch,
            QuoteRequest request) {

        scope.branch(
                hotel,
                branch,
                PAYMENT_RECORD
        );

        if (request == null
                || request.folioId() == null) {

            throw new IllegalArgumentException(
                    "Folio is required."
            );
        }

        Folio folio =
                folios.lock(
                        hotel,
                        branch,
                        request.folioId()
                );

        QuotedPart quoted =
                resolvePart(
                        hotel,
                        folio,
                        new Part(
                                PaymentMethod.CASH,
                                request.currency(),
                                request.amount()
                        )
                );

        return new QuoteResponse(
                request.folioId(),
                normalizeCurrency(
                        folio.getCurrency()
                ),
                quoted.currency(),
                quoted.amount(),
                quoted.fxRate(),
                quoted.baseAmount(),
                quoted.exchangeRateId(),
                quoted.rateEffectiveFrom()
        );
    }

    /**
     * Creates a pending payment approval.
     *
     * Foreign-currency values are converted here and the exact
     * rate/base amount are frozen into payment_approvals.payload.
     */
    public Map<String, Object> request(
            UUID hotel,
            UUID branch,
            Request request) {

        validateRequest(
                request
        );

        UUID actor =
                scope.branch(
                        hotel,
                        branch,
                        PAYMENT_RECORD
                );

        Folio folio =
                folios.lock(
                        hotel,
                        branch,
                        request.folioId()
                );

        boolean room =
                Boolean.TRUE.equals(
                        db.queryForObject(
                                """
                                select exists(
                                    select 1
                                    from folio_entries
                                    where folio_id = ?
                                      and kind = 'ACCOMMODATION'
                                )
                                """,
                                Boolean.class,
                                request.folioId()
                        )
                );

        String collectionScope =
                request.collectionScope() == null
                        ? room
                            ? "ROOM"
                            : "FOOD"
                        : request
                                .collectionScope()
                                .trim()
                                .toUpperCase(
                                        Locale.ROOT
                                );

        if (!Set.of(
                "ROOM",
                "FOOD"
        ).contains(collectionScope)) {

            throw new IllegalArgumentException(
                    "Invalid collection scope."
            );
        }

        if (collectionScope.equals("ROOM")
                && !hasRole(
                        actor,
                        hotel,
                        Set.of(
                                "OWNER",
                                "MANAGER",
                                "RECEPTIONIST"
                        )
                )) {

            throw new AccessDeniedException(
                    "Room payments require reception."
            );
        }

        /*
         * Idempotency is checked BEFORE resolving the current exchange rate.
         *
         * Example:
         *
         * First request:
         *     1 USD = 1450 RWF
         *
         * Hotel later changes:
         *     1 USD = 1470 RWF
         *
         * A retry using the same requestId must still return the first
         * approval containing 1450. It must not create a new conversion.
         */
        var existing =
                db.queryForList(
                        """
                        select *
                        from payment_approvals
                        where hotel_id = ?
                          and branch_id = ?
                          and request_id = ?
                        """,
                        hotel,
                        branch,
                        request.requestId()
                );

        if (!existing.isEmpty()) {

            ApprovalPayload storedPayload =
                    readStoredPayload(
                            (String) existing
                                    .getFirst()
                                    .get("payload"),
                            folio
                    );

            if (!sameClientRequest(
                    storedPayload,
                    request,
                    folio,
                    collectionScope
            )) {

                throw new IllegalStateException(
                        "Request key reused for different payment details."
                );
            }

            return existing.getFirst();
        }

        List<QuotedPart> quotedParts =
                request
                        .parts()
                        .stream()
                        .map(
                                part ->
                                        resolvePart(
                                                hotel,
                                                folio,
                                                part
                                        )
                        )
                        .toList();

        ApprovalPayload payload =
                new ApprovalPayload(
                        request.folioId(),
                        request.requestId(),
                        quotedParts,
                        collectionScope,
                        normalizeCurrency(
                                folio.getCurrency()
                        )
                );

        String payloadJson =
                json.writeValueAsString(
                        payload
                );

        BigDecimal totalBase =
                quotedParts
                        .stream()
                        .map(
                                QuotedPart::baseAmount
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        )
                        .setScale(
                                MONEY_SCALE,
                                RoundingMode.HALF_UP
                        );

        BigDecimal folioBalance =
                folios.balance(
                        hotel,
                        branch,
                        request.folioId()
                );

        if (totalBase.compareTo(
                folioBalance
        ) > 0) {

            throw new IllegalStateException(
                    "Payment exceeds folio balance."
            );
        }

        checkCollection(
                payload,
                totalBase
        );

        UUID approvalId =
                UUID.randomUUID();

        db.update(
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
                hotel,
                branch,
                request.folioId(),
                request.requestId(),
                actor,
                payloadJson
        );

        return Map.of(
                "id",
                approvalId,
                "status",
                "PENDING",
                "folioCurrency",
                normalizeCurrency(
                        folio.getCurrency()
                ),
                "baseAmount",
                totalBase
        );
    }

    public Map<String, Object> approve(
            UUID hotel,
            UUID branch,
            UUID id,
            boolean approved) {

        return approve(
                hotel,
                branch,
                id,
                approved,
                null
        );
    }

    /**
     * Approves or rejects a pending payment.
     *
     * On approval, the previously frozen exchange-rate snapshot is used.
     * The hotel's current rate is NOT queried again.
     */
    public Map<String, Object> approve(
            UUID hotel,
            UUID branch,
            UUID id,
            boolean approved,
            String reason) {

        UUID actor =
                scope.branch(
                        hotel,
                        branch,
                        PAYMENT_VIEW
                );

        if (!hasRole(
                actor,
                hotel,
                Set.of(
                        "OWNER",
                        "MANAGER",
                        "ACCOUNTANT",
                        "CASHIER",
                        "SUPERVISOR"
                )
        )) {

            throw new AccessDeniedException(
                    "Payment approval not permitted."
            );
        }

        var rows =
                db.queryForList(
                        """
                        select *
                        from payment_approvals
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

        var row =
                rows.getFirst();

        if (!row
                .get("status")
                .equals("PENDING")) {

            throw new IllegalStateException(
                    "Request already decided."
            );
        }

        boolean self =
                row
                        .get("requested_by")
                        .equals(actor);

        if (self
                && !hasRole(
                        actor,
                        hotel,
                        Set.of("OWNER")
                )) {

            throw new AccessDeniedException(
                    "A different user must approve payment."
            );
        }

        if (reason != null
                && reason.length() > 1000) {

            throw new IllegalArgumentException(
                    "Reason is too long."
            );
        }

        if (self
                && (reason == null
                || reason.isBlank())) {

            throw new IllegalStateException(
                    "Owner approval of their own payment requires a reason."
            );
        }

        if (approved) {

            /*
             * The person approving/collecting the payment must have
             * an active cashier shift.
             */
            if (db.queryForList(
                    """
                    select id
                    from cashier_shifts
                    where hotel_id = ?
                      and branch_id = ?
                      and cashier_user_id = ?
                      and status = 'OPEN'
                    for update
                    """,
                    hotel,
                    branch,
                    actor
            ).isEmpty()) {

                throw new ApiException(
                        409,
                        "SHIFT_REQUIRED",
                        "Open your cashier shift before approving a payment."
                );
            }

            UUID rowFolioId =
                    (UUID) row.get(
                            "folio_id"
                    );

            Folio folio =
                    folios.lock(
                            hotel,
                            branch,
                            rowFolioId
                    );

            /*
             * readStoredPayload understands:
             *
             * 1. New FX-aware approvals.
             * 2. Old base-currency approvals created before FX support.
             */
            ApprovalPayload payload =
                    readStoredPayload(
                            (String) row.get(
                                    "payload"
                            ),
                            folio
                    );

            if (!rowFolioId.equals(
                    payload.folioId()
            )) {

                throw new IllegalStateException(
                        "Payment approval folio does not match its stored payload."
                );
            }

            if (!normalizeCurrency(
                    folio.getCurrency()
            ).equals(
                    payload.folioCurrency()
            )) {

                throw new IllegalStateException(
                        "Folio currency changed after the payment request was created."
                );
            }

            BigDecimal totalBase =
                    payload
                            .parts()
                            .stream()
                            .map(
                                    QuotedPart::baseAmount
                            )
                            .reduce(
                                    BigDecimal.ZERO,
                                    BigDecimal::add
                            )
                            .setScale(
                                    MONEY_SCALE,
                                    RoundingMode.HALF_UP
                            );

            /*
             * Recheck the charge allocation at approval time because another
             * payment might have been approved while this one was pending.
             */
            checkCollection(
                    payload,
                    totalBase
            );

            BigDecimal currentFolioBalance =
                    folios.balance(
                            hotel,
                            branch,
                            payload.folioId()
                    );

            if (totalBase.compareTo(
                    currentFolioBalance
            ) > 0) {

                throw new IllegalStateException(
                        "Payment exceeds the current folio balance."
                );
            }

            int index = 0;

            for (QuotedPart part
                    : payload.parts()) {

                validateSnapshot(
                        part
                );

                var payment =
                        payments.postApproved(
                                hotel,
                                branch,
                                payload.folioId(),
                                part.method(),
                                part.currency(),
                                part.amount(),
                                part.fxRate(),
                                part.baseAmount(),
                                null,
                                id
                                        + "-"
                                        + index++
                        );

                db.update(
                        """
                        update payments
                        set collection_scope = ?
                        where id = ?
                        """,
                        payload.collectionScope(),
                        payment.id()
                );
            }
        }

        String status =
                approved
                        ? "APPROVED"
                        : "REJECTED";

        db.update(
                """
                update payment_approvals
                set status = ?,
                    approved_by = ?,
                    decision_reason = ?,
                    decided_at = current_timestamp
                where id = ?
                """,
                status,
                actor,
                reason,
                id
        );

        audit.detailed(
                hotel,
                branch,
                actor,
                self
                        ? "OWNER_PAYMENT_DECISION"
                        : "PAYMENT_DECISION",
                "PAYMENT_APPROVAL",
                id,
                json.writeValueAsString(
                        Map.of(
                                "status",
                                "PENDING"
                        )
                ),
                json.writeValueAsString(
                        Map.of(
                                "status",
                                status,
                                "ownerOverride",
                                self
                        )
                ),
                reason,
                status
        );

        return Map.of(
                "id",
                id,
                "status",
                status
        );
    }

    /**
     * Lists payment approvals for the branch.
     */
    public List<Map<String, Object>> list(
            UUID hotel,
            UUID branch) {

        scope.branch(
                hotel,
                branch,
                PAYMENT_VIEW
        );

        return db.queryForList(
                """
                select
                    a.id,
                    a.folio_id,
                    a.requested_by,
                    a.approved_by,
                    a.payload,
                    a.status,
                    a.created_at,
                    a.decision_reason,
                    a.decided_at,
                    c.name customer_name,
                    f.currency
                from payment_approvals a
                join folios f
                  on f.id = a.folio_id
                join customers c
                  on c.id = f.customer_id
                where a.hotel_id = ?
                  and a.branch_id = ?
                order by a.created_at desc
                limit 100
                """,
                hotel,
                branch
        );
    }

    /**
     * Resolves one cashier-entered payment part into an immutable
     * server-generated monetary snapshot.
     */
    private QuotedPart resolvePart(
            UUID hotel,
            Folio folio,
            Part part) {

        validatePart(
                part
        );

        BigDecimal amount =
                part
                        .amount()
                        .setScale(
                                MONEY_SCALE,
                                RoundingMode.HALF_UP
                        );

        String currency =
                normalizeCurrency(
                        part.currency() == null
                                || part.currency().isBlank()
                                    ? folio.getCurrency()
                                    : part.currency()
                );

        String folioCurrency =
                normalizeCurrency(
                        folio.getCurrency()
                );

        /*
         * Base-currency payment.
         *
         * No exchange-rate record is required.
         */
        if (currency.equals(
                folioCurrency
        )) {

            return new QuotedPart(
                    part.method(),
                    currency,
                    amount,
                    BigDecimal.ONE.setScale(
                            FX_SCALE,
                            RoundingMode.HALF_UP
                    ),
                    amount,
                    null,
                    null
            );
        }

        /*
         * Foreign-currency payment.
         *
         * ExchangeRateService chooses the applicable immutable historical
         * rate configured by the hotel.
         */
        ExchangeRateResponse rate =
                exchangeRates.getApplicableRate(
                        hotel,
                        currency,
                        null
                );

        if (!folioCurrency.equals(
                normalizeCurrency(
                        rate.baseCurrencyCode()
                )
        )) {

            throw new IllegalStateException(
                    "Configured exchange rate does not convert into the folio currency."
            );
        }

        BigDecimal fxRate =
                rate
                        .rateToBase()
                        .setScale(
                                FX_SCALE,
                                RoundingMode.HALF_UP
                        );

        BigDecimal baseAmount =
                amount
                        .multiply(
                                fxRate
                        )
                        .setScale(
                                MONEY_SCALE,
                                RoundingMode.HALF_UP
                        );

        return new QuotedPart(
                part.method(),
                currency,
                amount,
                fxRate,
                baseAmount,
                rate.id(),
                rate.effectiveFrom()
        );
    }

    /**
     * Reads both:
     *
     * - Current FX-aware approval payloads.
     * - Legacy base-currency approval payloads.
     *
     * Legacy approvals are safe to interpret as rate 1 because the old
     * payment workflow did not permit a foreign payment currency.
     */
    private ApprovalPayload readStoredPayload(
            String payloadJson,
            Folio folio) {

        if (payloadJson == null
                || payloadJson.isBlank()) {

            throw new IllegalStateException(
                    "Stored payment approval payload is missing."
            );
        }

        ApprovalPayload stored =
                json.readValue(
                        payloadJson,
                        ApprovalPayload.class
                );

        if (stored == null
                || stored.folioId() == null
                || stored.requestId() == null
                || stored.parts() == null
                || stored.parts().isEmpty()) {

            throw new IllegalStateException(
                    "Stored payment approval payload is invalid."
            );
        }

        String folioCurrency =
                normalizeCurrency(
                        folio.getCurrency()
                );

        String collectionScope =
                stored.collectionScope();

        /*
         * Very old approvals may not contain collectionScope.
         *
         * Reconstruct the same historical default:
         * a folio with accommodation charges is ROOM;
         * otherwise it is FOOD.
         */
        if (collectionScope == null
                || collectionScope.isBlank()) {

            boolean room =
                    Boolean.TRUE.equals(
                            db.queryForObject(
                                    """
                                    select exists(
                                        select 1
                                        from folio_entries
                                        where folio_id = ?
                                          and kind = 'ACCOMMODATION'
                                    )
                                    """,
                                    Boolean.class,
                                    folio.getId()
                            )
                    );

            collectionScope =
                    room
                            ? "ROOM"
                            : "FOOD";

        } else {

            collectionScope =
                    collectionScope
                            .trim()
                            .toUpperCase(
                                    Locale.ROOT
                            );
        }

        if (!Set.of(
                "ROOM",
                "FOOD"
        ).contains(collectionScope)) {

            throw new IllegalStateException(
                    "Stored payment collection scope is invalid."
            );
        }

        /*
         * Legacy payload detection is intentionally strict.
         *
         * Every part must be missing all FX-specific fields.
         *
         * We do NOT silently convert a partially corrupted modern
         * FX payload into a rate-1 payment.
         */
        boolean legacy =
                stored.folioCurrency() == null
                && stored
                        .parts()
                        .stream()
                        .allMatch(
                                part ->
                                        part.currency() == null
                                        && part.fxRate() == null
                                        && part.baseAmount() == null
                                        && part.exchangeRateId() == null
                                        && part.rateEffectiveFrom() == null
                        );

        if (legacy) {

            List<QuotedPart> converted =
                    stored
                            .parts()
                            .stream()
                            .map(
                                    part -> {

                                        if (part.method() == null
                                                || part.method()
                                                        == PaymentMethod.CREDIT
                                                || part.amount() == null
                                                || part.amount().signum() <= 0
                                                || part.amount().scale() > 4) {

                                            throw new IllegalStateException(
                                                    "Legacy payment approval payload is invalid."
                                            );
                                        }

                                        BigDecimal amount =
                                                part
                                                        .amount()
                                                        .setScale(
                                                                MONEY_SCALE,
                                                                RoundingMode.HALF_UP
                                                        );

                                        return new QuotedPart(
                                                part.method(),
                                                folioCurrency,
                                                amount,
                                                BigDecimal.ONE.setScale(
                                                        FX_SCALE,
                                                        RoundingMode.HALF_UP
                                                ),
                                                amount,
                                                null,
                                                null
                                        );
                                    }
                            )
                            .toList();

            return new ApprovalPayload(
                    stored.folioId(),
                    stored.requestId(),
                    converted,
                    collectionScope,
                    folioCurrency
            );
        }

        /*
         * A modern payload must explicitly contain its accounting
         * currency and complete monetary snapshots.
         */
        if (stored.folioCurrency() == null
                || stored.folioCurrency().isBlank()) {

            throw new IllegalStateException(
                    "Stored payment exchange-rate snapshot is incomplete."
            );
        }

        String storedFolioCurrency =
                normalizeCurrency(
                        stored.folioCurrency()
                );

        List<QuotedPart> normalizedParts =
                stored
                        .parts()
                        .stream()
                        .map(
                                part -> {

                                    if (part == null
                                            || part.currency() == null
                                            || part.currency().isBlank()) {

                                        throw new IllegalStateException(
                                                "Stored payment exchange-rate snapshot is incomplete."
                                        );
                                    }

                                    return new QuotedPart(
                                            part.method(),
                                            normalizeCurrency(
                                                    part.currency()
                                            ),
                                            part.amount(),
                                            part.fxRate(),
                                            part.baseAmount(),
                                            part.exchangeRateId(),
                                            part.rateEffectiveFrom()
                                    );
                                }
                        )
                        .toList();

        return new ApprovalPayload(
                stored.folioId(),
                stored.requestId(),
                normalizedParts,
                collectionScope,
                storedFolioCurrency
        );
    }

    /**
     * Checks whether a retry using the same requestId represents the
     * same cashier request.
     *
     * The frozen FX rate is deliberately NOT compared.
     *
     * This allows an idempotent retry to continue returning the original
     * approval even after the hotel's configured rate changes.
     */
    private boolean sameClientRequest(
            ApprovalPayload stored,
            Request incoming,
            Folio folio,
            String collectionScope) {

        if (!Objects.equals(
                stored.folioId(),
                incoming.folioId()
        )
                || !Objects.equals(
                        stored.requestId(),
                        incoming.requestId()
                )
                || !Objects.equals(
                        stored.collectionScope(),
                        collectionScope
                )
                || stored.parts().size()
                        != incoming.parts().size()) {

            return false;
        }

        for (int index = 0;
                index < incoming.parts().size();
                index++) {

            Part incomingPart =
                    incoming
                            .parts()
                            .get(index);

            QuotedPart storedPart =
                    stored
                            .parts()
                            .get(index);

            validatePart(
                    incomingPart
            );

            String incomingCurrency =
                    normalizeCurrency(
                            incomingPart.currency() == null
                                    || incomingPart.currency().isBlank()
                                        ? folio.getCurrency()
                                        : incomingPart.currency()
                    );

            if (storedPart.method()
                    != incomingPart.method()) {

                return false;
            }

            if (storedPart.currency() == null
                    || !normalizeCurrency(
                            storedPart.currency()
                    ).equals(
                            incomingCurrency
                    )) {

                return false;
            }

            if (storedPart.amount() == null
                    || storedPart
                            .amount()
                            .compareTo(
                                    incomingPart.amount()
                            ) != 0) {

                return false;
            }
        }

        return true;
    }

    private void validateRequest(
            Request request) {

        if (request == null
                || request.folioId() == null
                || request.requestId() == null
                || request.parts() == null
                || request.parts().isEmpty()
                || request.parts().size() > 8) {

            throw new IllegalArgumentException(
                    "Invalid payment."
            );
        }

        request
                .parts()
                .forEach(
                        this::validatePart
                );
    }

    private void validatePart(
            Part part) {

        if (part == null
                || part.method() == null
                || part.method()
                        == PaymentMethod.CREDIT
                || part.amount() == null
                || part.amount().signum() <= 0
                || part.amount().scale() > 4) {

            throw new IllegalArgumentException(
                    "Invalid payment part."
            );
        }
    }

    /**
     * Validates that the frozen base amount still mathematically matches
     * the frozen original amount and exchange rate.
     */
    private void validateSnapshot(
            QuotedPart part) {

        if (part == null
                || part.method() == null
                || part.method() == PaymentMethod.CREDIT
                || part.currency() == null
                || part.currency().isBlank()
                || part.amount() == null
                || part.fxRate() == null
                || part.baseAmount() == null
                || part.amount().signum() <= 0
                || part.fxRate().signum() <= 0
                || part.baseAmount().signum() <= 0) {

            throw new IllegalStateException(
                    "Payment exchange-rate snapshot is invalid."
            );
        }

        BigDecimal expected =
                part
                        .amount()
                        .multiply(
                                part.fxRate()
                        )
                        .setScale(
                                MONEY_SCALE,
                                RoundingMode.HALF_UP
                        );

        if (expected.compareTo(
                part.baseAmount()
        ) != 0) {

            throw new IllegalStateException(
                    "Payment exchange-rate snapshot is inconsistent."
            );
        }
    }

    /**
     * Ensures that the payment does not exceed the outstanding amount
     * belonging to the selected collection category.
     */
    private void checkCollection(
            ApprovalPayload request,
            BigDecimal totalBase) {

        String collection =
                request.collectionScope();

        if (!Set.of(
                "ROOM",
                "FOOD"
        ).contains(collection)) {

            throw new IllegalStateException(
                    "Invalid payment collection scope."
            );
        }

        BigDecimal charges =
                db.queryForObject(
                        "select coalesce(sum(amount),0) "
                                + "from folio_entries "
                                + "where folio_id=? and "
                                + (
                                collection.equals("ROOM")
                                        ? "kind='ACCOMMODATION'"
                                        : "kind not in ('ACCOMMODATION','PAYMENT','REFUND')"
                        ),
                        BigDecimal.class,
                        request.folioId()
                );

        BigDecimal paid =
                db.queryForObject(
                        """
                        select coalesce(sum(base_amount),0)
                        from payments
                        where folio_id = ?
                          and collection_scope = ?
                          and status = 'POSTED'
                        """,
                        BigDecimal.class,
                        request.folioId(),
                        collection
                );

        BigDecimal outstanding =
                charges.subtract(
                        paid
                );

        if (totalBase.compareTo(
                outstanding
        ) > 0) {

            throw new IllegalStateException(
                    "Payment exceeds the selected charge balance."
            );
        }
    }

    private String normalizeCurrency(
            String currency) {

        if (currency == null) {

            throw new IllegalArgumentException(
                    "Payment currency is required."
            );
        }

        String normalized =
                currency
                        .trim()
                        .toUpperCase(
                                Locale.ROOT
                        );

        if (!normalized.matches(
                "^[A-Z]{3}$"
        )) {

            throw new IllegalArgumentException(
                    "Payment currency must contain exactly three letters."
            );
        }

        return normalized;
    }

    private boolean hasRole(
            UUID actor,
            UUID hotel,
            Set<String> allowed) {

        return db.queryForList(
                        """
                        select r.code
                        from hotel_memberships m
                        join membership_roles mr
                          on mr.membership_id = m.id
                        join roles r
                          on r.id = mr.role_id
                        where m.user_id = ?
                          and m.hotel_id = ?
                          and m.status = 'ACTIVE'
                          and r.active = true
                        """,
                        String.class,
                        actor,
                        hotel
                )
                .stream()
                .anyMatch(
                        allowed::contains
                );
    }
}