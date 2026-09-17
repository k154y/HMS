package com.hotelmanagement.hms.report.web;

import com.hotelmanagement.hms.audit.service.AuditService;
import com.hotelmanagement.hms.shared.service.OperationScope;
import com.hotelmanagement.hms.shared.web.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.hotelmanagement.hms.identity.authorization.model.PermissionCode.EXPENSE_CATEGORY_MANAGE;
import static com.hotelmanagement.hms.identity.authorization.model.PermissionCode.EXPENSE_VIEW;

@RestController
@Transactional
@RequestMapping(
        "/api/v1/hotels/{hotel}/expense-categories"
)
public class ExpenseCategoryController {

    private final JdbcTemplate db;
    private final OperationScope scope;
    private final AuditService audit;

    public ExpenseCategoryController(
            JdbcTemplate db,
            OperationScope scope,
            AuditService audit) {

        this.db = db;
        this.scope = scope;
        this.audit = audit;
    }

    public record CategoryRequest(

            @NotBlank(
                    message =
                            "Category name is required."
            )
            @Size(
                    max = 100,
                    message =
                            "Category name must not exceed 100 characters."
            )
            String name) {
    }

    public record CategoryUpdate(

            @NotBlank(
                    message =
                            "Category name is required."
            )
            @Size(
                    max = 100,
                    message =
                            "Category name must not exceed 100 characters."
            )
            String name,

            @NotNull(
                    message =
                            "Category status is required."
            )
            Boolean active) {
    }

    public record CategoryResponse(
            UUID id,
            String name,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<CategoryResponse> list(
            @PathVariable UUID hotel,
            @RequestParam(
                    defaultValue = "false"
            ) boolean includeInactive) {

        if (includeInactive) {
            scope.hotel(
                    hotel,
                    EXPENSE_CATEGORY_MANAGE
            );
        } else {
            scope.hotel(
                    hotel,
                    EXPENSE_VIEW
            );
        }

        String sql =
                includeInactive
                        ? """
                          select
                              id,
                              name,
                              active,
                              created_at,
                              updated_at
                          from expense_categories
                          where hotel_id = ?
                          order by active desc, lower(name), id
                          """
                        : """
                          select
                              id,
                              name,
                              active,
                              created_at,
                              updated_at
                          from expense_categories
                          where hotel_id = ?
                            and active = true
                          order by lower(name), id
                          """;

        return db.query(
                sql,
                this::category,
                hotel
        );
    }

    @PostMapping
    @ResponseStatus(
            HttpStatus.CREATED
    )
    public CategoryResponse create(
            @PathVariable UUID hotel,
            @Valid
            @RequestBody
            CategoryRequest request) {

        UUID actor =
                scope.hotel(
                        hotel,
                        EXPENSE_CATEGORY_MANAGE
                );

        String name =
                request
                        .name()
                        .trim();

        ensureUniqueName(
                hotel,
                null,
                name
        );

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

        audit.record(
                hotel,
                null,
                actor,
                "EXPENSE_CATEGORY_CREATED",
                "EXPENSE_CATEGORY",
                id
        );

        return get(
                hotel,
                id
        );
    }

    @PutMapping("/{id}")
    public CategoryResponse update(
            @PathVariable UUID hotel,
            @PathVariable UUID id,
            @Valid
            @RequestBody
            CategoryUpdate request) {

        UUID actor =
                scope.hotel(
                        hotel,
                        EXPENSE_CATEGORY_MANAGE
                );

        requireCategory(
                hotel,
                id
        );

        String name =
                request
                        .name()
                        .trim();

        ensureUniqueName(
                hotel,
                id,
                name
        );

        db.update(
                """
                update expense_categories
                set
                    name = ?,
                    active = ?,
                    updated_at = current_timestamp
                where hotel_id = ?
                  and id = ?
                """,
                name,
                request.active(),
                hotel,
                id
        );

        audit.record(
                hotel,
                null,
                actor,
                "EXPENSE_CATEGORY_UPDATED",
                "EXPENSE_CATEGORY",
                id
        );

        return get(
                hotel,
                id
        );
    }

    private CategoryResponse get(
            UUID hotel,
            UUID id) {

        List<CategoryResponse> result =
                db.query(
                        """
                        select
                            id,
                            name,
                            active,
                            created_at,
                            updated_at
                        from expense_categories
                        where hotel_id = ?
                          and id = ?
                        """,
                        this::category,
                        hotel,
                        id
                );

        if (result.isEmpty()) {
            throw ApiException.notFound();
        }

        return result.getFirst();
    }

    private void requireCategory(
            UUID hotel,
            UUID id) {

        Boolean exists =
                db.queryForObject(
                        """
                        select exists(
                            select 1
                            from expense_categories
                            where hotel_id = ?
                              and id = ?
                        )
                        """,
                        Boolean.class,
                        hotel,
                        id
                );

        if (!Boolean.TRUE.equals(exists)) {
            throw ApiException.notFound();
        }
    }

    private void ensureUniqueName(
            UUID hotel,
            UUID excludedId,
            String name) {

        Boolean exists;

        if (excludedId == null) {

            exists =
                    db.queryForObject(
                            """
                            select exists(
                                select 1
                                from expense_categories
                                where hotel_id = ?
                                  and lower(btrim(name)) =
                                      lower(btrim(?))
                            )
                            """,
                            Boolean.class,
                            hotel,
                            name
                    );

        } else {

            exists =
                    db.queryForObject(
                            """
                            select exists(
                                select 1
                                from expense_categories
                                where hotel_id = ?
                                  and id <> ?
                                  and lower(btrim(name)) =
                                      lower(btrim(?))
                            )
                            """,
                            Boolean.class,
                            hotel,
                            excludedId,
                            name
                    );
        }

        if (Boolean.TRUE.equals(exists)) {
            throw new ApiException(
                    409,
                    "EXPENSE_CATEGORY_EXISTS",
                    "An expense category with this name already exists."
            );
        }
    }

    private CategoryResponse category(
            ResultSet result,
            int rowNumber)
            throws SQLException {

        return new CategoryResponse(
                result.getObject(
                        "id",
                        UUID.class
                ),
                result.getString(
                        "name"
                ),
                result.getBoolean(
                        "active"
                ),
                result.getTimestamp(
                        "created_at"
                ).toInstant(),
                result.getTimestamp(
                        "updated_at"
                ).toInstant()
        );
    }
}