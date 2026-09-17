"use client";

import {
  useCallback,
  useEffect,
  useMemo,
  useState,
} from "react";

import {
  useSession,
} from "next-auth/react";

import Link from "next/link";

import {
  CheckCircle2,
  Clock3,
  FileBarChart2,
  X,
} from "lucide-react";

import {
  Shifts,
} from "@/components/operations/Shifts";

import {
  api,
  all,
} from "@/lib/hms-api";

import {
  Panel,
  Empty,
  buttonStyle,
  Field,
  inputStyle,
} from "@/components/operations/ui";

import {
  useLocale,
} from "@/components/LocaleProvider";

type Approval = {
  id: string;
  folio_id: string;
  requested_by: string;
  customer_name: string;
  currency: string;
  status: string;
  payload: string;
  created_at: string;
};

type ApprovalPart = {
  method: string;
  currency?: string;
  amount: number;
  fxRate?: number | null;
  baseAmount?: number | null;
  exchangeRateId?: string | null;
  rateEffectiveFrom?: string | null;
};

type ApprovalPayload = {
  folioId?: string;
  requestId?: string;
  parts: ApprovalPart[];
  collectionScope?: string;
  folioCurrency?: string;
};

type Transaction = {
  id: string;
  customer: string;
  method: string;
  status: string;
  base_amount: number;
  created_at: string;
};

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

function parsePayload(
  row: Approval
): ApprovalPayload {

  try {

    const parsed =
      JSON.parse(
        row.payload
      ) as Partial<ApprovalPayload>;

    if (
      !Array.isArray(
        parsed.parts
      )
    ) {

      return {
        parts: [],
        folioCurrency:
          row.currency,
      };
    }

    return {
      ...parsed,

      folioCurrency:
        parsed.folioCurrency
        ?? row.currency,

      parts:
        parsed.parts.map(
          (part) => ({
            method:
              String(
                part.method
                ?? ""
              ),

            currency:
              part.currency
              ?? row.currency,

            amount:
              Number(
                part.amount
                ?? 0
              ),

            fxRate:
              part.fxRate == null
                ? null
                : Number(
                    part.fxRate
                  ),

            baseAmount:
              part.baseAmount == null
                ? Number(
                    part.amount
                    ?? 0
                  )
                : Number(
                    part.baseAmount
                  ),

            exchangeRateId:
              part.exchangeRateId
              ?? null,

            rateEffectiveFrom:
              part.rateEffectiveFrom
              ?? null,
          })
        ),
    };

  } catch {

    return {
      parts: [],
      folioCurrency:
        row.currency,
    };
  }
}

function partBaseAmount(
  part: ApprovalPart
) {
  return Number(
    part.baseAmount
    ?? part.amount
    ?? 0
  );
}

function paymentCurrency(
  part: ApprovalPart,
  folioCurrency: string
) {
  return (
    part.currency
    ?? folioCurrency
  )
    .trim()
    .toUpperCase();
}

type SessionProfile = {
  id?: string;
  role?: string;
  roles?: string[];
  permissions?: string[];
};

