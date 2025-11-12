package com.awakenedredstone.autowhitelist.stonecutter;

import com.awakenedredstone.autowhitelist.whitelist.override.LinkedPlayerProfile;
import com.mojang.authlib.GameProfile;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record PlayerProfile(UUID id, String name) {
    @Contract(value = " -> new", pure = true)
    public @NotNull GameProfile toProfile() {
        return new GameProfile(id, name);
    }

    @Contract(value = " -> new", pure = true)
    public /*$ WhitelistProfile >>*/net.minecraft.server.@NotNull PlayerConfigEntry toEntryType() {
        return new /*$ WhitelistProfile {*/net.minecraft.server.PlayerConfigEntry/*$}*/(id, name);
    }

    @Contract(value = " -> new", pure = true)
    public @NotNull LinkedPlayerProfile linked() {
        return new LinkedPlayerProfile(id, name);
    }
}
