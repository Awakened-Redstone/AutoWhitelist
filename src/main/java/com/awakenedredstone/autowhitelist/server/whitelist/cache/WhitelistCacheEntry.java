package com.awakenedredstone.autowhitelist.server.whitelist.cache;

import com.awakenedredstone.autowhitelist.server.profile.LinkedNameAndId;
import com.awakenedredstone.autowhitelist.server.profile.PlayerProfile;
import com.google.gson.JsonObject;
import net.minecraft.server.players.StoredUserEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class WhitelistCacheEntry extends StoredUserEntry<LinkedNameAndId> {
    public WhitelistCacheEntry(@Nullable LinkedNameAndId key) {
        super(key);
    }

    public WhitelistCacheEntry(JsonObject json) {
        this(profileFromJson(json));
    }

    private static LinkedNameAndId profileFromJson(JsonObject json) {
        String string = json.get("uuid").getAsString();

        UUID uuid;
        try {
            uuid = UUID.fromString(string);
        } catch (IllegalArgumentException e) {
            return null;
        }

        return new LinkedNameAndId(uuid, json.get("name").getAsString(), json.get("discordId").getAsString(), null, -1);
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