export default function Cashier() {
  const { t } = useLocale();
  const { data: session } = useSession();

  const profile = session?.user as SessionProfile | undefined;

  const owner =
    profile?.roles?.includes("OWNER")
    || profile?.role === "OWNER";

  const [
    rows,
    setRows,
  ] = useState<Approval[]>([]);

  const [
    error,
    setError,
  ] = useState("");

  const [
    busy,
    setBusy,
  ] = useState(false);

  const [
    from,
    setFrom,
  ] = useState("");

  const [
    to,
    setTo,
  ] = useState("");

  const [
    method,
    setMethod,
  ] = useState("CASH");

  const [
    transactions,
    setTransactions,
  ] = useState<Transaction[]>([]);

  const [
    decision,
    setDecision,
  ] = useState<{
    row: Approval;
    approve: boolean;
  } | null>(null);

  const [
    reason,
    setReason,
  ] = useState("");

  const [
    message,
    setMessage,
  ] = useState("");

  const load =
    useCallback(
      () =>
        all<Approval>(
          "payment-approvals"
        )
          .then(
            setRows
          ),
      []
    );

  const loadTransactions =
    useCallback(
      async () => {

        if (
          from
          && to
          && to >= from
        ) {

          setTransactions(
            await api<Transaction[]>(
              `reports/transactions?from=${from}&to=${to}&method=${method}`
            )
          );
        }

      },
      [
        from,
        to,
        method,
      ]
    );

  useEffect(() => {

    const day =
      new Date()
        .toISOString()
        .slice(
          0,
          10
        );

    setFrom(day);
    setTo(day);

    load()
      .catch(
        (
          loadError
        ) =>
          setError(
            loadError instanceof Error
              ? loadError.message
              : "Unable to load payment approvals."
          )
      );

  }, [load]);

  useEffect(() => {

    loadTransactions()
      .catch(
        (
          loadError
        ) =>
          setError(
            loadError instanceof Error
              ? loadError.message
              : "Unable to load transactions."
          )
      );

  }, [
    loadTransactions,
  ]);

  async function act(
    action:
      () => Promise<unknown>
  ) {

    setBusy(true);
    setError("");
    setMessage("");

    try {

      await action();

      await Promise.all([
        load(),
        loadTransactions(),
      ]);

      setDecision(null);
      setReason("");

      setMessage(
        "Saved"
      );

    } catch (
      actionError
    ) {

      setError(
        actionError instanceof Error
          ? actionError.message
          : "Unable to complete payment decision."
      );

    } finally {

      setBusy(false);
    }
  }

  const pending =
    useMemo(
      () =>
        rows.filter(
          (
            row
          ) =>
            row.status
            === "PENDING"
        ),
      [rows]
    );

  const pendingValue =
    useMemo(
      () =>
        pending.reduce(
          (
            total,
            row
          ) => {

            const payload =
              parsePayload(
                row
              );

            return (
              total
              +
              payload.parts.reduce(
                (
                  partTotal,
                  part
                ) =>
                  partTotal
                  +
                  partBaseAmount(
                    part
                  ),
                0
              )
            );
          },
          0
        ),
      [pending]
    );

  const pendingCurrency =
    pending[0]?.currency
    ?? "";

  const selectedPayload =
    decision
      ? parsePayload(
          decision.row
        )
      : null;

  const selectedFolioCurrency =
    selectedPayload?.folioCurrency
    ?? decision?.row.currency
    ?? "";

  const selectedTotal =
    selectedPayload
      ? selectedPayload.parts.reduce(
          (
            total,
            part
          ) =>
            total
            +
            partBaseAmount(
              part
            ),
          0
        )
      : 0;

  return (
    <Panel
      title="Cashier"
      error={error}
    >

      <div className="flex flex-wrap justify-between gap-4">

        <p className="text-slate-500">
          {t(
            "Review payments, manage your shift and reconcile collections."
          )}
        </p>

        <Link
          className="font-medium text-blue-700"
          href="/reports"
        >
          <FileBarChart2 className="mr-2 inline h-4 w-4" />

          {t(
            "Finance and reports"
          )}
        </Link>

      </div>

      <div className="grid gap-4 md:grid-cols-3">

        <div className="rounded-2xl border bg-white p-5">

          <p className="text-sm text-slate-500">
            {t(
              "Pending payment approvals"
            )}
          </p>

          <p className="mt-3 text-3xl font-semibold">
            {pending.length}
          </p>

        </div>

        <div className="rounded-2xl border bg-white p-5">

          <p className="text-sm text-slate-500">
            {t(
              "Awaiting approval amount"
            )}
          </p>

          <p className="mt-3 text-3xl font-semibold tabular-nums">
            {money(
              pendingValue
            )}
            {pendingCurrency
              ? ` ${pendingCurrency}`
              : ""}
          </p>

        </div>

        <div className="rounded-2xl border bg-white p-5">

          <p className="text-sm text-slate-500">
            {t(
              "Approved payments"
            )}
          </p>

          <p className="mt-3 text-3xl font-semibold">
            {
              rows.filter(
                (
                  row
                ) =>
                  row.status
                  === "APPROVED"
              ).length
            }
          </p>

        </div>

      </div>

      <Shifts />

      {message && (

        <p
          role="status"
          className="rounded-xl bg-emerald-50 p-4 text-emerald-800"
        >
          {t(
            message
          )}
        </p>

      )}

      <section className="space-y-4">

        <h2 className="flex items-center gap-2 text-lg font-semibold">

          <Clock3 className="h-5 w-5 text-amber-500" />

          {t(
            "Payment approvals"
          )}

        </h2>

        <div className="grid gap-4 xl:grid-cols-2">

          {pending.map(
            (
              row
            ) => {

              const payload =
                parsePayload(
                  row
                );

              const folioCurrency =
                payload.folioCurrency
                ?? row.currency;

              const self =
                row.requested_by
                === profile?.id;

              const totalBase =
                payload.parts.reduce(
                  (
                    total,
                    part
                  ) =>
                    total
                    +
                    partBaseAmount(
                      part
                    ),
                  0
                );

              return (

                <article
                  key={
                    row.id
                  }
                  className="rounded-2xl border bg-white p-6 shadow-sm"
                >

                  <div className="flex justify-between gap-3">

                    <div>

                      <h3 className="text-lg font-semibold">
                        {
                          row.customer_name
                        }
                      </h3>

                      <Link
                        className="text-sm font-medium text-blue-700"
                        href={
                          `/folios/${row.folio_id}`
                        }
                      >
                        {t(
                          "Open customer folio"
                        )}
                      </Link>

                    </div>

                    <span className="h-fit rounded-full bg-amber-50 px-3 py-1 text-xs text-amber-800">
                      {t(
                        "PENDING"
                      )}
                    </span>

                  </div>

                  <p className="my-4 text-xs uppercase tracking-wide text-slate-500">

                    {t(
                      payload.collectionScope
                      === "ROOM"
                        ? "Room charges"
                        : "Food and services"
                    )}

                  </p>

                  <div className="divide-y rounded-xl border">

                    {payload.parts.map(
                      (
                        part,
                        index
                      ) => {

                        const currency =
                          paymentCurrency(
                            part,
                            folioCurrency
                          );

                        const foreign =
                          currency
                          !== folioCurrency;

                        const baseAmount =
                          partBaseAmount(
                            part
                          );

                        return (

                          <div
                            key={
                              index
                            }
                            className="space-y-2 p-4"
                          >

                            <div className="flex flex-wrap items-center justify-between gap-3">

                              <span className="font-medium">
                                {t(
                                  part.method
                                )}
                              </span>

                              <strong className="tabular-nums">
                                {money(
                                  part.amount
                                )}
                                {" "}
                                {currency}
                              </strong>

                            </div>

                            {
                              foreign
                              &&
                              Number(
                                part.fxRate
                                ?? 0
                              ) > 0
                              && (

                                <div className="rounded-lg bg-slate-50 p-3 text-sm">

                                  <p>

                                    {t(
                                      "Exchange rate"
                                    )}
                                    :{" "}

                                    <strong>
                                      1{" "}
                                      {currency}
                                      {" = "}
                                      {rateNumber(
                                        Number(
                                          part.fxRate
                                        )
                                      )}
                                      {" "}
                                      {folioCurrency}
                                    </strong>

                                  </p>

                                  <p className="mt-1">

                                    {t(
                                      "Payment equivalent"
                                    )}
                                    :{" "}

                                    <strong className="tabular-nums">
                                      {money(
                                        baseAmount
                                      )}
                                      {" "}
                                      {folioCurrency}
                                    </strong>

                                  </p>

                                  {
                                    part.rateEffectiveFrom
                                    && (

                                      <p className="mt-1 text-xs text-slate-500">

                                        {t(
                                          "Effective from"
                                        )}
                                        :{" "}

                                        {
                                          new Date(
                                            part.rateEffectiveFrom
                                          )
                                            .toLocaleString()
                                        }

                                      </p>

                                    )
                                  }

                                </div>

                              )
                            }

                          </div>

                        );
                      }
                    )}

                  </div>

                  <div className="mt-4 flex justify-between rounded-xl bg-slate-50 p-4">

                    <span>
                      {t(
                        "Total to apply"
                      )}
                    </span>

                    <strong className="tabular-nums">
                      {money(
                        totalBase
                      )}
                      {" "}
                      {folioCurrency}
                    </strong>

                  </div>

                  {self && (

                    <p className="my-3 text-sm text-slate-500">

                      {t(
                        owner
                          ? "Owner self-approval requires a reason."
                          : "A different user must approve this payment."
                      )}

                    </p>

                  )}

                  <div className="mt-4 flex gap-3">

                    <button
                      className={
                        buttonStyle
                      }
                      disabled={
                        busy
                        ||
                        (
                          self
                          &&
                          !owner
                        )
                      }
                      onClick={
                        () => {

                          setReason("");

                          setDecision({
                            row,
                            approve:
                              true,
                          });
                        }
                      }
                    >

                      <CheckCircle2 className="mr-2 inline h-4 w-4" />

                      {t(
                        "Approve"
                      )}

                    </button>

                    <button
                      className="rounded-xl border px-5 py-2.5 text-sm font-medium text-red-700 disabled:opacity-40"
                      disabled={
                        busy
                        ||
                        (
                          self
                          &&
                          !owner
                        )
                      }
                      onClick={
                        () => {

                          setReason("");

                          setDecision({
                            row,
                            approve:
                              false,
                          });
                        }
                      }
                    >
                      {t(
                        "Reject"
                      )}
                    </button>

                  </div>

                </article>

              );
            }
          )}

        </div>

        {!pending.length && (

          <div className="rounded-2xl border bg-white p-6">
            <Empty />
          </div>

        )}

      </section>

      <section className="overflow-hidden rounded-2xl border bg-white">

        <header className="space-y-4 border-b p-6">

          <h2 className="text-lg font-semibold">
            {t(
              "Transactions by payment method"
            )}
          </h2>

          <div className="grid gap-3 sm:grid-cols-2">

            <Field label="From">

              <input
                className={
                  inputStyle
                }
                type="date"
                value={from}
                onChange={
                  (
                    event
                  ) =>
                    setFrom(
                      event
                        .target
                        .value
                    )
                }
              />

            </Field>

            <Field label="To">

              <input
                className={
                  inputStyle
                }
                type="date"
                value={to}
                onChange={
                  (
                    event
                  ) =>
                    setTo(
                      event
                        .target
                        .value
                    )
                }
              />

            </Field>

          </div>

          <div className="flex flex-wrap gap-2">

            {[
              "CASH",
              "MOBILE_MONEY",
              "CARD",
              "BANK_TRANSFER",
            ].map(
              (
                paymentMethod
              ) => (

                <button
                  type="button"
                  className={
                    `rounded-lg px-4 py-2 text-sm font-medium ${
                      method
                      === paymentMethod
                        ? "bg-blue-600 text-white"
                        : "bg-slate-100 text-slate-600"
                    }`
                  }
                  key={
                    paymentMethod
                  }
                  onClick={
                    () =>
                      setMethod(
                        paymentMethod
                      )
                  }
                >
                  {t(
                    paymentMethod
                  )}
                </button>

              )
            )}

          </div>

        </header>

        <div className="overflow-auto">

          <table className="w-full text-left text-sm">

            <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">

              <tr>

                {[
                  "Customer",
                  "Amount",
                  "Status",
                  "Actions",
                ].map(
                  (
                    heading
                  ) => (

                    <th
                      className="px-6 py-3"
                      key={
                        heading
                      }
                    >
                      {t(
                        heading
                      )}
                    </th>

                  )
                )}

              </tr>

            </thead>

            <tbody>

              {transactions.map(
                (
                  transaction
                ) => (

                  <tr
                    className="border-t hover:bg-slate-50"
                    key={
                      transaction.id
                    }
                  >

                    <td className="px-6 py-4 font-medium">
                      {
                        transaction.customer
                      }
                    </td>

                    <td className="px-6 py-4 tabular-nums">
                      {money(
                        transaction.base_amount
                      )}
                    </td>

                    <td className="px-6 py-4">
                      {t(
                        transaction.status
                      )}
                    </td>

                    <td className="px-6 py-4">

                      {
                        transaction.status
                        === "POSTED"
                        &&
                        profile?.permissions?.includes(
                          "PAYMENT_REFUND"
                        )
                        && (

                          <button
                            className="font-medium text-red-700"
                            disabled={
                              busy
                            }
                            onClick={
                              () =>
                                act(
                                  () =>
                                    api(
                                      `payments/${transaction.id}/refund`,
                                      "POST"
                                    )
                                )
                            }
                          >
                            {t(
                              "Refund"
                            )}
                          </button>

                        )
                      }

                    </td>

                  </tr>

                )
              )}

            </tbody>

          </table>

        </div>

        {!transactions.length && (

          <div className="p-6">
            <Empty />
          </div>

        )}

      </section>

      {decision && selectedPayload && (

        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 p-5 backdrop-blur-sm">

          <section
            role="dialog"
            aria-modal="true"
            aria-label={
              t(
                "Payment decision"
              )
            }
            className="max-h-[90vh] w-full max-w-lg space-y-5 overflow-y-auto rounded-2xl bg-white p-6 shadow-xl"
          >

            <header className="flex justify-between gap-4">

              <h2 className="text-xl font-semibold">

                {t(
                  decision.approve
                    ? "Approve payment"
                    : "Reject payment"
                )}

              </h2>

              <button
                type="button"
                aria-label={
                  t(
                    "Close"
                  )
                }
                onClick={
                  () =>
                    setDecision(
                      null
                    )
                }
                disabled={
                  busy
                }
              >
                <X className="h-5 w-5" />
              </button>

            </header>

            <div>

              <p className="font-medium">
                {
                  decision.row.customer_name
                }
              </p>

              <p className="mt-1 text-sm text-slate-500">

                {t(
                  selectedPayload.collectionScope
                  === "ROOM"
                    ? "Room charges"
                    : "Food and services"
                )}

              </p>

            </div>

            <div className="divide-y rounded-xl border">

              {selectedPayload.parts.map(
                (
                  part,
                  index
                ) => {

                  const currency =
                    paymentCurrency(
                      part,
                      selectedFolioCurrency
                    );

                  const foreign =
                    currency
                    !==
                    selectedFolioCurrency;

                  return (

                    <div
                      key={
                        index
                      }
                      className="space-y-2 p-4 text-sm"
                    >

                      <div className="flex justify-between gap-3">

                        <span>
                          {t(
                            part.method
                          )}
                        </span>

                        <strong className="tabular-nums">
                          {money(
                            part.amount
                          )}
                          {" "}
                          {currency}
                        </strong>

                      </div>

                      {
                        foreign
                        &&
                        Number(
                          part.fxRate
                          ?? 0
                        ) > 0
                        && (

                          <>
                            <p className="text-slate-600">

                              1{" "}
                              {currency}
                              {" = "}
                              {rateNumber(
                                Number(
                                  part.fxRate
                                )
                              )}
                              {" "}
                              {selectedFolioCurrency}

                            </p>

                            <p>

                              {t(
                                "Payment equivalent"
                              )}
                              :{" "}

                              <strong className="tabular-nums">
                                {money(
                                  partBaseAmount(
                                    part
                                  )
                                )}
                                {" "}
                                {selectedFolioCurrency}
                              </strong>

                            </p>
                          </>

                        )
                      }

                    </div>

                  );
                }
              )}

            </div>

            <div className="flex justify-between rounded-xl bg-slate-50 p-4">

              <span>
                {t(
                  "Total to apply"
                )}
              </span>

              <strong className="tabular-nums">
                {money(
                  selectedTotal
                )}
                {" "}
                {selectedFolioCurrency}
              </strong>

            </div>

            <Field label="Reason">

              <textarea
                className={
                  inputStyle
                }
                maxLength={
                  1000
                }
                value={
                  reason
                }
                onChange={
                  (
                    event
                  ) =>
                    setReason(
                      event
                        .target
                        .value
                    )
                }
              />

            </Field>

            <p className="text-sm text-slate-500">
              {t(
                "Payment decisions are recorded in the audit trail."
              )}
            </p>

            {error && (

              <p
                role="alert"
                className="text-sm text-red-700"
              >
                {t(
                  error
                )}
              </p>

            )}

            <button
              className={
                buttonStyle
              }
              disabled={
                busy
                ||
                (
                  decision.row.requested_by
                  === profile?.id
                  &&
                  !reason.trim()
                )
              }
              onClick={
                () =>
                  act(
                    () =>
                      api(
                        `payment-approvals/${decision.row.id}/${decision.approve ? "approve" : "reject"}`,
                        "POST",
                        {
                          reason:
                            reason.trim()
                            || null,
                        }
                      )
                  )
              }
            >
              {t(
                busy
                  ? "Saving"
                  : "Confirm"
              )}
            </button>

          </section>

        </div>

      )}

    </Panel>
  );
}