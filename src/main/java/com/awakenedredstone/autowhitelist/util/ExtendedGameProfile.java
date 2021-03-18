package com.awakenedredstone.autowhitelist.util;

import com.mojang.authlib.GameProfile;

import java.util.UUID;

public class ExtendedGameProfile extends GameProfile {

    private final String team;
    private final String discordId;

    public ExtendedGameProfile(UUID id, String name, String team, String discordId) {
        super(id, name);
        this.team = team;
        this.discordId = discordId;
    }

    public String getTeam() {
        return team;
    }

    public String getDiscordId() {
        return discordId;
    }
}
