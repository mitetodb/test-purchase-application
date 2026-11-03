# Account-Manager-Handbuch (Deutsch)

Handbuch für **Account Manager** — betreute Kunden, Shops, Testkäufe und Einladungen für Testkäufer.

**URL:** https://testkauf.getinweb.de

---

## Anmeldung

1. Login unter `/login`.
2. Passwort mindestens **12 Zeichen** bei Neuanlage/Änderung.
3. **TOTP-2FA** ist nur für **Admin** und **Account Manager** Pflicht. Beim **ersten Login** als Account Manager nach dem Passwort → **`/2fa/enroll`**, Codes auf **`/2fa/recovery-codes`** speichern; danach **`/2fa/verify`**. Neue Code-Sätze **invalidieren** unbenutzte alte Codes (Zeilen bleiben in der DB).
4. Nach erfolgreichem Login und 2FA → **`/home`**.
5. Telefon verloren → Wiederherstellungscode auf `/2fa/verify`. **Unlock** nur durch Admin unter Users.

**Profil:** `/profile`. **Sprache:** `?lang=de|en|bg`.

---

## Zugriff (Menü Management)

| Bereich | Umfang |
|---------|--------|
| Test purchases | Nur **betreute** Kunden |
| Customers | Anlegen (automatisch zugeordnet), betreute bearbeiten |
| Shops | Shops anlegen und pflegen |
| Invites | Mystery-Shopper-Einladungen |

Kein Zugriff auf **Users** oder **Audit log**.

---

## Tätigkeiten

- **Kunden:** anlegen/bearbeiten; **Deaktivieren** nur über Admin-UI.
- **Shops:** pflegen; Deaktivieren/Reaktivieren nur Admin.
- **Testkäufe:** anlegen/bearbeiten; **Mystery Shopper zuweisen**; **Status** ändern; **Anhänge hochladen**; Listen mit Filter, Paginierung, Export. **Archivieren** nur Admin.
- **Invites:** Link `/register?token=…` (7 Tage, einmalig).

Kunden und Sales Manager sehen **keine** Mystery-Shopper-Namen.

[ROLES.md](../ROLES.md)
