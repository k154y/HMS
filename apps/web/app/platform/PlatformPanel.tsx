"use client";

import {
  FormEvent,
  useEffect,
  useState,
} from "react";

import { signOut } from "next-auth/react";

import { useLocale } from "@/components/LocaleProvider";

type HotelListItem = {
  id: string;
  code: string;
  name: string;
  status: string;
};

type ApiFieldError = {
  field: string;
  message: string;
};

type ApiErrorResponse = {
  code?: string;
  message?: string;
  fieldErrors?: ApiFieldError[];
};

type FormField =
  | "hotelName"
  | "code"
  | "fullName"
  | "email"
  | "password"
  | "language";

type FieldErrors = Partial<
  Record<FormField, string>
>;

const fieldOrder: FormField[] = [
  "hotelName",
  "code",
  "fullName",
  "email",
  "password",
  "language",
];

const backendFieldToFormField: Record<
  string,
  FormField
> = {
  email: "email",
  password: "password",
  fullName: "fullName",
  preferredLanguage: "language",

  "hotel.code": "code",
  "hotel.legalName": "hotelName",
  "hotel.displayName": "hotelName",
  "hotel.defaultLanguage": "language",
};

function focusFirstInvalidField(
  form: HTMLFormElement,
  errors: FieldErrors
) {
  const field =
    fieldOrder.find(
      (candidate) => Boolean(errors[candidate])
    );

  if (!field) {
    return;
  }

  requestAnimationFrame(() => {
    const element =
      form.elements.namedItem(field);

    if (
      element instanceof
        HTMLInputElement
      || element instanceof
        HTMLSelectElement
    ) {
      element.focus();
    }
  });
}

function codePointLength(
  value: string
) {
  return [...value].length;
}

function validateClientForm(
  form: HTMLFormElement,
  values: {
    hotelName: string;
    code: string;
    fullName: string;
    email: string;
    password: string;
    language: string;
  }
): FieldErrors {

  const errors: FieldErrors = {};

  const hotelName =
    values.hotelName.trim();

  const code =
    values.code.trim();

  const fullName =
    values.fullName.trim();

  const email =
    values.email.trim();

  if (!hotelName) {
    errors.hotelName =
      "Hotel name is required.";
  } else if (
    codePointLength(hotelName) > 200
  ) {
    errors.hotelName =
      "Hotel name must contain no more than 200 characters.";
  }

  if (!code) {
    errors.code =
      "Hotel code is required.";
  } else if (
    codePointLength(code) > 50
  ) {
    errors.code =
      "Hotel code must contain no more than 50 characters.";
  }

  if (!fullName) {
    errors.fullName =
      "Owner full name is required.";
  } else if (
    codePointLength(fullName) > 200
  ) {
    errors.fullName =
      "Owner full name must contain no more than 200 characters.";
  }

  if (!email) {
    errors.email =
      "Owner email is required.";
  } else {
    const emailInput =
      form.elements.namedItem(
        "email"
      );

    if (
      emailInput instanceof
        HTMLInputElement
      && emailInput.validity.typeMismatch
    ) {
      errors.email =
        "Enter a valid owner email address.";
    } else if (
      codePointLength(email) > 255
    ) {
      errors.email =
        "Owner email must contain no more than 255 characters.";
    }
  }

  if (!values.password) {
    errors.password =
      "Password is required.";
  } else if (
    values.password.trim().length === 0
  ) {
    errors.password =
      "Password cannot contain only whitespace.";
  } else if (
    codePointLength(
      values.password
    ) < 15
  ) {
    errors.password =
      "Password must contain at least 15 characters.";
  } else if (
    codePointLength(
      values.password
    ) > 128
  ) {
    errors.password =
      "Password must contain no more than 128 characters.";
  }

  if (
    !["en", "fr", "rw"].includes(
      values.language
    )
  ) {
    errors.language =
      "Select English, French, or Kinyarwanda.";
  }

  return errors;
}

