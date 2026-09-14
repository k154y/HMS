# HMS frontend

Next.js App Router application. Run `npm ci`, configure `.env.local` using `.env.local.example`, then run `npm run dev -- -p 3001`.

- `app/(auth)`: sign-in page.
- `app/(dashboard)`: hotel operations pages and navigation.
- `app/platform`: platform administrator hotel onboarding.
- `app/api`: NextAuth handlers and authenticated Spring API proxies.
- `components/operations`: shared forms, records, ledgers and controls.
- `lib/auth.ts`: Spring login, session refresh and access validation.
- `lib/hms-api.ts`: client requests through the authenticated proxy.
- `lib/i18n.ts`: English, French and Kinyarwanda translation source.
- `types/next-auth.d.ts`: session types.

The browser does not connect directly to PostgreSQL. The old Prisma client, legacy route copies and unused translation/API helpers have been moved out of the active application into the local recovery archive.

`HMS_API_URL` is a server-side setting and defaults to `http://localhost:8081/api/v1`. Configure a private authentication secret in `.env.local`. Do not commit local credentials.

Checks: `npx tsc --noEmit`, `npm run build`, and `node ../../scripts/check-translations.cjs`.
