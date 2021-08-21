package com.awakenedredstone.autowhitelist.whitelist;

import com.awakenedredstone.autowhitelist.mixin.ServerConfigEntryMixin;
import com.awakenedredstone.autowhitelist.util.ExtendedGameProfile;
import com.google.gson.JsonObject;
import net.minecraft.server.WhitelistEntry;

import java.util.UUID;

public class ExtendedWhitelistEntry extends WhitelistEntry {

    public ExtendedWhitelistEntry(ExtendedGameProfile profile) {
        super(profile);
    }

    public ExtendedWhitelistEntry(JsonObject json) {
        super(profileFromJson(json));
    }

    public ExtendedGameProfile getProfile() {
        ((ServerConfigEntryMixin<?>) this).callGetKey();
        return (ExtendedGameProfile) ((ServerConfigEntryMixin<?>) this).getKey();
    }

    protected void fromJson(JsonObject json) {
        ((ServerConfigEntryMixin<?>) this).callGetKey();
        if (((ServerConfigEntryMixin<?>) this).getKey() != null && ((ExtendedGameProfile) ((ServerConfigEntryMixin<?>) this).getKey()).getId() != null) {
            json.addProperty("uuid", ((ExtendedGameProfile) ((ServerConfigEntryMixin<?>) this).getKey()).getId().toString());
            json.addProperty("name", ((ExtendedGameProfile) ((ServerConfigEntryMixin<?>) this).getKey()).getName());
            json.addProperty("team", ((ExtendedGameProfile) ((ServerConfigEntryMixin<?>) this).getKey()).getTeam());
            json.addProperty("discordId", ((ExtendedGameProfile) ((ServerConfigEntryMixin<?>) this).getKey()).getDiscordId());
        }
    }

    private static ExtendedGameProfile profileFromJson(JsonObject json) {
        if (json.has("uuid") && json.has("name") && json.has("discordId") && json.has("team")) {
            String string = json.get("uuid").getAsString();

            UUID uuid;
            try {
                uuid = UUID.fromString(string);
            } catch (Throwable var4) {
                return null;
            }

            return new ExtendedGameProfile(uuid, json.get("name").getAsString(), json.get("team").getAsString(), json.get("discordId").getAsString());
        } else {
            return null;
        }
    }
}
