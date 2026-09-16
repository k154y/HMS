"use client";

import {
  use,
  useCallback,
  useEffect,
  useRef,
  useState,
} from "react";

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

import { useLocale } from "@/components/LocaleProvider";

type Folio = {
  id: string;
  customerId: string;
  currency: string;
  status: string;
  balance: number;
};

type Entry = {
  id: string;
  kind: string;
  amount: number;
  memo: string;
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

  const currency =
    normalizeCurrency(
      part.currency
    );

  return (
    quote.paymentCurrency
      === currency
    &&
    Math.abs(
      Number(
        quote.originalAmount
      )
      - Number(
        part.amount
      )
    ) < 0.0000001
  );
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
  } = use(params);

  const {
    t,
  } = useLocale();

  const [
    folio,
    setFolio,
  ] = useState<Folio | null>(
    null
  );

  const [
    entries,
    setEntries,
  ] = useState<Entry[]>([]);

  const [
    customer,
    setCustomer,
  ] = useState("");

  const [
    parts,
    setParts,
  ] = useState<PaymentPart[]>([
    {
      method: "CASH",
      currency: "",
      amount: 0,
    },
  ]);

  const [
    quotes,
    setQuotes,
  ] = useState<
    Array<PaymentQuote | null>
  >([]);

  const [
    quoteErrors,
    setQuoteErrors,
  ] = useState<string[]>([]);

  const [
    quoteBusy,
    setQuoteBusy,
  ] = useState(false);

  const [
    error,
    setError,
  ] = useState("");

  const [
    message,
    setMessage,
  ] = useState("");

  const [
    busy,
    setBusy,
  ] = useState(false);

  const [
    collectionScope,
    setCollectionScope,
  ] = useState("FOOD");

  const requestKey =
    useRef("");

  const load =
    useCallback(
      async () => {

        const loadedFolio =
          await api<Folio>(
            `folios/${id}`
          );

        setFolio(
          loadedFolio
        );

        setEntries(
          await all<Entry>(
            `folios/${id}/entries`
          )
        );

        const loadedCustomer =
          await api<{
            name: string;
          }>(
            `customers/${loadedFolio.customerId}`
          );

        setCustomer(
          loadedCustomer.name
        );

        setParts(
          (current) => {

            if (
              current.length === 1
              && !current[0].currency
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
      },
      [id]
    );

  useEffect(() => {

    load()
      .catch(
        (loadError) =>
          setError(
            loadError instanceof Error
              ? loadError.message
              : "Unable to load folio."
          )
      );

  }, [load]);

  /*
   * Recalculate payment quotes whenever the cashier changes
   * an amount or currency.
   *
   * Foreign-currency conversion is always performed by the backend.
   */
  useEffect(() => {

    if (!folio) {
      return;
    }

    let active = true;

    setQuoteBusy(true);

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

                  /*
                   * Base-currency payments do not need an FX lookup.
                   */
                  if (
                    currency
                    ===
                    folio.currency
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
                        quoteError
                          instanceof Error
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
              (result) =>
                result.quote
            )
          );

          setQuoteErrors(
            results.map(
              (result) =>
                result.error
            )
          );

          setQuoteBusy(false);

        },
        350
      );

    return () => {

      active = false;

      window.clearTimeout(
        timer
      );
    };

  }, [
    parts,
    folio,
  ]);

  const validQuotes =
    parts.map(
      (
        part,
        index
      ) =>
        quoteMatches(
          part,
          quotes[index]
            ?? null
        )
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
        validQuotes[index]
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
            quote ?? null
          )
        ) {

          return total;
        }

        return (
          total
          +
          Number(
            quote?.baseAmount
            ?? 0
          )
        );
      },
      0
    );

  const remaining =
    Number(
      folio?.balance
      ?? 0
    )
    - totalBase;

  function updatePart(
    index: number,
    changes: Partial<PaymentPart>
  ) {

    requestKey.current = "";
    setMessage("");

    setParts(
      (current) =>
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

  function removePart(
    index: number
  ) {

    requestKey.current = "";
    setMessage("");

    setParts(
      (current) =>
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

  function addPart() {

    requestKey.current = "";
    setMessage("");

    setParts(
      (current) => [
        ...current,
        {
          method: "CASH",
          currency:
            folio?.currency
            ?? "",
          amount: 0,
        },
      ]
    );
  }

  async function submit() {

    if (!folio) {
      return;
    }

    setBusy(true);
    setError("");
    setMessage("");

    try {

      requestKey.current
        ||= crypto.randomUUID();

      await api(
        "payment-approvals",
        "POST",
        {
          folioId: id,

          requestId:
            requestKey.current,

          parts:
            parts.map(
              (part) => ({
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

      setQuotes([]);
      setQuoteErrors([]);

      requestKey.current = "";

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

      setBusy(false);
    }
  }

  return (
    <Panel
      title="Guest Folios"
      error={error}
    >

      {folio && (
        <>

          <div className="rounded-xl border bg-white p-5 space-y-2">

            <h2 className="text-xl font-semibold">
              {customer}
            </h2>

            <p>
              {t(
                folio.status
              )}
            </p>

            <p className="text-2xl">
              {t("Balance")}
              :{" "}
              {money(
                folio.balance
              )}
              {" "}
              {folio.currency}
            </p>

            <table className="w-full text-left text-sm">

              <thead>

                <tr>

                  {[
                    "Type",
                    "Description",
                    "Amount",
                  ].map(
                    (heading) => (

                      <th
                        className="py-3"
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

                {entries.map(
                  (entry) => (

                    <tr
                      className="border-t"
                      key={
                        entry.id
                      }
                    >

                      <td className="py-3">
                        {t(
                          entry.kind
                        )}
                      </td>

                      <td>
                        {entry.memo}
                      </td>

                      <td className="tabular-nums">
                        {money(
                          entry.amount
                        )}
                        {" "}
                        {folio.currency}
                      </td>

                    </tr>

                  )
                )}

              </tbody>

            </table>

          </div>

          {
            folio.status
            !== "CLOSED"
            &&
            Number(
              folio.balance
            ) > 0
            && (

              <div className="rounded-xl border bg-white p-5 space-y-5">

                <div>

                  <h2 className="font-semibold">
                    {t(
                      "Split payment"
                    )}
                  </h2>

                  <p className="mt-1 text-sm text-slate-500">
                    {t(
                      "Foreign-currency payments use the hotel's configured exchange rate."
                    )}
                  </p>

                </div>

                <Field label="Payment covers">

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

                        requestKey.current
                          = "";

                        setCollectionScope(
                          event
                            .target
                            .value
                        );
                      }
                    }
                  >

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

                {
                  parts.map(
                    (
                      part,
                      index
                    ) => {

                      const quote =
                        quotes[
                          index
                        ];

                      const quoteIsCurrent =
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
                          className="rounded-xl border p-4 space-y-4"
                        >

                          <div className="grid gap-3 md:grid-cols-4">

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
                                          event
                                            .target
                                            .value,
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

                            <Field label="Payment currency">

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
                                placeholder={
                                  folio.currency
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

                            <div className="flex items-end">

                              <button
                                type="button"
                                className="rounded-xl border px-4 py-3 text-sm font-medium disabled:opacity-40"
                                disabled={
                                  busy
                                  ||
                                  parts.length
                                  === 1
                                }
                                onClick={
                                  () =>
                                    removePart(
                                      index
                                    )
                                }
                              >
                                {t(
                                  "Remove"
                                )}
                              </button>

                            </div>

                          </div>

                          {
                            Number(
                              part.amount
                            ) > 0
                            && quoteBusy
                            && !quoteIsCurrent
                            && (

                              <p className="text-sm text-slate-500">
                                {t(
                                  "Calculating exchange rate..."
                                )}
                              </p>

                            )
                          }

                          {
                            quoteErrors[
                              index
                            ]
                            && (

                              <p
                                role="alert"
                                className="text-sm text-red-700"
                              >
                                {t(
                                  quoteErrors[
                                    index
                                  ]
                                )}
                              </p>

                            )
                          }

                          {
                            quoteIsCurrent
                            && quote
                            && (

                              <div className="rounded-lg bg-slate-50 p-4 space-y-2">

                                {foreign && (

                                  <p className="text-sm">

                                    {t(
                                      "Exchange rate"
                                    )}
                                    :{" "}

                                    <strong>
                                      1{" "}
                                      {
                                        quote.paymentCurrency
                                      }
                                      {" = "}
                                      {
                                        rateNumber(
                                          quote.fxRate
                                        )
                                      }
                                      {" "}
                                      {
                                        quote.folioCurrency
                                      }
                                    </strong>

                                  </p>

                                )}

                                <p>

                                  {t(
                                    "Payment equivalent"
                                  )}
                                  :{" "}

                                  <strong className="tabular-nums">
                                    {money(
                                      quote.baseAmount
                                    )}
                                    {" "}
                                    {
                                      quote.folioCurrency
                                    }
                                  </strong>

                                </p>

                                {
                                  foreign
                                  &&
                                  quote.rateEffectiveFrom
                                  && (

                                    <p className="text-xs text-slate-500">

                                      {t(
                                        "Effective from"
                                      )}
                                      :{" "}
                                      {
                                        new Date(
                                          quote.rateEffectiveFrom
                                        )
                                          .toLocaleString()
                                      }

                                    </p>

                                  )
                                }

                              </div>

                            )
                          }

                        </section>

                      );
                    }
                  )
                }

                <button
                  type="button"
                  className={
                    buttonStyle
                  }
                  disabled={
                    busy
                    ||
                    parts.length
                    >= 8
                  }
                  onClick={
                    addPart
                  }
                >
                  {t(
                    "Add payment method"
                  )}
                </button>

                <div className="rounded-xl bg-slate-50 p-4 space-y-2">

                  <div className="flex justify-between gap-4">

                    <span>
                      {t(
                        "Payment total in base currency"
                      )}
                    </span>

                    <strong className="tabular-nums">
                      {money(
                        totalBase
                      )}
                      {" "}
                      {folio.currency}
                    </strong>

                  </div>

                  <div className="flex justify-between gap-4">

                    <span>
                      {t(
                        "Remaining after approval"
                      )}
                    </span>

                    <strong
                      className={
                        remaining < 0
                          ? "text-red-700 tabular-nums"
                          : "tabular-nums"
                      }
                    >
                      {money(
                        remaining
                      )}
                      {" "}
                      {folio.currency}
                    </strong>

                  </div>

                </div>

                <button
                  type="button"
                  className={
                    buttonStyle
                  }
                  disabled={
                    busy
                    ||
                    quoteBusy
                    ||
                    !allQuoted
                    ||
                    totalBase
                    <= 0
                    ||
                    totalBase
                    >
                    Number(
                      folio.balance
                    )
                  }
                  onClick={
                    submit
                  }
                >
                  {t(
                    busy
                      ? "Saving"
                      : "Confirm payment for approval"
                  )}
                </button>

                {message && (

                  <p
                    role="status"
                    className="text-sm text-emerald-700"
                  >
                    {t(
                      message
                    )}
                  </p>

                )}

              </div>

            )
          }

        </>
      )}

    </Panel>
  );
}