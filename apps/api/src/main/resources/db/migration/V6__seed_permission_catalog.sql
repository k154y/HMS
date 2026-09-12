INSERT INTO permissions (
    id,
    code,
    name,
    description
)
VALUES

-- Hotel / tenant administration
(
    '10000000-0000-0000-0000-000000000001',
    'HOTEL_SETTINGS_VIEW',
    'View Hotel Settings',
    'View hotel-level operational and configuration settings.'
),
(
    '10000000-0000-0000-0000-000000000002',
    'HOTEL_SETTINGS_MANAGE',
    'Manage Hotel Settings',
    'Modify hotel-level operational and configuration settings.'
),

-- Branches
(
    '10000000-0000-0000-0000-000000000003',
    'BRANCH_VIEW',
    'View Branches',
    'View hotel branches and branch information.'
),
(
    '10000000-0000-0000-0000-000000000004',
    'BRANCH_MANAGE',
    'Manage Branches',
    'Create and update hotel branches.'
),

-- Users / memberships
(
    '10000000-0000-0000-0000-000000000005',
    'USER_VIEW',
    'View Users',
    'View hotel users and memberships.'
),
(
    '10000000-0000-0000-0000-000000000006',
    'USER_MANAGE',
    'Manage Users',
    'Create, activate, suspend, revoke, and update hotel user access.'
),

-- Roles / permissions
(
    '10000000-0000-0000-0000-000000000007',
    'ROLE_VIEW',
    'View Roles',
    'View roles and their assigned permissions.'
),
(
    '10000000-0000-0000-0000-000000000008',
    'ROLE_MANAGE',
    'Manage Roles',
    'Create and configure hotel roles and role permissions.'
),

-- Rooms
(
    '10000000-0000-0000-0000-000000000009',
    'ROOM_VIEW',
    'View Rooms',
    'View room information, status, capacity, and availability.'
),
(
    '10000000-0000-0000-0000-000000000010',
    'ROOM_MANAGE',
    'Manage Rooms',
    'Create and update rooms, beds, capacities, and room configuration.'
),
(
    '10000000-0000-0000-0000-000000000011',
    'ROOM_RATE_MANAGE',
    'Manage Room Rates',
    'Create and modify room pricing.'
),

-- Reservations
(
    '10000000-0000-0000-0000-000000000012',
    'RESERVATION_VIEW',
    'View Reservations',
    'View reservations and availability.'
),
(
    '10000000-0000-0000-0000-000000000013',
    'RESERVATION_CREATE',
    'Create Reservations',
    'Create hotel reservations.'
),
(
    '10000000-0000-0000-0000-000000000014',
    'RESERVATION_MODIFY',
    'Modify Reservations',
    'Modify existing reservations.'
),
(
    '10000000-0000-0000-0000-000000000015',
    'RESERVATION_CANCEL',
    'Cancel Reservations',
    'Cancel reservations according to hotel policy.'
),

-- Stay operations
(
    '10000000-0000-0000-0000-000000000016',
    'CHECKIN_PERFORM',
    'Perform Check-In',
    'Check guests into reserved or permitted rooms.'
),
(
    '10000000-0000-0000-0000-000000000017',
    'CHECKOUT_PERFORM',
    'Perform Check-Out',
    'Complete guest checkout subject to financial controls.'
),

-- Customers
(
    '10000000-0000-0000-0000-000000000018',
    'CUSTOMER_VIEW',
    'View Customers',
    'View individual and company customer records.'
),
(
    '10000000-0000-0000-0000-000000000019',
    'CUSTOMER_MANAGE',
    'Manage Customers',
    'Create and update individual and company customer records.'
),

-- Folios
(
    '10000000-0000-0000-0000-000000000020',
    'FOLIO_VIEW',
    'View Folios',
    'View guest and customer folios and outstanding balances.'
),
(
    '10000000-0000-0000-0000-000000000021',
    'FOLIO_MANAGE',
    'Manage Folios',
    'Manage charges and permitted folio operations.'
),
(
    '10000000-0000-0000-0000-000000000022',
    'FOLIO_TRANSFER',
    'Transfer Folio Charges',
    'Transfer or split permitted charges between folios.'
),

