# Kundenhandbuch (Client, Deutsch)

Handbuch für die Rolle **Customer** — Unternehmen, die **Testkäufe** für die eigene Firma anfordern und verfolgen.

**URL:** https://testkauf.getinweb.de

---

## Anmeldung

1. Zugangsdaten vom Administrator (keine öffentliche Kunden-Registrierung).
2. Passwort mindestens **12 Zeichen**.
3. **Keine 2FA** für Customer (TOTP nur für **Admin** und **Account Manager**).
4. Nach erfolgreichem Passwort → **`/home`**.
5. **Profil:** `/profile` — E-Mail, Land, Sprache, Avatar.

**Sprache:** Globus oder `?lang=de|en|bg`.

---

## Umfang

Sie sehen **nur Testkäufe Ihres Unternehmens**.

**Erlaubt:**

- Testkäufe **anlegen** (aktiver Kunde + aktiver Shop, optional **Frist**)
- **Rechnungen** (nur eigene Firma) und **Score trends** (`/score-trends`) öffnen
- **Positionen** bearbeiten, solange der Kauf editierbar ist
- **Status** und **Historie** lesen
- **Preisvorschau**

**Nicht erlaubt:**

- Andere Firmen oder Mystery-Shopper-Zuweisung
- **Name** des Mystery Shoppers sehen
- `/shops` oder Kundenverwaltung
- **Anhänge hochladen** (Upload nur Admin, Account Manager, Mystery Shopper); **Download** möglich

---

## Erstellung und Verfolgung

Neuer Testkauf über **Test purchases**; Statussteuerung durch Account Manager. **PDF-Bericht**, **Frist**, Fragebogen-Ergebnis (Lesen), **Rechnungen** (Lesen/PDF). **ARCHIVED** = nur Lesen.

[ROLES.md](../ROLES.md)
