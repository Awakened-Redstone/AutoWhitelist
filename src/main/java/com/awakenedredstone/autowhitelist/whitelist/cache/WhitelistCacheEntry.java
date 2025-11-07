package com.awakenedredstone.autowhitelist.whitelist.cache;

import com.awakenedredstone.autowhitelist.util.Stonecutter;
import com.awakenedredstone.autowhitelist.whitelist.override.ExtendedPlayerProfile;
import com.google.gson.JsonObject;
import net.minecraft.server.ServerConfigEntry;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class WhitelistCacheEntry extends ServerConfigEntry<ExtendedPlayerProfile> {
    public WhitelistCacheEntry(@Nullable ExtendedPlayerProfile key) {
        super(key);
    }

    public WhitelistCacheEntry(JsonObject json) {
        this(profileFromJson(json));
    }

    private static ExtendedPlayerProfile profileFromJson(JsonObject json) {
        String string = json.get("uuid").getAsString();

        UUID uuid;
        try {
            uuid = UUID.fromString(string);
        } catch (Throwable var4) {
            return null;
        }

        return new ExtendedPlayerProfile(uuid, json.get("name").getAsString(), null, json.get("discordId").getAsString(), -1);
    }

    public ExtendedPlayerProfile getProfile() {
        return this.getKey();
    }

    @Override
    protected void write(JsonObject json) {
        if (this.getKey() != null) {
            json.addProperty("uuid", Stonecutter.profileId(this.getKey()).toString());
            json.addProperty("name", Stonecutter.profileName(this.getKey()));
            json.addProperty("discordId", this.getKey().getDiscordId());
        }
    }
}
