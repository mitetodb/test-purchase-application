# Testkäufer-Handbuch (Mystery Shopper, Deutsch)

Handbuch für **Mystery Shopper** — durchführung zugewiesener Testkäufe.

**URL:** https://testkauf.getinweb.de

---

## Beitritt und Login

1. **Admin** oder **Account Manager** sendet Einladungslink (`/register?token=…`).
2. Registrierung mit Passwort (**min. 12 Zeichen**). Rolle immer **Mystery Shopper**.
3. Login unter `/login` — nach erfolgreichem Passwort → **`/home`**.

**TOTP-2FA** ist nur für **Admin** und **Account Manager** Pflicht, nicht für Mystery Shopper (auch nicht für Customer / Sales Manager).

**Profil:** `/profile`. **Sprache:** `?lang=de|en|bg`.

---

## Sichtbarkeit

- **Test purchases** — nur **Ihnen zugewiesene** Käufe.
- Kein Zugriff auf `/shops`-Verwaltung, Kunden- oder Benutzerlisten.
- Kunden sehen **Ihren Namen nicht**.

---

## Ablauf

1. Kauf in der Liste öffnen.
2. Shop, Positionen und Anweisungen prüfen.
3. **Status** anpassen (z. B. INITIALISED, IN_PROGRESS, PRODUCT_ORDERED, PRODUCT_RECEIVED, Weiterleitung/Rücksendung, CLOSED, CANCELLED — nach Vorgabe des Account Managers).
4. Optional **Kommentar** bei Statuswechsel.
5. **Anhänge hochladen** (Allowlist + Magic Bytes, Standard max. 10 MB).
6. **Download** möglich; **Löschen** nicht (404).

Status **ARCHIVED** = nur Lesen.

[ROLES.md](../ROLES.md)
