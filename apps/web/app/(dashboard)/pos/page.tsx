"use client";

import {
  useEffect,
  useMemo,
  useState,
} from "react";

import Link from "next/link";
import { useSession } from "next-auth/react";

import {
  BedDouble,
  Building2,
  Bus,
  CheckCircle2,
  ChevronRight,
  CircleUserRound,
  GlassWater,
  Minus,
  Plus,
  ReceiptText,
  Search,
  Shirt,
  ShoppingCart,
  Trash2,
  UtensilsCrossed,
  UsersRound,
  Waves,
} from "lucide-react";

import {
  api,
  all,
} from "@/lib/hms-api";

import {
  Panel,
  inputStyle,
} from "@/components/operations/ui";

import { useLocale } from "@/components/LocaleProvider";
import { OrderHistory } from "@/components/operations/OrderHistory";

type BillingMode =
  | "GUEST"
  | "NON_RESIDENT";

type Customer = {
  id: string;
  name: string;
  kind?: string;
  active: boolean;
};

type Product = {
  id: string;
  name: string;
  sellingPrice: number;
  taxRate: number;
  active: boolean;
  sellable: boolean;
  destination: string;
  category?: string;
};

type Line = {
  product: Product;
  quantity: number;
};

type OrderResult = {
  id: string;
  folioId: string;
};

type NonResidentBill = {
  id: string;
  folioId: string;
  customerId: string;
  customerName: string;
  reference: string;
  billType: string;
  tableReference: string | null;
  status: string;
  total: number;
  paid: number;
  balance: number;
};

const BILL_TYPES = [
  "RESTAURANT",
  "BAR",
  "LAUNDRY",
  "TRANSPORT",
  "SWIMMING_POOL",
  "DAY_USE",
  "EVENT",
  "CONFERENCE",
  "OUTSIDE_CATERING",
  "OTHER",
];

const FILTERS = [
  "ALL",
  "BAR",
  "KITCHEN",
  "SERVICE",
];

function money(
  value: number
) {
  return Number(
    value || 0
  ).toLocaleString(
    undefined,
    {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }
  );
}

function productTotal(
  product: Product,
  quantity: number
) {
  const subtotal =
    Number(product.sellingPrice)
    * quantity;

  const tax =
    subtotal
    * Number(
      product.taxRate || 0
    );

  return Math.round(
    (subtotal + tax)
    * 10000
  ) / 10000;
}

function billTypeIcon(
  type: string
) {
  switch (type) {

    case "RESTAURANT":
      return UtensilsCrossed;

    case "BAR":
      return GlassWater;

    case "LAUNDRY":
      return Shirt;

    case "TRANSPORT":
      return Bus;

    case "SWIMMING_POOL":
    case "DAY_USE":
      return Waves;

    case "EVENT":
    case "CONFERENCE":
      return UsersRound;

    default:
      return ReceiptText;
  }
}

