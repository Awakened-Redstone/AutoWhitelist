package com.awakenedredstone.autowhitelist.config;

import java.util.List;
import java.util.Map;

public class ConfigData {

    public long whitelistScheduledVerificationSeconds;
    public String prefix;
    public String token;
    public String clientId;
    public String discordServerId;
    public Map<String, List<String>> whitelist;
}
