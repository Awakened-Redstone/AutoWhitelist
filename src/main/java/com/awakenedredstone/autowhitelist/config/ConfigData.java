package com.awakenedredstone.autowhitelist.config;

import java.util.*;

public class ConfigData {

    public boolean devVersion;
    public short whitelistScheduledVerificationSeconds = 60;
    public List<String> owners = new ArrayList<>();
    public String prefix = "np!";
    public String token = "";
    public String clientId = "";
    public String discordServerId = "";
    public Map<String, List<String>> whitelist = Map.of(
            "tier2-team-id", Arrays.asList("youtube-role-id", "twitch-role-id"),
            "tier3-team-id", Arrays.asList("youtube-role-id", "twitch-role-id"));
}
