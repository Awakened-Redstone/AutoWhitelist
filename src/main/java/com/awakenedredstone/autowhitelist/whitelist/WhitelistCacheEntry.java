package com.awakenedredstone.autowhitelist.whitelist;

import com.awakenedredstone.autowhitelist.mixin.ServerConfigEntryMixin;
import com.awakenedredstone.autowhitelist.util.ExtendedGameProfile;
import com.google.gson.JsonObject;
import net.minecraft.server.ServerConfigEntry;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class WhitelistCacheEntry extends ServerConfigEntry<ExtendedGameProfile> {
    public WhitelistCacheEntry(@Nullable ExtendedGameProfile key) {
        super(key);
    }

    public WhitelistCacheEntry(JsonObject json) {
        this(profileFromJson(json));
    }

    private static ExtendedGameProfile profileFromJson(JsonObject json) {
        String string = json.get("uuid").getAsString();

        UUID uuid;
        try {
            uuid = UUID.fromString(string);
        } catch (Throwable var4) {
            return null;
        }

        return new ExtendedGameProfile(uuid, json.get("name").getAsString(), null, json.get("discordId").getAsString());
    }

    public ExtendedGameProfile getProfile() {
        return (ExtendedGameProfile) ((ServerConfigEntryMixin<?>) this).getKey();
    }

    @Override
    protected void write(JsonObject json) {
        json.addProperty("uuid", ((ExtendedGameProfile) ((ServerConfigEntryMixin<?>) this).getKey()).getId().toString());
        json.addProperty("name", ((ExtendedGameProfile) ((ServerConfigEntryMixin<?>) this).getKey()).getName());
        json.addProperty("discordId", ((ExtendedGameProfile) ((ServerConfigEntryMixin<?>) this).getKey()).getDiscordId());
    }
}
