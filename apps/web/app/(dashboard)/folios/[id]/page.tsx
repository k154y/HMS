"use client";

import {
  use,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";

import Link from "next/link";

import {
  ArrowLeft,
  Banknote,
  CheckCircle2,
  CircleDollarSign,
  CreditCard,
  FileText,
  Plus,
  ReceiptText,
  Trash2,
  TriangleAlert,
  UserRound,
  WalletCards,
} from "lucide-react";

import {
  api,
  all,
} from "@/lib/hms-api";

import {
  Field,
  inputStyle,
} from "@/components/operations/ui";

import { useLocale } from "@/components/LocaleProvider";

type Folio = {
  id: string;
  customerId: string;
  currency: string;
  status: string;
  balance: number;
};

type Customer = {
  id: string;
  name: string;
  email?: string | null;
  phone?: string | null;
  kind?: string | null;
};

type Entry = {
  id: string;
  kind: string;
  amount: number;
  memo: string;
  sourceId?: string;
  createdAt?: string;
};

type PaymentPart = {
  method: string;
  currency: string;
  amount: number;
};

type PaymentQuote = {
  folioId: string;
  folioCurrency: string;
  paymentCurrency: string;
  originalAmount: number;
  fxRate: number;
  baseAmount: number;
  exchangeRateId: string | null;
  rateEffectiveFrom: string | null;
};

function normalizeCurrency(
  value: string
) {
  return value
    .trim()
    .toUpperCase();
}

function money(
  value: number
) {
  return Number(
    value ?? 0
  ).toLocaleString(
    undefined,
    {
      minimumFractionDigits: 2,
      maximumFractionDigits: 4,
    }
  );
}

function rateNumber(
  value: number
) {
  return Number(
    value ?? 0
  ).toLocaleString(
    undefined,
    {
      maximumFractionDigits: 8,
    }
  );
}

function quoteMatches(
  part: PaymentPart,
  quote: PaymentQuote | null
) {

  if (!quote) {
    return false;
  }

  return (
    quote.paymentCurrency
    === normalizeCurrency(
      part.currency
    )
    &&
    Math.abs(
      Number(
        quote.originalAmount
      )
      -
      Number(
        part.amount
      )
    )
    < 0.0000001
  );
}

function initials(
  value: string
) {

  return value
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map(
      (
        part
      ) =>
        part[0]
        ?.toUpperCase()
    )
    .join("")
    || "?";
}

function entryBadge(
  kind: string
) {

  switch (kind) {

    case "ACCOMMODATION":
      return "bg-blue-50 text-blue-700";

    case "ORDER":
      return "bg-teal-50 text-teal-700";

    case "PAYMENT":
      return "bg-emerald-50 text-emerald-700";

    case "REFUND":
      return "bg-amber-50 text-amber-700";

    case "REVERSAL":
      return "bg-red-50 text-red-700";

    default:
      return "bg-slate-100 text-slate-600";
  }
}

export default function FolioDetail(
  {
    params,
  }: {
    params: Promise<{
      id: string;
    }>;
  }
) {

  const {
    id,
  } = use(
    params
  );

  const {
    t,
  } = useLocale();

  const [
    folio,
    setFolio,
  ] =
    useState<Folio | null>(
      null
    );

  const [
    customer,
    setCustomer,
  ] =
    useState<Customer | null>(
      null
    );

  const [
    entries,
    setEntries,
  ] =
    useState<Entry[]>([]);

  const [
    parts,
    setParts,
  ] =
    useState<PaymentPart[]>([
      {
        method: "CASH",
        currency: "",
        amount: 0,
      },
    ]);

  const [
    quotes,
    setQuotes,
  ] =
    useState<
      Array<PaymentQuote | null>
    >([]);

  const [
    quoteErrors,
    setQuoteErrors,
  ] =
    useState<string[]>([]);

  const [
    quoteBusy,
    setQuoteBusy,
  ] =
    useState(false);

  const [
    collectionScope,
    setCollectionScope,
  ] =
    useState("");

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

  const requestKey =
    useRef("");

  const load =
    useCallback(
      async () => {

        const loadedFolio =
          await api<Folio>(
            `folios/${id}`
          );

        const loadedEntries =
          await all<Entry>(
            `folios/${id}/entries`
          );

        const loadedCustomer =
          await api<Customer>(
            `customers/${loadedFolio.customerId}`
          );

        setFolio(
          loadedFolio
        );

        setEntries(
          loadedEntries
        );

        setCustomer(
          loadedCustomer
        );

        setParts(
          (
            current
          ) => {

            if (
              current.length
              === 1
              &&
              !current[0]
                .currency
            ) {

              return [
                {
                  ...current[0],
                  currency:
                    loadedFolio.currency,
                },
              ];
            }

            return current;
          }
        );

        const hasRoomCharges =
          loadedEntries.some(
            (
              entry
            ) =>
              entry.kind
              === "ACCOMMODATION"
              &&
              Number(
                entry.amount
              ) > 0
          );

        const hasOtherCharges =
          loadedEntries.some(
            (
              entry
            ) =>
              ![
                "ACCOMMODATION",
                "PAYMENT",
                "REFUND",
              ].includes(
                entry.kind
              )
              &&
              Number(
                entry.amount
              ) > 0
          );

        setCollectionScope(
          (
            current
          ) => {

            if (
              current
            ) {
              return current;
            }

            if (
              hasRoomCharges
              &&
              !hasOtherCharges
            ) {
              return "ROOM";
            }

            if (
              hasOtherCharges
              &&
              !hasRoomCharges
            ) {
              return "FOOD";
            }

            return "";
          }
        );
      },
      [
        id,
      ]
    );

  useEffect(
    () => {

      load()
        .catch(
          (
            loadError
          ) =>
            setError(
              loadError instanceof Error
                ? loadError.message
                : "Unable to load folio."
            )
        );

    },
    [
      load,
    ]
  );

  useEffect(
    () => {

      if (!folio) {
        return;
      }

      let active =
        true;

      setQuoteBusy(
        true
      );

      const timer =
        window.setTimeout(
          async () => {

            const results =
              await Promise.all(
                parts.map(
                  async (
                    part
                  ) => {

                    const currency =
                      normalizeCurrency(
                        part.currency
                      );

                    if (
                      Number(
                        part.amount
                      ) <= 0
                    ) {

                      return {
                        quote: null,
                        error: "",
                      };
                    }

                    if (
                      !/^[A-Z]{3}$/.test(
                        currency
                      )
                    ) {

                      return {
                        quote: null,
                        error:
                          "Enter a three-letter currency code.",
                      };
                    }

                    if (
                      currency
                      === folio.currency
                    ) {

                      return {
                        quote: {
                          folioId:
                            folio.id,

                          folioCurrency:
                            folio.currency,

                          paymentCurrency:
                            currency,

                          originalAmount:
                            Number(
                              part.amount
                            ),

                          fxRate: 1,

                          baseAmount:
                            Number(
                              part.amount
                            ),

                          exchangeRateId:
                            null,

                          rateEffectiveFrom:
                            null,
                        } satisfies PaymentQuote,

                        error: "",
                      };
                    }

                    try {

                      const quote =
                        await api<PaymentQuote>(
                          "payments/quote",
                          "POST",
                          {
                            folioId:
                              folio.id,

                            currency,

                            amount:
                              Number(
                                part.amount
                              ),
                          }
                        );

                      return {
                        quote,
                        error: "",
                      };

                    } catch (
                      quoteError
                    ) {

                      return {
                        quote: null,

                        error:
                          quoteError instanceof Error
                            ? quoteError.message
                            : "Unable to calculate exchange rate.",
                      };
                    }
                  }
                )
              );

            if (!active) {
              return;
            }

            setQuotes(
              results.map(
                (
                  result
                ) =>
                  result.quote
              )
            );

            setQuoteErrors(
              results.map(
                (
                  result
                ) =>
                  result.error
              )
            );

            setQuoteBusy(
              false
            );
          },
          350
        );

      return () => {

        active = false;

        window.clearTimeout(
          timer
        );
      };

    },
    [
      parts,
      folio,
    ]
  );

  const allQuoted =
    parts.length > 0
    &&
    parts.every(
      (
        part,
        index
      ) =>
        Number(
          part.amount
        ) > 0
        &&
        quoteMatches(
          part,
          quotes[index]
          ?? null
        )
        &&
        !quoteErrors[index]
    );

  const totalBase =
    parts.reduce(
      (
        total,
        part,
        index
      ) => {

        const quote =
          quotes[index];

        if (
          !quoteMatches(
            part,
            quote
            ?? null
          )
        ) {
          return total;
        }

        return (
          total
          +
          Number(
            quote
              ?.baseAmount
            ?? 0
          )
        );
      },
      0
    );

  const remaining =
    Number(
      folio
        ?.balance
      ?? 0
    )
    -
    totalBase;

  const totals =
    useMemo(
      () => {

        let charges =
          0;

        let payments =
          0;

        for (
          const entry
          of entries
        ) {

          const amount =
            Number(
              entry.amount
              || 0
            );

          if (
            entry.kind
            === "PAYMENT"
          ) {

            payments +=
              Math.abs(
                amount
              );

            continue;
          }

          if (
            entry.kind
            === "REFUND"
          ) {

            payments -=
              Math.abs(
                amount
              );

            continue;
          }

          charges +=
            amount;
        }

        return {
          charges,
          payments,
        };
      },
      [
        entries,
      ]
    );

  function updatePart(
    index: number,
    changes:
      Partial<PaymentPart>
  ) {

    requestKey.current =
      "";

    setMessage(
      ""
    );

    setParts(
      (
        current
      ) =>
        current.map(
          (
            part,
            partIndex
          ) =>
            partIndex
            === index
              ? {
                  ...part,
                  ...changes,
                }
              : part
        )
    );
  }

  function addPart() {

    requestKey.current =
      "";

    setParts(
      (
        current
      ) => [
        ...current,
        {
          method: "CASH",
          currency:
            folio
              ?.currency
            ?? "",
          amount: 0,
        },
      ]
    );
  }

  function removePart(
    index: number
  ) {

    requestKey.current =
      "";

    setParts(
      (
        current
      ) =>
        current.filter(
          (
            _,
            partIndex
          ) =>
            partIndex
            !== index
        )
    );
  }

  async function submit() {

    if (!folio) {
      return;
    }

    if (
      !collectionScope
    ) {

      setError(
        "Choose what this payment covers."
      );

      return;
    }

    setBusy(
      true
    );

    setError(
      ""
    );

    setMessage(
      ""
    );

    try {

      requestKey.current
        ||= crypto.randomUUID();

      await api(
        "payment-approvals",
        "POST",
        {
          folioId:
            id,

          requestId:
            requestKey.current,

          parts:
            parts.map(
              (
                part
              ) => ({
                method:
                  part.method,

                currency:
                  normalizeCurrency(
                    part.currency
                  ),

                amount:
                  Number(
                    part.amount
                  ),
              })
            ),

          collectionScope,
        }
      );

      setMessage(
        "Payment pending approval"
      );

      setParts([
        {
          method: "CASH",
          currency:
            folio.currency,
          amount: 0,
        },
      ]);

      setQuotes(
        []
      );

      setQuoteErrors(
        []
      );

      setCollectionScope(
        ""
      );

      requestKey.current =
        "";

      await load();

    } catch (
      submitError
    ) {

      setError(
        submitError instanceof Error
          ? submitError.message
          : "Unable to submit payment."
      );

    } finally {

      setBusy(
        false
      );
    }
  }

  if (!folio) {

    return (
      <div className="rounded-2xl border bg-white p-8 text-slate-500">
        {t(
          "Loading"
        )}
        ...
      </div>
    );
  }

  const balance =
    Number(
      folio.balance
    );

  const partial =
    totals.payments > 0
    &&
    balance > 0;

  return (

    <div className="space-y-6">

      <header className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">

        <div>

          <h1 className="text-3xl font-bold tracking-tight text-slate-950">
            {t(
              "Guest Folios"
            )}
          </h1>

          <p className="mt-1 text-sm text-slate-500">
            {t(
              "View charges, payments and settle the customer balance."
            )}
          </p>

        </div>

        <Link
          href="/folios"
          className="inline-flex items-center gap-2 self-start rounded-xl border bg-white px-4 py-2.5 text-sm font-semibold text-blue-700 shadow-sm"
        >
          <ArrowLeft className="h-4 w-4" />

          {t(
            "Back to folios"
          )}
        </Link>

      </header>

      {error && (

        <div
          role="alert"
          className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700"
        >
          {t(
            error
          )}
        </div>

      )}

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_390px]">

        <main className="min-w-0 space-y-5">

          {/* Customer */}

          <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">

            <div className="flex flex-col gap-5 md:flex-row md:items-center md:justify-between">

              <div className="flex items-center gap-4">

                <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-blue-600 to-teal-500 text-xl font-bold text-white shadow-sm">
                  {initials(
                    customer
                      ?.name
                    ?? ""
                  )}
                </div>

                <div>

                  <div className="flex flex-wrap items-center gap-2">

                    <h2 className="text-xl font-bold text-slate-950">
                      {customer
                        ?.name
                      ?? t(
                        "Customer"
                      )}
                    </h2>

                    <span className={`rounded-full px-3 py-1 text-xs font-semibold ${
                      folio.status
                      === "OPEN"
                        ? "bg-emerald-50 text-emerald-700"
                        : folio.status
                        === "CREDIT"
                        ? "bg-amber-50 text-amber-700"
                        : "bg-slate-100 text-slate-600"
                    }`}>
                      {t(
                        folio.status
                      )}
                    </span>

                  </div>

                  <div className="mt-2 flex flex-wrap gap-x-5 gap-y-1 text-sm text-slate-500">

                    {customer
                      ?.email
                      && (
                        <span>
                          {
                            customer.email
                          }
                        </span>
                      )}

                    {customer
                      ?.phone
                      && (
                        <span>
                          {
                            customer.phone
                          }
                        </span>
                      )}

                    <span>
                      {folio.currency}
                    </span>

                  </div>

                </div>

              </div>

              <div className="md:text-right">

                <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                  {t(
                    "Outstanding balance"
                  )}
                </p>

                <p className={`mt-1 text-3xl font-bold tabular-nums ${
                  balance > 0
                    ? "text-red-600"
                    : "text-emerald-600"
                }`}>
                  {money(
                    balance
                  )}
                  {" "}
                  {folio.currency}
                </p>

              </div>

            </div>

            {balance > 0 && (

              <div className="mt-5 flex items-start gap-3 rounded-xl border border-red-100 bg-red-50 p-4 text-sm text-red-700">

                <TriangleAlert className="mt-0.5 h-5 w-5 shrink-0" />

                <p>
                  {t(
                    "The folio has an unpaid balance. Settle or move the approved balance to credit before closing it."
                  )}
                </p>

              </div>

            )}

          </section>

          {/* Summary */}

          <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">

            <SummaryCard
              icon={
                ReceiptText
              }
              label={
                t(
                  "Total charges"
                )
              }
              value={
                `${money(
                  totals.charges
                )} ${folio.currency}`
              }
            />

            <SummaryCard
              icon={
                Banknote
              }
              label={
                t(
                  "Paid amount"
                )
              }
              value={
                `${money(
                  totals.payments
                )} ${folio.currency}`
              }
            />

            <SummaryCard
              icon={
                WalletCards
              }
              label={
                t(
                  "Remaining amount"
                )
              }
              value={
                `${money(
                  balance
                )} ${folio.currency}`
              }
              alert={
                balance > 0
              }
            />

            <SummaryCard
              icon={
                CircleDollarSign
              }
              label={
                t(
                  "Folio status"
                )
              }
              value={
                t(
                  partial
                    ? "PARTIAL"
                    : folio.status
                )
              }
            />

          </section>

          {/* Ledger */}

          <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">

            <header className="flex flex-col gap-2 border-b border-slate-100 px-5 py-4 sm:flex-row sm:items-center sm:justify-between">

              <div>

                <h2 className="font-semibold text-slate-950">
                  {t(
                    "Folio ledger"
                  )}
                </h2>

                <p className="text-sm text-slate-500">
                  {t(
                    "All charges, payments, refunds and reversals."
                  )}
                </p>

              </div>

              <FileText className="h-5 w-5 text-slate-400" />

            </header>

            <div className="overflow-x-auto">

              <table className="w-full min-w-[720px] text-left text-sm">

                <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">

                  <tr>

                    <th className="px-5 py-3">
                      {t(
                        "Date & time"
                      )}
                    </th>

                    <th className="px-5 py-3">
                      {t(
                        "Description"
                      )}
                    </th>

                    <th className="px-5 py-3">
                      {t(
                        "Type"
                      )}
                    </th>

                    <th className="px-5 py-3 text-right">
                      {t(
                        "Amount"
                      )}
                    </th>

                  </tr>

                </thead>

                <tbody>

                  {entries.map(
                    (
                      entry
                    ) => (

                      <tr
                        key={
                          entry.id
                        }
                        className="border-t border-slate-100 hover:bg-slate-50/70"
                      >

                        <td className="whitespace-nowrap px-5 py-4 text-slate-500">

                          {entry.createdAt
                            ? new Date(
                                entry.createdAt
                              )
                                .toLocaleString()
                            : "—"}

                        </td>

                        <td className="px-5 py-4 font-medium text-slate-800">
                          {entry.memo}
                        </td>

                        <td className="px-5 py-4">

                          <span className={`rounded-full px-3 py-1 text-xs font-semibold ${entryBadge(
                            entry.kind
                          )}`}>
                            {t(
                              entry.kind
                            )}
                          </span>

                        </td>

                        <td className={`px-5 py-4 text-right font-semibold tabular-nums ${
                          Number(
                            entry.amount
                          ) < 0
                            ? "text-emerald-700"
                            : "text-slate-900"
                        }`}>
                          {money(
                            entry.amount
                          )}
                          {" "}
                          {folio.currency}
                        </td>

                      </tr>

                    )
                  )}

                  {!entries.length && (

                    <tr>

                      <td
                        colSpan={
                          4
                        }
                        className="px-5 py-12 text-center text-slate-500"
                      >
                        {t(
                          "No folio activity yet."
                        )}
                      </td>

                    </tr>

                  )}

                </tbody>

              </table>

            </div>

          </section>

        </main>

        {/* Payment */}

        <aside className="h-fit rounded-2xl border border-slate-200 bg-white shadow-sm xl:sticky xl:top-5">

          <header className="border-b border-slate-100 p-5">

            <div className="flex items-center gap-3">

              <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-teal-50 text-teal-700">
                <CreditCard className="h-5 w-5" />
              </div>

              <div>

                <h2 className="font-semibold text-slate-950">
                  {t(
                    "Record payment"
                  )}
                </h2>

                <p className="text-sm text-slate-500">
                  {t(
                    "Full or partial settlement"
                  )}
                </p>

              </div>

            </div>

          </header>

          {folio.status
          !== "CLOSED"
          &&
          balance > 0
            ? (

              <div className="space-y-5 p-5">

                <Field
                  label="Payment covers"
                >

                  <select
                    className={
                      inputStyle
                    }
                    value={
                      collectionScope
                    }
                    disabled={
                      busy
                    }
                    onChange={
                      (
                        event
                      ) => {

                        requestKey.current =
                          "";

                        setError(
                          ""
                        );

                        setCollectionScope(
                          event.target.value
                        );
                      }
                    }
                  >

                    <option value="">
                      {t(
                        "Choose what this payment covers."
                      )}
                    </option>

                    <option value="FOOD">
                      {t(
                        "Food and services"
                      )}
                    </option>

                    <option value="ROOM">
                      {t(
                        "Room charges"
                      )}
                    </option>

                  </select>

                </Field>

                <div className="space-y-4">

                  {parts.map(
                    (
                      part,
                      index
                    ) => {

                      const quote =
                        quotes[index];

                      const quoteCurrent =
                        quoteMatches(
                          part,
                          quote
                          ?? null
                        );

                      const foreign =
                        normalizeCurrency(
                          part.currency
                        )
                        !==
                        folio.currency;

                      return (

                        <section
                          key={
                            index
                          }
                          className="rounded-xl border border-slate-200 bg-slate-50/60 p-4"
                        >

                          <div className="space-y-3">

                            <Field label="Payment method">

                              <select
                                className={
                                  inputStyle
                                }
                                value={
                                  part.method
                                }
                                disabled={
                                  busy
                                }
                                onChange={
                                  (
                                    event
                                  ) =>
                                    updatePart(
                                      index,
                                      {
                                        method:
                                          event.target.value,
                                      }
                                    )
                                }
                              >

                                {[
                                  "CASH",
                                  "MOBILE_MONEY",
                                  "CARD",
                                  "BANK_TRANSFER",
                                ].map(
                                  (
                                    method
                                  ) => (

                                    <option
                                      key={
                                        method
                                      }
                                      value={
                                        method
                                      }
                                    >
                                      {t(
                                        method
                                      )}
                                    </option>

                                  )
                                )}

                              </select>

                            </Field>

                            <div className="grid grid-cols-[1fr_110px] gap-3">

                              <Field label="Amount">

                                <input
                                  type="number"
                                  min="0.0001"
                                  step="0.0001"
                                  className={
                                    inputStyle
                                  }
                                  value={
                                    part.amount
                                  }
                                  disabled={
                                    busy
                                  }
                                  onChange={
                                    (
                                      event
                                    ) =>
                                      updatePart(
                                        index,
                                        {
                                          amount:
                                            Number(
                                              event
                                                .target
                                                .value
                                            ),
                                        }
                                      )
                                  }
                                />

                              </Field>

                              <Field label="Currency">

                                <input
                                  className={
                                    inputStyle
                                  }
                                  value={
                                    part.currency
                                  }
                                  maxLength={
                                    3
                                  }
                                  disabled={
                                    busy
                                  }
                                  onChange={
                                    (
                                      event
                                    ) =>
                                      updatePart(
                                        index,
                                        {
                                          currency:
                                            event
                                              .target
                                              .value
                                              .toUpperCase(),
                                        }
                                      )
                                  }
                                />

                              </Field>

                            </div>

                            {quoteBusy
                              &&
                              Number(
                                part.amount
                              ) > 0
                              &&
                              !quoteCurrent
                              && (

                                <p className="text-xs text-slate-500">
                                  {t(
                                    "Calculating exchange rate..."
                                  )}
                                </p>

                              )}

                            {quoteErrors[index] && (

                              <p className="text-xs text-red-700">
                                {t(
                                  quoteErrors[index]
                                )}
                              </p>

                            )}

                            {quoteCurrent
                              &&
                              quote
                              && (

                                <div className="rounded-xl bg-white p-3 text-sm">

                                  {foreign && (

                                    <p className="text-slate-500">
                                      {t(
                                        "Exchange rate"
                                      )}
                                      :{" "}
                                      <strong className="text-slate-800">
                                        1
                                        {" "}
                                        {quote.paymentCurrency}
                                        {" = "}
                                        {rateNumber(
                                          quote.fxRate
                                        )}
                                        {" "}
                                        {quote.folioCurrency}
                                      </strong>
                                    </p>

                                  )}

                                  <p className="mt-1 text-slate-500">
                                    {t(
                                      "Payment equivalent"
                                    )}
                                    :{" "}

                                    <strong className="text-slate-900">
                                      {money(
                                        quote.baseAmount
                                      )}
                                      {" "}
                                      {quote.folioCurrency}
                                    </strong>
                                  </p>

                                </div>

                              )}

                            {parts.length
                            > 1
                              && (

                                <button
                                  type="button"
                                  onClick={
                                    () =>
                                      removePart(
                                        index
                                      )
                                  }
                                  className="inline-flex items-center gap-2 text-sm font-semibold text-red-600"
                                >
                                  <Trash2 className="h-4 w-4" />

                                  {t(
                                    "Remove"
                                  )}
                                </button>

                              )}

                          </div>

                        </section>

                      );
                    }
                  )}

                </div>

                <button
                  type="button"
                  disabled={
                    busy
                    ||
                    parts.length
                    >= 8
                  }
                  onClick={
                    addPart
                  }
                  className="flex w-full items-center justify-center gap-2 rounded-xl border border-blue-200 bg-blue-50 px-4 py-2.5 text-sm font-semibold text-blue-700 transition hover:bg-blue-100 disabled:opacity-40"
                >
                  <Plus className="h-4 w-4" />

                  {t(
                    "Add payment method"
                  )}
                </button>

                <div className="rounded-xl bg-slate-50 p-4">

                  <div className="flex justify-between text-sm text-slate-500">

                    <span>
                      {t(
                        "Payment equivalent"
                      )}
                    </span>

                    <span className="font-semibold text-slate-900 tabular-nums">
                      {money(
                        totalBase
                      )}
                      {" "}
                      {folio.currency}
                    </span>

                  </div>

                  <div className="mt-3 flex justify-between border-t border-slate-200 pt-3">

                    <span className="font-semibold text-slate-700">
                      {t(
                        "Remaining after approval"
                      )}
                    </span>

                    <span className={`font-bold tabular-nums ${
                      remaining < 0
                        ? "text-red-600"
                        : "text-slate-950"
                    }`}>
                      {money(
                        remaining
                      )}
                      {" "}
                      {folio.currency}
                    </span>

                  </div>

                </div>

                <button
                  type="button"
                  disabled={
                    busy
                    ||
                    quoteBusy
                    ||
                    !allQuoted
                    ||
                    totalBase <= 0
                    ||
                    totalBase
                    >
                    balance
                  }
                  onClick={
                    submit
                  }
                  className="flex w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-blue-600 to-teal-600 px-5 py-3.5 font-semibold text-white shadow-sm transition hover:shadow-md disabled:cursor-not-allowed disabled:opacity-40"
                >
                  <CheckCircle2 className="h-4 w-4" />

                  {t(
                    busy
                      ? "Saving"
                      : "Confirm payment for approval"
                  )}
                </button>

                {message && (

                  <div
                    role="status"
                    className="rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-800"
                  >
                    {t(
                      message
                    )}
                  </div>

                )}

                <p className="text-xs leading-relaxed text-slate-400">
                  {t(
                    "Foreign-currency payments use the hotel's configured exchange rate."
                  )}
                </p>

              </div>

            )
            : (

              <div className="p-8 text-center">

                <CheckCircle2 className="mx-auto h-12 w-12 text-emerald-500" />

                <h3 className="mt-4 font-semibold text-slate-900">
                  {t(
                    "Folio settled"
                  )}
                </h3>

                <p className="mt-1 text-sm text-slate-500">
                  {t(
                    "There is no outstanding balance to collect."
                  )}
                </p>

              </div>

            )}

        </aside>

      </div>

    </div>
  );
}

function SummaryCard(
  {
    icon: Icon,
    label,
    value,
    alert = false,
  }: {
    icon:
      typeof ReceiptText;

    label:
      string;

    value:
      string;

    alert?: boolean;
  }
) {

  return (

    <article className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">

      <div className="flex items-center gap-3">

        <div className={`flex h-10 w-10 items-center justify-center rounded-xl ${
          alert
            ? "bg-red-50 text-red-600"
            : "bg-blue-50 text-blue-600"
        }`}>
          <Icon className="h-5 w-5" />
        </div>

        <p className="text-sm text-slate-500">
          {label}
        </p>

      </div>

      <p className={`mt-4 text-xl font-bold tabular-nums ${
        alert
          ? "text-red-600"
          : "text-slate-950"
      }`}>
        {value}
      </p>

    </article>
  );
}