-- Orders / POS
(
    '10000000-0000-0000-0000-000000000023',
    'ORDER_VIEW',
    'View Orders',
    'View food, beverage, and service orders.'
),
(
    '10000000-0000-0000-0000-000000000024',
    'ORDER_CREATE',
    'Create Orders',
    'Create customer food, beverage, or service orders.'
),
(
    '10000000-0000-0000-0000-000000000025',
    'ORDER_MODIFY',
    'Modify Orders',
    'Modify orders before prohibited processing stages.'
),
(
    '10000000-0000-0000-0000-000000000026',
    'ORDER_SEND',
    'Send Orders',
    'Send confirmed order items to the appropriate preparation department.'
),
(
    '10000000-0000-0000-0000-000000000027',
    'ORDER_VOID',
    'Void Orders',
    'Void permitted orders or order items while preserving audit history.'
),

-- Kitchen / bar preparation
(
    '10000000-0000-0000-0000-000000000028',
    'KITCHEN_VIEW',
    'View Kitchen Orders',
    'View orders routed to the kitchen.'
),
(
    '10000000-0000-0000-0000-000000000029',
    'KITCHEN_UPDATE',
    'Update Kitchen Orders',
    'Update preparation status for kitchen orders.'
),
(
    '10000000-0000-0000-0000-000000000030',
    'BAR_VIEW',
    'View Bar Orders',
    'View orders routed to the bar.'
),
(
    '10000000-0000-0000-0000-000000000031',
    'BAR_UPDATE',
    'Update Bar Orders',
    'Update preparation status for bar orders.'
),

-- Payments
(
    '10000000-0000-0000-0000-000000000032',
    'PAYMENT_VIEW',
    'View Payments',
    'View permitted hotel payment transactions.'
),
(
    '10000000-0000-0000-0000-000000000033',
    'PAYMENT_RECORD',
    'Record Payments',
    'Record cash, card, mobile money, bank, credit, and other supported payments.'
),
(
    '10000000-0000-0000-0000-000000000034',
    'PAYMENT_REFUND',
    'Refund Payments',
    'Perform an authorized payment refund.'
),
(
    '10000000-0000-0000-0000-000000000035',
    'PAYMENT_VOID',
    'Void Payments',
    'Void or reverse eligible payment transactions while preserving audit history.'
),

-- Cashier
(
    '10000000-0000-0000-0000-000000000036',
    'CASHIER_SHIFT_OPEN',
    'Open Cashier Shift',
    'Open a cashier collection shift.'
),
(
    '10000000-0000-0000-0000-000000000037',
    'CASHIER_SHIFT_CLOSE',
    'Close Cashier Shift',
    'Close a cashier shift and record closing amounts.'
),
(
    '10000000-0000-0000-0000-000000000038',
    'CASHIER_RECONCILE',
    'Reconcile Cashier Shift',
    'Review and reconcile cashier collections and differences.'
),

-- Foreign currency
(
    '10000000-0000-0000-0000-000000000039',
    'EXCHANGE_RATE_VIEW',
    'View Exchange Rates',
    'View hotel currencies and applicable exchange rates.'
),
(
    '10000000-0000-0000-0000-000000000040',
    'EXCHANGE_RATE_MANAGE',
    'Manage Exchange Rates',
    'Create hotel exchange rates and accepted foreign-currency configurations.'
),

-- Discounts / complimentary
(
    '10000000-0000-0000-0000-000000000041',
    'DISCOUNT_APPLY',
    'Apply Discounts',
    'Apply discounts within permitted limits.'
),
(
    '10000000-0000-0000-0000-000000000042',
    'DISCOUNT_APPROVE',
    'Approve Discounts',
    'Approve discounts requiring elevated authorization.'
),
(
    '10000000-0000-0000-0000-000000000043',
    'COMPLIMENTARY_APPROVE',
    'Approve Complimentary Items',
    'Approve free or complimentary rooms, products, or services.'
),

