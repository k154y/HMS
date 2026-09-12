package com.hotelmanagement.hms.identity.authorization.model;

import java.util.Arrays;
import java.util.Set;

public enum DefaultHotelRole {

    OWNER(
            "Owner",
            "Hotel owner with full tenant-level authority.",
            PermissionCode.values()
    ),

    MANAGER(
            "Manager",
            "Hotel manager responsible for broad operational control.",
            PermissionCode.HOTEL_SETTINGS_VIEW,
            PermissionCode.HOTEL_SETTINGS_MANAGE,

            PermissionCode.BRANCH_VIEW,
            PermissionCode.BRANCH_MANAGE,

            PermissionCode.USER_VIEW,
            PermissionCode.ROLE_VIEW,

            PermissionCode.ROOM_VIEW,
            PermissionCode.ROOM_MANAGE,
            PermissionCode.ROOM_RATE_MANAGE,

            PermissionCode.RESERVATION_VIEW,
            PermissionCode.RESERVATION_CREATE,
            PermissionCode.RESERVATION_MODIFY,
            PermissionCode.RESERVATION_CANCEL,

            PermissionCode.CHECKIN_PERFORM,
            PermissionCode.CHECKOUT_PERFORM,

            PermissionCode.CUSTOMER_VIEW,
            PermissionCode.CUSTOMER_MANAGE,

            PermissionCode.FOLIO_VIEW,
            PermissionCode.FOLIO_MANAGE,
            PermissionCode.FOLIO_TRANSFER,

            PermissionCode.ORDER_VIEW,
            PermissionCode.ORDER_CREATE,
            PermissionCode.ORDER_MODIFY,
            PermissionCode.ORDER_SEND,
            PermissionCode.ORDER_VOID,

            PermissionCode.KITCHEN_VIEW,
            PermissionCode.KITCHEN_UPDATE,

            PermissionCode.BAR_VIEW,
            PermissionCode.BAR_UPDATE,

            PermissionCode.PAYMENT_VIEW,
            PermissionCode.PAYMENT_RECORD,
            PermissionCode.PAYMENT_REFUND,
            PermissionCode.PAYMENT_VOID,

            PermissionCode.CASHIER_RECONCILE,

            PermissionCode.EXCHANGE_RATE_VIEW,
            PermissionCode.EXCHANGE_RATE_MANAGE,

            PermissionCode.DISCOUNT_APPLY,
            PermissionCode.DISCOUNT_APPROVE,
            PermissionCode.COMPLIMENTARY_APPROVE,

            PermissionCode.CREDIT_VIEW,
            PermissionCode.CREDIT_MANAGE,
            PermissionCode.CREDIT_APPROVE,

            PermissionCode.PRODUCT_VIEW,
            PermissionCode.PRODUCT_MANAGE,

            PermissionCode.INVENTORY_VIEW,
            PermissionCode.INVENTORY_ADJUST,
            PermissionCode.INVENTORY_TRANSFER,

            PermissionCode.PURCHASE_VIEW,
            PermissionCode.PURCHASE_CREATE,
            PermissionCode.PURCHASE_APPROVE,
            PermissionCode.PURCHASE_RECEIVE,

            PermissionCode.VENDOR_VIEW,
            PermissionCode.VENDOR_MANAGE,

            PermissionCode.HOUSEKEEPING_VIEW,
            PermissionCode.HOUSEKEEPING_UPDATE,

            PermissionCode.MAINTENANCE_VIEW,
            PermissionCode.MAINTENANCE_MANAGE,

            PermissionCode.REPORT_VIEW,
            PermissionCode.FINANCIAL_REPORT_VIEW,

            PermissionCode.AUDIT_VIEW
    ),