export default function POS() {

  const {
    t,
  } = useLocale();

  const {
    data: session,
  } = useSession();

  const permissions =
    (
      session?.user as {
        permissions?: string[];
      }
    )?.permissions ?? [];

  const canManageMenu =
    permissions.includes(
      "PRODUCT_MANAGE"
    );

  const [
    billingMode,
    setBillingMode,
  ] =
    useState<BillingMode>(
      "GUEST"
    );

  const [
    customers,
    setCustomers,
  ] =
    useState<Customer[]>([]);

  const [
    products,
    setProducts,
  ] =
    useState<Product[]>([]);

  const [
    guestCustomerId,
    setGuestCustomerId,
  ] =
    useState("");

  const [
    nonResidentCustomerId,
    setNonResidentCustomerId,
  ] =
    useState("");

  const [
    billType,
    setBillType,
  ] =
    useState(
      "RESTAURANT"
    );

  const [
    tableReference,
    setTableReference,
  ] =
    useState("");

  const [
    billNotes,
    setBillNotes,
  ] =
    useState("");

  const [
    activeBill,
    setActiveBill,
  ] =
    useState<NonResidentBill | null>(
      null
    );

  const [
    search,
    setSearch,
  ] =
    useState("");

  const [
    filter,
    setFilter,
  ] =
    useState("ALL");

  const [
    cart,
    setCart,
  ] =
    useState<Line[]>([]);

  const [
    busy,
    setBusy,
  ] =
    useState(false);

  const [
    error,
    setError,
  ] =
    useState("");

  const [
    message,
    setMessage,
  ] =
    useState("");

  const [
    lastFolioId,
    setLastFolioId,
  ] =
    useState("");

  useEffect(
    () => {

      Promise.all([
        all<Customer>(
          "customers"
        ),
        all<Product>(
          "products"
        ),
      ])
        .then(
          ([
            loadedCustomers,
            loadedProducts,
          ]) => {

            setCustomers(
              loadedCustomers.filter(
                (
                  customer
                ) =>
                  customer.active
              )
            );

            setProducts(
              loadedProducts.filter(
                (
                  product
                ) =>
                  product.active
                  &&
                  product.sellable
              )
            );
          }
        )
        .catch(
          (
            loadError
          ) => {

            setError(
              loadError instanceof Error
                ? loadError.message
                : "Unable to load POS."
            );
          }
        );

    },
    []
  );

  const visibleProducts =
    useMemo(
      () => {

        const query =
          search
            .trim()
            .toLowerCase();

        return products.filter(
          (
            product
          ) => {

            const matchesFilter =
              filter === "ALL"
              ||
              product.destination
              === filter;

            const matchesSearch =
              !query
              ||
              product.name
                .toLowerCase()
                .includes(
                  query
                )
              ||
              String(
                product.category
                ?? ""
              )
                .toLowerCase()
                .includes(
                  query
                );

            return (
              matchesFilter
              &&
              matchesSearch
            );
          }
        );

      },
      [
        products,
        search,
        filter,
      ]
    );

  const subtotal =
    cart.reduce(
      (
        total,
        line
      ) =>
        total
        +
        (
          Number(
            line.product
              .sellingPrice
          )
          *
          line.quantity
        ),
      0
    );

  const tax =
    cart.reduce(
      (
        total,
        line
      ) => {

        const lineSubtotal =
          Number(
            line.product
              .sellingPrice
          )
          *
          line.quantity;

        return (
          total
          +
          lineSubtotal
          *
          Number(
            line.product
              .taxRate
              || 0
          )
        );
      },
      0
    );

  const grandTotal =
    subtotal + tax;

  function clearResult() {
    setMessage("");
    setError("");
    setLastFolioId("");
  }

  function changeBillingMode(
    mode: BillingMode
  ) {

    setBillingMode(
      mode
    );

    setActiveBill(
      null
    );

    clearResult();
  }

  function addProduct(
    product: Product
  ) {

    clearResult();

    setCart(
      (
        current
      ) => {

        const existing =
          current.find(
            (
              line
            ) =>
              line.product.id
              === product.id
          );

        if (existing) {

          return current.map(
            (
              line
            ) =>
              line.product.id
              === product.id
                ? {
                    ...line,
                    quantity:
                      line.quantity
                      + 1,
                  }
                : line
          );
        }

        return [
          ...current,
          {
            product,
            quantity: 1,
          },
        ];
      }
    );
  }

  function setQuantity(
    productId: string,
    quantity: number
  ) {

    clearResult();

    if (quantity <= 0) {

      setCart(
        (
          current
        ) =>
          current.filter(
            (
              line
            ) =>
              line.product.id
              !== productId
          )
      );

      return;
    }

    setCart(
      (
        current
      ) =>
        current.map(
          (
            line
          ) =>
            line.product.id
            === productId
              ? {
                  ...line,
                  quantity,
                }
              : line
        )
    );
  }

  function removeProduct(
    productId: string
  ) {

    clearResult();

    setCart(
      (
        current
      ) =>
        current.filter(
          (
            line
          ) =>
            line.product.id
            !== productId
        )
    );
  }

  async function ensureNonResidentBill() {

    if (activeBill) {
      return activeBill;
    }

    const created =
      await api<NonResidentBill>(
        "non-resident-bills",
        "POST",
        {
          customerId:
            nonResidentCustomerId
            || null,

          billType,

          tableReference:
            tableReference.trim()
            || null,

          notes:
            billNotes.trim()
            || null,
        }
      );

    setActiveBill(
      created
    );

    return created;
  }

  async function send() {

    if (!cart.length) {
      return;
    }

    if (
      billingMode
      === "GUEST"
      &&
      !guestCustomerId
    ) {

      setError(
        "Choose a checked-in guest."
      );

      return;
    }

    setBusy(true);
    setError("");
    setMessage("");

    try {

      let customerId =
        guestCustomerId;

      let folioId:
        string | undefined;

      if (
        billingMode
        === "NON_RESIDENT"
      ) {

        const bill =
          await ensureNonResidentBill();

        customerId =
          bill.customerId;

        folioId =
          bill.folioId;
      }

      /*
       * Product destinations remain stored on each order item.
       *
       * The order header requires one valid destination. We keep
       * the first line's destination, matching the existing
       * backend contract.
       */
      const result =
        await api<OrderResult>(
          "orders",
          "POST",
          {
            customerId,

            folioId,

            destination:
              cart[0]
                .product
                .destination,

            items:
              cart.map(
                (
                  line
                ) => ({
                  productId:
                    line.product.id,

                  quantity:
                    line.quantity,
                })
              ),
          }
        );

      await api(
        `orders/${result.id}/send`,
        "POST"
      );

      setLastFolioId(
        result.folioId
      );

      setCart([]);

      setMessage(
        billingMode
        === "NON_RESIDENT"
          ? "Order sent to the non-resident bill."
          : "Order sent to the guest folio."
      );

    } catch (
      sendError
    ) {

      setError(
        sendError instanceof Error
          ? sendError.message
          : "Unable to send order."
      );

    } finally {

      setBusy(false);
    }
  }

  function newNonResidentBill() {

    setActiveBill(
      null
    );

    setNonResidentCustomerId(
      ""
    );

    setBillType(
      "RESTAURANT"
    );

    setTableReference(
      ""
    );

    setBillNotes(
      ""
    );

    setCart([]);

    clearResult();
  }

  const selectedCustomer =
    customers.find(
      (
        customer
      ) =>
        customer.id
        === (
          billingMode
          === "GUEST"
            ? guestCustomerId
            : nonResidentCustomerId
        )
    );

  return (
    <Panel
      title="POS / Orders"
      error={error}
    >

      <div className="space-y-6">

        {/* Header */}

        <div className="flex flex-col gap-4 xl:flex-row xl:items-center xl:justify-between">

          <div>

            <p className="max-w-2xl text-sm text-slate-500">
              {t(
                "Choose a checked-in guest folio or open a non-resident bill."
              )}
            </p>

          </div>

          {canManageMenu && (

            <Link
              href="/menu"
              className="inline-flex items-center justify-center gap-2 rounded-xl border border-blue-200 bg-white px-4 py-2.5 text-sm font-semibold text-blue-700 shadow-sm transition hover:bg-blue-50"
            >
              <UtensilsCrossed className="h-4 w-4" />

              {t(
                "Manage menu items"
              )}
            </Link>

          )}

        </div>

        {/* Billing choice */}

        <section className="grid gap-4 md:grid-cols-2">

          <button
            type="button"
            onClick={
              () =>
                changeBillingMode(
                  "GUEST"
                )
            }
            className={`group rounded-2xl border p-5 text-left transition ${
              billingMode
              === "GUEST"
                ? "border-blue-500 bg-blue-50 shadow-sm ring-1 ring-blue-500"
                : "border-slate-200 bg-white hover:border-blue-300 hover:shadow-sm"
            }`}
          >

            <div className="flex items-center gap-4">

              <div className={`flex h-14 w-14 items-center justify-center rounded-2xl ${
                billingMode
                === "GUEST"
                  ? "bg-blue-600 text-white"
                  : "bg-blue-50 text-blue-600"
              }`}>
                <BedDouble className="h-7 w-7" />
              </div>

              <div className="min-w-0 flex-1">

                <h2 className="text-lg font-semibold text-slate-950">
                  {t(
                    "Checked-in guest folio"
                  )}
                </h2>

                <p className="mt-1 text-sm text-slate-500">
                  {t(
                    "Post charges to a guest staying in the hotel."
                  )}
                </p>

              </div>

              <ChevronRight className="h-5 w-5 text-slate-400" />

            </div>

          </button>

          <button
            type="button"
            onClick={
              () =>
                changeBillingMode(
                  "NON_RESIDENT"
                )
            }
            className={`group rounded-2xl border p-5 text-left transition ${
              billingMode
              === "NON_RESIDENT"
                ? "border-teal-500 bg-teal-50 shadow-sm ring-1 ring-teal-500"
                : "border-slate-200 bg-white hover:border-teal-300 hover:shadow-sm"
            }`}
          >

            <div className="flex items-center gap-4">

              <div className={`flex h-14 w-14 items-center justify-center rounded-2xl ${
                billingMode
                === "NON_RESIDENT"
                  ? "bg-teal-600 text-white"
                  : "bg-teal-50 text-teal-600"
              }`}>
                <UsersRound className="h-7 w-7" />
              </div>

              <div className="min-w-0 flex-1">

                <h2 className="text-lg font-semibold text-slate-950">
                  {t(
                    "Non-resident bill"
                  )}
                </h2>

                <p className="mt-1 text-sm text-slate-500">
                  {t(
                    "Walk-in and outside customers. No room number required."
                  )}
                </p>

              </div>

              {billingMode
                === "NON_RESIDENT"
                ? (
                  <CheckCircle2 className="h-6 w-6 text-teal-600" />
                )
                : (
                  <ChevronRight className="h-5 w-5 text-slate-400" />
                )}

            </div>

          </button>

        </section>

        {/* Billing target */}

        {billingMode
          === "GUEST"
          ? (

            <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">

              <div className="mb-4 flex items-center gap-3">

                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-50 text-blue-600">
                  <CircleUserRound className="h-5 w-5" />
                </div>

                <div>

                  <h2 className="font-semibold text-slate-950">
                    {t(
                      "Guest billing"
                    )}
                  </h2>

                  <p className="text-sm text-slate-500">
                    {t(
                      "Choose the customer who is currently checked in."
                    )}
                  </p>

                </div>

              </div>

              <select
                className={
                  inputStyle
                }
                value={
                  guestCustomerId
                }
                disabled={
                  busy
                }
                onChange={
                  (
                    event
                  ) => {

                    setGuestCustomerId(
                      event.target.value
                    );

                    clearResult();
                  }
                }
              >

                <option value="">
                  {t(
                    "Select checked-in guest"
                  )}
                </option>

                {customers
                  .filter(
                    (
                      customer
                    ) =>
                      customer.kind
                      !== "WALK_IN"
                  )
                  .map(
                    (
                      customer
                    ) => (

                      <option
                        key={
                          customer.id
                        }
                        value={
                          customer.id
                        }
                      >
                        {customer.name}
                      </option>

                    )
                  )}

              </select>

              <p className="mt-3 text-xs text-slate-500">
                {t(
                  "The backend verifies that the selected customer has an active checked-in stay."
                )}
              </p>

            </section>

          )
          : (

            <section className="rounded-2xl border border-teal-100 bg-white p-5 shadow-sm">

              <div className="mb-5 flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">

                <div>

                  <h2 className="text-lg font-semibold text-slate-950">
                    {t(
                      "Non-resident bill setup"
                    )}
                  </h2>

                  <p className="mt-1 text-sm text-slate-500">
                    {t(
                      "Use a walk-in customer or attach the bill to an existing customer or organization."
                    )}
                  </p>

                </div>

                {activeBill && (

                  <button
                    type="button"
                    onClick={
                      newNonResidentBill
                    }
                    className="rounded-xl border px-4 py-2 text-sm font-semibold text-slate-700 transition hover:bg-slate-50"
                  >
                    {t(
                      "Start new bill"
                    )}
                  </button>

                )}

              </div>

              {activeBill
                ? (

                  <div className="rounded-2xl border border-teal-200 bg-teal-50 p-4">

                    <div className="flex flex-wrap items-center justify-between gap-4">

                      <div>

                        <p className="text-xs font-semibold uppercase tracking-wider text-teal-700">
                          {t(
                            "Active non-resident bill"
                          )}
                        </p>

                        <h3 className="mt-1 text-lg font-semibold text-slate-950">
                          {activeBill.reference}
                        </h3>

                        <p className="mt-1 text-sm text-slate-600">
                          {activeBill.customerName}
                          {" · "}
                          {t(
                            activeBill.billType
                          )}
                          {activeBill.tableReference
                            ? ` · ${activeBill.tableReference}`
                            : ""}
                        </p>

                      </div>

                      <Link
                        href={
                          `/folios/${activeBill.folioId}`
                        }
                        className="rounded-xl bg-white px-4 py-2 text-sm font-semibold text-teal-700 shadow-sm"
                      >
                        {t(
                          "Open bill"
                        )}
                      </Link>

                    </div>

                  </div>

                )
                : (

                  <div className="grid gap-4 lg:grid-cols-4">

                    <div>

                      <label className="mb-2 block text-sm font-medium text-slate-700">
                        {t(
                          "Bill type"
                        )}
                      </label>

                      <select
                        className={
                          inputStyle
                        }
                        value={
                          billType
                        }
                        disabled={
                          busy
                        }
                        onChange={
                          (
                            event
                          ) =>
                            setBillType(
                              event.target.value
                            )
                        }
                      >

                        {BILL_TYPES.map(
                          (
                            type
                          ) => {

                            const Icon =
                              billTypeIcon(
                                type
                              );

                            return (

                              <option
                                key={
                                  type
                                }
                                value={
                                  type
                                }
                              >
                                {t(
                                  type
                                )}
                              </option>

                            );
                          }
                        )}

                      </select>

                    </div>

                    <div>

                      <label className="mb-2 block text-sm font-medium text-slate-700">
                        {t(
                          "Customer"
                        )}
                      </label>

                      <select
                        className={
                          inputStyle
                        }
                        value={
                          nonResidentCustomerId
                        }
                        disabled={
                          busy
                        }
                        onChange={
                          (
                            event
                          ) =>
                            setNonResidentCustomerId(
                              event.target.value
                            )
                        }
                      >

                        <option value="">
                          {t(
                            "Walk-in customer"
                          )}
                        </option>

                        {customers
                          .filter(
                            (
                              customer
                            ) =>
                              customer.kind
                              !== "WALK_IN"
                          )
                          .map(
                            (
                              customer
                            ) => (

                              <option
                                key={
                                  customer.id
                                }
                                value={
                                  customer.id
                                }
                              >
                                {customer.name}
                              </option>

                            )
                          )}

                      </select>

                    </div>

                    <div>

                      <label className="mb-2 block text-sm font-medium text-slate-700">
                        {t(
                          "Table / reference"
                        )}
                      </label>

                      <input
                        className={
                          inputStyle
                        }
                        value={
                          tableReference
                        }
                        maxLength={
                          100
                        }
                        disabled={
                          busy
                        }
                        placeholder="T05 / Event A / REF123"
                        onChange={
                          (
                            event
                          ) =>
                            setTableReference(
                              event.target.value
                            )
                        }
                      />

                    </div>

                    <div>

                      <label className="mb-2 block text-sm font-medium text-slate-700">
                        {t(
                          "Notes"
                        )}
                      </label>

                      <input
                        className={
                          inputStyle
                        }
                        value={
                          billNotes
                        }
                        maxLength={
                          1000
                        }
                        disabled={
                          busy
                        }
                        placeholder={
                          t(
                            "Optional notes"
                          )
                        }
                        onChange={
                          (
                            event
                          ) =>
                            setBillNotes(
                              event.target.value
                            )
                        }
                      />

                    </div>

                  </div>

                )}

            </section>

          )}

        {/* Main POS */}

        <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_390px]">

          {/* Products */}

          <section className="min-w-0 rounded-2xl border border-slate-200 bg-white shadow-sm">

            <header className="border-b border-slate-100 p-5">

              <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">

                <div>

                  <h2 className="text-lg font-semibold text-slate-950">
                    {t(
                      "Menu items"
                    )}
                  </h2>

                  <p className="text-sm text-slate-500">
                    {t(
                      "Tap an item to add it to the order."
                    )}
                  </p>

                </div>

                <div className="relative w-full lg:max-w-sm">

                  <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />

                  <input
                    className="w-full rounded-xl border border-slate-200 bg-slate-50 py-2.5 pl-10 pr-3 text-sm outline-none transition focus:border-blue-400 focus:bg-white focus:ring-2 focus:ring-blue-100"
                    value={
                      search
                    }
                    placeholder={
                      t(
                        "Search menu items"
                      )
                    }
                    onChange={
                      (
                        event
                      ) =>
                        setSearch(
                          event.target.value
                        )
                    }
                  />

                </div>

              </div>

              <div className="mt-4 flex flex-wrap gap-2">

                {FILTERS.map(
                  (
                    destination
                  ) => (

                    <button
                      key={
                        destination
                      }
                      type="button"
                      onClick={
                        () =>
                          setFilter(
                            destination
                          )
                      }
                      className={`rounded-full px-4 py-2 text-sm font-semibold transition ${
                        filter
                        === destination
                          ? "bg-blue-600 text-white shadow-sm"
                          : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                      }`}
                    >
                      {t(
                        destination
                        === "ALL"
                          ? "All"
                          : destination
                      )}
                    </button>

                  )
                )}

              </div>

            </header>

            <div className="grid gap-4 p-5 sm:grid-cols-2 lg:grid-cols-3">

              {visibleProducts.map(
                (
                  product
                ) => {

                  const Icon =
                    product.destination
                    === "BAR"
                      ? GlassWater
                      : product.destination
                      === "KITCHEN"
                      ? UtensilsCrossed
                      : ReceiptText;

                  return (

                    <button
                      key={
                        product.id
                      }
                      type="button"
                      disabled={
                        busy
                      }
                      onClick={
                        () =>
                          addProduct(
                            product
                          )
                      }
                      className="group flex min-h-40 flex-col rounded-2xl border border-slate-200 bg-white p-4 text-left transition hover:-translate-y-0.5 hover:border-blue-300 hover:shadow-md disabled:opacity-50"
                    >

                      <div className="flex items-start justify-between gap-4">

                        <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-blue-50 text-blue-600 transition group-hover:bg-blue-600 group-hover:text-white">
                          <Icon className="h-5 w-5" />
                        </div>

                        <span className="rounded-lg bg-slate-100 px-2 py-1 text-[11px] font-semibold uppercase tracking-wide text-slate-500">
                          {t(
                            product.destination
                          )}
                        </span>

                      </div>

                      <div className="mt-4 flex-1">

                        <h3 className="font-semibold text-slate-950">
                          {product.name}
                        </h3>

                        {product.category && (

                          <p className="mt-1 text-xs text-slate-500">
                            {product.category}
                          </p>

                        )}

                      </div>

                      <div className="mt-4 flex items-center justify-between">

                        <span className="text-lg font-bold text-blue-700 tabular-nums">
                          {money(
                            productTotal(
                              product,
                              1
                            )
                          )}
                        </span>

                        <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-blue-600 text-white shadow-sm">
                          <Plus className="h-4 w-4" />
                        </span>

                      </div>

                    </button>

                  );
                }
              )}

              {!visibleProducts.length && (

                <div className="col-span-full rounded-2xl border border-dashed border-slate-300 p-10 text-center text-sm text-slate-500">
                  {t(
                    "No menu items match your search."
                  )}
                </div>

              )}

            </div>

          </section>

          {/* Cart */}

          <aside className="h-fit rounded-2xl border border-slate-200 bg-white shadow-sm xl:sticky xl:top-5">

            <header className="border-b border-slate-100 p-5">

              <div className="flex items-center justify-between gap-4">

                <div className="flex items-center gap-3">

                  <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-50 text-blue-600">
                    <ShoppingCart className="h-5 w-5" />
                  </div>

                  <div>

                    <h2 className="font-semibold text-slate-950">
                      {t(
                        "Order summary"
                      )}
                    </h2>

                    <p className="text-xs text-slate-500">
                      {cart.length}
                      {" "}
                      {t(
                        "items"
                      )}
                    </p>

                  </div>

                </div>

                <span className={`rounded-full px-3 py-1 text-xs font-semibold ${
                  billingMode
                  === "NON_RESIDENT"
                    ? "bg-teal-50 text-teal-700"
                    : "bg-blue-50 text-blue-700"
                }`}>
                  {t(
                    billingMode
                    === "NON_RESIDENT"
                      ? "Non-resident"
                      : "Guest folio"
                  )}
                </span>

              </div>

            </header>

            <div className="max-h-[430px] space-y-3 overflow-y-auto p-5">

              {cart.map(
                (
                  line
                ) => (

                  <article
                    key={
                      line.product.id
                    }
                    className="rounded-xl border border-slate-100 p-3"
                  >

                    <div className="flex items-start gap-3">

                      <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-slate-100 text-slate-600">
                        {line.product.destination
                        === "BAR"
                          ? (
                            <GlassWater className="h-5 w-5" />
                          )
                          : (
                            <UtensilsCrossed className="h-5 w-5" />
                          )}
                      </div>

                      <div className="min-w-0 flex-1">

                        <p className="font-semibold text-slate-950">
                          {line.product.name}
                        </p>

                        <p className="mt-1 text-sm font-medium text-blue-700 tabular-nums">
                          {money(
                            productTotal(
                              line.product,
                              line.quantity
                            )
                          )}
                        </p>

                      </div>

                      <button
                        type="button"
                        disabled={
                          busy
                        }
                        onClick={
                          () =>
                            removeProduct(
                              line.product.id
                            )
                        }
                        className="rounded-lg p-2 text-red-500 transition hover:bg-red-50"
                        aria-label={
                          t(
                            "Remove"
                          )
                        }
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>

                    </div>

                    <div className="mt-3 flex items-center justify-end gap-2">

                      <button
                        type="button"
                        disabled={
                          busy
                        }
                        onClick={
                          () =>
                            setQuantity(
                              line.product.id,
                              line.quantity
                              - 1
                            )
                        }
                        className="flex h-8 w-8 items-center justify-center rounded-lg border border-slate-200 hover:bg-slate-50"
                      >
                        <Minus className="h-3.5 w-3.5" />
                      </button>

                      <span className="min-w-9 text-center text-sm font-semibold tabular-nums">
                        {line.quantity}
                      </span>

                      <button
                        type="button"
                        disabled={
                          busy
                        }
                        onClick={
                          () =>
                            setQuantity(
                              line.product.id,
                              line.quantity
                              + 1
                            )
                        }
                        className="flex h-8 w-8 items-center justify-center rounded-lg border border-slate-200 hover:bg-slate-50"
                      >
                        <Plus className="h-3.5 w-3.5" />
                      </button>

                    </div>

                  </article>

                )
              )}

              {!cart.length && (

                <div className="py-12 text-center">

                  <ShoppingCart className="mx-auto h-10 w-10 text-slate-300" />

                  <p className="mt-3 font-medium text-slate-600">
                    {t(
                      "Your order is empty"
                    )}
                  </p>

                  <p className="mt-1 text-sm text-slate-400">
                    {t(
                      "Select menu items to begin."
                    )}
                  </p>

                </div>

              )}

            </div>

            <div className="border-t border-slate-100 p-5">

              <div className="space-y-2 text-sm">

                <div className="flex justify-between text-slate-500">
                  <span>
                    {t(
                      "Subtotal"
                    )}
                  </span>

                  <span className="tabular-nums">
                    {money(
                      subtotal
                    )}
                  </span>
                </div>

                <div className="flex justify-between text-slate-500">
                  <span>
                    {t(
                      "Tax"
                    )}
                  </span>

                  <span className="tabular-nums">
                    {money(
                      tax
                    )}
                  </span>
                </div>

                <div className="mt-3 flex items-end justify-between border-t border-slate-100 pt-4">

                  <span className="font-semibold text-slate-800">
                    {t(
                      "Grand total"
                    )}
                  </span>

                  <span className="text-2xl font-bold text-slate-950 tabular-nums">
                    {money(
                      grandTotal
                    )}
                  </span>

                </div>

              </div>

              {selectedCustomer && (

                <div className="mt-4 rounded-xl bg-slate-50 px-4 py-3 text-sm">

                  <p className="text-xs uppercase tracking-wide text-slate-400">
                    {t(
                      "Customer"
                    )}
                  </p>

                  <p className="mt-1 font-semibold text-slate-700">
                    {selectedCustomer.name}
                  </p>

                </div>

              )}

              {billingMode
                === "NON_RESIDENT"
                &&
                !nonResidentCustomerId
                && (

                  <div className="mt-4 rounded-xl bg-teal-50 px-4 py-3 text-sm text-teal-800">

                    <Building2 className="mr-2 inline h-4 w-4" />

                    {t(
                      "This bill will use the hotel walk-in customer."
                    )}

                  </div>

                )}

              <button
                type="button"
                disabled={
                  busy
                  ||
                  !cart.length
                  ||
                  (
                    billingMode
                    === "GUEST"
                    &&
                    !guestCustomerId
                  )
                }
                onClick={
                  send
                }
                className="mt-5 flex w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-blue-600 to-teal-600 px-5 py-3.5 font-semibold text-white shadow-sm transition hover:shadow-md disabled:cursor-not-allowed disabled:opacity-40"
              >
                {busy
                  ? t(
                      "Saving"
                    )
                  : t(
                      "Confirm and send"
                    )}

                {!busy && (
                  <ChevronRight className="h-4 w-4" />
                )}
              </button>

              {message && (

                <div
                  role="status"
                  className="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-800"
                >
                  <div className="flex gap-2">

                    <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0" />

                    <div>

                      <p>
                        {t(
                          message
                        )}
                      </p>

                      {lastFolioId && (

                        <Link
                          href={
                            `/folios/${lastFolioId}`
                          }
                          className="mt-2 inline-block font-semibold underline"
                        >
                          {t(
                            "Open folio and record payment"
                          )}
                        </Link>

                      )}

                    </div>

                  </div>
                </div>

              )}

            </div>

          </aside>

        </div>

        <OrderHistory />

      </div>

    </Panel>
  );
}