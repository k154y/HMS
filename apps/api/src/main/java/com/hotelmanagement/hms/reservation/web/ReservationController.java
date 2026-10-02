package com.hotelmanagement.hms.reservation.web;
import com.hotelmanagement.hms.reservation.dto.ReservationUpdateRequest;

import com.hotelmanagement.hms.payment.service.PaymentWorkflow;
import com.hotelmanagement.hms.reservation.dto.ReservationPaymentSummary;
import com.hotelmanagement.hms.reservation.dto.ReservationRequest;
import com.hotelmanagement.hms.reservation.dto.ReservationResponse;
import com.hotelmanagement.hms.reservation.service.ReservationService;
import com.hotelmanagement.hms.shared.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(
        "/api/v1/hotels/{hotel}/branches/{branch}/reservations"
)
public class ReservationController {

    private final ReservationService service;
    private final PaymentWorkflow paymentWorkflow;

    public ReservationController(
            ReservationService service,
            PaymentWorkflow paymentWorkflow) {

        this.service = service;
        this.paymentWorkflow = paymentWorkflow;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse create(
            @PathVariable UUID hotel,
            @PathVariable UUID branch,
            @Valid @RequestBody ReservationRequest request) {

        return service.create(
                hotel,
                branch,
                request
        );
    }

    @GetMapping("/availability")
    public PageResponse<com.hotelmanagement.hms.room.dto.RoomResponse> availability(
            @PathVariable UUID hotel,
            @PathVariable UUID branch,
            @RequestParam LocalDate checkIn,
            @RequestParam LocalDate checkOut,
            @RequestParam(defaultValue = "1") int adults,
            @RequestParam(defaultValue = "0") int children,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        return service.available(
                hotel,
                branch,
                checkIn,
                checkOut,
                adults,
                children,
                page,
                size
        );
    }

    @GetMapping
    public PageResponse<ReservationResponse> list(
            @PathVariable UUID hotel,
            @PathVariable UUID branch,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        return service.list(
                hotel,
                branch,
                page,
                size
        );
    }

    @GetMapping("/{id}")
    public ReservationResponse get(
            @PathVariable UUID hotel,
            @PathVariable UUID branch,
            @PathVariable UUID id) {

        return service.get(
                hotel,
                branch,
                id
        );
    }

    @GetMapping("/{id}/payment-summary")
    public ReservationPaymentSummary paymentSummary(
            @PathVariable UUID hotel,
            @PathVariable UUID branch,
            @PathVariable UUID id) {

        return service.paymentSummary(
                hotel,
                branch,
                id
        );
    }

    @PostMapping("/{id}/advance-payments")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> requestAdvance(
            @PathVariable UUID hotel,
            @PathVariable UUID branch,
            @PathVariable UUID id,
            @RequestBody PaymentWorkflow.AdvanceRequest request) {

        return paymentWorkflow.requestReservationAdvance(
                hotel,
                branch,
                id,
                request
        );
    }

    @PutMapping("/{id}")
    public ReservationResponse update(
            @PathVariable UUID hotel,
            @PathVariable UUID branch,
            @PathVariable UUID id,
            @Valid @RequestBody ReservationUpdateRequest request) {

        return service.update(
                hotel,
                branch,
                id,
                request
        );
    }


    @PostMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(
            @PathVariable UUID hotel,
            @PathVariable UUID branch,
            @PathVariable UUID id) {

        service.cancel(
                hotel,
                branch,
                id
        );
    }
}
