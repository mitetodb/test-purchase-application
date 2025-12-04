package app.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@ControllerAdvice(annotations = RestController.class)
public class RestExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        String errorId = UUID.randomUUID().toString();
        log.warn("[REST {}] Access denied: {}", errorId, ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        "timestamp", Instant.now(),
                        "status", 403,
                        "error", "Forbidden",
                        "message", ex.getMessage(),
                        "errorId", errorId
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        String errorId = UUID.randomUUID().toString();
        log.error("[REST {}] Unexpected error", errorId, ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "timestamp", Instant.now(),
                        "status", 500,
                        "error", "Internal Server Error",
                        "message", "Unexpected error",
                        "errorId", errorId
                ));
    }
}
