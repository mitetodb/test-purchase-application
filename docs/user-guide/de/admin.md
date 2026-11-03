# Admin-Benutzerhandbuch (Deutsch)

Dieses Handbuch beschreibt die Möglichkeiten der Rolle **Administrator** in der Testkauf-Anwendung — abgestimmt auf UI und serverseitige Zugriffskontrolle.

**URL:** https://testkauf.getinweb.de

---

## Anmeldung

1. Benutzername und Passwort auf der Login-Seite eingeben.
2. **Passwortrichtlinie:** neue Passwörter mindestens **12 Zeichen** (`app.security.password.min-length`).
3. **Zwei-Faktor-Authentifizierung (Pflicht für Admin):**
   - Erster Login nach Deployment: Weiterleitung zu **`/2fa/enroll`** — QR-Code mit Authenticator-App scannen (TOTP, 6 Stellen, 30 s).
   - Code bestätigen, dann die **10 Wiederherstellungscodes** auf **`/2fa/recovery-codes`** sichern (jeder Code einmal verwendbar).
   - Spätere Logins: Passwort → **`/2fa/verify`** (TOTP oder Wiederherstellungscode).
4. Nach erfolgreicher 2FA gelangen Sie zum Dashboard (`/dashboard`).

**Passwort ändern:** Profilmenü → **Profile**. **Sprache:** Globus oder `?lang=de|en|bg`.

---

## Navigation (Menü Admin)

| Eintrag | Pfad | Zweck |
|---------|------|--------|
| Users | `/users` | Anlegen, bearbeiten, deaktivieren/reaktivieren, entsperren |
| Audit log | `/audit` | Filterbare Historie sensibler Änderungen |
| Test purchases | `/testpurchases` | Liste, Filter, Export, Archivierung |
| Customers | `/customers` | Kunden, Preise, deaktivieren/reaktivieren |
| Shops | `/shops` | Shops pflegen, deaktivieren/reaktivieren |

---

## Wichtige Funktionen

- **Benutzer:** Paginierung, Filter, CSV/Excel-Export; **Deaktivieren** statt Löschen; **Unlock** bei Login-Sperre.
- **Testkäufe:** voller Zugriff; Mystery Shopper zuweisen; Status ändern; **Archivieren** (Status ARCHIVED, nur Lesen); kein Hard-Delete (alte Delete-URLs → **404**).
- **Anhänge:** Upload mit Magic-Byte-Prüfung; **kein Löschen** (Endpoint 404), max. 10 MB standardmäßig.
- **Invites:** `/invites` — Einladungslinks für Mystery Shopper (7 Tage, Rolle MYSTERY_SHOPPER).
- **Audit:** `/audit` mit Filtern.

Betrieb und 2FA-Notfall: [ADMIN-OPS.md](../ADMIN-OPS.md). Matrix: [ROLES.md](../ROLES.md).
