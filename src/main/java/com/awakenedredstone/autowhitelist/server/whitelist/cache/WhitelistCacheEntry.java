package com.awakenedredstone.autowhitelist.server.whitelist.cache;

import com.awakenedredstone.autowhitelist.server.profile.LinkedPlayerProfile;
import com.awakenedredstone.autowhitelist.server.profile.PlayerProfile;
import com.google.gson.JsonObject;
import net.minecraft.server.players.StoredUserEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class WhitelistCacheEntry extends StoredUserEntry<LinkedPlayerProfile> {
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
        } catch (IllegalArgumentException e) {
            return null;
        }

        return new LinkedPlayerProfile(uuid, json.get("name").getAsString(), json.get("discordId").getAsString(), null, -1);
    }

    @Override
    protected void serialize(@NotNull JsonObject json) {
        if (this.getUser() != null) {
            json.addProperty("uuid", PlayerProfile.id(this.getUser()).toString());
            json.addProperty("name", PlayerProfile.name(this.getUser()));
            json.addProperty("discordId", this.getUser().getDiscordId());
        }
    }
}
