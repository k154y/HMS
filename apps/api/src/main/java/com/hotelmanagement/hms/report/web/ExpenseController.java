package com.hotelmanagement.hms.report.web;

import com.hotelmanagement.hms.audit.service.AuditService;
import com.hotelmanagement.hms.payment.model.PaymentMethod;
import com.hotelmanagement.hms.shared.model.Money;
import com.hotelmanagement.hms.shared.service.OperationScope;
import com.hotelmanagement.hms.shared.web.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.hotelmanagement.hms.identity.authorization.model.PermissionCode.EXPENSE_RECORD;
import static com.hotelmanagement.hms.identity.authorization.model.PermissionCode.EXPENSE_VIEW;

@RestController
@Transactional
@RequestMapping(
        "/api/v1/hotels/{hotel}/branches/{branch}/expenses"
)
public class ExpenseController {

    private final JdbcTemplate db;
    private final OperationScope scope;
    private final AuditService audit;

    public ExpenseController(
            JdbcTemplate db,
            OperationScope scope,
            AuditService audit) {

        this.db = db;
        this.scope = scope;
        this.audit = audit;
    }

    /**
     * HTTP expense request.
     *
     * The category is selected by ID from the hotel's managed
     * expense-category list.
     */
    public record ExpenseRequest(

            @NotNull(
                    message =
                            "Expense date is required."
            )
            LocalDate date,

            @NotNull(
                    message =
                            "Expense category is required."
            )
            UUID categoryId,

            @NotBlank(
                    message =
                            "Description is required."
            )
            @Size(
                    max = 1000,
                    message =
                            "Description must not exceed 1000 characters."
            )
            String description,

            @NotNull(
                    message =
                            "Amount is required."
            )
            @DecimalMin(
                    value = "0.0001",
                    message =
                            "Amount must be greater than zero."
            )
            @Digits(
                    integer = 15,
                    fraction = 4,
                    message =
                            "Amount can have at most 15 integer digits and 4 decimal places."
            )
            BigDecimal amount,

            @NotBlank(
                    message =
                            "Payment method is required."
            )
            @Pattern(
                    regexp =
                            "CASH|MOBILE_MONEY|CARD|BANK_TRANSFER",
                    message =
                            "Choose a valid expense payment method."
            )
            String method,

            @NotNull(
                    message =
                            "Request identifier is required."
            )
            UUID requestId) {
    }

    /**
     * Compatibility type retained for Java integration tests and
     * direct internal callers that existed before managed categories.
     *
     * It is NOT the HTTP request body.
     */
    @Deprecated
    public record Expense(
            LocalDate date,
            String category,
            String description,
            BigDecimal amount,
            PaymentMethod method,
            UUID requestId) {
    }

    @GetMapping
    @Transactional(readOnly = true)
    public Object list(
            @PathVariable UUID hotel,
            @PathVariable UUID branch) {

        scope.branch(
                hotel,
                branch,
                EXPENSE_VIEW
        );

        return db.queryForList(
                """
                select
                    id,
                    expense_date,
                    category_id,
                    category,
                    description,
                    amount,
                    method,
                    request_id,
                    actor_id,
                    created_at
                from expenses
                where hotel_id = ?
                  and branch_id = ?
                order by
                    expense_date desc,
                    created_at desc,
                    id
                limit 1000
                """,
                hotel,
                branch
        );
    }

    @PostMapping
    @ResponseStatus(
            HttpStatus.CREATED
    )
    public Object create(
            @PathVariable UUID hotel,
            @PathVariable UUID branch,
            @Valid
            @RequestBody
            ExpenseRequest request) {

        return createManaged(
                hotel,
                branch,
                request.date(),
                request.categoryId(),
                request.description(),
                request.amount(),
                PaymentMethod.valueOf(
                        request.method()
                ),
                request.requestId()
        );
    }

    /**
     * Backward-compatible direct Java method.
     *
     * This is intentionally not annotated as an HTTP endpoint.
     *
     * It allows the existing integration tests/direct callers to
     * continue compiling while production HTTP requests are forced
     * to use a managed category ID.
     */
    @Deprecated
    public Object create(
            UUID hotel,
            UUID branch,
            Expense request) {

        UUID categoryId =
                legacyCategory(
                        hotel,
                        request.category()
                );

        return createManaged(
                hotel,
                branch,
                request.date(),
                categoryId,
                request.description(),
                request.amount(),
                request.method(),
                request.requestId()
        );
    }

