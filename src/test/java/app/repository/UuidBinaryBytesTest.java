package app.repository;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UuidBinaryBytesTest {

    @Test
    void encodesUuidAsSixteenBytes() {
        UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        assertEquals(16, UuidBinaryBytes.toBytes(uuid).length);
    }
}
