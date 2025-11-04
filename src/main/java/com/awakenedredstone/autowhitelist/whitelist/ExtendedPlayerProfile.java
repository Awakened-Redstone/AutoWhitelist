package com.awakenedredstone.autowhitelist.whitelist;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.util.Stonecutter;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import discord4j.core.object.entity.Role;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ExtendedPlayerProfile extends /*$ WhitelistProfile >>*/net.minecraft.server.PlayerConfigEntry {
    private final String role;
    private final String discordId;
    private final long lockedUntil;

    //? if >=1.21.9 {
    public ExtendedPlayerProfile(net.minecraft.server.PlayerConfigEntry profile) {
        super(Stonecutter.profileId(profile), Stonecutter.profileName(profile));
        this.role = null;
        this.discordId = null;
        this.lockedUntil = -1;
    }
    //?}

    public ExtendedPlayerProfile(GameProfile profile) {
        super(Stonecutter.profileId(profile), Stonecutter.profileName(profile));
        this.role = null;
        this.discordId = null;
        this.lockedUntil = -1;
    }

    public ExtendedPlayerProfile(UUID id, String name, String role, String discordId, long lockedUntil) {
        super(id, name);
        this.role = role;
        this.discordId = discordId;
        this.lockedUntil = lockedUntil;
    }

    public String getRole() {
        return role;
    }

    public String getDiscordId() {
        return discordId;
    }

    public long getLockedUntil() {
        if (AutoWhitelist.CONFIG.lockTime() == -1) return -1;
        return AutoWhitelist.getServer().getPlayerManager().getUserBanList().contains(this) ? -1 : lockedUntil;
    }

    public ExtendedPlayerProfile withRole(String newRole) {
        return new ExtendedPlayerProfile(Stonecutter.profileId(this), Stonecutter.profileName(this), newRole, discordId, lockedUntil);
    }

    public ExtendedPlayerProfile withRole(Role newRole) {
        return withRole(newRole.getId().asString());
    }

    public ExtendedPlayerProfile withLockedUntil(long newLockedUntil) {
        return new ExtendedPlayerProfile(Stonecutter.profileId(this), Stonecutter.profileName(this), role, discordId, newLockedUntil);
    }

    public boolean isLocked() {
        return AutoWhitelist.CONFIG.lockTime() == -1 || lockedUntil == -1 || lockedUntil > System.currentTimeMillis() || AutoWhitelist.getServer().getPlayerManager().getUserBanList().contains(this);
    }

    /*? if <1.21.9 {*//*
    @Override
    public String toString() {
        return new ToStringBuilder(this)
          .append("id", Stonecutter.profileId(this))
          .append("name", Stonecutter.profileName(this))
          .append("properties", this.getProperties())
          .append("role", role)
          .append("discordId", discordId)
          .append("lockedUntil", lockedUntil)
          .toString();
    }
    *//*?}*/

    @Nullable
    public static ExtendedPlayerProfile read(JsonObject object) {
        if (jsonHasAllKeys(object, "uuid", "name", "discordId", "role", "lockedUntil")) {
            String uuidString = object.get("uuid").getAsString();

            UUID uuid;
            try {
                uuid = UUID.fromString(uuidString);
            } catch (Throwable e) {
                return null;
            }

            return new ExtendedPlayerProfile(
              uuid,
              object.get("name").getAsString(),
              object.get("role").getAsString(),
              object.get("discordId").getAsString(),
              object.get("lockedUntil").getAsLong()
            );
        } else {
            return null;
        }
    }

    public void write(JsonObject object) {
        object.addProperty("uuid", Stonecutter.profileId(this).toString());
        object.addProperty("name", Stonecutter.profileName(this));
        object.addProperty("role", getRole());
        object.addProperty("discordId", getDiscordId());
        object.addProperty("lockedUntil", getLockedUntil());
    }

    private static boolean jsonHasAllKeys(JsonObject json, String... keys) {
        for (String key : keys) {
            if (!json.has(key)) {
                return false;
            }
        }
        return true;
    }
}
