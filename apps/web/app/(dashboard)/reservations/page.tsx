"use client";

import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";

import Link from "next/link";
import { useSession } from "next-auth/react";

import {
  CalendarX2,
  Pencil,
  WalletCards,
} from "lucide-react";

import {
  api,
  all,
} from "@/lib/hms-api";

import {
  Panel,
  Field,
  inputStyle,
  buttonStyle,
} from "@/components/operations/ui";

import {
  useLocale,
} from "@/components/LocaleProvider";

type Room = {
  id: string;
  code: string;
  active: boolean;
  operational: string;
  bedDimensions: string;
};

type Reservation = {
  id: string;
  reference: string;
  customerId: string;
  checkIn: string;
  checkOut: string;
  status: string;
  roomIds: string[];
  adults: number;
  children: number;
  folioId: string;
};

type PaymentSummary = {
  reservationId: string;
  reference: string;
  folioId: string;
  currency: string;
  reservationTotal: number;
  advancePaid: number;
  remaining: number;
  paymentStatus: string;
};

const shift = (
  day: string,
  days: number,
) =>
  new Date(
    new Date(
      `${day}T00:00:00Z`,
    ).getTime()
      + days * 86400000,
  )
    .toISOString()
    .slice(0, 10);

const money = (
  value: number | undefined,
) =>
  value == null
    ? "—"
    : Number(value).toLocaleString(
        undefined,
        {
          minimumFractionDigits: 2,
          maximumFractionDigits: 4,
        },
      );

const nightsBetween = (
  checkIn: string,
  checkOut: string,
) => {

  if (
    !checkIn
    ||
    !checkOut
    ||
    checkOut <= checkIn
  ) {
    return 0;
  }

  return Math.round(
    (
      new Date(
        `${checkOut}T00:00:00Z`,
      ).getTime()
      -
      new Date(
        `${checkIn}T00:00:00Z`,
      ).getTime()
    )
    /
    86400000,
  );
};


