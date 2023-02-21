package com.awakenedredstone.autowhitelist.util;

import com.mojang.authlib.GameProfile;

import java.util.UUID;

public class ExtendedGameProfile extends GameProfile {

    private final String role;
    private final String discordId;

    public ExtendedGameProfile(UUID id, String name, String role, String discordId) {
        super(id, name);
        this.role = role;
        this.discordId = discordId;
    }

    public String getRole() {
        return role;
    }

    public String getDiscordId() {
        return discordId;
    }

    public ExtendedGameProfile withRole(String newRole) {
        return new ExtendedGameProfile(getId(), getName(), newRole, discordId);
    }
}
