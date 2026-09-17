"use client";

import {
  useState,
} from "react";
import { useSession } from "next-auth/react";
import {
  Row,
} from "./MasterData";
import {
  productFields,
} from "./fields";
import {
  Field,
  inputStyle,
  buttonStyle,
} from "./ui";
import {
  api,
  isHmsApiError,
} from "@/lib/hms-api";
import {
  useLocale,
} from "@/components/LocaleProvider";

type ProductEditorProps = {
  product: Row;
  onSaved?: () => Promise<void>;

  /*
   * Used by Stock so clicking Edit in the Actions
   * column immediately opens the editor.
   *
   * Existing uses such as Menu management retain
   * their current behavior because this defaults
   * to false.
   */
  startEditing?: boolean;

  /*
   * Used when ProductEditor is presented as a drawer.
   */
  onClose?: () => void;
};

export function ProductEditor({
  product,
  onSaved,
  startEditing = false,
  onClose,
}: ProductEditorProps) {
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

  const [editing, setEditing] =
    useState(startEditing);

  const [values, setValues] =
    useState<Row>({
      ...product,
    });

  const [busy, setBusy] =
    useState(false);

  const [error, setError] =
    useState("");

  const [message, setMessage] =
    useState("");

  const [fieldErrors, setFieldErrors] =
    useState<Record<string, string>>({});

  if (
    !permissions.includes(
      "PRODUCT_MANAGE",
    )
  ) {
    return null;
  }

  function clearFieldError(
    key: string,
  ) {
    setFieldErrors(
      (current) => {
        if (!current[key]) {
          return current;
        }

        const next = {
          ...current,
        };

        delete next[key];

        return next;
      },
    );
  }

  function changeValue(
    key: string,
    value: unknown,
  ) {
    clearFieldError(key);

    setValues(
      (current) => ({
        ...current,
        [key]: value,
      }),
    );
  }

  function focusField(
    key: string,
  ) {
    window.requestAnimationFrame(
      () => {
        const element =
          document.querySelector<
            | HTMLInputElement
            | HTMLSelectElement
          >(
            `[name="product-${key}"]`,
          );

        element?.focus();
      },
    );
  }

  function validateProduct() {
    const problems: Record<
      string,
      string
    > = {};

    for (const field of productFields) {
      const value =
        values[field.key];

      if (
        field.type !==
          "checkbox" &&
        (
          value === null ||
          value === undefined ||
          String(value).trim() ===
            ""
        )
      ) {
        problems[field.key] =
          `${t(field.label)} ${t("is required.")}`;

        continue;
      }

      if (
        field.type ===
        "number"
      ) {
        const numberValue =
          Number(value);

        if (
          !Number.isFinite(
            numberValue,
          )
        ) {
          problems[field.key] =
            `${t(field.label)} ${t("must be a valid number.")}`;

          continue;
        }

        const minimum =
          field.min ?? 0;

        if (
          numberValue <
          minimum
        ) {
          problems[field.key] =
            `${t(field.label)} ${t("must be at least")} ${minimum}.`;
        }
      }
    }

    const taxRate =
      Number(
        values.taxRate,
      );

    if (
      Number.isFinite(
        taxRate,
      ) &&
      taxRate > 1
    ) {
      problems.taxRate =
        `${t("Tax rate (0–1)")} ${t("must not be greater than")} 1.`;
    }

    return problems;
  }

  async function save(
    event: React.FormEvent,
  ) {
    event.preventDefault();

    const localErrors =
      validateProduct();

    if (
      Object.keys(localErrors)
        .length > 0
    ) {
      setFieldErrors(
        localErrors,
      );

      setError(
        "Please correct the highlighted fields.",
      );

      focusField(
        Object.keys(
          localErrors,
        )[0],
      );

      return;
    }

    setBusy(true);
    setError("");
    setMessage("");
    setFieldErrors({});

    try {
      /*
       * Only send fields belonging to ProductRequest.
       * Do not send table-only fields such as id.
       */
      const body =
        Object.fromEntries(
          productFields.map(
            (field) => [
              field.key,
              values[
                field.key
              ],
            ],
          ),
        );

      await api(
        `products/${product.id}`,
        "PUT",
        body,
      );

      await onSaved?.();

      if (
        startEditing &&
        onClose
      ) {
        onClose();
      } else {
        setEditing(false);
        setMessage("Saved");
      }
    } catch (requestError) {
      if (
        isHmsApiError(
          requestError,
        ) &&
        requestError
          .fieldErrors.length >
          0
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

        setFieldErrors(
          problems,
        );

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
          requestError instanceof
            Error
            ? requestError.message
            : "Unable to save product.",
        );
      }
    } finally {
      setBusy(false);
    }
  }

  function cancel() {
    setError("");
    setFieldErrors({});

    if (
      startEditing &&
      onClose
    ) {
      onClose();
      return;
    }

    setEditing(false);
  }

  const form = (
    <form
      noValidate
      onSubmit={save}
      className="space-y-5"
    >
      <p className="text-sm text-slate-500">
        {t(
          "Stock corrections are recorded as movements to preserve history.",
        )}
      </p>

      {error && (
        <p
          role="alert"
          className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700"
        >
          {t(error)}
        </p>
      )}

      <div className="grid gap-4 md:grid-cols-3">
        {productFields.map(
          (field) => {
            const problem =
              fieldErrors[
                field.key
              ];

            const fieldClass =
              `${inputStyle} ${
                problem
                  ? "border-red-500 ring-1 ring-red-200"
                  : ""
              }`;

            return (
              <div
                key={
                  field.key
                }
              >
                <Field
                  label={
                    field.label
                  }
                >
                  {field.options ? (
                    <select
                      name={`product-${field.key}`}
                      className={
                        fieldClass
                      }
                      aria-invalid={
                        Boolean(
                          problem,
                        )
                      }
                      value={String(
                        values[
                          field.key
                        ] ?? "",
                      )}
                      onChange={(
                        event,
                      ) =>
                        changeValue(
                          field.key,
                          event
                            .target
                            .value,
                        )
                      }
                    >
                      {field.options.map(
                        (
                          option,
                        ) => (
                          <option
                            key={
                              option
                            }
                            value={
                              option
                            }
                          >
                            {t(
                              option,
                            )}
                          </option>
                        ),
                      )}
                    </select>
                  ) : field.type ===
                    "checkbox" ? (
                    <input
                      name={`product-${field.key}`}
                      type="checkbox"
                      checked={Boolean(
                        values[
                          field.key
                        ],
                      )}
                      onChange={(
                        event,
                      ) =>
                        changeValue(
                          field.key,
                          event
                            .target
                            .checked,
                        )
                      }
                    />
                  ) : (
                    <input
                      name={`product-${field.key}`}
                      className={
                        fieldClass
                      }
                      aria-invalid={
                        Boolean(
                          problem,
                        )
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
                        values[
                          field.key
                        ] ?? "",
                      )}
                      onChange={(
                        event,
                      ) =>
                        changeValue(
                          field.key,
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
                    {t(
                      problem,
                    )}
                  </p>
                )}
              </div>
            );
          },
        )}
      </div>

      <div className="flex gap-3 border-t pt-5">
        <button
          type="button"
          className="rounded-lg border px-5 py-2 text-sm font-medium"
          disabled={busy}
          onClick={cancel}
        >
          {t("Cancel")}
        </button>

        <button
          className={`${buttonStyle} flex-1`}
          disabled={busy}
        >
          {t(
            busy
              ? "Saving"
              : "Save",
          )}
        </button>
      </div>
    </form>
  );

  /*
   * Stock action mode:
   * display a proper right-hand editor drawer.
   */
  if (startEditing) {
    return (
      <div className="fixed inset-0 z-50 flex justify-end bg-slate-950/30 backdrop-blur-sm">
        <section
          role="dialog"
          aria-modal="true"
          aria-label={t(
            "Product details",
          )}
          className="h-full w-full max-w-3xl overflow-y-auto bg-white p-7 shadow-2xl"
        >
          <header className="mb-6 flex items-start justify-between gap-4">
            <div>
              <p className="text-xs uppercase tracking-widest text-blue-600">
                {t(
                  "Edit product",
                )}
              </p>

              <h2 className="mt-2 text-2xl font-semibold">
                {String(
                  product.name,
                )}
              </h2>

              <p className="mt-1 text-sm text-slate-500">
                {String(
                  product.sku,
                )}
              </p>
            </div>

            <button
              type="button"
              aria-label={t(
                "Close",
              )}
              disabled={busy}
              onClick={cancel}
              className="rounded-full bg-slate-100 px-3 py-2 text-lg text-slate-600 hover:bg-slate-200"
            >
              ×
            </button>
          </header>

          {form}
        </section>
      </div>
    );
  }

  /*
   * Existing default mode.
   * This keeps Menu management compatible.
   */
  return (
    <section className="space-y-4 rounded-2xl border border-slate-200 bg-white p-6">
      <div className="flex items-center justify-between">
        <h2 className="font-semibold">
          {t(
            "Product details",
          )}
        </h2>

        <button
          type="button"
          className={buttonStyle}
          onClick={() => {
            setEditing(
              !editing,
            );

            setError("");
            setFieldErrors({});
          }}
        >
          {t(
            editing
              ? "Cancel"
              : "Edit",
          )}
        </button>
      </div>

      {message && (
        <p
          role="status"
          className="rounded-xl bg-emerald-50 px-4 py-3 text-sm text-emerald-800"
        >
          {t(message)}
        </p>
      )}

      {editing && form}
    </section>
  );
}