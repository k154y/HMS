package com.hotelmanagement.hms.nonresident.web;

import com.hotelmanagement.hms.nonresident.dto.NonResidentBillActionRequest;
import com.hotelmanagement.hms.nonresident.dto.NonResidentBillRequest;
import com.hotelmanagement.hms.nonresident.dto.NonResidentBillResponse;
import com.hotelmanagement.hms.nonresident.service.NonResidentBillService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(
        "/api/v1/hotels/{hotel}/branches/{branch}/non-resident-bills"
)
public class NonResidentBillController {

    private final NonResidentBillService service;

    public NonResidentBillController(
            NonResidentBillService service) {

        this.service =
                service;
    }

    @GetMapping
    public List<NonResidentBillResponse> list(
            @PathVariable UUID hotel,
            @PathVariable UUID branch) {

        return service.list(
                hotel,
                branch
        );
    }

    @GetMapping("/{id}")
    public NonResidentBillResponse get(
            @PathVariable UUID hotel,
            @PathVariable UUID branch,
            @PathVariable UUID id) {

        return service.get(
                hotel,
                branch,
                id
        );
    }

    @PostMapping
    @ResponseStatus(
            HttpStatus.CREATED
    )
    public NonResidentBillResponse create(
            @PathVariable UUID hotel,
            @PathVariable UUID branch,
            @Valid
            @RequestBody
            NonResidentBillRequest request) {

        return service.create(
                hotel,
                branch,
                request
        );
    }

    @PostMapping("/{id}/cancel")
    public NonResidentBillResponse cancel(
            @PathVariable UUID hotel,
            @PathVariable UUID branch,
            @PathVariable UUID id,
            @Valid
            @RequestBody
            NonResidentBillActionRequest request) {

        return service.cancel(
                hotel,
                branch,
                id,
                request
        );
    }

    @PostMapping("/{id}/void")
    public NonResidentBillResponse voidBill(
            @PathVariable UUID hotel,
            @PathVariable UUID branch,
            @PathVariable UUID id,
            @Valid
            @RequestBody
            NonResidentBillActionRequest request) {

        return service.voidBill(
                hotel,
                branch,
                id,
                request
        );
    }
}