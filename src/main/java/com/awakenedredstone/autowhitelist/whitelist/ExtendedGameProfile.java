package com.awakenedredstone.autowhitelist.whitelist;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.mojang.authlib.GameProfile;
import net.dv8tion.jda.api.entities.Role;

import java.util.UUID;

public class ExtendedGameProfile extends GameProfile {
    private final String role;
    private final String discordId;
    private final long lockedUntil;

    public ExtendedGameProfile(UUID id, String name, String role, String discordId, long lockedUntil) {
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
        return AutoWhitelist.getServer().getPlayerManager().getUserBanList().contains(this) ? -1 : lockedUntil;
    }

    public ExtendedGameProfile withRole(String newRole) {
        return new ExtendedGameProfile(getId(), getName(), newRole, discordId, lockedUntil);
    }

    public ExtendedGameProfile withRole(Role newRole) {
        return withRole(newRole.getId());
    }

    public ExtendedGameProfile withLockedUntil(long newLockedUntil) {
        return new ExtendedGameProfile(getId(), getName(), role, discordId, newLockedUntil);
    }

    public boolean isLocked() {
        return lockedUntil == -1 || lockedUntil > System.currentTimeMillis() || AutoWhitelist.getServer().getPlayerManager().getUserBanList().contains(this);
    }
}