function mapBackendFieldErrors(
  data: ApiErrorResponse
): FieldErrors {

  const errors: FieldErrors = {};

  for (
    const error
    of data.fieldErrors ?? []
  ) {
    const field =
      backendFieldToFormField[
        error.field
      ];

    if (
      field
      && !errors[field]
    ) {
      errors[field] =
        error.message;
    }
  }

  /*
   * Current backend business conflicts use
   * STATE_CONFLICT with a safe message.
   *
   * Associate the known conflicts with
   * the field the administrator needs
   * to correct.
   */
  const message =
    data.message ?? "";

  const normalized =
    message.toLowerCase();

  if (
    normalized.includes(
      "user account with this email"
    )
  ) {
    errors.email = message;
  }

  if (
    normalized.includes(
      "hotel with code"
    )
  ) {
    errors.code = message;
  }

  return errors;
}

function FieldMessage({
  id,
  message,
}: {
  id: string;
  message?: string;
}) {
  if (!message) {
    return null;
  }

  return (
    <span
      id={id}
      role="alert"
      className="mt-1 block text-sm font-normal text-red-700"
    >
      {message}
    </span>
  );
}

export default function PlatformPanel() {

  const {
    t,
    locale,
    setLocale,
  } = useLocale();

  const [hotels, setHotels] =
    useState<HotelListItem[]>([]);

  const [error, setError] =
    useState("");

  const [success, setSuccess] =
    useState("");

  const [busy, setBusy] =
    useState(false);

  const [
    fieldErrors,
    setFieldErrors,
  ] = useState<FieldErrors>({});

  async function load() {

    const response =
      await fetch(
        "/api/platform/hotels",
        {
          cache: "no-store",
        }
      );

    const data =
      await response
        .json()
        .catch(() => null);

    if (!response.ok) {
      throw new Error(
        data?.message
          ?? "Unable to load hotels."
      );
    }

    setHotels(
      Array.isArray(data)
        ? data
        : []
    );
  }

  useEffect(() => {

    load().catch(
      (loadError) => {

        setError(
          loadError instanceof Error
            ? loadError.message
            : "Unable to load hotels."
        );
      }
    );
  }, []);

  async function submit(
    event:
      FormEvent<HTMLFormElement>
  ) {

    event.preventDefault();

    const form =
      event.currentTarget;

    const formData =
      new FormData(form);

    const value =
      (key: string) =>
        String(
          formData.get(key) ?? ""
        );

    const values = {
      hotelName:
        value("hotelName"),
      code:
        value("code"),
      fullName:
        value("fullName"),
      email:
        value("email"),
      password:
        value("password"),
      language:
        value("language"),
    };

    setError("");
    setSuccess("");
    setFieldErrors({});

    const clientErrors =
      validateClientForm(
        form,
        values
      );

    if (
      Object.keys(
        clientErrors
      ).length > 0
    ) {

      setFieldErrors(
        clientErrors
      );

      setError(
        "Please correct the highlighted fields."
      );

      focusFirstInvalidField(
        form,
        clientErrors
      );

      return;
    }

    setBusy(true);

    try {

      const response =
        await fetch(
          "/api/platform/hotels",
          {
            method: "POST",

            headers: {
              "Content-Type":
                "application/json",
            },

            body: JSON.stringify({
              email:
                values.email.trim(),

              password:
                values.password,

              fullName:
                values.fullName.trim(),

              preferredLanguage:
                values.language,

              hotel: {
                code:
                  values.code
                    .trim()
                    .toUpperCase(),

                legalName:
                  values.hotelName
                    .trim(),

                displayName:
                  values.hotelName
                    .trim(),

                currencyCode:
                  "RWF",

                timezone:
                  "Africa/Kigali",

                defaultLanguage:
                  values.language,
              },
            }),
          }
        );

      const data:
        ApiErrorResponse | unknown =
          await response
            .json()
            .catch(() => ({}));

      if (!response.ok) {

        const apiError =
          data as ApiErrorResponse;

        const mappedErrors =
          mapBackendFieldErrors(
            apiError
          );

        setFieldErrors(
          mappedErrors
        );

        if (
          Object.keys(
            mappedErrors
          ).length > 0
        ) {
          setError(
            apiError.message
              ?? "Please correct the highlighted fields."
          );

          focusFirstInvalidField(
            form,
            mappedErrors
          );
        } else {
          setError(
            apiError.message
              ?? "Unable to create the hotel. Please review the information and try again."
          );
        }

        return;
      }

      setSuccess(
        "Hotel, owner account, three-month trial, and main branch created successfully. The owner can now sign in with the email and password you entered."
      );

      setFieldErrors({});

      form.reset();

      await load();

    } catch {

      setError(
        "The hotel could not be created because the service is currently unavailable. Please try again."
      );

    } finally {

      setBusy(false);
    }
  }

  const inputClass =
    (
      field: FormField
    ) =>
      [
        "block w-full rounded-lg border p-3 mt-1 outline-none transition",
        fieldErrors[field]
          ? "border-red-500 focus:border-red-600 focus:ring-2 focus:ring-red-100"
          : "border-slate-300 focus:border-blue-500 focus:ring-2 focus:ring-blue-100",
      ].join(" ");

  return (
    <main className="min-h-screen bg-slate-50 p-6 text-slate-900 md:p-12">

      <div className="mx-auto max-w-5xl">

        <select
          aria-label={t("Language")}
          value={locale}
          onChange={(event) =>
            setLocale(
              event.target.value as
                | "en"
                | "fr"
                | "rw"
            )
          }
          className="mb-4 rounded border p-2"
        >
          <option value="en">
            English
          </option>

          <option value="fr">
            Français
          </option>

          <option value="rw">
            Kinyarwanda
          </option>
        </select>

        <header className="mb-8 flex items-center justify-between gap-4">

          <div>
            <p className="font-semibold text-blue-700">
              HotelPro ·{" "}
              {t(
                "Platform administration"
              )}
            </p>

            <h1 className="mt-2 text-3xl font-bold">
              {t(
                "Hotels and owners"
              )}
            </h1>
          </div>

          <button
            type="button"
            onClick={() =>
              signOut({
                callbackUrl:
                  "/login",
              })
            }
            className="rounded-lg border px-4 py-2"
          >
            {t("Sign out")}
          </button>
        </header>

        {error && (
          <div
            role="alert"
            aria-live="assertive"
            className="mb-4 rounded-lg border border-red-200 bg-red-50 p-4 text-red-800"
          >
            {t(error)}
          </div>
        )}

        {success && (
          <div
            role="status"
            aria-live="polite"
            className="mb-4 rounded-lg border border-green-200 bg-green-50 p-4 text-green-800"
          >
            {t(success)}
          </div>
        )}

        <section className="mb-8 rounded-2xl border bg-white p-6">

          <h2 className="mb-1 text-xl font-semibold">
            {t(
              "Create a hotel and owner account"
            )}
          </h2>

          <p className="mb-6 text-sm text-slate-600">
            {t(
              "Creating the hotel starts its three-month trial and creates the first hotel owner account."
            )}
          </p>

          <form
            onSubmit={submit}
            noValidate
            className="grid gap-4 md:grid-cols-2"
          >

            <label className="text-sm font-medium">

              {t("Hotel name")}

              <input
                name="hotelName"
                type="text"
                maxLength={200}
                autoComplete="organization"
                aria-invalid={
                  Boolean(
                    fieldErrors.hotelName
                  )
                }
                aria-describedby={
                  fieldErrors.hotelName
                    ? "hotelName-error"
                    : undefined
                }
                className={inputClass(
                  "hotelName"
                )}
              />

              <FieldMessage
                id="hotelName-error"
                message={
                  fieldErrors.hotelName
                }
              />
            </label>

            <label className="text-sm font-medium">

              {t(
                "Unique hotel code"
              )}

              <input
                name="code"
                type="text"
                maxLength={50}
                autoComplete="off"
                aria-invalid={
                  Boolean(
                    fieldErrors.code
                  )
                }
                aria-describedby={
                  fieldErrors.code
                    ? "code-error"
                    : undefined
                }
                className={inputClass(
                  "code"
                )}
              />

              <FieldMessage
                id="code-error"
                message={
                  fieldErrors.code
                }
              />
            </label>

            <label className="text-sm font-medium">

              {t(
                "Owner full name"
              )}

              <input
                name="fullName"
                type="text"
                maxLength={200}
                autoComplete="name"
                aria-invalid={
                  Boolean(
                    fieldErrors.fullName
                  )
                }
                aria-describedby={
                  fieldErrors.fullName
                    ? "fullName-error"
                    : undefined
                }
                className={inputClass(
                  "fullName"
                )}
              />

              <FieldMessage
                id="fullName-error"
                message={
                  fieldErrors.fullName
                }
              />
            </label>

            <label className="text-sm font-medium">

              {t("Owner email")}

              <input
                name="email"
                type="email"
                maxLength={255}
                autoComplete="email"
                aria-invalid={
                  Boolean(
                    fieldErrors.email
                  )
                }
                aria-describedby={
                  fieldErrors.email
                    ? "email-error"
                    : undefined
                }
                className={inputClass(
                  "email"
                )}
              />

              <FieldMessage
                id="email-error"
                message={
                  fieldErrors.email
                }
              />
            </label>

            <label className="text-sm font-medium">

              {t(
                "Initial password (at least 15 characters)"
              )}

              <input
                name="password"
                type="password"
                minLength={15}
                maxLength={128}
                autoComplete="new-password"
                aria-invalid={
                  Boolean(
                    fieldErrors.password
                  )
                }
                aria-describedby={
                  fieldErrors.password
                    ? "password-error"
                    : undefined
                }
                className={inputClass(
                  "password"
                )}
              />

              <FieldMessage
                id="password-error"
                message={
                  fieldErrors.password
                }
              />
            </label>

            <label className="text-sm font-medium">

              {t("Language")}

              <select
                name="language"
                defaultValue="en"
                aria-invalid={
                  Boolean(
                    fieldErrors.language
                  )
                }
                aria-describedby={
                  fieldErrors.language
                    ? "language-error"
                    : undefined
                }
                className={inputClass(
                  "language"
                )}
              >
                <option value="en">
                  English
                </option>

                <option value="fr">
                  Français
                </option>

                <option value="rw">
                  Kinyarwanda
                </option>
              </select>

              <FieldMessage
                id="language-error"
                message={
                  fieldErrors.language
                }
              />
            </label>

            <div className="md:col-span-2">

              <button
                type="submit"
                disabled={busy}
                className="rounded-lg bg-blue-600 px-6 py-3 font-medium text-white disabled:cursor-not-allowed disabled:opacity-50"
              >
                {t(
                  busy
                    ? "Creating…"
                    : "Create hotel and owner"
                )}
              </button>

            </div>
          </form>
        </section>

        <section className="rounded-2xl border bg-white p-6">

          <h2 className="mb-4 text-xl font-semibold">
            {t(
              "Registered hotels"
            )}
          </h2>

          {hotels.length === 0 ? (

            <p>
              {t(
                "No hotels registered."
              )}
            </p>

          ) : (

            <div className="overflow-x-auto">

              <table className="w-full text-left">

                <thead>
                  <tr>
                    <th className="p-3">
                      {t("Hotel")}
                    </th>

                    <th className="p-3">
                      {t("Code")}
                    </th>

                    <th className="p-3">
                      {t("Status")}
                    </th>
                  </tr>
                </thead>

                <tbody>
                  {hotels.map(
                    (hotel) => (
                      <tr
                        key={hotel.id}
                        className="border-t"
                      >
                        <td className="p-3">
                          {hotel.name}
                        </td>

                        <td className="p-3">
                          {hotel.code}
                        </td>

                        <td className="p-3">
                          {t(
                            hotel.status
                          )}
                        </td>
                      </tr>
                    )
                  )}
                </tbody>
              </table>

            </div>
          )}
        </section>
      </div>
    </main>
  );
}