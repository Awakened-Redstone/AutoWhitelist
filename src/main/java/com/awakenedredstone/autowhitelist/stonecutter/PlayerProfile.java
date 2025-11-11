package com.awakenedredstone.autowhitelist.stonecutter;

import com.awakenedredstone.autowhitelist.whitelist.override.LinkedPlayerProfile;
import com.mojang.authlib.GameProfile;

import java.util.UUID;

public record PlayerProfile(UUID id, String name) {
    public GameProfile toProfile() {
        return new GameProfile(id, name);
    }

    public /*$ WhitelistProfile >>*/net.minecraft.server.PlayerConfigEntry toEntryType() {
        return new /*$ WhitelistProfile {*/net.minecraft.server.PlayerConfigEntry/*?}*/(id, name);
    }

    public LinkedPlayerProfile linked() {
        return new LinkedPlayerProfile(id, name);
    }
}