    private Object createManaged(
            UUID hotel,
            UUID branch,
            LocalDate date,
            UUID categoryId,
            String description,
            BigDecimal requestedAmount,
            PaymentMethod method,
            UUID requestId) {

        UUID actor =
                scope.branch(
                        hotel,
                        branch,
                        EXPENSE_RECORD
                );

        if (date == null) {
            throw new ApiException(
                    400,
                    "EXPENSE_DATE_REQUIRED",
                    "Expense date is required."
            );
        }

        if (categoryId == null) {
            throw new ApiException(
                    400,
                    "EXPENSE_CATEGORY_INVALID",
                    "Select an expense category."
            );
        }

        if (
                description == null
                        || description.isBlank()
        ) {
            throw new ApiException(
                    400,
                    "EXPENSE_DESCRIPTION_REQUIRED",
                    "Description is required."
            );
        }

        if (description.length() > 1000) {
            throw new ApiException(
                    400,
                    "EXPENSE_DESCRIPTION_INVALID",
                    "Description must not exceed 1000 characters."
            );
        }

        if (method == null) {
            throw new ApiException(
                    400,
                    "EXPENSE_METHOD_REQUIRED",
                    "Payment method is required."
            );
        }

        if (method == PaymentMethod.CREDIT) {
            throw new ApiException(
                    400,
                    "EXPENSE_METHOD_INVALID",
                    "Choose a valid expense payment method."
            );
        }

        if (requestId == null) {
            throw new ApiException(
                    400,
                    "EXPENSE_REQUEST_ID_REQUIRED",
                    "Request identifier is required."
            );
        }

        BigDecimal amount =
                Money.positive(
                        requestedAmount
                );

        String cleanDescription =
                description.trim();

        List<Map<String, Object>> categories =
                db.queryForList(
                        """
                        select
                            id,
                            name
                        from expense_categories
                        where hotel_id = ?
                          and id = ?
                          and active = true
                        """,
                        hotel,
                        categoryId
                );

        if (categories.isEmpty()) {
            throw new ApiException(
                    400,
                    "EXPENSE_CATEGORY_INVALID",
                    "Select an active expense category."
            );
        }

        String categoryName =
                String.valueOf(
                        categories
                                .getFirst()
                                .get("name")
                );

        List<Map<String, Object>> existing =
                db.queryForList(
                        """
                        select *
                        from expenses
                        where hotel_id = ?
                          and branch_id = ?
                          and request_id = ?
                        """,
                        hotel,
                        branch,
                        requestId
                );

        if (!existing.isEmpty()) {

            Boolean sameRequest =
                    db.queryForObject(
                            """
                            select exists(
                                select 1
                                from expenses
                                where hotel_id = ?
                                  and branch_id = ?
                                  and request_id = ?
                                  and expense_date = ?
                                  and category_id = ?
                                  and category = ?
                                  and description = ?
                                  and amount = ?
                                  and method = ?
                            )
                            """,
                            Boolean.class,
                            hotel,
                            branch,
                            requestId,
                            date,
                            categoryId,
                            categoryName,
                            cleanDescription,
                            amount,
                            method.name()
                    );

            if (!Boolean.TRUE.equals(
                    sameRequest
            )) {
                throw new IllegalStateException(
                        "Expense request identifier was already used for different information."
                );
            }

            return existing.getFirst();
        }

        UUID id =
                UUID.randomUUID();

        db.update(
                """
                insert into expenses(
                    id,
                    hotel_id,
                    branch_id,
                    expense_date,
                    category_id,
                    category,
                    description,
                    amount,
                    method,
                    request_id,
                    actor_id
                )
                values(
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
                )
                """,
                id,
                hotel,
                branch,
                date,
                categoryId,
                categoryName,
                cleanDescription,
                amount,
                method.name(),
                requestId,
                actor
        );

        audit.record(
                hotel,
                branch,
                actor,
                "EXPENSE_RECORDED",
                "EXPENSE",
                id
        );

        return Map.of(
                "id",
                id,
                "categoryId",
                categoryId,
                "category",
                categoryName
        );
    }

    /**
     * Compatibility support for pre-V27 direct Java callers.
     *
     * Production HTTP requests never enter this method.
     */
    private UUID legacyCategory(
            UUID hotel,
            String requestedName) {

        String name =
                requestedName == null
                        || requestedName.isBlank()
                        ? "Legacy Expense"
                        : requestedName.trim();

        List<UUID> existing =
                db.query(
                        """
                        select id
                        from expense_categories
                        where hotel_id = ?
                          and lower(btrim(name)) =
                              lower(btrim(?))
                        limit 1
                        """,
                        (result, row) ->
                                result.getObject(
                                        "id",
                                        UUID.class
                                ),
                        hotel,
                        name
                );

        if (!existing.isEmpty()) {
            return existing.getFirst();
        }

        UUID id =
                UUID.randomUUID();

        db.update(
                """
                insert into expense_categories(
                    id,
                    hotel_id,
                    name,
                    active
                )
                values(
                    ?, ?, ?, true
                )
                """,
                id,
                hotel,
                name
        );

        return id;
    }
}