-- Credit / receivables
(
    '10000000-0000-0000-0000-000000000044',
    'CREDIT_VIEW',
    'View Credit',
    'View customer credit balances and receivables.'
),
(
    '10000000-0000-0000-0000-000000000045',
    'CREDIT_MANAGE',
    'Manage Credit',
    'Manage permitted customer credit information.'
),
(
    '10000000-0000-0000-0000-000000000046',
    'CREDIT_APPROVE',
    'Approve Credit',
    'Approve customer credit or transactions requiring credit authorization.'
),

-- Products
(
    '10000000-0000-0000-0000-000000000047',
    'PRODUCT_VIEW',
    'View Products',
    'View products, categories, units, and prices.'
),
(
    '10000000-0000-0000-0000-000000000048',
    'PRODUCT_MANAGE',
    'Manage Products',
    'Create and update products, categories, units, and pricing.'
),

-- Inventory
(
    '10000000-0000-0000-0000-000000000049',
    'INVENTORY_VIEW',
    'View Inventory',
    'View stock quantities and inventory activity.'
),
(
    '10000000-0000-0000-0000-000000000050',
    'INVENTORY_ADJUST',
    'Adjust Inventory',
    'Perform authorized stock adjustments with audit history.'
),
(
    '10000000-0000-0000-0000-000000000051',
    'INVENTORY_TRANSFER',
    'Transfer Inventory',
    'Transfer inventory between authorized stock locations.'
),

-- Purchasing
(
    '10000000-0000-0000-0000-000000000052',
    'PURCHASE_VIEW',
    'View Purchases',
    'View purchase transactions and purchase history.'
),
(
    '10000000-0000-0000-0000-000000000053',
    'PURCHASE_CREATE',
    'Create Purchases',
    'Create purchase transactions or purchase orders.'
),
(
    '10000000-0000-0000-0000-000000000054',
    'PURCHASE_APPROVE',
    'Approve Purchases',
    'Approve purchases requiring authorization.'
),
(
    '10000000-0000-0000-0000-000000000055',
    'PURCHASE_RECEIVE',
    'Receive Purchases',
    'Confirm receipt of purchased goods into inventory.'
),

-- Vendors
(
    '10000000-0000-0000-0000-000000000056',
    'VENDOR_VIEW',
    'View Vendors',
    'View supplier and vendor records.'
),
(
    '10000000-0000-0000-0000-000000000057',
    'VENDOR_MANAGE',
    'Manage Vendors',
    'Create and update suppliers and vendors.'
),

-- Housekeeping
(
    '10000000-0000-0000-0000-000000000058',
    'HOUSEKEEPING_VIEW',
    'View Housekeeping',
    'View housekeeping work and room-cleaning status.'
),
(
    '10000000-0000-0000-0000-000000000059',
    'HOUSEKEEPING_UPDATE',
    'Update Housekeeping',
    'Update housekeeping assignments and room-cleaning status.'
),

-- Maintenance
(
    '10000000-0000-0000-0000-000000000060',
    'MAINTENANCE_VIEW',
    'View Maintenance',
    'View maintenance issues and work status.'
),
(
    '10000000-0000-0000-0000-000000000061',
    'MAINTENANCE_MANAGE',
    'Manage Maintenance',
    'Create, assign, update, and close maintenance work.'
),

-- Reporting
(
    '10000000-0000-0000-0000-000000000062',
    'REPORT_VIEW',
    'View Operational Reports',
    'View authorized operational hotel reports.'
),
(
    '10000000-0000-0000-0000-000000000063',
    'FINANCIAL_REPORT_VIEW',
    'View Financial Reports',
    'View authorized financial, revenue, payment, receivable, and purchasing reports.'
),

-- Audit
(
    '10000000-0000-0000-0000-000000000064',
    'AUDIT_VIEW',
    'View Audit Trail',
    'View authorized audit and activity records.'
);