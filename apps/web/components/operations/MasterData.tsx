"use client";

import {
  useCallback,
  useEffect,
  useState,
} from "react";
import { useSession } from "next-auth/react";
import {
  api,
  all,
  isHmsApiError,
} from "@/lib/hms-api";
import {
  Panel,
  Field,
  inputStyle,
  buttonStyle,
  Empty,
} from "./ui";
import { useLocale } from "@/components/LocaleProvider";

export type Row = Record<string, unknown> & {
  id: string;
};

export type Input = {
  key: string;
  label: string;
  type?:
    | "number"
    | "checkbox"
    | "email"
    | "date"
    | "password";
  optional?: boolean;
  default?: unknown;
  options?: string[];
  lookup?: string;
  min?: number;
};

type MasterDataProps = {
  title: string;
  resource: string;
  fields: Input[];
  columns: string[];
  defaults?: Record<string, unknown>;

  /*
   * Existing detail behavior used by other pages.
   */
  extra?: (
    row: Row,
    refresh: () => Promise<void>,
  ) => React.ReactNode;

  /*
   * Optional true row actions.
   *
   * Stock uses this so Edit and Stock movements live
   * inside the Actions column instead of underneath
   * the table.
   */
  actions?: (
    row: Row,
    refresh: () => Promise<void>,
  ) => React.ReactNode;
};

function initialValues(fields: Input[]) {
  return Object.fromEntries(
    fields.map((field) => [
      field.key,
      field.default ??
        (field.type === "checkbox"
          ? false
          : field.type === "number"
            ? 0
            : ""),
    ]),
  );
}

