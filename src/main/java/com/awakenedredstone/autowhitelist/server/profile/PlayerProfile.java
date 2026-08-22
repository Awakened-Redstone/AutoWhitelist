package com.awakenedredstone.autowhitelist.server.profile;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.mojang.authlib.GameProfile;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record PlayerProfile(@NotNull UUID id, @NotNull String name, String discordId, String role, long lockedUntil) {
    public PlayerProfile(@NotNull UUID id, @NotNull String name) {
        this(id, name, null, null, -1);
    }

    public PlayerProfile(LinkedNameAndId profile) {
        this(id(profile), name(profile), profile.getDiscordId(), profile.getRole(), profile.getLockedUntil());
    }

    private PlayerProfile(/*$ WhitelistProfile >>*/net.minecraft.server.players.NameAndId profile) {
        this(id(profile), name(profile));
    }

    /*? if >=1.21.9 {*/
    public PlayerProfile(GameProfile profile) {
        this(id(profile), name(profile));
    } /*?}*/

    public boolean isLinked() {
        return discordId != null;
    }

    public boolean isGeyser() {
        return id.getMostSignificantBits() == 0;
    }

    public String getSkinId() {
        if (!isGeyser()) return id.toString();
        return AutoWhitelist.config().vanity.unknownSkin;
    }

    public boolean matchesNameAndId(/*$ WhitelistProfile >>*/net.minecraft.server.players.NameAndId profile) {
        return name.equals(name(profile)) && id.equals(id(profile));
    }

    public @NotNull GameProfile asGameProfile() {
        return new GameProfile(id, name);
    }

    public @NotNull /*$ WhitelistProfile >>*/net.minecraft.server.players.NameAndId asEntryProfile() {
        return new /*$ WhitelistProfile {*/net.minecraft.server.players.NameAndId/*$}*/(id, name);
    }

    public @NotNull LinkedNameAndId asLinkedProfile() {
        return new LinkedNameAndId(id, name, discordId, role, lockedUntil);
    }

    public static @NotNull PlayerProfile from(@NotNull /*$ WhitelistProfile >>*/net.minecraft.server.players.NameAndId profile) {
        if (profile instanceof LinkedNameAndId linkedProfile) return new PlayerProfile(linkedProfile);
        return new PlayerProfile(profile);
    }

    public static String name(GameProfile profile) {
        return /*? if <1.21.9 {*/ /*profile.getName() *//*?} else {*/ profile.name() /*?}*/;
    }

    public static UUID id(GameProfile profile) {
        return /*? if <1.21.9 {*/ /*profile.getId() *//*?} else {*/ profile.id() /*?}*/;
    }

    //? if >=1.21.9 {
    public static String name(net.minecraft.server.players.NameAndId profile) {
        return profile.name();
    }

    public static UUID id(net.minecraft.server.players.NameAndId profile) {
        return profile.id();
    }

    public static String name(com.mojang.authlib.yggdrasil.response.NameAndId profile) {
        return profile.name();
    }

    public static UUID id(com.mojang.authlib.yggdrasil.response.NameAndId profile) {
        return profile.id();
    }
    //?}
}
