# Backup reference scripts (not installed by default)

These files are **examples** for operators implementing [docs/BACKUP.md](../../docs/BACKUP.md). They are **not** referenced from `docker-compose.prod.yml`, `server-setup.sh`, or any running service.

To use on a server:

1. Copy or symlink to `/usr/local/bin/` (or run with absolute paths).
2. Set `COMPOSE_FILE`, `ENV_FILE`, and `BACKUP_ROOT` in cron or a small wrapper.
3. Configure restic/borg credentials on the host (never commit them).
4. Run `restore-test.example.sh` against **demo** backup copies first.

Do not enable cron until paths and Storage Box placeholders are replaced with your environment.
