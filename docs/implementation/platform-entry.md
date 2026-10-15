# Multi-product entry and shared authentication — 2026-10-14

Implemented a public HotelPro landing page, separate HMS/ISDMS entry actions, shared login with validated destinations, protected-route interception, localized workspace access states, and conservative workspace navigation. Existing backend, migrations, commerce proxy, feature flags, startup scripts, and unrelated working-tree changes were preserved.

## Flow and security

- `/` is public. Product buttons lead to `/dashboard` or `/commerce`.
- The existing NextAuth integration in the Next.js proxy redirects anonymous workspace requests to `/login?next=...` before rendering. Server layouts remain protected. A client-supplied destination header is overwritten.
- Login accepts only canonical, allowlisted internal pathnames. External URLs, encoded paths, backslashes, dot segments, query strings, fragments, and authentication/API destinations fall back to `/dashboard`. Nested hotel paths are preserved; query/filter state is deliberately not preserved.
- Identity validation no longer requires hotel membership. A shared account can authenticate without receiving hotel or commerce permissions. Hotel layouts require hotel context; commerce entry retrieves the user's businesses from the backend. Existing backend membership, entitlement, branch, and platform checks remain authoritative.
- Session expiry preserves the intended pathname. Token refresh clears removed hotel context. Password controls, recovery links, CSRF handling and token rotation remain in place. Tokens are not placed in URLs.
- Commerce entry distinguishes disabled, unassigned, denied, and unavailable responses using safe translated messages. It does not create businesses or enable either commerce flag.
- Navigation offers **Choose workspace**, rather than claiming either product is authorized. The existing business-list endpoint does not establish all module entitlements; a unified product-access endpoint remains a possible follow-up.
- Commerce labels now use the existing central translation table, replacing its component-local dictionaries.

## Files created

- `apps/web/components/PlatformLanding.tsx`
- `apps/web/components/WorkspaceNavigation.tsx`
- `apps/web/components/WorkspaceAccessState.tsx`
- `apps/web/lib/navigation.ts`
- `apps/web/proxy.ts`
- `scripts/test-workspace-navigation.cjs`
- `docs/implementation/platform-entry.md`

## Existing files modified

- `apps/web/app/page.tsx`
- `apps/web/app/layout.tsx`
- `apps/web/app/(auth)/login/page.tsx`
- `apps/web/app/(dashboard)/layout.tsx`
- `apps/web/app/(dashboard)/TopBar.tsx`
- `apps/web/app/platform/page.tsx`
- `apps/web/app/security/page.tsx`
- `apps/web/app/commerce/layout.tsx`
- `apps/web/app/commerce/page.tsx`
- `apps/web/components/commerce/CommercePanel.tsx`
- `apps/web/components/SessionGuard.tsx`
- `apps/web/lib/auth.ts`
- `apps/web/lib/hms-api.ts`
- `apps/web/lib/i18n.ts`
- `scripts/test-web-errors.cjs`

The three existing commerce files above were already untracked when this request began; their existing functionality was retained. Earlier HMS consistency fixes and V33–V35 are not new changes from this request.

## Inspection

Read the modified files above, dashboard sidebar, LocaleProvider, NextAuth types, commerce API proxy, shared API/error proxy, route inventory, package/TypeScript/Next configuration, translation checker, startup/restart scripts, Spring Security configuration, CurrentUserController, and commerce foundation/catalog access checks. Verified running frontend/backend processes and ports before browser testing. No backend restart or database changes were performed.

## Verification

Executed using the existing `hotel_hms_app` container and installed dependencies:

```powershell
docker exec -w /workspace/apps/web hotel_hms_app npm run check:translations
docker exec -w /workspace/apps/web hotel_hms_app npm run typecheck
docker exec -w /workspace hotel_hms_app node --test scripts/test-workspace-navigation.cjs scripts/test-web-errors.cjs
docker exec -w /workspace/apps/web -e NEXT_OUTPUT_DIR=.next-build hotel_hms_app npm run build
```

- Translation checker: PASS, 739 keys in all three languages; 456 literal UI keys checked.
- TypeScript: PASS.
- Focused Node tests: PASS, 13 tests. Includes unsafe destinations, callback URLs, shared identity, removed hotel context, expiry, spoofed proxy headers, safe commerce states, and existing API-error handling. Authentication/backend responses in these tests are mocked, not real account sign-ins.
- Production build: PASS, including TypeScript and generation of all 34 static-page build entries. Separate `.next-build` output protected the running `.next` development output; automatic edits to generated type references and the TypeScript include list were restored afterward.
- HTTP checks against `http://localhost:3001`: `/` and `/forgot-password` return 200; `/dashboard`, `/commerce`, `/reservations/new`, and `/platform` return 307 with the matching login destination; anonymous `/api/commerce/businesses` returns 401. A spoofed `x-workspace-path` did not change the redirect.
- Browser: both product entry buttons and product indicators verified; English/French/Kinyarwanda switching verified; landing layouts inspected at 390, 768, and 1280 pixels with no horizontal overflow. An initial React development performance-timestamp error during layout redirects was addressed by moving anonymous redirects ahead of rendering into the NextAuth proxy.
- No backend files changed in this request, so Maven was not rerun for it. The preceding consistency milestone separately passed 38 tests; that result is not verification of the subsequently added commerce backend.

## Unverified and follow-up

No real-account browser sign-in, authenticated workspace navigation, cross-tenant attack test, password-reset email delivery, or password change was performed. Complete these with approved test accounts in the isolated environment before release. Commerce backend authorization was inspected and left intact; frontend tests cannot prove database isolation. This is not a production-readiness certification.

Local URLs: `http://localhost:3001/`, `http://localhost:3001/login?next=/dashboard`, `http://localhost:3001/login?next=/commerce`, `http://localhost:3001/commerce`, and `http://localhost:3001/platform`.
