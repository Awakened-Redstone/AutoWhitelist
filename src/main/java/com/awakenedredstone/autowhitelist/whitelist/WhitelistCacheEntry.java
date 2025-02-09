package com.awakenedredstone.autowhitelist.whitelist;

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

        return new ExtendedGameProfile(uuid, json.get("name").getAsString(), null, json.get("discordId").getAsString(), -1);
    }

    public ExtendedGameProfile getProfile() {
        return this.getKey();
    }

    @Override
    protected void write(JsonObject json) {
        json.addProperty("uuid", this.getKey().getId().toString());
        json.addProperty("name", this.getKey().getName());
        json.addProperty("discordId", this.getKey().getDiscordId());
    }
}
