package com.hotelmanagement.hms.payment.web;

import com.hotelmanagement.hms.payment.dto.PaymentRequest;
import com.hotelmanagement.hms.payment.dto.PaymentResponse;
import com.hotelmanagement.hms.payment.service.PaymentService;
import com.hotelmanagement.hms.payment.service.PaymentWorkflow;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(
        "/api/v1/hotels/{hotel}/branches/{branch}/payments"
)
public class PaymentController {

    private final PaymentService service;
    private final PaymentWorkflow workflow;

    public PaymentController(
            PaymentService service,
            PaymentWorkflow workflow) {

        this.service = service;
        this.workflow = workflow;
    }

    /**
     * Existing single-payment request endpoint.
     *
     * The backend resolves the authoritative FX rate.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> create(
            @PathVariable UUID hotel,
            @PathVariable UUID branch,
            @Valid @RequestBody PaymentRequest request) {

        return workflow.requestSingle(
                hotel,
                branch,
                request
        );
    }

    /**
     * Returns a server-authoritative payment conversion preview.
     *
     * Example:
     *
     * 100 USD -> 145,000 RWF
     */
    @PostMapping("/quote")
    public PaymentWorkflow.QuoteResponse quote(
            @PathVariable UUID hotel,
            @PathVariable UUID branch,
            @RequestBody PaymentWorkflow.QuoteRequest request) {

        return workflow.quote(
                hotel,
                branch,
                request
        );
    }

    @GetMapping("/{id}")
    public PaymentResponse get(
            @PathVariable UUID hotel,
            @PathVariable UUID branch,
            @PathVariable UUID id) {

        return service.get(
                hotel,
                branch,
                id
        );
    }

    @PostMapping("/{id}/refund")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void refund(
            @PathVariable UUID hotel,
            @PathVariable UUID branch,
            @PathVariable UUID id) {

        service.refund(
                hotel,
                branch,
                id
        );
    }

    @PostMapping("/{id}/void")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void voidPayment(
            @PathVariable UUID hotel,
            @PathVariable UUID branch,
            @PathVariable UUID id) {

        service.voidPayment(
                hotel,
                branch,
                id
        );
    }
}