    ACCOUNTANT(
            "Accountant",
            "Accounting and financial operations role.",
            PermissionCode.HOTEL_SETTINGS_VIEW,
            PermissionCode.BRANCH_VIEW,
            PermissionCode.USER_VIEW,
            PermissionCode.ROLE_VIEW,

            PermissionCode.ROOM_VIEW,
            PermissionCode.RESERVATION_VIEW,

            PermissionCode.CUSTOMER_VIEW,

            PermissionCode.FOLIO_VIEW,
            PermissionCode.FOLIO_MANAGE,

            PermissionCode.ORDER_VIEW,

            PermissionCode.PAYMENT_VIEW,
            PermissionCode.PAYMENT_RECORD,

            PermissionCode.CASHIER_RECONCILE,

            PermissionCode.EXCHANGE_RATE_VIEW,
            PermissionCode.EXCHANGE_RATE_MANAGE,

            PermissionCode.CREDIT_VIEW,
            PermissionCode.CREDIT_MANAGE,
            PermissionCode.CREDIT_APPROVE,

            PermissionCode.PRODUCT_VIEW,
            PermissionCode.INVENTORY_VIEW,

            PermissionCode.PURCHASE_VIEW,
            PermissionCode.PURCHASE_CREATE,
            PermissionCode.PURCHASE_APPROVE,

            PermissionCode.VENDOR_VIEW,
            PermissionCode.VENDOR_MANAGE,

            PermissionCode.REPORT_VIEW,
            PermissionCode.FINANCIAL_REPORT_VIEW,

            PermissionCode.AUDIT_VIEW
    ),

    RECEPTIONIST(
            "Receptionist",
            "Front-office role for reservations, guests, stays, and permitted collections.",
            PermissionCode.BRANCH_VIEW,

            PermissionCode.ROOM_VIEW,

            PermissionCode.RESERVATION_VIEW,
            PermissionCode.RESERVATION_CREATE,
            PermissionCode.RESERVATION_MODIFY,
            PermissionCode.RESERVATION_CANCEL,

            PermissionCode.CHECKIN_PERFORM,
            PermissionCode.CHECKOUT_PERFORM,

            PermissionCode.CUSTOMER_VIEW,
            PermissionCode.CUSTOMER_MANAGE,

            PermissionCode.FOLIO_VIEW,
            PermissionCode.FOLIO_MANAGE,
            PermissionCode.FOLIO_TRANSFER,

            PermissionCode.PAYMENT_VIEW,
            PermissionCode.PAYMENT_RECORD,

            PermissionCode.EXCHANGE_RATE_VIEW,

            PermissionCode.DISCOUNT_APPLY,

            PermissionCode.CREDIT_VIEW
    ),

    CASHIER(
            "Cashier",
            "Role responsible for collecting, recording, and reconciling permitted payments.",
            PermissionCode.BRANCH_VIEW,

            PermissionCode.CUSTOMER_VIEW,
            PermissionCode.FOLIO_VIEW,
            PermissionCode.ORDER_VIEW,

            PermissionCode.PAYMENT_VIEW,
            PermissionCode.PAYMENT_RECORD,

            PermissionCode.CASHIER_SHIFT_OPEN,
            PermissionCode.CASHIER_SHIFT_CLOSE,

            PermissionCode.EXCHANGE_RATE_VIEW,

            PermissionCode.CREDIT_VIEW
    ),

    WAITER(
            "Waiter",
            "Food and beverage service role responsible for customer orders.",
            PermissionCode.BRANCH_VIEW,

            PermissionCode.ORDER_VIEW,
            PermissionCode.ORDER_CREATE,
            PermissionCode.ORDER_MODIFY,
            PermissionCode.ORDER_SEND,

            PermissionCode.EXCHANGE_RATE_VIEW
    ),

    BARTENDER(
            "Bartender",
            "Bar operations role responsible for beverage order preparation.",
            PermissionCode.BRANCH_VIEW,

            PermissionCode.ORDER_VIEW,

            PermissionCode.BAR_VIEW,
            PermissionCode.BAR_UPDATE
    ),

    KITCHEN_STAFF(
            "Kitchen Staff",
            "Kitchen operations role responsible for food preparation workflow.",
            PermissionCode.BRANCH_VIEW,

            PermissionCode.ORDER_VIEW,

            PermissionCode.KITCHEN_VIEW,
            PermissionCode.KITCHEN_UPDATE
    ),

    STOREKEEPER(
            "Storekeeper",
            "Inventory and goods-receiving role.",
            PermissionCode.BRANCH_VIEW,

            PermissionCode.PRODUCT_VIEW,

            PermissionCode.INVENTORY_VIEW,
            PermissionCode.INVENTORY_TRANSFER,

            PermissionCode.PURCHASE_VIEW,
            PermissionCode.PURCHASE_RECEIVE,

            PermissionCode.VENDOR_VIEW
    ),

