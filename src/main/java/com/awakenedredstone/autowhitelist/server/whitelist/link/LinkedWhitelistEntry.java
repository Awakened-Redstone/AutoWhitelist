package com.awakenedredstone.autowhitelist.server.whitelist.link;

import com.awakenedredstone.autowhitelist.server.profile.LinkedPlayerProfile;
import com.awakenedredstone.autowhitelist.server.profile.PlayerProfile;
import com.google.gson.JsonObject;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserWhiteListEntry;

import java.util.Objects;

public class LinkedWhitelistEntry extends UserWhiteListEntry {

    public LinkedWhitelistEntry(LinkedPlayerProfile profile) {
        super(profile);
    }

    public LinkedWhitelistEntry(JsonObject json) {
        this(LinkedPlayerProfile.fromJson(json));
    }

    @Override
    public LinkedPlayerProfile getUser() {
        return (LinkedPlayerProfile) super.getUser();
    }

    public static UserWhiteListEntry withNewName(UserWhiteListEntry oldEntry, String newName) {
        if (oldEntry instanceof LinkedWhitelistEntry linkedEntry) {
            LinkedPlayerProfile user = linkedEntry.getUser();
            return new LinkedWhitelistEntry(Objects.requireNonNull(user).withName(newName));
        }

        NameAndId user = oldEntry.getUser();
        return new UserWhiteListEntry(new NameAndId(PlayerProfile.id(Objects.requireNonNull(user)), newName));
    }

    /*? if <1.21.9 {*//*
    @Override
    protected void serialize(JsonObject json) {
        ExtendedPlayerProfile profile = (ExtendedPlayerProfile) getUser();
        if (profile != null) {
            profile.write(json);
        }
    }
    *//*?}*/
}
