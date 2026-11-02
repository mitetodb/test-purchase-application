# Manual check: admin Deactivate ends live sessions

Use this when you want to confirm on a prod-like environment that clicking **Deactivate** on the users list immediately logs the target user out (via `SessionRegistry.expireNow()`), not only on their next DB-backed inactive check.

## Prerequisites

- Application running with a real servlet session and Spring Security session registry (default in this project).
- A **temporary demo admin** account (create in UI or DB; deactivate when finished via **Deactivate** on the users list).
- A **demo target user** who can log in (any non-admin role is fine).

## Steps

1. Open two separate browsers (or one normal window and one private/incognito window) so each side has its own cookie jar.
2. **Target user:** open `/login`, sign in with the demo user, and confirm `GET /home` loads (dashboard/home page).
3. **Admin:** sign in as the demo admin, open `/users`, find the demo user, and click **Deactivate** (POST with CSRF — same as the inline form on the list).
4. **Target user (without signing out):** refresh or navigate to `/home`.
   - Expected: **302** redirect to `/login` (or login page content).
5. Optional: in browser devtools, note the old `JSESSIONID` cookie before deactivation; after step 4, requests that still send that cookie must **not** return `/home` with 200.

## Automated equivalent

CI runs `AdminDeactivateLiveSessionIT`, which uses two real `java.net.http.HttpClient` instances (separate cookie jars), form login with CSRF, asserts `SessionRegistry.getAllSessions(principal, false)` is non-empty before deactivation, then (with `includeExpiredSessions=true`) that those entries are expired after admin POST `/users/deactivate/{id}`, and that `getAllSessions(principal, false)` is empty afterward.

```bash
./mvnw -B test -Dtest=AdminDeactivateLiveSessionIT
```

Full pipeline: `scripts/ci.sh`.

## Cleanup

On `/users`, click **Deactivate** for the temporary demo admin and for the demo target user if you no longer need them active. Users are never deleted—only deactivated. Use **Reactivate** on the same list if you need an account active again.
