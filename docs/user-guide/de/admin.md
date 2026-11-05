# Admin-Benutzerhandbuch (Deutsch)

Dieses Handbuch beschreibt die Möglichkeiten der Rolle **Administrator** in der Testkauf-Anwendung — abgestimmt auf UI und serverseitige Zugriffskontrolle.

**URL:** https://testkauf.getinweb.de

---

## Anmeldung

1. Benutzername und Passwort auf der Login-Seite eingeben.
2. **Passwortrichtlinie:** neue Passwörter mindestens **12 Zeichen** (`app.security.password.min-length`).
3. **TOTP-2FA** ist nur für **Admin** und **Account Manager** Pflicht (nicht für Sales Manager, Customer oder Mystery Shopper). Als Administrator nach erfolgreichem Passwort:
   - **Erster Login (noch nicht enrolled):** **`/2fa/enroll`** — QR-Code, Bestätigung, dann **alle Wiederherstellungscodes** auf **`/2fa/recovery-codes`** speichern.
   - **Bereits enrolled:** **`/2fa/verify`** — TOTP oder Wiederherstellungscode.
4. Nach erfolgreichem Login (und 2FA falls nötig) Weiterleitung zu **`/home`** (nicht `/dashboard`).

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
| Invoices | `/invoices` | Rechnungen, Monatsabrechnung, PDF |

**Score trends:** `/score-trends` — aggregierte Fragebogen-Scores.

---

## Wichtige Funktionen

- **Benutzer:** Paginierung, Filter, CSV/Excel-Export; **Deaktivieren** statt Löschen; **Unlock** bei Login-Sperre.
- **Testkäufe:** voller Zugriff; **Frist (deadline)**; **Shop-Termin**; **Fragebogen**; **PDF-Bericht** (`/testpurchases/view/{id}/report.pdf`); Mystery Shopper zuweisen; Status ändern; **Archivieren** (Status ARCHIVED, nur Lesen); kein Hard-Delete (alte Delete-URLs → **404**).
- **Anhänge:** Upload mit Magic-Byte-Prüfung; **kein Löschen** (Endpoint 404), max. 10 MB standardmäßig.
- **Invites:** `/invites` — Einladungslinks für Mystery Shopper (7 Tage, Rolle MYSTERY_SHOPPER).
- **Audit:** `/audit` mit Filtern (auch Rechnungen, Profil/2FA).
- **Rechnungen:** `/invoices` — Monatsabrechnung, Entwurf → ausgestellt → bezahlt / storniert; PDF-Download.

E-Mail für Fristen, Termine und Monitoring standardmäßig **aus** — siehe [STATUS.md](../STATUS.md).

Betrieb und 2FA-Notfall: [ADMIN-OPS.md](../ADMIN-OPS.md). Matrix: [ROLES.md](../ROLES.md).