export function MasterData({
  title,
  resource,
  fields,
  columns,
  defaults = {},
  extra,
  actions,
}: MasterDataProps) {
  const { t } = useLocale();
  const { data: session } = useSession();

  const permissions =
    (
      session?.user as {
        permissions?: string[];
      }
    )?.permissions ?? [];

  const permission =
    (
      {
        customers: "CUSTOMER_MANAGE",
        vendors: "VENDOR_MANAGE",
        products: "PRODUCT_MANAGE",
        rooms: "ROOM_MANAGE",
        "room-types": "ROOM_MANAGE",
        memberships: "USER_MANAGE",
        maintenance: "MAINTENANCE_MANAGE",
      } as Record<string, string>
    )[resource];

  const canCreate =
    Boolean(permission) &&
    permissions.includes(permission);

  const [showCreate, setShowCreate] =
    useState(false);

  const [rows, setRows] =
    useState<Row[]>([]);

  const [values, setValues] =
    useState<Record<string, unknown>>(
      () => initialValues(fields),
    );

  const [choices, setChoices] =
    useState<Record<string, Row[]>>({});

  const [error, setError] =
    useState("");

  const [fieldErrors, setFieldErrors] =
    useState<Record<string, string>>({});

  const [busy, setBusy] =
    useState(false);

  const [selected, setSelected] =
    useState<Row | null>(null);

  const [search, setSearch] =
    useState("");

  const load = useCallback(
    async () => {
      setRows(
        await all<Row>(resource),
      );
    },
    [resource],
  );

  useEffect(() => {
    load().catch((requestError) =>
      setError(
        requestError instanceof Error
          ? requestError.message
          : "Unable to load records.",
      ),
    );

    for (const field of fields) {
      if (!field.lookup) {
        continue;
      }

      all<Row>(field.lookup)
        .then((records) =>
          setChoices((current) => ({
            ...current,
            [field.key]: records,
          })),
        )
        .catch((requestError) =>
          setError(
            requestError instanceof Error
              ? requestError.message
              : "Unable to load choices.",
          ),
        );
    }
  }, [fields, load]);

  function clearFieldError(key: string) {
    setFieldErrors((current) => {
      if (!current[key]) {
        return current;
      }

      const next = {
        ...current,
      };

      delete next[key];

      return next;
    });
  }

  function changeValue(
    field: Input,
    value: unknown,
  ) {
    clearFieldError(field.key);

    setValues((current) => ({
      ...current,
      [field.key]: value,
    }));
  }

  function focusField(key: string) {
    window.requestAnimationFrame(() => {
      const element =
        document.querySelector<
          HTMLInputElement | HTMLSelectElement
        >(`[name="master-${key}"]`);

      element?.focus();
    });
  }

  function validateCreate() {
    const problems: Record<string, string> =
      {};

    for (const field of fields) {
      const value = values[field.key];

      if (
        !field.optional &&
        field.type !== "checkbox"
      ) {
        if (
          value === null ||
          value === undefined ||
          String(value).trim() === ""
        ) {
          problems[field.key] =
            `${t(field.label)} ${t("is required.")}`;

          continue;
        }
      }

      if (field.type === "number") {
        const numberValue =
          Number(value);

        if (!Number.isFinite(numberValue)) {
          problems[field.key] =
            `${t(field.label)} ${t("must be a valid number.")}`;

          continue;
        }

        if (
          field.min !== undefined &&
          numberValue < field.min
        ) {
          problems[field.key] =
            `${t(field.label)} ${t("must be at least")} ${field.min}.`;
        }
      }

      if (
        field.type === "email" &&
        value &&
        !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(
          String(value),
        )
      ) {
        problems[field.key] =
          `${t(field.label)} ${t("must be a valid email address.")}`;
      }
    }

    return problems;
  }

  async function save(
    event: React.FormEvent,
  ) {
    event.preventDefault();

    const localErrors =
      validateCreate();

    if (
      Object.keys(localErrors).length > 0
    ) {
      setFieldErrors(localErrors);
      setError(
        "Please correct the highlighted fields.",
      );

      focusField(
        Object.keys(localErrors)[0],
      );

      return;
    }

    setBusy(true);
    setError("");
    setFieldErrors({});

    try {
      const body: Record<string, unknown> = {
        ...defaults,
        ...values,
      };

      for (const field of fields) {
        if (
          field.optional &&
          body[field.key] === ""
        ) {
          body[field.key] = null;
        }
      }

      await api(
        resource,
        "POST",
        body,
      );

      setValues(
        initialValues(fields),
      );

      setShowCreate(false);

      await load();
    } catch (requestError) {
      if (
        isHmsApiError(requestError) &&
        requestError.fieldErrors.length > 0
      ) {
        const problems =
          Object.fromEntries(
            requestError.fieldErrors.map(
              (problem) => [
                problem.field,
                problem.message,
              ],
            ),
          );

        setFieldErrors(problems);

        setError(
          requestError.message,
        );

        focusField(
          requestError
            .fieldErrors[0]
            .field,
        );
      } else {
        setError(
          requestError instanceof Error
            ? requestError.message
            : "Unable to save the record.",
        );
      }
    } finally {
      setBusy(false);
    }
  }

  const visibleRows =
    rows.filter((row) =>
      columns.some((key) =>
        String(row[key] ?? "")
          .toLowerCase()
          .includes(
            search.toLowerCase(),
          ),
      ),
    );

  const hasActions =
    Boolean(actions || extra);

  return (
    <Panel
      title={title}
      error={error}
    >
      {canCreate && (
        <div className="flex justify-end">
          <button
            type="button"
            className={buttonStyle}
            onClick={() => {
              setShowCreate(
                !showCreate,
              );

              setError("");
              setFieldErrors({});
            }}
          >
            {t(
              showCreate
                ? "Cancel"
                : "Create record",
            )}
          </button>
        </div>
      )}

      {canCreate && showCreate && (
        <form
          noValidate
          onSubmit={save}
          className="space-y-4 rounded-xl border bg-white p-5"
        >
          <h2 className="font-semibold">
            {t("Create record")}
          </h2>

          <div className="grid gap-4 md:grid-cols-3">
            {fields.map((field) => {
              const problem =
                fieldErrors[field.key];

              const fieldClass =
                `${inputStyle} ${
                  problem
                    ? "border-red-500 ring-1 ring-red-200"
                    : ""
                }`;

              return (
                <div key={field.key}>
                  <Field
                    label={field.label}
                  >
                    {field.options ||
                    field.lookup ? (
                      <select
                        name={`master-${field.key}`}
                        className={fieldClass}
                        aria-invalid={
                          Boolean(problem)
                        }
                        value={String(
                          values[field.key] ??
                            "",
                        )}
                        onChange={(event) =>
                          changeValue(
                            field,
                            event.target
                              .value,
                          )
                        }
                      >
                        <option value="">
                          {t("Select")}
                        </option>

                        {field.options?.map(
                          (option) => (
                            <option
                              key={option}
                              value={option}
                            >
                              {t(option)}
                            </option>
                          ),
                        )}

                        {choices[
                          field.key
                        ]?.map(
                          (row) => (
                            <option
                              key={row.id}
                              value={row.id}
                            >
                              {String(
                                row.name ??
                                  row.fullName ??
                                  row.code ??
                                  row.id,
                              )}
                            </option>
                          ),
                        )}
                      </select>
                    ) : field.type ===
                      "checkbox" ? (
                      <input
                        name={`master-${field.key}`}
                        type="checkbox"
                        checked={Boolean(
                          values[
                            field.key
                          ],
                        )}
                        onChange={(event) =>
                          changeValue(
                            field,
                            event.target
                              .checked,
                          )
                        }
                      />
                    ) : (
                      <input
                        name={`master-${field.key}`}
                        className={fieldClass}
                        aria-invalid={
                          Boolean(problem)
                        }
                        type={
                          field.type ??
                          "text"
                        }
                        min={
                          field.type ===
                          "number"
                            ? field.min ??
                              0
                            : undefined
                        }
                        step={
                          field.type ===
                          "number"
                            ? "0.0001"
                            : undefined
                        }
                        value={String(
                          values[field.key] ??
                            "",
                        )}
                        onChange={(event) =>
                          changeValue(
                            field,
                            field.type ===
                              "number"
                              ? Number(
                                  event
                                    .target
                                    .value,
                                )
                              : event
                                  .target
                                  .value,
                          )
                        }
                      />
                    )}
                  </Field>

                  {problem && (
                    <p
                      role="alert"
                      className="mt-1 text-sm text-red-600"
                    >
                      {t(
                        field.label,
                      )}
                      :{" "}
                      {t(problem)}
                    </p>
                  )}
                </div>
              );
            })}
          </div>

          <button
            className={buttonStyle}
            disabled={busy}
          >
            {t(
              busy
                ? "Saving"
                : "Save",
            )}
          </button>
        </form>
      )}

      <Field label="Search">
        <input
          className={inputStyle}
          value={search}
          onChange={(event) =>
            setSearch(
              event.target.value,
            )
          }
        />
      </Field>

      <div className="overflow-auto rounded-xl border bg-white">
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
            <tr>
              {columns.map((key) => (
                <th
                  className="p-3"
                  key={key}
                >
                  {t(
                    fields.find(
                      (field) =>
                        field.key === key,
                    )?.label ?? key,
                  )}
                </th>
              ))}

              {hasActions && (
                <th className="p-3">
                  {t("Actions")}
                </th>
              )}
            </tr>
          </thead>

          <tbody>
            {visibleRows.map((row) => (
              <tr
                key={row.id}
                className="border-t border-slate-100 hover:bg-slate-50/70"
              >
                {columns.map((key) => (
                  <td
                    className="p-3"
                    key={key}
                  >
                    {typeof row[key] ===
                    "boolean"
                      ? t(
                          row[key]
                            ? "Yes"
                            : "No",
                        )
                      : t(
                          String(
                            row[key] ??
                              "",
                          ),
                        )}
                  </td>
                ))}

                {hasActions && (
                  <td className="p-3">
                    {actions ? (
                      actions(
                        row,
                        load,
                      )
                    ) : (
                      <button
                        type="button"
                        className="font-medium text-blue-700 underline"
                        onClick={() =>
                          setSelected(row)
                        }
                      >
                        {t("Details")}
                      </button>
                    )}
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>

        {!visibleRows.length && (
          <Empty />
        )}
      </div>

      {selected &&
        extra?.(
          selected,
          load,
        )}
    </Panel>
  );
}