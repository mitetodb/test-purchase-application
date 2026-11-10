# Статус на проекта (кратко, български)

Пълен списък на английски: [../STATUS.md](../STATUS.md).

**На живо на `master` (PR #75–#81):** PDF отчет за тестова покупка, крайни срокове, записване на посещение в магазин, въпросник с оценка, трендове (`/score-trends`), фактури и месечни отчети (`/invoices`), мониторинг през Actuator (`backup`, `serverErrors`), разширен audit log.

**Flyway:** до **V15** (`invoices`). Мониторингът (#81) **няма** нова миграция.

**Имейли изключени по подразбиране:** `MAIL_ENABLED=false`; напомняния за крайни срокове (`DEADLINE_REMINDER_ENABLED`), имейл за посещение (`MAIL_VISIT_APPOINTMENT_ENABLED`), alert stub (`MONITORING_ALERT_ENABLED`) — включват се само на сървъра.

**Отложено:** [#10](https://github.com/mitetodb/test-purchase-application/issues/10) — off-site backup (Storage Box) и cron чакат решение на Димитър; план с placeholders: [BACKUP.md §12](../BACKUP.md#12-storage-box-off-site-pending-dimitar).
