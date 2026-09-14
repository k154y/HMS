# Frontend integration reference

The supplied `app.zip` is a Next.js dashboard reference. Its navigation and API routes define
the workflows the backend must expose: dashboard, reservations, room calendar, check-in/out,
folios, POS, kitchen, cashier, purchasing, stock, credit, reports, housekeeping and maintenance.

The backend uses tenant-scoped Spring endpoints under `/api/v1/hotels/{hotel}/branches/{branch}`.
Reservation creation should submit dates and occupancy first, call the reservation availability
endpoint, then submit selected rooms. A reservation may belong to a reusable person or company
customer and may contain multiple room allocations and guests. Room responses include bed size,
housekeeping state and operational availability.

The frontend should keep English, French and Kinyarwanda labels in translation resources and send
stable enum values to the API. Cart lines should show an item subtotal immediately; only the
preview/confirmation action should submit the order. Orders must carry the guest folio when room
service is posted, while room charges remain payable at reception. Payments are recorded as
separate tender rows so cashier views can filter by method and reconcile transactions.

The reference app also expects supplier purchase orders, stock receipt, credit customers, cashier
shifts, reports and housekeeping tasks. These require additional backend endpoints and migrations;
the current backend has the core product/vendor/inventory, payment, maintenance and basic order
foundations, but those advanced workflows remain explicitly tracked in the implementation status.