    HOUSEKEEPER(
            "Housekeeper",
            "Housekeeping role responsible for cleaning and room readiness.",
            PermissionCode.BRANCH_VIEW,
            PermissionCode.ROOM_VIEW,

            PermissionCode.HOUSEKEEPING_VIEW,
            PermissionCode.HOUSEKEEPING_UPDATE
    ),

    MAINTENANCE(
            "Maintenance",
            "Maintenance role responsible for property maintenance work.",
            PermissionCode.BRANCH_VIEW,
            PermissionCode.ROOM_VIEW,

            PermissionCode.MAINTENANCE_VIEW,
            PermissionCode.MAINTENANCE_MANAGE
    ),

    AUDITOR(
            "Auditor",
            "Read-only oversight role for operational, financial, and audit information.",
            PermissionCode.HOTEL_SETTINGS_VIEW,
            PermissionCode.BRANCH_VIEW,

            PermissionCode.USER_VIEW,
            PermissionCode.ROLE_VIEW,

            PermissionCode.ROOM_VIEW,
            PermissionCode.RESERVATION_VIEW,

            PermissionCode.CUSTOMER_VIEW,

            PermissionCode.FOLIO_VIEW,
            PermissionCode.ORDER_VIEW,

            PermissionCode.PAYMENT_VIEW,

            PermissionCode.EXCHANGE_RATE_VIEW,

            PermissionCode.CREDIT_VIEW,

            PermissionCode.PRODUCT_VIEW,
            PermissionCode.INVENTORY_VIEW,

            PermissionCode.PURCHASE_VIEW,
            PermissionCode.VENDOR_VIEW,

            PermissionCode.HOUSEKEEPING_VIEW,
            PermissionCode.MAINTENANCE_VIEW,

            PermissionCode.REPORT_VIEW,
            PermissionCode.FINANCIAL_REPORT_VIEW,

            PermissionCode.AUDIT_VIEW
    ),

    SUPERVISOR(
            "Supervisor",
            "Operational supervisory role with elevated day-to-day controls.",
            PermissionCode.BRANCH_VIEW,

            PermissionCode.ROOM_VIEW,

            PermissionCode.RESERVATION_VIEW,
            PermissionCode.RESERVATION_CREATE,
            PermissionCode.RESERVATION_MODIFY,

            PermissionCode.CHECKIN_PERFORM,
            PermissionCode.CHECKOUT_PERFORM,

            PermissionCode.CUSTOMER_VIEW,
            PermissionCode.CUSTOMER_MANAGE,

            PermissionCode.FOLIO_VIEW,
            PermissionCode.FOLIO_MANAGE,

            PermissionCode.ORDER_VIEW,
            PermissionCode.ORDER_CREATE,
            PermissionCode.ORDER_MODIFY,
            PermissionCode.ORDER_SEND,
            PermissionCode.ORDER_VOID,

            PermissionCode.KITCHEN_VIEW,
            PermissionCode.KITCHEN_UPDATE,

            PermissionCode.BAR_VIEW,
            PermissionCode.BAR_UPDATE,

            PermissionCode.PAYMENT_VIEW,
            PermissionCode.PAYMENT_RECORD,

            PermissionCode.CASHIER_RECONCILE,

            PermissionCode.EXCHANGE_RATE_VIEW,

            PermissionCode.DISCOUNT_APPLY,
            PermissionCode.DISCOUNT_APPROVE,

            PermissionCode.CREDIT_VIEW,

            PermissionCode.PRODUCT_VIEW,

            PermissionCode.INVENTORY_VIEW,

            PermissionCode.PURCHASE_VIEW,
            PermissionCode.PURCHASE_RECEIVE,

            PermissionCode.VENDOR_VIEW,

            PermissionCode.HOUSEKEEPING_VIEW,
            PermissionCode.HOUSEKEEPING_UPDATE,

            PermissionCode.MAINTENANCE_VIEW,

            PermissionCode.REPORT_VIEW
    );

    private final String displayName;
    private final String description;
    private final Set<PermissionCode> permissions;

    DefaultHotelRole(
            String displayName,
            String description,
            PermissionCode... permissions) {

        this.displayName = displayName;
        this.description = description;

        this.permissions = Set.copyOf(
                Arrays.asList(permissions)
        );
    }

    public String getCode() {
        return name();
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public Set<PermissionCode> getPermissions() {
        return permissions;
    }
}