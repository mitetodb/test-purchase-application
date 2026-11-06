# Projektstatus (Kurzfassung, Deutsch)

Vollständige Liste auf Englisch: [../STATUS.md](../STATUS.md).

**Live auf `master` (PR #75–#81):** PDF-Bericht pro Testkauf, Fristen, Shop-Besuchstermin, Fragebogen mit Score, Score-Trends (`/score-trends`), Rechnungen und Monatsabrechnungen (`/invoices`), Monitoring über Actuator (`backup`, `serverErrors`), erweitertes Audit-Log.

**Flyway:** bis **V15** (`invoices`). Monitoring (#81) **ohne** neue Migration.

**E-Mail standardmäßig aus:** `MAIL_ENABLED=false`; Frist-Erinnerungen (`DEADLINE_REMINDER_ENABLED`), Besuchs-Mail (`MAIL_VISIT_APPOINTMENT_ENABLED`), Alert-Stub (`MONITORING_ALERT_ENABLED`) — nur auf dem Server aktivieren.

**Zurückgestellt:** [#10](https://github.com/mitetodb/test-purchase-application/issues/10) — Off-Site-Backup (Storage Box) und Cron warten auf Dimitars Entscheidung; Plan mit Platzhaltern: [BACKUP.md §12](../BACKUP.md#12-storage-box-off-site-pending-dimitar).
