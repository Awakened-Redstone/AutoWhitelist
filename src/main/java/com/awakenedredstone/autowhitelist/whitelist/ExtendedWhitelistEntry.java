package com.awakenedredstone.autowhitelist.whitelist;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.mixin.ServerConfigEntryMixin;
import com.google.gson.JsonObject;
import net.minecraft.server.WhitelistEntry;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ExtendedWhitelistEntry extends WhitelistEntry {

    public ExtendedWhitelistEntry(ExtendedGameProfile profile) {
        super(profile);
    }

    public ExtendedWhitelistEntry(JsonObject json, ExtendedWhitelist extendedWhitelist) {
        super(profileFromJson(json, extendedWhitelist));
    }

    @Nullable
    private static ExtendedGameProfile profileFromJson(JsonObject json, @Nullable ExtendedWhitelist extendedWhitelist) {
        if (jsonHasAllKeys(json, "uuid", "name", "discordId", "role")) {
            String uuidString = json.get("uuid").getAsString();

            UUID uuid;
            try {
                uuid = UUID.fromString(uuidString);
            } catch (Throwable e) {
                return null;
            }

            long lockedUntil;
            if (json.has("lockedUntil")) {
                lockedUntil = json.get("lockedUntil").getAsLong();
            } else {
                lockedUntil = AutoWhitelist.CONFIG.lockTime();
                if (extendedWhitelist != null) {
                    extendedWhitelist.setDirty(true);
                }
            }

            return new ExtendedGameProfile(
              uuid,
              json.get("name").getAsString(),
              json.get("role").getAsString(),
              json.get("discordId").getAsString(),
              lockedUntil
            );
        } else {
            return null;
        }
    }

    private static boolean jsonHasAllKeys(JsonObject json, String... keys) {
        for (String key : keys) {
            if (!json.has(key)) {
                return false;
            }
        }
        return true;
    }

    public ExtendedGameProfile getProfile() {
        return (ExtendedGameProfile) ((ServerConfigEntryMixin<?>) this).getKey();
    }

    @Override
    protected void write(JsonObject json) {
        ServerConfigEntryMixin<?> entry = (ServerConfigEntryMixin<?>) this;
        ExtendedGameProfile profile = (ExtendedGameProfile) entry.getKey();
        if (entry.getKey() != null && profile.getId() != null) {
            json.addProperty("uuid", profile.getId().toString());
            json.addProperty("name", profile.getName());
            json.addProperty("role", profile.getRole());
            json.addProperty("discordId", profile.getDiscordId());
            json.addProperty("lockedUntil", profile.getLockedUntil());
        }
    }
}
