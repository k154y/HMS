package com.hotelmanagement.hms.payment.service;

import com.hotelmanagement.hms.audit.service.AuditService;
import com.hotelmanagement.hms.folio.model.EntryKind;
import com.hotelmanagement.hms.folio.service.FolioService;
import com.hotelmanagement.hms.payment.dto.PaymentResponse;
import com.hotelmanagement.hms.payment.model.Payment;
import com.hotelmanagement.hms.payment.model.PaymentMethod;
import com.hotelmanagement.hms.payment.repository.PaymentRepository;
import com.hotelmanagement.hms.shared.model.Money;
import com.hotelmanagement.hms.shared.service.OperationScope;
import com.hotelmanagement.hms.shared.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import static com.hotelmanagement.hms.identity.authorization.model.PermissionCode.PAYMENT_RECORD;
import static com.hotelmanagement.hms.identity.authorization.model.PermissionCode.PAYMENT_REFUND;
import static com.hotelmanagement.hms.identity.authorization.model.PermissionCode.PAYMENT_VIEW;
import static com.hotelmanagement.hms.identity.authorization.model.PermissionCode.PAYMENT_VOID;

@Service
@Transactional
public class PaymentService {

    private static final int MONEY_SCALE = 4;

    private final PaymentRepository payments;
    private final FolioService folios;
    private final OperationScope scope;
    private final AuditService audit;

    public PaymentService(
            PaymentRepository payments,
            FolioService folios,
            OperationScope scope,
            AuditService audit) {

        this.payments = payments;
        this.folios = folios;
        this.scope = scope;
        this.audit = audit;
    }

    /**
     * Posts a payment whose FX rate has already been resolved and frozen
     * by the trusted payment workflow.
     *
     * The original foreign-currency amount, FX rate and resulting folio/base
     * amount are stored permanently on the payment.
     */
    public PaymentResponse postApproved(
            UUID hotel,
            UUID branch,
            UUID folioId,
            PaymentMethod method,
            String currency,
            BigDecimal originalAmount,
            BigDecimal fxRate,
            BigDecimal expectedBaseAmount,
            String externalReference,
            String idempotencyKey) {

        UUID actor =
                scope.branch(
                        hotel,
                        branch,
                        PAYMENT_RECORD
                );

        if (folioId == null) {
            throw new IllegalArgumentException(
                    "Folio is required."
            );
        }

        if (method == null) {
            throw new IllegalArgumentException(
                    "Payment method is required."
            );
        }

        if (method == PaymentMethod.CREDIT) {
            throw new IllegalArgumentException(
                    "Credit is not a payment method."
            );
        }

        if (currency == null
                || !currency.matches("^[A-Z]{3}$")) {

            throw new IllegalArgumentException(
                    "Payment currency must contain exactly three uppercase letters."
            );
        }

        BigDecimal amount =
                Money.positive(originalAmount);

        if (fxRate == null
                || fxRate.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Exchange rate must be greater than zero."
            );
        }

        BigDecimal normalizedRate =
                fxRate.setScale(
                        8,
                        RoundingMode.HALF_UP
                );

        BigDecimal baseAmount =
                amount
                        .multiply(normalizedRate)
                        .setScale(
                                MONEY_SCALE,
                                RoundingMode.HALF_UP
                        );

        if (expectedBaseAmount == null
                || baseAmount.compareTo(
                        expectedBaseAmount.setScale(
                                MONEY_SCALE,
                                RoundingMode.HALF_UP
                        )
                ) != 0) {

            throw new IllegalStateException(
                    "Payment exchange-rate snapshot is inconsistent."
            );
        }

        var prior =
                payments
                        .findByHotelIdAndBranchIdAndIdempotencyKey(
                                hotel,
                                branch,
                                idempotencyKey
                        );

        if (prior.isPresent()) {

            var existing =
                    prior.get();

            boolean samePayment =
                    existing
                            .getFolioId()
                            .equals(folioId)
                    && existing
                            .getOriginalAmount()
                            .compareTo(amount) == 0
                    && existing.getMethod() == method
                    && existing
                            .getCurrency()
                            .equals(currency)
                    && existing
                            .getFxRate()
                            .compareTo(normalizedRate) == 0
                    && existing
                            .getBaseAmount()
                            .compareTo(baseAmount) == 0;

            if (!samePayment) {
                throw new IllegalStateException(
                        "Payment key reused for different payment details."
                );
            }

            return PaymentResponse.from(
                    existing
            );
        }

