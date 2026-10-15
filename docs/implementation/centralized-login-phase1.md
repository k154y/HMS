# Centralized login: Phase 1

## Implemented
- Public login routes: /hms/login and /commerce/login. Internal entry: /admin/login.
- SharedLogin uses the existing NextAuth Credentials provider and Spring identity/password/session infrastructure.
- ProductLoginEntry validates existing sessions through fresh server requests. HMS requires the existing server-selected hotel, branch and role. Commerce requires active authorized business membership and entitlements. Platform entry requires the existing SUPER_ADMIN privilege from the backend profile.
- Protected HMS and platform pages refresh their authorization checks. Existing backend membership, tenant, branch and platform-administrator guards remain unchanged.
- Generic /login redirects to /. Landing links only to the two public products. Choose Workspace links are replaced with return-to-platform navigation; login never switches products automatically.
- Single Commerce business opens automatically. Multiple businesses restore a last selection only when it remains in the server-authorized list; otherwise use the existing selector. Selection storage is not authorization.
- Password recovery, forced password changes, sign-out and session-expiration links retain product context. Reset links without a product hint or same-tab recovery context return to the public landing page.
- Removed the requested disclaimer from both sales and purchase invoice output.

## Files created
- apps/web/app/hms/login/page.tsx
- apps/web/app/commerce/login/page.tsx
- apps/web/app/admin/login/page.tsx
- apps/web/components/SharedLogin.tsx
- apps/web/components/ProductLoginEntry.tsx
- apps/web/lib/product-access.ts
- docs/implementation/centralized-login-phase1.md

## Existing files modified in this task
- apps/web/lib/navigation.ts
- apps/web/lib/auth.ts
- apps/web/lib/hms-api.ts
- apps/web/proxy.ts
- apps/web/components/PlatformLanding.tsx
- apps/web/components/WorkspaceNavigation.tsx
- apps/web/components/SessionGuard.tsx
- apps/web/components/operations/PasswordForm.tsx
- apps/web/components/commerce/CommercePanel.tsx
- apps/web/components/commerce/CommerceInvoice.tsx
- apps/web/app/(auth)/login/page.tsx
- apps/web/app/(auth)/forgot-password/page.tsx
- apps/web/app/(auth)/reset-password/page.tsx
- apps/web/app/(dashboard)/layout.tsx
- apps/web/app/(dashboard)/TopBar.tsx
- apps/web/app/(dashboard)/settings/page.tsx
- apps/web/app/commerce/layout.tsx
- apps/web/app/commerce/page.tsx
- apps/web/app/platform/page.tsx
- apps/web/app/platform/PlatformPanel.tsx
- apps/web/app/platform/organizations/page.tsx
- apps/web/app/platform/organizations/OrganizationPanel.tsx
- apps/web/app/security/page.tsx
- scripts/test-workspace-navigation.cjs
- scripts/test-web-errors.cjs
- scripts/test-commerce-review.cjs

## Verification
- docker exec -w /workspace/apps/web hotel_hms_app npx --no-install tsc --noEmit: passed.
- docker exec -w /workspace hotel_hms_app node --test scripts/test-workspace-navigation.cjs scripts/test-commerce-review.cjs scripts/test-web-errors.cjs: 29 passed, 0 failed.
- Production build: cancelled when account usage reached the user-requested 10% remaining threshold. Full production build remains unverified.
- Backend authentication code and migrations were not changed; backend tests were not rerun for this task.

## Manual acceptance
Test the three login routes, wrong-product accounts, multi-product accounts, already-authenticated sessions, expired sessions, password reset links in the same and a new browser tab, business/branch selection, and sign-out. HMS retains its existing backend-selected hotel/branch behavior; no new hotel-switching mechanism was introduced. No browser testing, application startup/restart, production configuration, MFA or administration subdomain work was performed.
