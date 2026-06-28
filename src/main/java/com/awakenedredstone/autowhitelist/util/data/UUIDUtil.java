package com.awakenedredstone.autowhitelist.util.data;

import java.util.UUID;

public class UUIDUtil {
    public static boolean isValidUuid(String uuid) {
        try {
            parseUuid(uuid);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    public static UUID parseUuid(String uuid) {
        // A complete UUID, parse it normally
        if (uuid.length() == 36) {
            return UUID.fromString(uuid);
        }

        // Too short to be a valid UUID
        if (uuid.length() < 32) {
            throw new IllegalArgumentException("Invalid UUID string: " + uuid);
        }

        // We seem to have partial dashes, we should clear them first
        if (uuid.length() > 32) {
            var original = uuid;
            uuid = uuid.replace("-", "");
            // Oh, those weren't partial dashes
            if (uuid.length() != 32) {
                throw new IllegalArgumentException("Invalid UUID string: " + original);
            }
        }

        return UUID.fromString(addDashesToUuid(uuid));
    }

    private static String addDashesToUuid(String uuid) {


        StringBuilder sb = new StringBuilder(uuid);
        sb.insert(8, '-');
        sb.insert(13, '-');
        sb.insert(18, '-');
        sb.insert(23, '-');
        return sb.toString();
    }
}