export default function Reservations() {

  const { t } =
    useLocale();

  const { data: session } =
    useSession();

  const permissions =
    (
      session?.user as {
        permissions?: string[];
      }
    )?.permissions ?? [];

  const canRecordPayments =
    permissions.includes(
      "PAYMENT_RECORD",
    );

  const canModifyReservations =
    permissions.includes(
      "RESERVATION_MODIFY",
    );

  const [
    rooms,
    setRooms,
  ] =
    useState<Room[]>([]);

  const [
    rows,
    setRows,
  ] =
    useState<Reservation[]>([]);

  const [
    summaries,
    setSummaries,
  ] =
    useState<
      Record<
        string,
        PaymentSummary
      >
    >({});

  const [
    names,
    setNames,
  ] =
    useState<
      Record<string, string>
    >({});

  const [
    start,
    setStart,
  ] =
    useState("");

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
    busy,
    setBusy,
  ] =
    useState(false);

  const [
    search,
    setSearch,
  ] =
    useState("");

  const [
    statusFilter,
    setStatusFilter,
  ] =
    useState("ALL");

  const [
    paymentFilter,
    setPaymentFilter,
  ] =
    useState("ALL");

  const [
    sortBy,
    setSortBy,
  ] =
    useState("OPERATIONAL");

  const [
    page,
    setPage,
  ] =
    useState(0);

  const [
    editFor,
    setEditFor,
  ] =
    useState<Reservation | null>(
      null,
    );

  const [
    editCheckIn,
    setEditCheckIn,
  ] =
    useState("");

  const [
    editCheckOut,
    setEditCheckOut,
  ] =
    useState("");

  const [
    advanceFor,
    setAdvanceFor,
  ] =
    useState<Reservation | null>(
      null,
    );

  const [
    advanceSummary,
    setAdvanceSummary,
  ] =
    useState<PaymentSummary | null>(
      null,
    );

  const [
    amount,
    setAmount,
  ] =
    useState("");

  const [
    method,
    setMethod,
  ] =
    useState("CASH");

  const [
    currency,
    setCurrency,
  ] =
    useState("");

  const requestId =
    useRef("");

  const load =
    useCallback(
      async () => {

        const reservations =
          await all<Reservation>(
            "reservations",
          );

        setRows(
          reservations,
        );

        const nextSummaries:
          Record<
            string,
            PaymentSummary
          > = {};

        await Promise.all(
          reservations.map(
            async (
              reservation,
            ) => {

              try {

                nextSummaries[
                  reservation.id
                ] =
                  await api<PaymentSummary>(
                    `reservations/${reservation.id}/payment-summary`,
                  );

              } catch {

                /*
                 * Do not make the entire reservation screen fail because
                 * one historical reservation has incomplete legacy data.
                 */
              }
            },
          ),
        );

        setSummaries(
          nextSummaries,
        );
      },
      [],
    );

  useEffect(
    () => {

      setStart(
        new Date()
          .toISOString()
          .slice(
            0,
            10,
          ),
      );

      Promise.all([
        load(),

        all<Room>(
          "rooms",
        ).then(
          setRooms,
        ),

        all<{
          id: string;
          name: string;
        }>(
          "customers",
        ).then(
          (
            customers,
          ) =>
            setNames(
              Object.fromEntries(
                customers.map(
                  (
                    customer,
                  ) => [
                    customer.id,
                    customer.name,
                  ],
                ),
              ),
            ),
        ),
      ]).catch(
        (
          loadError,
        ) =>
          setError(
            loadError instanceof Error
              ? loadError.message
              : "Unable to load reservations.",
          ),
      );

    },
    [load],
  );

  const filteredRows =
    useMemo(
      () => {

        const query =
          search
            .trim()
            .toLowerCase();

        const filtered =
          rows.filter(
            (
              reservation,
            ) => {

              const customer =
                (
                  names[
                    reservation.customerId
                  ]
                  ?? ""
                ).toLowerCase();

              const roomText =
                rooms
                  .filter(
                    (
                      room,
                    ) =>
                      reservation.roomIds.includes(
                        room.id,
                      ),
                  )
                  .map(
                    (
                      room,
                    ) =>
                      room.code,
                  )
                  .join(" ")
                  .toLowerCase();

              const matchesSearch =
                !query
                ||
                reservation.reference
                  .toLowerCase()
                  .includes(
                    query,
                  )
                ||
                customer.includes(
                  query,
                )
                ||
                roomText.includes(
                  query,
                )
                ||
                reservation.checkIn.includes(
                  query,
                )
                ||
                reservation.checkOut.includes(
                  query,
                );

              const matchesStatus =
                statusFilter
                === "ALL"
                ||
                reservation.status
                === statusFilter;

              const summary =
                summaries[
                  reservation.id
                ];

              let matchesPayment =
                true;

              if (
                paymentFilter
                !== "ALL"
              ) {

                if (!summary) {

                  matchesPayment =
                    paymentFilter
                    === "NO_PAYMENT";

                } else {

                  matchesPayment =
                    summary.paymentStatus
                    === paymentFilter;
                }
              }

              return (
                matchesSearch
                &&
                matchesStatus
                &&
                matchesPayment
              );
            },
          );

        if (
          sortBy
          === "OPERATIONAL"
        ) {
          return filtered;
        }

        return [
          ...filtered,
        ].sort(
          (
            a,
            b,
          ) => {

            const customerA =
              names[
                a.customerId
              ]
              ?? "";

            const customerB =
              names[
                b.customerId
              ]
              ?? "";

            switch (
              sortBy
            ) {

              case "CHECK_IN_ASC":
                return a.checkIn.localeCompare(
                  b.checkIn,
                );

              case "CHECK_IN_DESC":
                return b.checkIn.localeCompare(
                  a.checkIn,
                );

              case "CHECK_OUT_ASC":
                return a.checkOut.localeCompare(
                  b.checkOut,
                );

              case "CHECK_OUT_DESC":
                return b.checkOut.localeCompare(
                  a.checkOut,
                );

              case "CUSTOMER_ASC":
                return customerA.localeCompare(
                  customerB,
                );

              case "CUSTOMER_DESC":
                return customerB.localeCompare(
                  customerA,
                );

              case "REFERENCE_ASC":
                return a.reference.localeCompare(
                  b.reference,
                );

              case "STATUS":
                return a.status.localeCompare(
                  b.status,
                );

              default:
                return 0;
            }
          },
        );
      },
      [
        rows,
        names,
        rooms,
        summaries,
        search,
        statusFilter,
        paymentFilter,
        sortBy,
      ],
    );

  const pageSize =
    10;

  const totalPages =
    Math.max(
      1,
      Math.ceil(
        filteredRows.length
        / pageSize,
      ),
    );

  const visibleRows =
    filteredRows.slice(
      page * pageSize,
      (
        page + 1
      ) * pageSize,
    );

  useEffect(
    () => {
      setPage(0);
    },
    [
      search,
      statusFilter,
      paymentFilter,
      sortBy,
    ],
  );

  useEffect(
    () => {

      if (
        page
        >= totalPages
      ) {

        setPage(
          totalPages - 1,
        );
      }

    },
    [
      page,
      totalPages,
    ],
  );


  const days =
    start
      ? Array.from(
          {
            length: 14,
          },
          (
            _,
            index,
          ) =>
            shift(
              start,
              index,
            ),
        )
      : [];

  function openEdit(
    reservation: Reservation,
  ) {

    setError("");
    setMessage("");

    setEditFor(
      reservation,
    );

    setEditCheckIn(
      reservation.checkIn,
    );

    setEditCheckOut(
      reservation.checkOut,
    );
  }


  async function saveEdit() {

    if (!editFor) {
      return;
    }

    if (
      !editCheckIn
      ||
      !editCheckOut
      ||
      editCheckOut <= editCheckIn
    ) {

      setError(
        "Check-out must be after check-in.",
      );

      return;
    }

    setBusy(true);
    setError("");
    setMessage("");

    try {

      await api(
        `reservations/${editFor.id}`,
        "PUT",
        {
          checkIn:
            editCheckIn,

          checkOut:
            editCheckOut,
        },
      );

      setEditFor(
        null,
      );

      setEditCheckIn("");
      setEditCheckOut("");

      await load();

      setMessage(
        "Reservation updated",
      );

    } catch (
      actionError
    ) {

      setError(
        actionError instanceof Error
          ? actionError.message
          : "Unable to update reservation.",
      );

    } finally {

      setBusy(false);
    }
  }


  async function cancel(
    id: string,
  ) {

    setBusy(true);
    setError("");
    setMessage("");

    try {

      await api(
        `reservations/${id}/cancel`,
        "POST",
      );

      await load();

      setMessage(
        "Reservation cancelled",
      );

    } catch (
      actionError
    ) {

      setError(
        actionError instanceof Error
          ? actionError.message
          : "Unable to cancel reservation.",
      );

    } finally {

      setBusy(false);
    }
  }

  async function openAdvance(
    reservation: Reservation,
  ) {

    setBusy(true);
    setError("");
    setMessage("");

    try {

      const summary =
        summaries[
          reservation.id
        ]
        ??
        await api<PaymentSummary>(
          `reservations/${reservation.id}/payment-summary`,
        );

      setSummaries(
        (
          current,
        ) => ({
          ...current,
          [reservation.id]:
            summary,
        }),
      );

      setAdvanceFor(
        reservation,
      );

      setAdvanceSummary(
        summary,
      );

      setAmount("");
      setMethod("CASH");

      setCurrency(
        summary.currency,
      );

      requestId.current =
        "";

    } catch (
      actionError
    ) {

      setError(
        actionError instanceof Error
          ? actionError.message
          : "Unable to load reservation payment information.",
      );

    } finally {

      setBusy(false);
    }
  }

  async function submitAdvance() {

    if (
      !advanceFor
      || !advanceSummary
    ) {
      return;
    }

    const numericAmount =
      Number(
        amount,
      );

    if (
      !Number.isFinite(
        numericAmount,
      )
      || numericAmount <= 0
    ) {

      setError(
        "Advance amount must be greater than zero.",
      );

      return;
    }

    const normalizedCurrency =
      currency
        .trim()
        .toUpperCase();

    if (
      !/^[A-Z]{3}$/.test(
        normalizedCurrency,
      )
    ) {

      setError(
        "Payment currency must contain exactly three letters.",
      );

      return;
    }

    setBusy(true);
    setError("");
    setMessage("");

    try {

      requestId.current ||=
        crypto.randomUUID();

      await api(
        `reservations/${advanceFor.id}/advance-payments`,
        "POST",
        {
          requestId:
            requestId.current,

          parts: [
            {
              method,
              currency:
                normalizedCurrency,
              amount:
                numericAmount,
            },
          ],
        },
      );

      requestId.current =
        "";

      setAdvanceFor(
        null,
      );

      setAdvanceSummary(
        null,
      );

      setAmount("");

      setMessage(
        "Advance payment submitted for approval",
      );

      await load();

    } catch (
      actionError
    ) {

      /*
       * Keep requestId after an error.
       * A retry therefore remains idempotent.
       */
      setError(
        actionError instanceof Error
          ? actionError.message
          : "Unable to submit reservation advance.",
      );

    } finally {

      setBusy(false);
    }
  }

  return (

    <Panel
      title="Reservations"
      error={error}
    >

      <div className="flex flex-wrap items-end justify-between gap-5">

        <Field label="Calendar start">

          <input
            type="date"
            className={
              inputStyle
            }
            value={start}
            onChange={
              (
                event,
              ) =>
                setStart(
                  event.target.value,
                )
            }
          />

        </Field>

        <Link
          href="/reservations/new"
          className={
            buttonStyle
          }
        >
          {t(
            "New reservation",
          )}
        </Link>

      </div>

      {message && (

        <p
          role="status"
          className="rounded-xl bg-emerald-50 px-4 py-3 text-sm text-emerald-800"
        >
          {t(
            message,
          )}

          {message.includes(
            "approval",
          ) && (
            <>
              {" · "}
              <Link
                href="/cashier"
                className="font-medium underline"
              >
                {t(
                  "Open Cashier",
                )}
              </Link>
            </>
          )}
        </p>

      )}

      <p className="text-slate-500">
        {t(
          "Select an available day to start a reservation",
        )}
      </p>

      <div className="overflow-auto rounded-xl border bg-white">

        <table className="w-full text-center text-sm">

          <thead>
            <tr>

              <th className="p-3">
                {t("Room")}
              </th>

              {days.map(
                (
                  day,
                ) => (

                  <th
                    key={
                      day
                    }
                    className="whitespace-nowrap p-2"
                  >
                    {day.slice(
                      5,
                    )}
                  </th>

                ),
              )}

            </tr>
          </thead>

          <tbody>

            {rooms.map(
              (
                room,
              ) => (

                <tr
                  key={
                    room.id
                  }
                  className="border-t"
                >

                  <th className="whitespace-nowrap p-3">

                    {room.code}

                    <small className="block font-normal">
                      {
                        room.bedDimensions
                      }
                    </small>

                  </th>

                  {days.map(
                    (
                      day,
                    ) => {

                      const reservation =
                        rows.find(
                          (
                            row,
                          ) =>
                            ![
                              "CANCELLED",
                              "CHECKED_OUT",
                            ].includes(
                              row.status,
                            )
                            &&
                            row.roomIds.includes(
                              room.id,
                            )
                            &&
                            row.checkIn <= day
                            &&
                            row.checkOut > day,
                        );

                      const available =
                        room.active
                        &&
                        room.operational
                        === "AVAILABLE"
                        &&
                        !reservation;

                      return (

                        <td
                          key={
                            day
                          }
                          className={
                            `border p-2 ${
                              available
                                ? "bg-emerald-50 text-emerald-800"
                                : "bg-rose-50 text-rose-800"
                            }`
                          }
                        >

                          {available
                            ? (

                              <Link
                                href={
                                  `/reservations/new?checkIn=${day}&checkOut=${shift(day, 1)}`
                                }
                              >
                                {t(
                                  "Available",
                                )}
                              </Link>

                            )
                            : (

                              <span>
                                {t(
                                  reservation
                                    ? "Reserved"
                                    : "Unavailable",
                                )}
                              </span>

                            )}

                        </td>
                      );
                    },
                  )}

                </tr>

              ),
            )}

          </tbody>

        </table>

      </div>

      <section className="rounded-xl border bg-white p-4">

        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">

          <Field label="Search">

            <input
              className={inputStyle}
              placeholder="Customer, reference or room"
              value={search}
              onChange={
                (
                  event,
                ) =>
                  setSearch(
                    event.target.value,
                  )
              }
            />

          </Field>

          <Field label="Status">

            <select
              className={inputStyle}
              value={statusFilter}
              onChange={
                (
                  event,
                ) =>
                  setStatusFilter(
                    event.target.value,
                  )
              }
            >
              <option value="ALL">
                {t("All statuses")}
              </option>
              <option value="CONFIRMED">
                {t("CONFIRMED")}
              </option>
              <option value="CHECKED_IN">
                {t("CHECKED_IN")}
              </option>
              <option value="CHECKED_OUT">
                {t("CHECKED_OUT")}
              </option>
              <option value="NO_SHOW">
                {t("NO_SHOW")}
              </option>
              <option value="CANCELLED">
                {t("CANCELLED")}
              </option>
            </select>

          </Field>

          <Field label="Payment">

            <select
              className={inputStyle}
              value={paymentFilter}
              onChange={
                (
                  event,
                ) =>
                  setPaymentFilter(
                    event.target.value,
                  )
              }
            >
              <option value="ALL">
                {t("All payments")}
              </option>
              <option value="NO_PAYMENT">
                {t("No advance")}
              </option>
              <option value="PARTIALLY_PREPAID">
                {t("Partially prepaid")}
              </option>
              <option value="FULLY_PREPAID">
                {t("Fully prepaid")}
              </option>
            </select>

          </Field>

          <Field label="Sort by">

            <select
              className={inputStyle}
              value={sortBy}
              onChange={
                (
                  event,
                ) =>
                  setSortBy(
                    event.target.value,
                  )
              }
            >
              <option value="OPERATIONAL">
                {t("Operational priority")}
              </option>
              <option value="CHECK_IN_ASC">
                {t("Arrival date - earliest first")}
              </option>
              <option value="CHECK_IN_DESC">
                {t("Arrival date - latest first")}
              </option>
              <option value="CHECK_OUT_ASC">
                {t("Checkout date - earliest first")}
              </option>
              <option value="CHECK_OUT_DESC">
                {t("Checkout date - latest first")}
              </option>
              <option value="CUSTOMER_ASC">
                {t("Customer A-Z")}
              </option>
              <option value="CUSTOMER_DESC">
                {t("Customer Z-A")}
              </option>
              <option value="REFERENCE_ASC">
                {t("Reservation reference")}
              </option>
              <option value="STATUS">
                {t("Status")}
              </option>
            </select>

          </Field>

        </div>

        <div className="mt-4 flex flex-wrap items-center justify-between gap-3 text-sm text-slate-500">

          <span>
            {filteredRows.length}
            {" "}
            {t("reservations found")}
          </span>

          {(search
            ||
            statusFilter !== "ALL"
            ||
            paymentFilter !== "ALL"
            ||
            sortBy !== "OPERATIONAL") && (

            <button
              type="button"
              className="font-medium text-blue-700"
              onClick={
                () => {
                  setSearch("");
                  setStatusFilter("ALL");
                  setPaymentFilter("ALL");
                  setSortBy("OPERATIONAL");
                }
              }
            >
              {t("Clear filters")}
            </button>

          )}

        </div>

      </section>

      <div className="overflow-auto rounded-xl border bg-white">

        <table className="w-full text-left text-sm">

          <thead className="bg-slate-50">

            <tr>

              {[
                "Reference",
                "Customer",
                "Dates",
                "Reservation total",
                "Advance received",
                "Remaining",
                "Payment status",
                "Status",
                "Actions",
              ].map(
                (
                  heading,
                ) => (

                  <th
                    key={
                      heading
                    }
                    className="whitespace-nowrap p-3"
                  >
                    {t(
                      heading,
                    )}
                  </th>

                ),
              )}

            </tr>

          </thead>

          <tbody>

            {visibleRows.map(
              (
                reservation,
              ) => {

                const summary =
                  summaries[
                    reservation.id
                  ];

                return (

                  <tr
                    key={
                      reservation.id
                    }
                    className="border-t transition-colors hover:bg-slate-50/70"
                  >

                    <td className="whitespace-nowrap p-3 font-medium">
                      {
                        reservation.reference
                      }
                    </td>

                    <td className="whitespace-nowrap p-3">
                      {
                        names[
                          reservation.customerId
                        ]
                        ?? "—"
                      }
                    </td>

                    <td className="whitespace-nowrap p-3">
                      {
                        reservation.checkIn
                      }
                      {" → "}
                      {
                        reservation.checkOut
                      }
                    </td>

                    <td className="whitespace-nowrap p-3 tabular-nums">
                      {summary
                        ? `${money(summary.reservationTotal)} ${summary.currency}`
                        : "—"}
                    </td>

                    <td className="whitespace-nowrap p-3 tabular-nums">
                      {summary
                        ? `${money(summary.advancePaid)} ${summary.currency}`
                        : "—"}
                    </td>

                    <td className="whitespace-nowrap p-3 tabular-nums">
                      {summary
                        ? `${money(summary.remaining)} ${summary.currency}`
                        : "—"}
                    </td>

                    <td className="whitespace-nowrap p-3">
                      {summary
                        ? t(
                            summary.paymentStatus,
                          )
                        : "—"}
                    </td>

                    <td className="whitespace-nowrap p-3">
                      {t(
                        reservation.status,
                      )}
                    </td>

                    <td className="whitespace-nowrap p-3 align-middle">

                      <div className="flex min-w-[148px] items-center gap-2">

                        {reservation.status
                          === "CONFIRMED"
                          &&
                          canModifyReservations
                          && (

                            <button
                              type="button"
                              disabled={
                                busy
                              }
                              title={t(
                                "Edit reservation",
                              )}
                              aria-label={t(
                                "Edit reservation",
                              )}
                              className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-slate-200 bg-white text-slate-700 shadow-sm transition hover:border-slate-300 hover:bg-slate-50 hover:text-slate-950 disabled:cursor-not-allowed disabled:opacity-40"
                              onClick={
                                () =>
                                  openEdit(
                                    reservation,
                                  )
                              }
                            >
                              <Pencil
                                className="h-4 w-4"
                                strokeWidth={2}
                              />
                            </button>

                          )}

                        {reservation.status
                          === "CONFIRMED"
                          &&
                          canRecordPayments
                          && (

                            <button
                              type="button"
                              disabled={
                                busy
                              }
                              title={t(
                                "Receive advance",
                              )}
                              aria-label={t(
                                "Receive advance",
                              )}
                              className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-blue-200 bg-blue-50 text-blue-700 shadow-sm transition hover:border-blue-300 hover:bg-blue-100 disabled:cursor-not-allowed disabled:opacity-40"
                              onClick={
                                () =>
                                  openAdvance(
                                    reservation,
                                  )
                              }
                            >
                              <WalletCards
                                className="h-4 w-4"
                                strokeWidth={2}
                              />
                            </button>

                          )}

                        {reservation.status
                          === "CONFIRMED"
                          && (

                            <button
                              type="button"
                              disabled={
                                busy
                              }
                              title={t(
                                "Cancel reservation",
                              )}
                              aria-label={t(
                                "Cancel reservation",
                              )}
                              className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-red-200 bg-red-50 text-red-700 shadow-sm transition hover:border-red-300 hover:bg-red-100 disabled:cursor-not-allowed disabled:opacity-40"
                              onClick={
                                () =>
                                  cancel(
                                    reservation.id,
                                  )
                              }
                            >
                              <CalendarX2
                                className="h-4 w-4"
                                strokeWidth={2}
                              />
                            </button>

                          )}

                      </div>

                    </td>

                  </tr>

                );
              },
            )}

          </tbody>

        </table>

      </div>

      <div className="flex flex-wrap items-center justify-between gap-4 rounded-xl border bg-white px-4 py-3">

        <p className="text-sm text-slate-500">
          {t("Page")}
          {" "}
          {page + 1}
          {" "}
          {t("of")}
          {" "}
          {totalPages}
          {" · "}
          {filteredRows.length}
          {" "}
          {t("reservations")}
        </p>

        <div className="flex gap-2">

          <button
            type="button"
            className="rounded-lg border px-4 py-2 text-sm font-medium disabled:opacity-40"
            disabled={
              page === 0
            }
            onClick={
              () =>
                setPage(
                  (
                    current,
                  ) =>
                    Math.max(
                      0,
                      current - 1,
                    ),
                )
            }
          >
            {t("Previous")}
          </button>

          <button
            type="button"
            className="rounded-lg border px-4 py-2 text-sm font-medium disabled:opacity-40"
            disabled={
              page + 1
              >= totalPages
            }
            onClick={
              () =>
                setPage(
                  (
                    current,
                  ) =>
                    Math.min(
                      totalPages - 1,
                      current + 1,
                    ),
                )
            }
          >
            {t("Next")}
          </button>

        </div>

      </div>

      {editFor && (() => {

        const summary =
          summaries[
            editFor.id
          ];

        const currentNights =
          nightsBetween(
            editFor.checkIn,
            editFor.checkOut,
          );

        const newNights =
          nightsBetween(
            editCheckIn,
            editCheckOut,
          );

        const newTotal =
          summary
          &&
          currentNights > 0
            ? (
                summary.reservationTotal
                /
                currentNights
                *
                newNights
              )
            : null;

        const assignedRooms =
          rooms
            .filter(
              (
                room,
              ) =>
                editFor.roomIds.includes(
                  room.id,
                ),
            )
            .map(
              (
                room,
              ) =>
                room.code,
            )
            .join(", ");

        return (

          <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 p-5 backdrop-blur-sm">

            <section className="w-full max-w-xl space-y-5 rounded-2xl bg-white p-6 shadow-xl">

              <header>

                <h2 className="text-xl font-semibold">
                  {t(
                    "Edit reservation",
                  )}
                </h2>

                <p className="mt-1 text-sm text-slate-500">
                  {
                    editFor.reference
                  }
                  {" · "}
                  {
                    names[
                      editFor.customerId
                    ]
                    ?? "—"
                  }
                </p>

              </header>

              <div className="rounded-xl bg-slate-50 p-4">

                <p className="text-xs uppercase tracking-wide text-slate-500">
                  {t(
                    "Assigned rooms",
                  )}
                </p>

                <p className="mt-1 font-medium">
                  {
                    assignedRooms
                    || "—"
                  }
                </p>

                <p className="mt-2 text-xs text-slate-500">
                  {t(
                    "The system will verify that these rooms are still available for the new dates before saving.",
                  )}
                </p>

              </div>

              <div className="grid gap-4 sm:grid-cols-2">

                <Field label="Check-in">

                  <input
                    type="date"
                    className={
                      inputStyle
                    }
                    value={
                      editCheckIn
                    }
                    disabled={
                      busy
                    }
                    onChange={
                      (
                        event,
                      ) =>
                        setEditCheckIn(
                          event.target.value,
                        )
                    }
                  />

                </Field>

                <Field label="Check-out">

                  <input
                    type="date"
                    className={
                      inputStyle
                    }
                    value={
                      editCheckOut
                    }
                    disabled={
                      busy
                    }
                    onChange={
                      (
                        event,
                      ) =>
                        setEditCheckOut(
                          event.target.value,
                        )
                    }
                  />

                </Field>

              </div>

              <div className="grid gap-3 sm:grid-cols-3">

                <div className="rounded-xl border p-4">

                  <p className="text-xs text-slate-500">
                    {t(
                      "Current nights",
                    )}
                  </p>

                  <p className="mt-1 text-lg font-semibold">
                    {
                      currentNights
                    }
                  </p>

                </div>

                <div className="rounded-xl border p-4">

                  <p className="text-xs text-slate-500">
                    {t(
                      "New nights",
                    )}
                  </p>

                  <p className="mt-1 text-lg font-semibold">
                    {
                      newNights
                    }
                  </p>

                </div>

                <div className="rounded-xl border p-4">

                  <p className="text-xs text-slate-500">
                    {t(
                      "New total",
                    )}
                  </p>

                  <p className="mt-1 font-semibold tabular-nums">

                    {newTotal != null
                      && summary
                        ? `${money(newTotal)} ${summary.currency}`
                        : "—"}

                  </p>

                </div>

              </div>

              {summary && (

                <div className="rounded-xl bg-blue-50 p-4 text-sm text-blue-900">

                  <div className="flex justify-between gap-4">

                    <span>
                      {t(
                        "Advance received",
                      )}
                    </span>

                    <strong className="tabular-nums">
                      {
                        money(
                          summary.advancePaid,
                        )
                      }
                      {" "}
                      {
                        summary.currency
                      }
                    </strong>

                  </div>

                </div>

              )}

              <p className="text-sm text-slate-500">
                {t(
                  "If the new dates overlap another reservation, the change will be rejected. If shortening the stay makes the reservation total lower than an advance already received, the excess advance must be refunded first.",
                )}
              </p>

              <footer className="flex justify-end gap-3 border-t pt-5">

                <button
                  type="button"
                  className="rounded-lg border px-5 py-2 font-medium"
                  disabled={
                    busy
                  }
                  onClick={
                    () => {

                      setEditFor(
                        null,
                      );

                      setEditCheckIn("");
                      setEditCheckOut("");
                    }
                  }
                >
                  {t(
                    "Cancel",
                  )}
                </button>

                <button
                  type="button"
                  className={
                    buttonStyle
                  }
                  disabled={
                    busy
                    ||
                    !editCheckIn
                    ||
                    !editCheckOut
                    ||
                    editCheckOut
                    <= editCheckIn
                  }
                  onClick={
                    saveEdit
                  }
                >
                  {t(
                    busy
                      ? "Saving"
                      : "Save changes",
                  )}
                </button>

              </footer>

            </section>

          </div>
        );
      })()}

      {advanceFor
        &&
        advanceSummary
        && (

          <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 p-5 backdrop-blur-sm">

            <section className="w-full max-w-lg space-y-5 rounded-2xl bg-white p-6 shadow-xl">

              <header>

                <h2 className="text-xl font-semibold">
                  {t(
                    "Receive reservation advance",
                  )}
                </h2>

                <p className="mt-1 text-sm text-slate-500">
                  {
                    advanceFor.reference
                  }
                  {" · "}
                  {
                    names[
                      advanceFor.customerId
                    ]
                  }
                </p>

              </header>

              <div className="grid grid-cols-3 gap-3">

                <div className="rounded-xl bg-slate-50 p-3">

                  <p className="text-xs text-slate-500">
                    {t(
                      "Reservation total",
                    )}
                  </p>

                  <strong className="tabular-nums">
                    {money(
                      advanceSummary.reservationTotal,
                    )}
                  </strong>

                </div>

                <div className="rounded-xl bg-slate-50 p-3">

                  <p className="text-xs text-slate-500">
                    {t(
                      "Advance received",
                    )}
                  </p>

                  <strong className="tabular-nums">
                    {money(
                      advanceSummary.advancePaid,
                    )}
                  </strong>

                </div>

                <div className="rounded-xl bg-slate-50 p-3">

                  <p className="text-xs text-slate-500">
                    {t(
                      "Remaining",
                    )}
                  </p>

                  <strong className="tabular-nums">
                    {money(
                      advanceSummary.remaining,
                    )}
                    {" "}
                    {
                      advanceSummary.currency
                    }
                  </strong>

                </div>

              </div>

              <Field label="Payment method">

                <select
                  className={
                    inputStyle
                  }
                  value={
                    method
                  }
                  onChange={
                    (
                      event,
                    ) => {
                      requestId.current =
                        "";

                      setMethod(
                        event.target.value,
                      );
                    }
                  }
                >

                  {[
                    "CASH",
                    "MOBILE_MONEY",
                    "CARD",
                    "BANK_TRANSFER",
                  ].map(
                    (
                      value,
                    ) => (

                      <option
                        key={
                          value
                        }
                        value={
                          value
                        }
                      >
                        {t(
                          value,
                        )}
                      </option>

                    ),
                  )}

                </select>

              </Field>

              <div className="grid gap-4 sm:grid-cols-2">

                <Field label="Currency">

                  <input
                    className={
                      inputStyle
                    }
                    maxLength={
                      3
                    }
                    value={
                      currency
                    }
                    onChange={
                      (
                        event,
                      ) => {
                        requestId.current =
                          "";

                        setCurrency(
                          event.target.value
                            .toUpperCase(),
                        );
                      }
                    }
                  />

                </Field>

                <Field label="Amount">

                  <input
                    className={
                      inputStyle
                    }
                    type="number"
                    min="0.0001"
                    step="0.0001"
                    value={
                      amount
                    }
                    onChange={
                      (
                        event,
                      ) => {
                        requestId.current =
                          "";

                        setAmount(
                          event.target.value,
                        );
                      }
                    }
                  />

                </Field>

              </div>

              <p className="text-sm text-slate-500">
                {t(
                  "The payment will be submitted for approval and will count as a receipt, not as revenue.",
                )}
              </p>

              <div className="flex gap-3 border-t pt-5">

                <button
                  type="button"
                  className="rounded-xl border px-5 py-2.5"
                  disabled={
                    busy
                  }
                  onClick={
                    () => {
                      requestId.current =
                        "";

                      setAdvanceFor(
                        null,
                      );

                      setAdvanceSummary(
                        null,
                      );
                    }
                  }
                >
                  {t(
                    "Cancel",
                  )}
                </button>

                <button
                  type="button"
                  className={
                    `${buttonStyle} flex-1`
                  }
                  disabled={
                    busy
                    ||
                    Number(
                      amount,
                    ) <= 0
                  }
                  onClick={
                    submitAdvance
                  }
                >
                  {t(
                    busy
                      ? "Saving"
                      : "Submit for approval",
                  )}
                </button>

              </div>

            </section>

          </div>

        )}

    </Panel>
  );
}
