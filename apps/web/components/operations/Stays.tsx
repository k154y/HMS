"use client";

import {
  useCallback,
  useEffect,
  useState,
} from "react";

import Link from "next/link";

import {
  api,
  all,
} from "@/lib/hms-api";

import {
  Panel,
  buttonStyle,
  Empty,
} from "@/components/operations/ui";

import {
  useLocale,
} from "@/components/LocaleProvider";

type Booking = {
  id: string;
  customerId: string;
  folioId: string;
  reference: string;
  checkIn: string;
  checkOut: string;
  status: string;
};

function localToday() {

  const now =
    new Date();

  return [
    now.getFullYear(),
    String(
      now.getMonth() + 1,
    ).padStart(
      2,
      "0",
    ),
    String(
      now.getDate(),
    ).padStart(
      2,
      "0",
    ),
  ].join("-");
}

export function Stays({
  checkout = false,
}: {
  checkout?: boolean;
}) {

  const { t } =
    useLocale();

  const [
    rows,
    setRows,
  ] =
    useState<Booking[]>([]);

  const [
    names,
    setNames,
  ] =
    useState<
      Record<string, string>
    >({});

  const [
    error,
    setError,
  ] =
    useState("");

  const [
    busy,
    setBusy,
  ] =
    useState(false);

  const today =
    localToday();

  const load =
    useCallback(
      async () => {

        const reservations =
          await all<Booking>(
            "reservations",
          );

        setRows(
          reservations.filter(
            (
              booking,
            ) =>
              checkout
                ? booking.status
                  === "CHECKED_IN"
                : booking.status
                  === "CONFIRMED",
          ),
        );
      },
      [
        checkout,
      ],
    );

  useEffect(
    () => {

      Promise.all([
        load(),

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
              : "Unable to load stays.",
          ),
      );

    },
    [
      load,
    ],
  );

  async function act(
    id: string,
  ) {

    setBusy(true);
    setError("");

    try {

      await api(
        `reservations/${id}/${checkout ? "check-out" : "check-in"}`,
        "POST",
      );

      await load();

    } catch (
      actionError
    ) {

      setError(
        actionError instanceof Error
          ? actionError.message
          : "Unable to update stay.",
      );

    } finally {

      setBusy(false);
    }
  }

  return (

    <Panel
      title={
        checkout
          ? "Check Out"
          : "Check In"
      }
      error={
        error
      }
    >

      <div className="grid gap-4 md:grid-cols-2">

        {rows.map(
          (
            reservation,
          ) => {

            const early =
              !checkout
              &&
              reservation.checkIn
              > today;

            const expired =
              !checkout
              &&
              reservation.checkOut
              <= today;

            const overdue =
              checkout
              &&
              reservation.checkOut
              < today;

            const eligible =
              checkout
              ||
              (
                !early
                &&
                !expired
              );

            return (

              <article
                className="space-y-3 rounded-xl border bg-white p-5"
                key={
                  reservation.id
                }
              >

                <div className="flex flex-wrap items-start justify-between gap-3">

                  <h2 className="font-semibold">
                    {
                      names[
                        reservation.customerId
                      ]
                      ?? "—"
                    }
                    {" · "}
                    {
                      reservation.reference
                    }
                  </h2>

                  {overdue && (

                    <span className="rounded-full bg-amber-50 px-3 py-1 text-xs font-medium text-amber-800">
                      {t(
                        "Overdue checkout",
                      )}
                    </span>

                  )}

                  {early && (

                    <span className="rounded-full bg-blue-50 px-3 py-1 text-xs font-medium text-blue-800">
                      {t(
                        "Future arrival",
                      )}
                    </span>

                  )}

                </div>

                <p>
                  {
                    reservation.checkIn
                  }
                  {" → "}
                  {
                    reservation.checkOut
                  }
                </p>

                {!checkout
                  &&
                  early
                  && (

                    <p className="text-sm text-slate-500">
                      {t(
                        "Check-in becomes available on",
                      )}
                      {" "}
                      {
                        reservation.checkIn
                      }
                    </p>

                  )}

                <div className="flex flex-wrap items-center gap-4">

                  <button
                    type="button"
                    className={
                      buttonStyle
                    }
                    disabled={
                      busy
                      ||
                      !eligible
                    }
                    onClick={
                      () =>
                        act(
                          reservation.id,
                        )
                    }
                  >
                    {t(
                      checkout
                        ? "Confirm check-out"
                        : "Confirm check-in",
                    )}
                  </button>

                  <Link
                    className="text-blue-700 underline"
                    href={
                      `/folios/${reservation.folioId}`
                    }
                  >
                    {t(
                      "Open customer folio",
                    )}
                  </Link>

                </div>

              </article>
            );
          },
        )}

      </div>

      {!rows.length && (
        <Empty />
      )}

    </Panel>
  );
}
