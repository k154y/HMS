"use client";

import {
  FormEvent,
  useCallback,
  useEffect,
  useMemo,
  useState,
} from "react";

import { useSession } from "next-auth/react";

import {
  all,
  api,
} from "@/lib/hms-api";

import {
  Panel,
  Field,
  inputStyle,
  buttonStyle,
} from "@/components/operations/ui";

import { useLocale } from "@/components/LocaleProvider";

const labels: Record<string, string> = {
  code: "Code",
  legalName: "Legal name",
  displayName: "Hotel name",
  tin: "Tax number",
  phone: "Phone",
  email: "Email",
  address: "Address",
  currencyCode: "Currency",
  timezone: "Time zone",
  defaultLanguage: "Language",
};

type ExchangeRate = {
  id: string;
  hotelId: string;
  baseCurrencyCode: string;
  currencyCode: string;
  rateToBase: number;
  enabled: boolean;
  effectiveFrom: string;
  createdAt: string;
};

type SessionUser = {
  permissions?: string[];
};

function formatDateTime(
  value: string
) {
  if (!value) {
    return "";
  }

  const date = new Date(value);

  if (
    Number.isNaN(
      date.getTime()
    )
  ) {
    return value;
  }

  return date.toLocaleString();
}

export default function Settings() {

  const {
    t,
  } = useLocale();

  const {
    data: session,
  } = useSession();

  const permissions =
  (session?.user as { permissions?: string[] })?.permissions ?? [];

const canViewRates =
  permissions.includes("EXCHANGE_RATE_VIEW")
  || permissions.includes("EXCHANGE_RATE_MANAGE");

const canManageRates =
  permissions.includes("EXCHANGE_RATE_MANAGE");

  const [
    values,
    setValues,
  ] = useState<
    Record<string, string>
  >({});

  const [
    loaded,
    setLoaded,
  ] = useState(false);

  const [
    error,
    setError,
  ] = useState("");

  const [
    busy,
    setBusy,
  ] = useState(false);

  const [
    saved,
    setSaved,
  ] = useState(false);

  const [
    rates,
    setRates,
  ] = useState<
    ExchangeRate[]
  >([]);

  const [
    rateCurrency,
    setRateCurrency,
  ] = useState("USD");

  const [
    rateValue,
    setRateValue,
  ] = useState("");

  const [
    rateEffectiveFrom,
    setRateEffectiveFrom,
  ] = useState("");

  const [
    rateBusy,
    setRateBusy,
  ] = useState(false);

  const [
    rateError,
    setRateError,
  ] = useState("");

  const [
    rateMessage,
    setRateMessage,
  ] = useState("");

  const baseCurrency =
    (
      values.currencyCode
      ?? ""
    )
      .trim()
      .toUpperCase();

  const loadRates =
    useCallback(
      async () => {

        if (!canViewRates) {
          setRates([]);
          return;
        }

        const result =
          await all<ExchangeRate>(
            "exchange-rates"
          );

        setRates(result);

      },
      [canViewRates]
    );

  useEffect(() => {

    api<Record<string, string>>(
      "settings"
    )
      .then((result) => {

        setValues(
          Object.fromEntries(
            Object
              .keys(labels)
              .map(
                (key) => [
                  key,
                  result[key] ?? "",
                ]
              )
          )
        );

        setLoaded(true);
      })
      .catch(
        (loadError) => {

          setError(
            loadError.message
          );
        }
      );

  }, []);

  useEffect(() => {

    if (!loaded) {
      return;
    }

    loadRates()
      .catch(
        (loadError) => {

          setRateError(
            loadError instanceof Error
              ? loadError.message
              : "Unable to load exchange rates."
          );
        }
      );

  }, [
    loaded,
    loadRates,
  ]);

  const latestRates =
    useMemo(() => {

      const byCurrency =
        new Map<
          string,
          ExchangeRate
        >();

      for (
        const rate
        of rates
      ) {

        if (
          !byCurrency.has(
            rate.currencyCode
          )
        ) {
          byCurrency.set(
            rate.currencyCode,
            rate
          );
        }
      }

      return [
        ...byCurrency.values(),
      ];

    }, [rates]);

  async function save(
    event:
      FormEvent<HTMLFormElement>
  ) {

    event.preventDefault();

    setBusy(true);
    setError("");
    setSaved(false);

    try {

      await api(
        "settings",
        "PUT",
        values
      );

      setSaved(true);

    } catch (
      saveError
    ) {

      setError(
        saveError instanceof Error
          ? saveError.message
          : "Unable to save hotel settings."
      );

    } finally {

      setBusy(false);
    }
  }

  async function saveExchangeRate(
    event:
      FormEvent<HTMLFormElement>
  ) {

    event.preventDefault();

    setRateBusy(true);
    setRateError("");
    setRateMessage("");

    try {

      const currency =
        rateCurrency
          .trim()
          .toUpperCase();

      const numericRate =
        Number(rateValue);

      if (
        !/^[A-Z]{3}$/.test(
          currency
        )
      ) {

        throw new Error(
          "Currency code must contain exactly three letters."
        );
      }

      if (
        currency
        === baseCurrency
      ) {

        throw new Error(
          "An exchange rate is not required for the hotel's base currency."
        );
      }

      if (
        !Number.isFinite(
          numericRate
        )
        || numericRate <= 0
      ) {

        throw new Error(
          "Exchange rate must be greater than zero."
        );
      }

      await api(
        "exchange-rates",
        "POST",
        {
          currencyCode:
            currency,

          rateToBase:
            rateValue,

          effectiveFrom:
            rateEffectiveFrom
              ? new Date(
                  rateEffectiveFrom
                ).toISOString()
              : null,
        }
      );

      setRateMessage(
        "Exchange rate saved successfully."
      );

      setRateCurrency(
        "USD"
      );

      setRateValue("");

      setRateEffectiveFrom("");

      await loadRates();

    } catch (
      saveError
    ) {

      setRateError(
        saveError instanceof Error
          ? saveError.message
          : "Unable to save exchange rate."
      );

    } finally {

      setRateBusy(false);
    }
  }

  return (
    <Panel
      title="Settings"
      error={error}
    >

      {loaded && (
        <div className="space-y-8">

          <form
            className="rounded-xl border bg-white p-5 space-y-5"
            onSubmit={save}
          >

            <div>

              <h2 className="text-lg font-semibold">
                {t(
                  "Hotel settings"
                )}
              </h2>

              <p className="mt-1 text-sm text-slate-500">
                {t(
                  "Manage the hotel's identity and operating defaults."
                )}
              </p>

            </div>

            <div className="grid gap-4 md:grid-cols-2">

              {Object
                .entries(labels)
                .map(
                  ([
                    key,
                    label,
                  ]) => (

                    <Field
                      label={label}
                      key={key}
                    >

                      {
                        key
                        ===
                        "defaultLanguage"
                          ? (

                            <select
                              className={
                                inputStyle
                              }
                              value={
                                values[
                                  key
                                ]
                              }
                              onChange={
                                (
                                  event
                                ) =>
                                  setValues(
                                    {
                                      ...values,
                                      [
                                        key
                                      ]:
                                        event
                                          .target
                                          .value,
                                    }
                                  )
                              }
                            >

                              {[
                                "en",
                                "fr",
                                "rw",
                              ].map(
                                (
                                  language
                                ) => (

                                  <option
                                    key={
                                      language
                                    }
                                    value={
                                      language
                                    }
                                  >
                                    {t(
                                      language
                                    )}
                                  </option>
                                )
                              )}

                            </select>

                          )
                          : (

                            <input
                              className={
                                inputStyle
                              }
                              type={
                                key
                                ===
                                "email"
                                  ? "email"
                                  : "text"
                              }
                              required={
                                [
                                  "code",
                                  "legalName",
                                  "displayName",
                                  "currencyCode",
                                  "timezone",
                                ].includes(
                                  key
                                )
                              }
                              value={
                                values[
                                  key
                                ]
                              }
                              onChange={
                                (
                                  event
                                ) =>
                                  setValues(
                                    {
                                      ...values,
                                      [
                                        key
                                      ]:
                                        event
                                          .target
                                          .value,
                                    }
                                  )
                              }
                            />

                          )
                      }

                    </Field>

                  )
                )}

            </div>

            <button
              className={
                buttonStyle
              }
              disabled={busy}
            >
              {t(
                busy
                  ? "Saving"
                  : "Save settings"
              )}
            </button>

            {saved && (
              <p
                role="status"
                className="text-sm text-emerald-700"
              >
                {t("Saved")}
              </p>
            )}

          </form>

          {canViewRates && (

            <section className="rounded-xl border bg-white p-5 space-y-6">

              <header>

                <h2 className="text-lg font-semibold">
                  {t(
                    "Accepted currencies and exchange rates"
                  )}
                </h2>

                <p className="mt-1 text-sm text-slate-500">
                  {t(
                    "Exchange rates convert foreign customer payments into the hotel's base currency."
                  )}
                </p>

                {baseCurrency && (

                  <p className="mt-3 rounded-lg bg-slate-50 px-4 py-3 text-sm">

                    {t(
                      "Hotel base currency"
                    )}
                    :{" "}
                    <strong>
                      {baseCurrency}
                    </strong>

                  </p>

                )}

              </header>

              {canManageRates && (

                <form
                  className="rounded-xl border bg-slate-50 p-4 space-y-4"
                  onSubmit={
                    saveExchangeRate
                  }
                >

                  <h3 className="font-semibold">
                    {t(
                      "Add exchange rate"
                    )}
                  </h3>

                  <div className="grid gap-4 md:grid-cols-3">

                    <Field
                      label="Foreign currency"
                    >

                      <input
                        className={
                          inputStyle
                        }
                        value={
                          rateCurrency
                        }
                        maxLength={3}
                        placeholder="USD"
                        onChange={
                          (
                            event
                          ) =>
                            setRateCurrency(
                              event
                                .target
                                .value
                                .toUpperCase()
                            )
                        }
                      />

                    </Field>

                    <Field
                      label="Rate to base currency"
                    >

                      <input
                        className={
                          inputStyle
                        }
                        type="number"
                        min="0.00000001"
                        step="0.00000001"
                        value={
                          rateValue
                        }
                        placeholder="1450"
                        onChange={
                          (
                            event
                          ) =>
                            setRateValue(
                              event
                                .target
                                .value
                            )
                        }
                      />

                    </Field>

                    <Field
                      label="Effective from"
                    >

                      <input
                        className={
                          inputStyle
                        }
                        type="datetime-local"
                        value={
                          rateEffectiveFrom
                        }
                        onChange={
                          (
                            event
                          ) =>
                            setRateEffectiveFrom(
                              event
                                .target
                                .value
                            )
                        }
                      />

                    </Field>

                  </div>

                  {
                    rateCurrency
                    && rateValue
                    && baseCurrency
                    && (
                      <div className="rounded-lg border bg-white p-4">

                        <p className="text-xs uppercase tracking-wide text-slate-500">
                          {t(
                            "Payment rate preview"
                          )}
                        </p>

                        <p className="mt-2 text-xl font-semibold">
                          1{" "}
                          {
                            rateCurrency
                              .trim()
                              .toUpperCase()
                          }
                          {" = "}
                          {
                            Number(
                              rateValue
                            )
                              .toLocaleString(
                                undefined,
                                {
                                  maximumFractionDigits:
                                    8,
                                }
                              )
                          }
                          {" "}
                          {
                            baseCurrency
                          }
                        </p>

                      </div>
                    )
                  }

                  {rateError && (

                    <p
                      role="alert"
                      className="text-sm text-red-700"
                    >
                      {t(
                        rateError
                      )}
                    </p>

                  )}

                  {rateMessage && (

                    <p
                      role="status"
                      className="text-sm text-emerald-700"
                    >
                      {t(
                        rateMessage
                      )}
                    </p>

                  )}

                  <button
                    className={
                      buttonStyle
                    }
                    disabled={
                      rateBusy
                      || !rateCurrency
                      || !rateValue
                    }
                  >
                    {t(
                      rateBusy
                        ? "Saving"
                        : "Save exchange rate"
                    )}
                  </button>

                </form>

              )}

              <div className="space-y-3">

                <h3 className="font-semibold">
                  {t(
                    "Current exchange rates"
                  )}
                </h3>

                {
                  latestRates.length
                  === 0
                    ? (

                      <p className="text-sm text-slate-500">
                        {t(
                          "No foreign exchange rates configured."
                        )}
                      </p>

                    )
                    : (

                      <div className="overflow-x-auto">

                        <table className="w-full text-left text-sm">

                          <thead>

                            <tr className="border-b text-slate-500">

                              <th className="py-3 pr-4">
                                {t(
                                  "Currency"
                                )}
                              </th>

                              <th className="py-3 pr-4">
                                {t(
                                  "Exchange rate"
                                )}
                              </th>

                              <th className="py-3 pr-4">
                                {t(
                                  "Effective from"
                                )}
                              </th>

                              <th className="py-3">
                                {t(
                                  "Status"
                                )}
                              </th>

                            </tr>

                          </thead>

                          <tbody>

                            {latestRates.map(
                              (
                                rate
                              ) => (

                                <tr
                                  key={
                                    rate.id
                                  }
                                  className="border-b last:border-0"
                                >

                                  <td className="py-3 pr-4 font-medium">
                                    {
                                      rate.currencyCode
                                    }
                                  </td>

                                  <td className="py-3 pr-4 tabular-nums">
                                    1{" "}
                                    {
                                      rate.currencyCode
                                    }
                                    {" = "}
                                    {
                                      Number(
                                        rate.rateToBase
                                      )
                                        .toLocaleString(
                                          undefined,
                                          {
                                            maximumFractionDigits:
                                              8,
                                          }
                                        )
                                    }
                                    {" "}
                                    {
                                      rate.baseCurrencyCode
                                    }
                                  </td>

                                  <td className="py-3 pr-4">
                                    {
                                      formatDateTime(
                                        rate.effectiveFrom
                                      )
                                    }
                                  </td>

                                  <td className="py-3">
                                    {t(
                                      rate.enabled
                                        ? "Active"
                                        : "Inactive"
                                    )}
                                  </td>

                                </tr>

                              )
                            )}

                          </tbody>

                        </table>

                      </div>

                    )
                }

              </div>

              {rates.length > 0 && (

                <details className="rounded-xl border p-4">

                  <summary className="cursor-pointer font-medium">
                    {t(
                      "Exchange rate history"
                    )}
                  </summary>

                  <div className="mt-4 overflow-x-auto">

                    <table className="w-full text-left text-sm">

                      <thead>

                        <tr className="border-b text-slate-500">

                          <th className="py-3 pr-4">
                            {t(
                              "Currency"
                            )}
                          </th>

                          <th className="py-3 pr-4">
                            {t(
                              "Exchange rate"
                            )}
                          </th>

                          <th className="py-3 pr-4">
                            {t(
                              "Effective from"
                            )}
                          </th>

                          <th className="py-3">
                            {t(
                              "Created"
                            )}
                          </th>

                        </tr>

                      </thead>

                      <tbody>

                        {rates.map(
                          (
                            rate
                          ) => (

                            <tr
                              key={
                                rate.id
                              }
                              className="border-b last:border-0"
                            >

                              <td className="py-3 pr-4">
                                {
                                  rate.currencyCode
                                }
                              </td>

                              <td className="py-3 pr-4 tabular-nums">
                                1{" "}
                                {
                                  rate.currencyCode
                                }
                                {" = "}
                                {
                                  Number(
                                    rate.rateToBase
                                  )
                                    .toLocaleString(
                                      undefined,
                                      {
                                        maximumFractionDigits:
                                          8,
                                      }
                                    )
                                }
                                {" "}
                                {
                                  rate.baseCurrencyCode
                                }
                              </td>

                              <td className="py-3 pr-4">
                                {
                                  formatDateTime(
                                    rate.effectiveFrom
                                  )
                                }
                              </td>

                              <td className="py-3">
                                {
                                  formatDateTime(
                                    rate.createdAt
                                  )
                                }
                              </td>

                            </tr>

                          )
                        )}

                      </tbody>

                    </table>

                  </div>

                </details>

              )}

            </section>

          )}

        </div>
      )}

    </Panel>
  );
}