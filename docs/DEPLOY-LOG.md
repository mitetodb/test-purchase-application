# Deploy log

Newest first. One entry per change applied on the server. This file does not install or deploy anything.

## 2026-11-26

2026-11-26 12:30 Europe/Kiev – baccardi: installed PR #91 uploads backup from master 7af44f650b7dac795fa079a1186c649c35be7e47. Script /usr/local/bin/testkauf-backup-uploads.sh root:root 700, sha256 e02d9b15f171d8daa7709cb63edadd93d8a8113d781cedfc0720164e7dd0c63a. Manual run exit 0, LAST_STATUS OK, archive uploads-20261126-103015-226003.tar.gz (202 bytes, mode 600), sha256sum -c OK, tar lists demo/demo-offer.txt, log 640, uploads dir 700. Cron: added MAILTO="", PATH=, and 25 3 * * * uploads job; 03:15 MySQL line unchanged. Logrotate example installed at /etc/logrotate.d/testkauf-backup but held: /var/log is 775 root:syslog, so logrotate skips without an su directive. The `su root syslog` fix followed in this PR. No app deploy, checkout left at 497d6cd, salon untouched.
