package com.awakenedredstone.autowhitelist.whitelist.cache;

import com.awakenedredstone.autowhitelist.stonecutter.Stonecutter;
import com.awakenedredstone.autowhitelist.whitelist.override.LinkedPlayerProfile;
import com.google.gson.JsonObject;
import net.minecraft.server.ServerConfigEntry;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class WhitelistCacheEntry extends ServerConfigEntry<LinkedPlayerProfile> {
    public WhitelistCacheEntry(@Nullable LinkedPlayerProfile key) {
        super(key);
    }

    public WhitelistCacheEntry(JsonObject json) {
        this(profileFromJson(json));
    }

    private static LinkedPlayerProfile profileFromJson(JsonObject json) {
        String string = json.get("uuid").getAsString();

        UUID uuid;
        try {
            uuid = UUID.fromString(string);
        } catch (Throwable var4) {
            return null;
        }

        return new LinkedPlayerProfile(uuid, json.get("name").getAsString(), null, json.get("discordId").getAsString(), -1);
    }

    public LinkedPlayerProfile getProfile() {
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
