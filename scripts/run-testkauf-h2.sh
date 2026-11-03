#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
exec ./mvnw -B -q spring-boot:run \
  -Dspring-boot.run.useTestClasspath=true \
  -Dspring-boot.run.jvmArguments="-Dserver.port=8080" \
  -Dspring-boot.run.arguments="\
--server.port=8080 \
--spring.datasource.url=jdbc:h2:mem:testkauf;DB_CLOSE_DELAY=-1;MODE=MySQL \
--spring.datasource.driver-class-name=org.h2.Driver \
--spring.datasource.username=sa \
--spring.datasource.password= \
--spring.jpa.hibernate.ddl-auto=create \
--spring.jpa.database-platform=org.hibernate.dialect.H2Dialect \
--spring.flyway.enabled=false \
--app.init-data.enabled=false \
--app.demo-data.enabled=false \
--app.security.totp.encryption-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
