"use client";

import {
  useEffect,
  useRef,
  useState,
} from "react";
import {
  useSession,
} from "next-auth/react";
import {
  ProductEditor,
} from "@/components/operations/ProductEditor";
import {
  MasterData,
  Row,
} from "@/components/operations/MasterData";
import {
  productFields,
} from "@/components/operations/fields";
import {
  api,
  all,
} from "@/lib/hms-api";
import {
  Field,
  inputStyle,
  buttonStyle,
} from "@/components/operations/ui";
import {
  useLocale,
} from "@/components/LocaleProvider";

type EditorState = {
  product: Row;
  refresh: () => Promise<void>;
};

function StockDetail({
  product,
  onClose,
}: {
  product: Row;
  onClose: () => void;
}) {
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

  const canAdjust =
    permissions.includes(
      "INVENTORY_ADJUST",
    );

  const [stock, setStock] =
    useState<{
      quantity: number;
      unit: string;
    } | null>(null);

  const [history, setHistory] =
    useState<Row[]>([]);

  const [quantity, setQuantity] =
    useState(0);

  const [kind, setKind] =
    useState("OPENING");

  const [reason, setReason] =
    useState("");

  const [error, setError] =
    useState("");

  const [message, setMessage] =
    useState("");

  const [quantityError, setQuantityError] =
    useState("");

  const [reasonError, setReasonError] =
    useState("");

  const [busy, setBusy] =
    useState(false);

  const key =
    useRef("");

  async function load() {
    setStock(
      await api(
        `inventory/${product.id}`,
      ),
    );

    setHistory(
      await all<Row>(
        `inventory/${product.id}/movements`,
      ),
    );
  }

  useEffect(() => {
    load().catch(
      (requestError) =>
        setError(
          requestError instanceof
            Error
            ? requestError.message
            : "Unable to load stock information.",
        ),
    );
  }, [product.id]);

  async function adjust() {
    let invalid = false;

    setError("");
    setMessage("");
    setQuantityError("");
    setReasonError("");

    if (
      !Number.isFinite(
        quantity,
      ) ||
      quantity === 0
    ) {
      setQuantityError(
        "Quantity cannot be zero.",
      );

      invalid = true;
    }

    if (!reason.trim()) {
      setReasonError(
        "Reason is required.",
      );

      invalid = true;
    }

    if (invalid) {
      setError(
        "Please correct the highlighted fields.",
      );

      return;
    }

    setBusy(true);

    try {
      key.current ||=
        crypto.randomUUID();

      await api(
        "inventory/adjustments",
        "POST",
        {
          productId:
            product.id,
          quantity,
          kind,
          reason:
            reason.trim(),
          requestId:
            key.current,
        },
      );

      key.current = "";

      setQuantity(0);
      setReason("");

      await load();

      setMessage(
        "Stock movement recorded.",
      );
    } catch (requestError) {
      setError(
        requestError instanceof
          Error
          ? requestError.message
          : "Unable to record stock movement.",
      );
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex justify-end bg-slate-950/30 backdrop-blur-sm">
      <section
        role="dialog"
        aria-modal="true"
        aria-label={t(
          "Stock movements",
        )}
        className="h-full w-full max-w-3xl overflow-y-auto bg-white p-7 shadow-2xl"
      >
        <header className="mb-6 flex items-start justify-between gap-4">
          <div>
            <p className="text-xs uppercase tracking-widest text-blue-600">
              {t(
                "Stock movements",
              )}
            </p>

            <h2 className="mt-2 text-2xl font-semibold">
              {String(
                product.name,
              )}
            </h2>

            <p className="mt-2 text-sm text-slate-500">
              {t(
                "Current stock",
              )}
              :{" "}
              <span className="font-semibold text-slate-900">
                {stock?.quantity ??
                  "—"}{" "}
                {stock?.unit ??
                  ""}
              </span>
            </p>
          </div>

          <button
            type="button"
            aria-label={t("Close")}
            disabled={busy}
            onClick={onClose}
            className="rounded-full bg-slate-100 px-3 py-2 text-lg text-slate-600 hover:bg-slate-200"
          >
            ×
          </button>
        </header>

        {error && (
          <p
            role="alert"
            className="mb-5 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700"
          >
            {t(error)}
          </p>
        )}

        {message && (
          <p
            role="status"
            className="mb-5 rounded-xl bg-emerald-50 px-4 py-3 text-sm text-emerald-800"
          >
            {t(message)}
          </p>
        )}

        {canAdjust && (
          <section className="mb-7 space-y-4 rounded-xl border bg-slate-50 p-5">
            <h3 className="font-semibold">
              {t(
                "Record stock movement",
              )}
            </h3>

            <div className="grid gap-4 md:grid-cols-3">
              <div>
                <Field label="Movement type">
                  <select
                    className={
                      inputStyle
                    }
                    value={kind}
                    onChange={(
                      event,
                    ) => {
                      key.current =
                        "";

                      setKind(
                        event.target
                          .value,
                      );
                    }}
                  >
                    {[
                      "OPENING",
                      "ADJUSTMENT",
                      "WASTE",
                    ].map(
                      (
                        movement,
                      ) => (
                        <option
                          key={
                            movement
                          }
                          value={
                            movement
                          }
                        >
                          {t(
                            movement,
                          )}
                        </option>
                      ),
                    )}
                  </select>
                </Field>
              </div>

              <div>
                <Field label="Quantity">
                  <input
                    type="number"
                    step="0.0001"
                    className={`${inputStyle} ${
                      quantityError
                        ? "border-red-500 ring-1 ring-red-200"
                        : ""
                    }`}
                    aria-invalid={
                      Boolean(
                        quantityError,
                      )
                    }
                    value={
                      quantity
                    }
                    onChange={(
                      event,
                    ) => {
                      key.current =
                        "";

                      setQuantityError(
                        "",
                      );

                      setQuantity(
                        Number(
                          event
                            .target
                            .value,
                        ),
                      );
                    }}
                  />
                </Field>

                {quantityError && (
                  <p
                    role="alert"
                    className="mt-1 text-sm text-red-600"
                  >
                    {t(
                      "Quantity",
                    )}
                    :{" "}
                    {t(
                      quantityError,
                    )}
                  </p>
                )}
              </div>

              <div>
                <Field label="Reason">
                  <input
                    className={`${inputStyle} ${
                      reasonError
                        ? "border-red-500 ring-1 ring-red-200"
                        : ""
                    }`}
                    aria-invalid={
                      Boolean(
                        reasonError,
                      )
                    }
                    value={reason}
                    onChange={(
                      event,
                    ) => {
                      key.current =
                        "";

                      setReasonError(
                        "",
                      );

                      setReason(
                        event.target
                          .value,
                      );
                    }}
                  />
                </Field>

                {reasonError && (
                  <p
                    role="alert"
                    className="mt-1 text-sm text-red-600"
                  >
                    {t(
                      "Reason",
                    )}
                    :{" "}
                    {t(
                      reasonError,
                    )}
                  </p>
                )}
              </div>
            </div>

            <button
              type="button"
              className={
                buttonStyle
              }
              disabled={busy}
              onClick={adjust}
            >
              {t(
                busy
                  ? "Saving"
                  : "Confirm stock movement",
              )}
            </button>
          </section>
        )}

        <section className="overflow-hidden rounded-xl border">
          <header className="border-b bg-slate-50 px-5 py-4">
            <h3 className="font-semibold">
              {t(
                "Movement history",
              )}
            </h3>
          </header>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
                <tr>
                  {[
                    "Type",
                    "Quantity",
                    "Reason",
                  ].map(
                    (heading) => (
                      <th
                        key={
                          heading
                        }
                        className="px-5 py-3"
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
                {history.map(
                  (movement) => (
                    <tr
                      key={
                        movement.id
                      }
                      className="border-t"
                    >
                      <td className="px-5 py-3">
                        {t(
                          String(
                            movement.kind,
                          ),
                        )}
                      </td>

                      <td className="px-5 py-3 tabular-nums">
                        {String(
                          movement.quantity,
                        )}
                      </td>

                      <td className="px-5 py-3">
                        {String(
                          movement.reason ??
                            "",
                        )}
                      </td>
                    </tr>
                  ),
                )}
              </tbody>
            </table>

            {!history.length && (
              <p className="p-5 text-sm text-slate-500">
                {t(
                  "No stock movements yet.",
                )}
              </p>
            )}
          </div>
        </section>
      </section>
    </div>
  );
}

export default function Stock() {
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

  const canEdit =
    permissions.includes(
      "PRODUCT_MANAGE",
    );

  const [editor, setEditor] =
    useState<EditorState | null>(
      null,
    );

  const [
    stockProduct,
    setStockProduct,
  ] =
    useState<Row | null>(
      null,
    );

  return (
    <>
      <MasterData
        title="Products and stock"
        resource="products"
        fields={productFields}
        columns={[
          "sku",
          "name",
          "category",
          "sellingPrice",
          "purchasePrice",
          "stockTracked",
          "active",
        ]}
        actions={(
          row,
          refresh,
        ) => (
          <div className="flex flex-wrap gap-3">
            {canEdit && (
              <button
                type="button"
                className="font-medium text-blue-700 hover:underline"
                onClick={() =>
                  setEditor({
                    product:
                      row,
                    refresh,
                  })
                }
              >
                {t("Edit")}
              </button>
            )}

            {Boolean(
              row.stockTracked,
            ) && (
              <button
                type="button"
                className="font-medium text-slate-700 hover:text-blue-700 hover:underline"
                onClick={() =>
                  setStockProduct(
                    row,
                  )
                }
              >
                {t(
                  "Stock movements",
                )}
              </button>
            )}
          </div>
        )}
      />

      {editor && (
        <ProductEditor
          key={
            editor.product.id
          }
          product={
            editor.product
          }
          startEditing
          onSaved={
            editor.refresh
          }
          onClose={() =>
            setEditor(null)
          }
        />
      )}

      {stockProduct && (
        <StockDetail
          key={
            stockProduct.id
          }
          product={
            stockProduct
          }
          onClose={() =>
            setStockProduct(
              null,
            )
          }
        />
      )}
    </>
  );
}