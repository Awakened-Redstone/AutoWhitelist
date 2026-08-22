package com.awakenedredstone.autowhitelist.server.profile;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.server.ServerDetails;
import com.awakenedredstone.autowhitelist.server.whitelist.link.LinkedWhitelistEntry;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import discord4j.core.object.entity.Role;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserWhiteListEntry;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public class LinkedNameAndId extends /*$ WhitelistProfile >>*/net.minecraft.server.players.NameAndId {
    /*? if >=1.21.9 {*/ private static final java.lang.invoke.MethodHandle EQUALS; /*?}*/
    private final String role;
    private final String discordId;
    private final long lockedUntil;

    //? if >=1.21.9 {
    public LinkedNameAndId(net.minecraft.server.players.NameAndId profile) {
        this(PlayerProfile.id(profile), PlayerProfile.name(profile));
    }
    //?}

    public LinkedNameAndId(GameProfile profile) {
        this(PlayerProfile.id(profile), PlayerProfile.name(profile));
    }

    public LinkedNameAndId(UUID id, String name) {
        this(id, name, null, null, -1);
    }

    public LinkedNameAndId(UUID id, String name, String discordId, String role, long lockedUntil) {
        super(id, name);
        this.discordId = discordId;
        this.role = role;
        this.lockedUntil = lockedUntil;
    }

    public String getRole() {
        return role;
    }

    public String getDiscordId() {
        return discordId;
    }

    public long getLockedUntil() {
        if (AutoWhitelist.config().whitelist.lockTime() == -1) return -1;
        return ServerDetails.getServer().getPlayerList().getBans().isBanned(this) ? -1 : lockedUntil;
    }

    public LinkedNameAndId withRole(String newRole) {
        return new LinkedNameAndId(PlayerProfile.id(this), PlayerProfile.name(this), discordId, newRole, lockedUntil);
    }

    public LinkedNameAndId withRole(Role newRole) {
        return withRole(newRole.getId().asString());
    }

    public LinkedNameAndId withLockedUntil(long newLockedUntil) {
        return new LinkedNameAndId(PlayerProfile.id(this), PlayerProfile.name(this), discordId, role, newLockedUntil);
    }

    public LinkedNameAndId withName(String name) {
        return new LinkedNameAndId(PlayerProfile.id(this), name, discordId, role, lockedUntil);
    }

    public boolean isLocked() {
        return AutoWhitelist.config().whitelist.lockTime() == -1 || lockedUntil == -1 || lockedUntil > System.currentTimeMillis() || ServerDetails.getServer().getPlayerList().getBans().isBanned(this);
    }

    public static LinkedNameAndId of(@NotNull UserWhiteListEntry entry) {
        if (entry instanceof LinkedWhitelistEntry linkedEntry) return linkedEntry.getUser();
        return new LinkedNameAndId(entry.getUser());
    }

    @Override
    public boolean equals(Object obj) {
        //? if >=1.21.9 {
        try {
            if (!(boolean) EQUALS.invoke(this, obj)) return false;
        } catch (Error error) {
            throw error; // We don't want to catch unrecoverable errors
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        //?} else {
        /*if (!super.equals(obj)) return false;*/
        //?}

        if (obj instanceof LinkedNameAndId other) {
            return Objects.equals(other.discordId, this.discordId) && Objects.equals(other.role, this.role) && Objects.equals(other.lockedUntil, this.lockedUntil);
        }

        return false;
    }

    @Override
    public @NotNull String toString() {
        return new ToStringBuilder(this)
          .append("id", PlayerProfile.id(this))
          .append("name", PlayerProfile.name(this))
          /*?if < 1.21.9*///.append("properties", this.getProperties())
          .append("discordId", discordId)
          .append("role", role)
          .append("lockedUntil", lockedUntil)
          .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(PlayerProfile.id(this), PlayerProfile.name(this), discordId, role, lockedUntil);
    }

    public static @Nullable LinkedNameAndId fromJson(JsonObject object) {
        if (jsonHasAllKeys(object, "uuid", "name", "discordId", "role", "lockedUntil")) {
            String uuidString = object.get("uuid").getAsString();

            UUID uuid;
            try {
                uuid = UUID.fromString(uuidString);
            } catch (IllegalArgumentException e) {
                return null;
            }

            return new LinkedNameAndId(
              uuid,
              object.get("name").getAsString(),
              object.get("discordId").getAsString(),
              object.get("role").getAsString(),
              object.get("lockedUntil").getAsLong()
            );
        } else {
            return null;
        }
    }

    public void appendTo(JsonObject object) {
        object.addProperty("uuid", PlayerProfile.id(this).toString());
        object.addProperty("name", PlayerProfile.name(this));
        object.addProperty("discordId", getDiscordId());
        object.addProperty("role", getRole());
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

    //? if >=1.21.9 {
    static {
        try {
            EQUALS = java.lang.invoke.MethodHandles.lookup().findSpecial(NameAndId.class, "equals", java.lang.invoke.MethodType.methodType(boolean.class, Object.class), LinkedNameAndId.class);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError("Failed to get known method handle!");
        }
    }
    //?}
}