        var folio =
                folios.lock(
                        hotel,
                        branch,
                        folioId
                );

        /*
         * The converted amount always has to settle the folio's
         * accounting currency.
         *
         * PaymentWorkflow is responsible for ensuring that the
         * configured FX rate converts into this currency.
         */
        BigDecimal outstandingBalance =
                folios.balance(
                        hotel,
                        branch,
                        folioId
                );

        if (baseAmount.compareTo(
                outstandingBalance
        ) > 0) {

            throw new IllegalStateException(
                    "Payment exceeds folio balance."
            );
        }

        Payment payment =
                payments.saveAndFlush(
                        Payment.create(
                                hotel,
                                branch,
                                folioId,
                                actor,
                                method,
                                currency,
                                amount,
                                normalizedRate,
                                baseAmount,
                                externalReference,
                                idempotencyKey,
                                actor
                        )
                );

        folios.post(
                hotel,
                branch,
                folio.getId(),
                baseAmount.negate(),
                EntryKind.PAYMENT,
                payment.getId(),
                paymentMemo(
                        method,
                        currency,
                        amount,
                        normalizedRate,
                        folio.getCurrency()
                ),
                actor
        );

        audit.record(
                hotel,
                branch,
                actor,
                "PAYMENT_POSTED",
                "PAYMENT",
                payment.getId()
        );

        return PaymentResponse.from(
                payment
        );
    }

    @Transactional(readOnly = true)
    public PaymentResponse get(
            UUID hotel,
            UUID branch,
            UUID id) {

        scope.branch(
                hotel,
                branch,
                PAYMENT_VIEW
        );

        return PaymentResponse.from(
                payments
                        .findByIdAndHotelIdAndBranchId(
                                id,
                                hotel,
                                branch
                        )
                        .orElseThrow(
                                ApiException::notFound
                        )
        );
    }

    public void voidPayment(
            UUID hotel,
            UUID branch,
            UUID id) {

        UUID actor =
                scope.branch(
                        hotel,
                        branch,
                        PAYMENT_VOID
                );

        var payment =
                payments
                        .lock(
                                id,
                                hotel,
                                branch
                        )
                        .orElseThrow(
                                ApiException::notFound
                        );

        payment.voidPayment();

        folios.post(
                hotel,
                branch,
                payment.getFolioId(),
                payment.getBaseAmount(),
                EntryKind.REFUND,
                payment.getId(),
                "Payment void",
                actor
        );

        audit.record(
                hotel,
                branch,
                actor,
                "PAYMENT_VOIDED",
                "PAYMENT",
                id
        );
    }

    public void refund(
            UUID hotel,
            UUID branch,
            UUID id) {

        UUID actor =
                scope.branch(
                        hotel,
                        branch,
                        PAYMENT_REFUND
                );

        var payment =
                payments
                        .lock(
                                id,
                                hotel,
                                branch
                        )
                        .orElseThrow(
                                ApiException::notFound
                        );

        payment.refund();

        folios.post(
                hotel,
                branch,
                payment.getFolioId(),
                payment.getBaseAmount(),
                EntryKind.REFUND,
                payment.getId(),
                "Payment refund",
                actor
        );

        audit.record(
                hotel,
                branch,
                actor,
                "PAYMENT_REFUNDED",
                "PAYMENT",
                id
        );
    }

    private String paymentMemo(
            PaymentMethod method,
            String paymentCurrency,
            BigDecimal originalAmount,
            BigDecimal fxRate,
            String folioCurrency) {

        if (paymentCurrency.equals(
                folioCurrency
        )) {

            return "Payment "
                    + method;
        }

        return "Payment "
                + method
                + " - "
                + originalAmount
                + " "
                + paymentCurrency
                + " @ "
                + fxRate
                + " "
                + folioCurrency;
    }
}