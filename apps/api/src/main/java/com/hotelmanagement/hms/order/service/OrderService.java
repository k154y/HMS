package com.hotelmanagement.hms.order.service;

import com.hotelmanagement.hms.audit.service.AuditService;
import com.hotelmanagement.hms.folio.model.EntryKind;
import com.hotelmanagement.hms.folio.model.FolioStatus;
import com.hotelmanagement.hms.folio.service.FolioService;
import com.hotelmanagement.hms.inventory.model.MovementKind;
import com.hotelmanagement.hms.inventory.service.InventoryService;
import com.hotelmanagement.hms.order.dto.OrderRequest;
import com.hotelmanagement.hms.order.dto.OrderResponse;
import com.hotelmanagement.hms.order.model.Order;
import com.hotelmanagement.hms.order.model.OrderItem;
import com.hotelmanagement.hms.order.model.OrderStatus;
import com.hotelmanagement.hms.order.repository.OrderItemRepository;
import com.hotelmanagement.hms.order.repository.OrderRepository;
import com.hotelmanagement.hms.product.repository.ProductRepository;
import com.hotelmanagement.hms.shared.service.OperationScope;
import com.hotelmanagement.hms.shared.web.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.hotelmanagement.hms.identity.authorization.model.PermissionCode.BAR_UPDATE;
import static com.hotelmanagement.hms.identity.authorization.model.PermissionCode.BAR_VIEW;
import static com.hotelmanagement.hms.identity.authorization.model.PermissionCode.KITCHEN_UPDATE;
import static com.hotelmanagement.hms.identity.authorization.model.PermissionCode.KITCHEN_VIEW;
import static com.hotelmanagement.hms.identity.authorization.model.PermissionCode.ORDER_CREATE;
import static com.hotelmanagement.hms.identity.authorization.model.PermissionCode.ORDER_SEND;
import static com.hotelmanagement.hms.identity.authorization.model.PermissionCode.ORDER_VIEW;
import static com.hotelmanagement.hms.identity.authorization.model.PermissionCode.ORDER_VOID;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orders;
    private final OrderItemRepository items;
    private final ProductRepository products;
    private final InventoryService inventory;
    private final OperationScope scope;
    private final AuditService audit;
    private final FolioService folios;
    private final JdbcTemplate db;

    public OrderService(
            OrderRepository orders,
            OrderItemRepository items,
            ProductRepository products,
            InventoryService inventory,
            OperationScope scope,
            AuditService audit,
            FolioService folios,
            JdbcTemplate db) {

        this.orders =
                orders;

        this.items =
                items;

        this.products =
                products;

        this.inventory =
                inventory;

        this.scope =
                scope;

        this.audit =
                audit;

        this.folios =
                folios;

        this.db =
                db;
    }

    public OrderResponse create(
            UUID hotel,
            UUID branch,
            OrderRequest request) {

        UUID actor =
                scope.branch(
                        hotel,
                        branch,
                        ORDER_CREATE
                );

        if (request.items() == null
                || request.items().isEmpty()
                || request.items().size() > 100) {

            throw new IllegalArgumentException(
                    "Order items are required."
            );
        }

        if (request.customerId() == null) {

            throw new IllegalArgumentException(
                    "Customer is required."
            );
        }

        UUID folioId =
                resolveBillingFolio(
                        hotel,
                        branch,
                        request.customerId(),
                        request.folioId()
                );

        if (!Set.of(
                "KITCHEN",
                "BAR",
                "SERVICE"
        ).contains(
                request.destination()
        )) {

            throw new IllegalArgumentException(
                    "Invalid destination."
            );
        }

        Order order =
                orders.saveAndFlush(
                        Order.create(
                                hotel,
                                branch,
                                request.customerId(),
                                folioId,
                                actor,
                                request.destination()
                        )
                );

        BigDecimal subtotal =
                BigDecimal.ZERO;

        BigDecimal tax =
                BigDecimal.ZERO;

        Set<UUID> seen =
                new HashSet<>();

        for (OrderRequest.Item requestedItem
                : request.items()) {

            if (requestedItem == null
                    || requestedItem.quantity() == null
                    || requestedItem.quantity().signum() <= 0
                    || requestedItem.quantity().scale() > 4
                    || !seen.add(
                    requestedItem.productId()
            )) {

                throw new IllegalArgumentException(
                        "Invalid or duplicate item."
                );
            }

            var product =
                    products
                            .findByIdAndHotelId(
                                    requestedItem.productId(),
                                    hotel
                            )
                            .filter(
                                    item ->
                                            item.getActive()
                                                    && item.getSellable()
                            )
                            .orElseThrow(
                                    ApiException::notFound
                            );

            BigDecimal line =
                    product
                            .getSellingPrice()
                            .multiply(
                                    requestedItem.quantity()
                            )
                            .setScale(
                                    4,
                                    RoundingMode.HALF_UP
                            );

            subtotal =
                    subtotal.add(
                            line
                    );

            tax =
                    tax.add(
                            line
                                    .multiply(
                                            product.getTaxRate()
                                    )
                                    .setScale(
                                            4,
                                            RoundingMode.HALF_UP
                                    )
                    );

            items.save(
                    OrderItem.create(
                            hotel,
                            branch,
                            order.getId(),
                            product.getId(),
                            product.getName(),
                            requestedItem.quantity(),
                            product.getSellingPrice(),
                            product.getTaxRate(),
                            product.getDestination().name()
                    )
            );
        }

        order.totals(
                subtotal,
                tax
        );

        audit.record(
                hotel,
                branch,
                actor,
                "ORDER_CREATED",
                "ORDER",
                order.getId()
        );

        return OrderResponse.from(
                order
        );
    }

    /**
     * Determines exactly where an order should be billed.
     *
     * Explicit folio:
     * must belong either to:
     *   - an active checked-in reservation, or
     *   - an active non-resident bill.
     *
     * Missing folio:
     * only a currently checked-in guest may be resolved
     * automatically.
     *
     * We no longer create/reuse an arbitrary customer folio.
     */
    private UUID resolveBillingFolio(
            UUID hotel,
            UUID branch,
            UUID customer,
            UUID requestedFolio) {

        if (requestedFolio == null) {

            List<UUID> checkedIn =
                    db.query(
                            """
                            select f.id
                            from folios f
                            join reservations r
                              on r.folio_id = f.id
                             and r.hotel_id = f.hotel_id
                             and r.branch_id = f.branch_id
                            where f.hotel_id = ?
                              and f.branch_id = ?
                              and f.customer_id = ?
                              and f.status = 'OPEN'
                              and r.status = 'CHECKED_IN'
                            order by
                                r.check_in desc,
                                r.id
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
                            branch,
                            customer
                    );

            if (checkedIn.isEmpty()) {

                throw new ApiException(
                        409,
                        "BILLING_TARGET_REQUIRED",
                        "Choose a checked-in guest folio or open a non-resident bill."
                );
            }

            return checkedIn.getFirst();
        }

        var folio =
                folios.lock(
                        hotel,
                        branch,
                        requestedFolio
                );

        if (!folio
                .getCustomerId()
                .equals(
                        customer
                )) {

            throw new IllegalArgumentException(
                    "Folio belongs to another customer."
            );
        }

        if (folio.getStatus()
                != FolioStatus.OPEN) {

            throw new ApiException(
                    409,
                    "FOLIO_NOT_OPEN",
                    "The selected billing folio is not open."
            );
        }

        Boolean residentFolio =
                db.queryForObject(
                        """
                        select exists(
                            select 1
                            from reservations r
                            where r.hotel_id = ?
                              and r.branch_id = ?
                              and r.folio_id = ?
                              and r.customer_id = ?
                              and r.status = 'CHECKED_IN'
                        )
                        """,
                        Boolean.class,
                        hotel,
                        branch,
                        requestedFolio,
                        customer
                );

        Boolean nonResidentFolio =
                db.queryForObject(
                        """
                        select exists(
                            select 1
                            from non_resident_bills b
                            where b.hotel_id = ?
                              and b.branch_id = ?
                              and b.folio_id = ?
                              and b.customer_id = ?
                              and b.cancelled_at is null
                              and b.voided_at is null
                        )
                        """,
                        Boolean.class,
                        hotel,
                        branch,
                        requestedFolio,
                        customer
                );

        if (!Boolean.TRUE.equals(
                residentFolio
        )
                && !Boolean.TRUE.equals(
                nonResidentFolio
        )) {

            throw new ApiException(
                    409,
                    "INVALID_BILLING_TARGET",
                    "The selected folio is not an active guest stay or non-resident bill."
            );
        }

        return requestedFolio;
    }

    public OrderResponse send(
            UUID hotel,
            UUID branch,
            UUID id) {

        UUID actor =
                scope.branch(
                        hotel,
                        branch,
                        ORDER_SEND
                );

        Order order =
                orders
                        .findByIdAndHotelIdAndBranchId(
                                id,
                                hotel,
                                branch
                        )
                        .orElseThrow(
                                ApiException::notFound
                        );

        if (order.getStatus()
                != OrderStatus.DRAFT) {

            throw new IllegalStateException(
                    "Only drafts may be sent."
            );
        }

        List<OrderItem> lines =
                new ArrayList<>(
                        items.findByOrderIdAndHotelIdAndBranchId(
                                id,
                                hotel,
                                branch
                        )
                );

        lines.sort(
                Comparator.comparing(
                        OrderItem::getProductId
                )
        );

        for (OrderItem item
                : lines) {

            var product =
                    products
                            .lock(
                                    item.getProductId(),
                                    hotel
                            )
                            .orElseThrow(
                                    ApiException::notFound
                            );

            if (product.getStockTracked()) {

                BigDecimal quantity =
                        item
                                .getQuantity()
                                .multiply(
                                        product.getSellingFactor()
                                )
                                .setScale(
                                        4,
                                        RoundingMode.HALF_UP
                                );

                inventory.move(
                        hotel,
                        branch,
                        item.getProductId(),
                        quantity.negate(),
                        MovementKind.SALE,
                        id,
                        actor,
                        "Order sale " + id
                );

                db.update(
                        """
                        update order_items
                        set consumed_quantity = ?
                        where order_id = ?
                          and product_id = ?
                        """,
                        quantity,
                        id,
                        item.getProductId()
                );
            }
        }

        if (order.getTotal()
                .signum() > 0) {

            folios.post(
                    hotel,
                    branch,
                    order.getFolioId(),
                    order.getTotal(),
                    EntryKind.ORDER,
                    id,
                    "Order " + id,
                    actor
            );
        }

        order.send();

        audit.record(
                hotel,
                branch,
                actor,
                "ORDER_SENT",
                "ORDER",
                id
        );

        return OrderResponse.from(
                order
        );
    }

    public void voidOrder(
            UUID hotel,
            UUID branch,
            UUID id) {

        UUID actor =
                scope.branch(
                        hotel,
                        branch,
                        ORDER_VOID
                );

        Order order =
                orders
                        .findByIdAndHotelIdAndBranchId(
                                id,
                                hotel,
                                branch
                        )
                        .orElseThrow(
                                ApiException::notFound
                        );

        if (order.getStatus()
                == OrderStatus.VOIDED) {

            throw new IllegalStateException(
                    "Already voided."
            );
        }

        if (order.getStatus()
                != OrderStatus.DRAFT) {

            for (Map<String, Object> row
                    : db.queryForList(
                    """
                    select
                        product_id,
                        consumed_quantity
                    from order_items
                    where order_id = ?
                    order by product_id
                    """,
                    id
            )) {

                BigDecimal quantity =
                        (BigDecimal) row.get(
                                "consumed_quantity"
                        );

                if (quantity.signum()
                        > 0) {

                    inventory.move(
                            hotel,
                            branch,
                            (UUID) row.get(
                                    "product_id"
                            ),
                            quantity,
                            MovementKind.REVERSAL,
                            id,
                            actor,
                            "Order void " + id
                    );
                }
            }

            if (order.getTotal()
                    .signum() > 0) {

                folios.post(
                        hotel,
                        branch,
                        order.getFolioId(),
                        order
                                .getTotal()
                                .negate(),
                        EntryKind.REVERSAL,
                        id,
                        "Order void " + id,
                        actor
                );
            }
        }

        order.voidOrder();

        audit.record(
                hotel,
                branch,
                actor,
                "ORDER_VOIDED",
                "ORDER",
                id
        );
    }

    public OrderResponse advance(
            UUID hotel,
            UUID branch,
            UUID id,
            OrderStatus next) {

        Order order =
                orders
                        .findByIdAndHotelIdAndBranchId(
                                id,
                                hotel,
                                branch
                        )
                        .orElseThrow(
                                ApiException::notFound
                        );

        UUID actor;

        if (next
                == OrderStatus.SERVED) {

            actor =
                    scope.branch(
                            hotel,
                            branch,
                            ORDER_SEND
                    );

        } else {

            scope.branch(
                    hotel,
                    branch,
                    ORDER_VIEW
            );

            throw new IllegalStateException(
                    "Update individual preparation items."
            );
        }

        order.advance(
                next
        );

        audit.record(
                hotel,
                branch,
                actor,
                "ORDER_" + next,
                "ORDER",
                id
        );

        return OrderResponse.from(
                order
        );
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> queue(
            UUID hotel,
            UUID branch,
            String destination) {

        if (!Set.of(
                "BAR",
                "KITCHEN",
                "SERVICE"
        ).contains(
                destination
        )) {

            throw new IllegalArgumentException(
                    "Invalid destination."
            );
        }

        scope.branch(
                hotel,
                branch,
                destination.equals(
                        "BAR"
                )
                        ? BAR_VIEW
                        : destination.equals(
                        "KITCHEN"
                )
                        ? KITCHEN_VIEW
                        : ORDER_VIEW
        );

        return db.queryForList(
                """
                select
                    i.*,
                    o.created_at,
                    c.name customer_name
                from order_items i
                join orders o
                  on o.id = i.order_id
                join customers c
                  on c.id = o.customer_id
                where o.hotel_id = ?
                  and o.branch_id = ?
                  and i.destination = ?
                  and o.status in (
                      'SENT',
                      'PREPARING',
                      'READY'
                  )
                order by
                    o.created_at,
                    i.id
                """,
                hotel,
                branch,
                destination
        );
    }

    public void prepare(
            UUID hotel,
            UUID branch,
            UUID id,
            UUID item,
            OrderStatus next) {

        scope.branch(
                hotel,
                branch,
                ORDER_VIEW
        );

        Order order =
                orders
                        .findByIdAndHotelIdAndBranchId(
                                id,
                                hotel,
                                branch
                        )
                        .orElseThrow(
                                ApiException::notFound
                        );

        if (order.getStatus()
                != OrderStatus.SENT
                && order.getStatus()
                != OrderStatus.PREPARING) {

            throw new IllegalStateException(
                    "Order cannot be prepared."
            );
        }

        List<Map<String, Object>> rows =
                db.queryForList(
                        """
                        select *
                        from order_items
                        where id = ?
                          and order_id = ?
                        """,
                        item,
                        id
                );

        if (rows.isEmpty()) {
            throw ApiException.notFound();
        }

        Map<String, Object> line =
                rows.getFirst();

        String destination =
                (String) line.get(
                        "destination"
                );

        UUID actor =
                scope.branch(
                        hotel,
                        branch,
                        destination.equals(
                                "BAR"
                        )
                                ? BAR_UPDATE
                                : destination.equals(
                                "KITCHEN"
                        )
                                ? KITCHEN_UPDATE
                                : ORDER_SEND
                );

        String current =
                (String) line.get(
                        "preparation_status"
                );

        if (!(
                current.equals(
                        "SENT"
                )
                        && next
                        == OrderStatus.PREPARING
                        ||
                        current.equals(
                                "PREPARING"
                        )
                                && next
                                == OrderStatus.READY
        )) {

            throw new IllegalStateException(
                    "Invalid preparation transition."
            );
        }

        db.update(
                """
                update order_items
                set preparation_status = ?
                where id = ?
                """,
                next.name(),
                item
        );

        if (order.getStatus()
                == OrderStatus.SENT) {

            order.advance(
                    OrderStatus.PREPARING
            );
        }

        Integer remaining =
                db.queryForObject(
                        """
                        select count(*)
                        from order_items
                        where order_id = ?
                          and preparation_status <> 'READY'
                        """,
                        Integer.class,
                        id
                );

        if (remaining != null
                && remaining == 0) {

            order.advance(
                    OrderStatus.READY
            );
        }

        audit.record(
                hotel,
                branch,
                actor,
                "ITEM_" + next,
                "ORDER_ITEM",
                item
        );
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(
            UUID hotel,
            UUID branch) {

        scope.branch(
                hotel,
                branch,
                ORDER_VIEW
        );

        return db.queryForList(
                """
                select
                    o.*,
                    c.name as customer_name
                from orders o
                left join customers c
                  on c.id = o.customer_id
                where o.hotel_id = ?
                  and o.branch_id = ?
                order by o.created_at desc
                limit 100
                """,
                hotel,
                branch
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(
            UUID hotel,
            UUID branch,
            UUID id) {

        scope.branch(
                hotel,
                branch,
                ORDER_VIEW
        );

        List<Map<String, Object>> rows =
                db.queryForList(
                        """
                        select *
                        from orders
                        where id = ?
                          and hotel_id = ?
                          and branch_id = ?
                        """,
                        id,
                        hotel,
                        branch
                );

        if (rows.isEmpty()) {
            throw ApiException.notFound();
        }

        Map<String, Object> result =
                new java.util.LinkedHashMap<>(
                        rows.getFirst()
                );

        result.put(
                "items",
                db.queryForList(
                        """
                        select *
                        from order_items
                        where order_id = ?
                        order by id
                        """,
                        id
                )
        );

        return result;
    }
}