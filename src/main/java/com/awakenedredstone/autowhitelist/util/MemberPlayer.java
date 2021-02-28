package com.awakenedredstone.autowhitelist.util;

import com.mojang.authlib.GameProfile;

public class MemberPlayer {

    private final String userId;
    private final GameProfile profile;
    private final String team;

    public MemberPlayer(GameProfile profile, String team, String userId) {
        this.profile = profile;
        this.team = team;
        this.userId = userId;
    }

    public GameProfile getProfile() {
        return profile;
    }

    public String getTeam() {
        return team;
    }

    public String getUserId() {
        return userId;
    }
}
