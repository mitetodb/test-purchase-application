package app.repository;

import java.nio.ByteBuffer;
import java.util.UUID;

public final class UuidBinaryBytes {

    private UuidBinaryBytes() {}

    /** MySQL BINARY(16) layout matching Hibernate's UUID Java type mapping. */
    public static byte[] toBytes(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }
}
