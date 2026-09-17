"use client";

import {
  useCallback,
  useEffect,
  useRef,
  useState,
} from "react";
import {
  useSession,
} from "next-auth/react";
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
} from "@/components/operations/ui";
import {
  ReadRecords,
} from "@/components/operations/ReadRecords";
import {
  useLocale,
} from "@/components/LocaleProvider";

type ExpenseCategory = {
  id: string;
  name: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
};

type ExpenseField =
  | "date"
  | "categoryId"
  | "amount"
  | "description"
  | "method";

type CategoryEditor = {
  id: string;
  name: string;
  active: boolean;
};

const paymentMethods = [
  "CASH",
  "MOBILE_MONEY",
  "CARD",
  "BANK_TRANSFER",
] as const;

export default function Expenses() {
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

  const canRecord =
    permissions.includes(
      "EXPENSE_RECORD",
    );

  const canManageCategories =
    permissions.includes(
      "EXPENSE_CATEGORY_MANAGE",
    );

  const [date, setDate] =
    useState("");

  const [
    categoryId,
    setCategoryId,
  ] =
    useState("");

  const [
    description,
    setDescription,
  ] =
    useState("");

  const [amount, setAmount] =
    useState(0);

  const [method, setMethod] =
    useState("CASH");

  const [
    categories,
    setCategories,
  ] =
    useState<
      ExpenseCategory[]
    >([]);

  const [
    newCategory,
    setNewCategory,
  ] =
    useState("");

  const [
    categoryEditor,
    setCategoryEditor,
  ] =
    useState<CategoryEditor | null>(
      null,
    );

  const [error, setError] =
    useState("");

  const [
    categoryError,
    setCategoryError,
  ] =
    useState("");

  const [
    message,
    setMessage,
  ] =
    useState("");

  const [
    fieldErrors,
    setFieldErrors,
  ] =
    useState<
      Partial<
        Record<
          ExpenseField,
          string
        >
      >
    >({});

  const [busy, setBusy] =
    useState(false);

  const [
    categoryBusy,
    setCategoryBusy,
  ] =
    useState(false);

  const [version, setVersion] =
    useState(0);

  const requestId =
    useRef("");

  const loadCategories =
    useCallback(
      async () => {
        const path =
          canManageCategories
            ? "expense-categories?includeInactive=true"
            : "expense-categories";

        setCategories(
          await all<ExpenseCategory>(
            path,
          ),
        );
      },
      [
        canManageCategories,
      ],
    );

  useEffect(() => {
    setDate(
      new Date()
        .toISOString()
        .slice(0, 10),
    );
  }, []);

  useEffect(() => {
    loadCategories().catch(
      (requestError) =>
        setCategoryError(
          requestError instanceof
            Error
            ? requestError.message
            : "Unable to load expense categories.",
        ),
    );
  }, [loadCategories]);

  function clearFieldError(
    field: ExpenseField,
  ) {
    setFieldErrors(
      (current) => {
        if (
          !current[field]
        ) {
          return current;
        }

        const next = {
          ...current,
        };

        delete next[field];

        return next;
      },
    );
  }

  function invalidateRequest() {
    requestId.current = "";
  }

  function focusField(
    field: ExpenseField,
  ) {
    window.requestAnimationFrame(
      () => {
        document
          .querySelector<
            | HTMLInputElement
            | HTMLSelectElement
          >(
            `[name="expense-${field}"]`,
          )
          ?.focus();
      },
    );
  }

  function validateExpense() {
    const problems:
      Partial<
        Record<
          ExpenseField,
          string
        >
      > = {};

    if (!date) {
      problems.date =
        "Expense date is required.";
    }

    if (!categoryId) {
      problems.categoryId =
        "Expense category is required.";
    }

    if (
      !Number.isFinite(
        amount,
      ) ||
      amount <= 0
    ) {
      problems.amount =
        "Amount must be greater than zero.";
    }

    if (
      !description.trim()
    ) {
      problems.description =
        "Description is required.";
    } else if (
      description.trim()
        .length > 1000
    ) {
      problems.description =
        "Description must not exceed 1000 characters.";
    }

    if (
      !paymentMethods.includes(
        method as
          (typeof paymentMethods)[number],
      )
    ) {
      problems.method =
        "Choose a valid expense payment method.";
    }

    return problems;
  }

  async function saveExpense(
    event:
      React.FormEvent,
  ) {
    event.preventDefault();

    const problems =
      validateExpense();

    if (
      Object.keys(problems)
        .length > 0
    ) {
      setFieldErrors(
        problems,
      );

      setError(
        "Please correct the highlighted fields.",
      );

      const first =
        Object.keys(
          problems,
        )[0] as ExpenseField;

      focusField(first);

      return;
    }

    setBusy(true);
    setError("");
    setMessage("");
    setFieldErrors({});

    try {
      requestId.current ||=
        crypto.randomUUID();

      await api(
        "expenses",
        "POST",
        {
          date,
          categoryId,
          description:
            description.trim(),
          amount,
          method,
          requestId:
            requestId.current,
        },
      );

      requestId.current =
        "";

      setCategoryId("");
      setDescription("");
      setAmount(0);

      setVersion(
        (current) =>
          current + 1,
      );

      setMessage(
        "Expense recorded successfully.",
      );
    } catch (requestError) {
      if (
        isHmsApiError(
          requestError,
        ) &&
        requestError
          .fieldErrors.length >
          0
      ) {
        const problems:
          Partial<
            Record<
              ExpenseField,
              string
            >
          > = {};

        for (
          const problem of requestError.fieldErrors
        ) {
          if (
            [
              "date",
              "categoryId",
              "amount",
              "description",
              "method",
            ].includes(
              problem.field,
            )
          ) {
            problems[
              problem.field as ExpenseField
            ] =
              problem.message;
          }
        }

        setFieldErrors(
          problems,
        );

        setError(
          requestError.message,
        );

        const first =
          Object.keys(
            problems,
          )[0] as
            | ExpenseField
            | undefined;

        if (first) {
          focusField(first);
        }
      } else {
        setError(
          requestError instanceof
            Error
            ? requestError.message
            : "Unable to record expense.",
        );
      }
    } finally {
      setBusy(false);
    }
  }

  async function createCategory(
    event:
      React.FormEvent,
  ) {
    event.preventDefault();

    const cleanName =
      newCategory.trim();

    setCategoryError("");
    setMessage("");

    if (!cleanName) {
      setCategoryError(
        "Category name is required.",
      );

      return;
    }

    if (
      cleanName.length > 100
    ) {
      setCategoryError(
        "Category name must not exceed 100 characters.",
      );

      return;
    }

    setCategoryBusy(true);

    try {
      await api(
        "expense-categories",
        "POST",
        {
          name: cleanName,
        },
      );

      setNewCategory("");

      await loadCategories();

      setMessage(
        "Expense category created successfully.",
      );
    } catch (requestError) {
      if (
        isHmsApiError(
          requestError,
        ) &&
        requestError
          .fieldErrors.length >
          0
      ) {
        setCategoryError(
          requestError
            .fieldErrors[0]
            .message,
        );
      } else {
        setCategoryError(
          requestError instanceof
            Error
            ? requestError.message
            : "Unable to create expense category.",
        );
      }
    } finally {
      setCategoryBusy(false);
    }
  }

  async function saveCategory(
    event:
      React.FormEvent,
  ) {
    event.preventDefault();

    if (!categoryEditor) {
      return;
    }

    const cleanName =
      categoryEditor
        .name
        .trim();

    setCategoryError("");
    setMessage("");

    if (!cleanName) {
      setCategoryError(
        "Category name is required.",
      );

      return;
    }

    if (
      cleanName.length > 100
    ) {
      setCategoryError(
        "Category name must not exceed 100 characters.",
      );

      return;
    }

    setCategoryBusy(true);

    try {
      await api(
        `expense-categories/${categoryEditor.id}`,
        "PUT",
        {
          name: cleanName,
          active:
            categoryEditor.active,
        },
      );

      setCategoryEditor(
        null,
      );

      /*
       * If a category was deactivated
       * while selected for a new
       * expense, remove that selection.
       */
      if (
        categoryId ===
        categoryEditor.id &&
        !categoryEditor.active
      ) {
        setCategoryId("");
        invalidateRequest();
      }

      await loadCategories();

      setMessage(
        "Expense category updated successfully.",
      );
    } catch (requestError) {
      if (
        isHmsApiError(
          requestError,
        ) &&
        requestError
          .fieldErrors.length >
          0
      ) {
        setCategoryError(
          requestError
            .fieldErrors[0]
            .message,
        );
      } else {
        setCategoryError(
          requestError instanceof
            Error
            ? requestError.message
            : "Unable to update expense category.",
        );
      }
    } finally {
      setCategoryBusy(false);
    }
  }

  const activeCategories =
    categories.filter(
      (category) =>
        category.active,
    );

  function fieldClass(
    field: ExpenseField,
  ) {
    return `${inputStyle} ${
      fieldErrors[field]
        ? "border-red-500 ring-1 ring-red-200"
        : ""
    }`;
  }

  return (
    <Panel
      title="Expenses"
      error={error}
    >
      {message && (
        <p
          role="status"
          className="rounded-xl bg-emerald-50 px-4 py-3 text-sm text-emerald-800"
        >
          {t(message)}
        </p>
      )}

      {canRecord && (
        <section className="rounded-2xl border bg-white p-6 shadow-sm">
          <div className="mb-5">
            <h2 className="text-lg font-semibold text-slate-900">
              {t(
                "Record operating expense",
              )}
            </h2>

            <p className="mt-1 text-sm text-slate-500">
              {t(
                "Choose the expense category configured by hotel management.",
              )}
            </p>
          </div>

          <form
            noValidate
            onSubmit={
              saveExpense
            }
            className="space-y-5"
          >
            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
              <div>
                <Field label="Date">
                  <input
                    name="expense-date"
                    className={fieldClass(
                      "date",
                    )}
                    aria-invalid={
                      Boolean(
                        fieldErrors.date,
                      )
                    }
                    type="date"
                    value={date}
                    onChange={(
                      event,
                    ) => {
                      invalidateRequest();
                      clearFieldError(
                        "date",
                      );
                      setDate(
                        event.target
                          .value,
                      );
                    }}
                  />
                </Field>

                {fieldErrors.date && (
                  <p
                    role="alert"
                    className="mt-1 text-sm text-red-600"
                  >
                    {t(
                      fieldErrors.date,
                    )}
                  </p>
                )}
              </div>

              <div>
                <Field label="Category">
                  <select
                    name="expense-categoryId"
                    className={fieldClass(
                      "categoryId",
                    )}
                    aria-invalid={
                      Boolean(
                        fieldErrors.categoryId,
                      )
                    }
                    value={
                      categoryId
                    }
                    onChange={(
                      event,
                    ) => {
                      invalidateRequest();
                      clearFieldError(
                        "categoryId",
                      );

                      setCategoryId(
                        event.target
                          .value,
                      );
                    }}
                  >
                    <option value="">
                      {t(
                        "Select expense category",
                      )}
                    </option>

                    {activeCategories.map(
                      (
                        category,
                      ) => (
                        <option
                          key={
                            category.id
                          }
                          value={
                            category.id
                          }
                        >
                          {
                            category.name
                          }
                        </option>
                      ),
                    )}
                  </select>
                </Field>

                {fieldErrors.categoryId && (
                  <p
                    role="alert"
                    className="mt-1 text-sm text-red-600"
                  >
                    {t(
                      fieldErrors.categoryId,
                    )}
                  </p>
                )}
              </div>

              <div>
                <Field label="Amount">
                  <input
                    name="expense-amount"
                    className={fieldClass(
                      "amount",
                    )}
                    aria-invalid={
                      Boolean(
                        fieldErrors.amount,
                      )
                    }
                    type="number"
                    min="0.0001"
                    step="0.0001"
                    value={amount}
                    onChange={(
                      event,
                    ) => {
                      invalidateRequest();
                      clearFieldError(
                        "amount",
                      );

                      setAmount(
                        Number(
                          event
                            .target
                            .value,
                        ),
                      );
                    }}
                  />
                </Field>

                {fieldErrors.amount && (
                  <p
                    role="alert"
                    className="mt-1 text-sm text-red-600"
                  >
                    {t(
                      fieldErrors.amount,
                    )}
                  </p>
                )}
              </div>

              <div className="md:col-span-2">
                <Field label="Description">
                  <input
                    name="expense-description"
                    className={fieldClass(
                      "description",
                    )}
                    aria-invalid={
                      Boolean(
                        fieldErrors.description,
                      )
                    }
                    value={
                      description
                    }
                    onChange={(
                      event,
                    ) => {
                      invalidateRequest();
                      clearFieldError(
                        "description",
                      );

                      setDescription(
                        event.target
                          .value,
                      );
                    }}
                  />
                </Field>

                {fieldErrors.description && (
                  <p
                    role="alert"
                    className="mt-1 text-sm text-red-600"
                  >
                    {t(
                      fieldErrors.description,
                    )}
                  </p>
                )}
              </div>

              <div>
                <Field label="Payment method">
                  <select
                    name="expense-method"
                    className={fieldClass(
                      "method",
                    )}
                    aria-invalid={
                      Boolean(
                        fieldErrors.method,
                      )
                    }
                    value={method}
                    onChange={(
                      event,
                    ) => {
                      invalidateRequest();
                      clearFieldError(
                        "method",
                      );

                      setMethod(
                        event.target
                          .value,
                      );
                    }}
                  >
                    {paymentMethods.map(
                      (
                        paymentMethod,
                      ) => (
                        <option
                          key={
                            paymentMethod
                          }
                          value={
                            paymentMethod
                          }
                        >
                          {t(
                            paymentMethod,
                          )}
                        </option>
                      ),
                    )}
                  </select>
                </Field>

                {fieldErrors.method && (
                  <p
                    role="alert"
                    className="mt-1 text-sm text-red-600"
                  >
                    {t(
                      fieldErrors.method,
                    )}
                  </p>
                )}
              </div>
            </div>

            {!activeCategories.length && (
              <p className="rounded-xl bg-amber-50 px-4 py-3 text-sm text-amber-800">
                {t(
                  "No active expense categories are available.",
                )}
              </p>
            )}

            <button
              className={
                buttonStyle
              }
              disabled={
                busy ||
                !activeCategories.length
              }
            >
              {t(
                busy
                  ? "Saving"
                  : "Record expense",
              )}
            </button>
          </form>
        </section>
      )}

      {canManageCategories && (
        <section className="rounded-2xl border bg-white p-6 shadow-sm">
          <div className="mb-5">
            <h2 className="text-lg font-semibold text-slate-900">
              {t(
                "Expense categories",
              )}
            </h2>

            <p className="mt-1 text-sm text-slate-500">
              {t(
                "Create and maintain the categories cashiers can use when recording expenses.",
              )}
            </p>
          </div>

          {categoryError && (
            <p
              role="alert"
              className="mb-5 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700"
            >
              {t(
                categoryError,
              )}
            </p>
          )}

          <form
            noValidate
            onSubmit={
              createCategory
            }
            className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-end"
          >
            <div className="flex-1">
              <Field label="New category">
                <input
                  className={
                    inputStyle
                  }
                  value={
                    newCategory
                  }
                  maxLength={100}
                  onChange={(
                    event,
                  ) => {
                    setCategoryError(
                      "",
                    );

                    setNewCategory(
                      event.target
                        .value,
                    );
                  }}
                  placeholder={t(
                    "Category name",
                  )}
                />
              </Field>
            </div>

            <button
              className={
                buttonStyle
              }
              disabled={
                categoryBusy
              }
            >
              {t(
                categoryBusy
                  ? "Saving"
                  : "Add category",
              )}
            </button>
          </form>

          <div className="overflow-x-auto rounded-xl border">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
                <tr>
                  <th className="px-5 py-3">
                    {t(
                      "Category",
                    )}
                  </th>

                  <th className="px-5 py-3">
                    {t(
                      "Status",
                    )}
                  </th>

                  <th className="px-5 py-3">
                    {t(
                      "Actions",
                    )}
                  </th>
                </tr>
              </thead>

              <tbody>
                {categories.map(
                  (
                    category,
                  ) => (
                    <tr
                      key={
                        category.id
                      }
                      className="border-t"
                    >
                      <td className="px-5 py-4 font-medium text-slate-900">
                        {
                          category.name
                        }
                      </td>

                      <td className="px-5 py-4">
                        <span
                          className={`rounded-full px-2.5 py-1 text-xs font-medium ${
                            category.active
                              ? "bg-emerald-50 text-emerald-700"
                              : "bg-slate-100 text-slate-600"
                          }`}
                        >
                          {t(
                            category.active
                              ? "Active"
                              : "Inactive",
                          )}
                        </span>
                      </td>

                      <td className="px-5 py-4">
                        <button
                          type="button"
                          className="font-medium text-blue-700 hover:underline"
                          onClick={() => {
                            setCategoryError(
                              "",
                            );

                            setCategoryEditor(
                              {
                                id:
                                  category.id,
                                name:
                                  category.name,
                                active:
                                  category.active,
                              },
                            );
                          }}
                        >
                          {t(
                            "Edit",
                          )}
                        </button>
                      </td>
                    </tr>
                  ),
                )}
              </tbody>
            </table>

            {!categories.length && (
              <p className="p-5 text-sm text-slate-500">
                {t(
                  "No expense categories configured.",
                )}
              </p>
            )}
          </div>
        </section>
      )}

      <ReadRecords
        key={version}
        title="Expense ledger"
        resource="expenses"
        columns={{
          expense_date:
            "Date",
          category:
            "Category",
          description:
            "Description",
          amount:
            "Amount",
          method:
            "Payment method",
        }}
      />

      {categoryEditor && (
        <div className="fixed inset-0 z-50 flex justify-end bg-slate-950/30 backdrop-blur-sm">
          <section
            role="dialog"
            aria-modal="true"
            aria-label={t(
              "Edit category",
            )}
            className="h-full w-full max-w-md overflow-y-auto bg-white p-7 shadow-2xl"
          >
            <header className="mb-6 flex items-center justify-between">
              <div>
                <p className="text-xs uppercase tracking-widest text-blue-600">
                  {t(
                    "Expense categories",
                  )}
                </p>

                <h2 className="mt-2 text-2xl font-semibold">
                  {t(
                    "Edit category",
                  )}
                </h2>
              </div>

              <button
                type="button"
                aria-label={t(
                  "Close",
                )}
                disabled={
                  categoryBusy
                }
                onClick={() =>
                  setCategoryEditor(
                    null,
                  )
                }
                className="rounded-full bg-slate-100 px-3 py-2 text-lg"
              >
                ×
              </button>
            </header>

            {categoryError && (
              <p
                role="alert"
                className="mb-5 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700"
              >
                {t(
                  categoryError,
                )}
              </p>
            )}

            <form
              onSubmit={
                saveCategory
              }
              className="space-y-5"
            >
              <Field label="Category name">
                <input
                  className={
                    inputStyle
                  }
                  maxLength={100}
                  value={
                    categoryEditor.name
                  }
                  onChange={(
                    event,
                  ) =>
                    setCategoryEditor(
                      {
                        ...categoryEditor,
                        name:
                          event
                            .target
                            .value,
                      },
                    )
                  }
                />
              </Field>

              <label className="flex items-center gap-3 rounded-xl border p-4">
                <input
                  type="checkbox"
                  checked={
                    categoryEditor.active
                  }
                  onChange={(
                    event,
                  ) =>
                    setCategoryEditor(
                      {
                        ...categoryEditor,
                        active:
                          event
                            .target
                            .checked,
                      },
                    )
                  }
                />

                <span>
                  {t(
                    "Active",
                  )}
                </span>
              </label>

              <p className="text-sm leading-relaxed text-slate-500">
                {t(
                  "Inactive categories remain in historical expenses but cannot be selected for new expenses.",
                )}
              </p>

              <div className="flex gap-3 border-t pt-5">
                <button
                  type="button"
                  className="rounded-lg border px-5 py-2 text-sm font-medium"
                  disabled={
                    categoryBusy
                  }
                  onClick={() =>
                    setCategoryEditor(
                      null,
                    )
                  }
                >
                  {t(
                    "Cancel",
                  )}
                </button>

                <button
                  className={`${buttonStyle} flex-1`}
                  disabled={
                    categoryBusy
                  }
                >
                  {t(
                    categoryBusy
                      ? "Saving"
                      : "Save changes",
                  )}
                </button>
              </div>
            </form>
          </section>
        </div>
      )}
    </Panel>
  );
}