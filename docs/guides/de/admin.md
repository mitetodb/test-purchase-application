# Administrator-Benutzerhandbuch

Willkommen bei der Testkauf-Anwendung! Dieses Handbuch ist für **Administratoren** mit vollem Systemzugriff.

## Inhaltsverzeichnis

- [Erste Schritte](#erste-schritte)
- [Dashboard](#dashboard)
- [Benutzerverwaltung](#benutzerverwaltung)
- [Kundenverwaltung](#kundenverwaltung)
- [Geschäftsverwaltung](#geschäftsverwaltung)
- [Testkaufverwaltung](#testkaufverwaltung)
- [Systemadministration](#systemadministration)
- [Sicherheits-Best-Practices](#sicherheits-best-practices)

## Erste Schritte

### Anmeldung

1. Navigieren Sie zu https://testkauf.getinweb.de
2. Geben Sie Ihren Benutzernamen ein: `admin`
3. Geben Sie Ihr Passwort ein (bei der Bereitstellung festgelegt)
4. Klicken Sie auf **Anmelden**

**Wichtig:** Ändern Sie Ihr Passwort sofort nach der ersten Anmeldung!

### Passwort ändern

1. Klicken Sie oben rechts auf Ihren Benutzernamen
2. Wählen Sie **Profil**
3. Klicken Sie auf **Passwort ändern**
4. Geben Sie Ihr aktuelles Passwort ein
5. Geben Sie Ihr neues Passwort ein (verwenden Sie ein sicheres Passwort!)
6. Bestätigen Sie Ihr neues Passwort
7. Klicken Sie auf **Speichern**

### Ihr Profil

Aktualisieren Sie Ihre Profilinformationen:
1. Klicken Sie auf Ihren Benutzernamen → **Profil**
2. Aktualisieren Sie E-Mail, Land oder Sprache
3. Laden Sie ein Profilbild hoch (optional)
4. Klicken Sie auf **Speichern**

## Dashboard

Nach der Anmeldung sehen Sie das Haupt-Dashboard mit:

- **Statistiken**: Gesamtzahl der Benutzer, Kunden, Geschäfte, Testkäufe
- **Letzte Aktivität**: Neueste Testkäufe und Statusänderungen
- **Schnellaktionen**: Häufige Aufgaben
- **Benachrichtigungen**: Wichtige Systemwarnungen

## Benutzerverwaltung

Als Administrator haben Sie volle Kontrolle über alle Benutzer im System.

### Benutzer anzeigen

1. Klicken Sie im Navigationsmenü auf **Benutzer**
2. Sehen Sie die Liste aller Benutzer mit ihren Rollen und Status
3. Verwenden Sie die Suchleiste, um bestimmte Benutzer zu finden
4. Filtern Sie nach Rolle oder Status

### Neuen Benutzer erstellen

1. Gehen Sie zu **Benutzer** → **Benutzer erstellen**
2. Füllen Sie die erforderlichen Felder aus:
   - Benutzername (eindeutig)
   - E-Mail (eindeutig)
   - Passwort (starkes Passwort erforderlich)
   - Rolle: Administrator, Kundenbetreuer, Vertriebsleiter, Kunde oder Mystery Shopper
   - Land
   - Sprachpräferenz
3. Klicken Sie auf **Erstellen**

**Rollen:**
- **Administrator**: Vollständiger Systemzugriff (wie Sie!)
- **Kundenbetreuer**: Verwaltung zugewiesener Kunden und Testkäufe, Mystery Shopper zuweisen
- **Vertriebsleiter**: Käufe eigener Kunden anzeigen und Berichte erstellen (keine Shopper-Namen sichtbar)
- **Kunde**: Testkäufe des eigenen Unternehmens erstellen und verfolgen
- **Mystery Shopper**: Zugewiesene Testkäufe durchführen

**Mystery Shopper einladen:**
Mystery Shopper können sich nicht selbst registrieren. Administratoren und Kundenbetreuer laden sie über einmalige Einladungslinks ein, die ablaufen. Nur Administratoren können Benutzer mit den Rollen Administrator, Kundenbetreuer, Vertriebsleiter oder Kunde erstellen.

### Benutzer bearbeiten

1. Gehen Sie zu **Benutzer** → Benutzer finden → Klicken Sie auf **Bearbeiten**
2. Aktualisieren Sie beliebige Felder außer Benutzername
3. Sie können ihre Rolle ändern
4. Klicken Sie auf **Speichern**

### Benutzer deaktivieren

Deaktivierte Benutzer können sich nicht anmelden, aber ihre Daten bleiben erhalten.

1. Gehen Sie zu **Benutzer** → Benutzer finden → Klicken Sie auf **Bearbeiten**
2. Deaktivieren Sie **Aktiv**
3. Klicken Sie auf **Speichern**

Zum Reaktivieren: Aktivieren Sie **Aktiv** und speichern Sie.

### Benutzer löschen (dauerhaft)

**Warnung:** Dies entfernt den Benutzer dauerhaft aus der Datenbank. Verwenden Sie nach Möglichkeit Deaktivierung.

1. Gehen Sie zu **Benutzer** → Benutzer finden
2. Klicken Sie auf **Löschen**
3. Bestätigen Sie die Aktion

**Hinweis:** Nur Sie (Administrator) können Daten dauerhaft löschen.

## Kundenverwaltung

Kunden sind Organisationen oder Einzelpersonen, die Testkäufe anfordern.

### Kunden anzeigen

1. Klicken Sie im Navigationsmenü auf **Kunden**
2. Sehen Sie alle Kunden mit ihren Details
3. Suchen Sie nach Name, E-Mail oder USt-IdNr.

### Kunden erstellen

1. Gehen Sie zu **Kunden** → **Kunde erstellen**
2. Füllen Sie die Details aus:
   - Name (erforderlich)
   - E-Mail
   - Telefon
   - USt-IdNr.
   - Adresse
   - Land
3. Weisen Sie einen Kundenbetreuer zu (optional)
4. Klicken Sie auf **Erstellen**

*(Der Rest des Handbuchs folgt der Struktur der englischen Version)*

## Mehrsprachige Unterstützung

Die Anwendung unterstützt drei Sprachen:
- **Bulgarisch** (bg)
- **Englisch** (en)
- **Deutsch** (de)

### Sprache wechseln

1. Klicken Sie auf das Globus-Symbol in der Navigationsleiste
2. Wählen Sie Ihre bevorzugte Sprache
3. Die Seite wird in dieser Sprache neu geladen

Oder fügen Sie `?lang=de` zu einer beliebigen URL hinzu.

## Support

Bei Problemen oder Fragen:
- **Technische Probleme**: Siehe [DEPLOYMENT.md](../../DEPLOYMENT.md)
- **Sicherheitsbedenken**: Siehe [SECURITY.md](../../SECURITY.md)
- **Entwicklung**: Siehe [CONTRIBUTING.md](../../CONTRIBUTING.md)
- **GitHub Issues**: [test-purchase-application/issues](https://github.com/mitetodb/test-purchase-application/issues)

## Tastenkombinationen

- `Alt + H`: Startseite/Dashboard
- `Alt + U`: Benutzer
- `Alt + C`: Kunden
- `Alt + S`: Geschäfte
- `Alt + T`: Testkäufe
- `Alt + P`: Profil

Vielen Dank für die Wartung der Testkauf-Anwendung!
