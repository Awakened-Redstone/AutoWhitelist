package com.awakenedredstone.autowhitelist.config;

import java.util.List;
import java.util.Map;

public class ConfigData {

    public boolean devVersion;
    public short whitelistScheduledVerificationSeconds;
    public String[] owners;
    public String prefix;
    public String token;
    public String clientId;
    public String discordServerId;
    public boolean enableSlashCommands;
    public Map<String, List<String>> whitelist;
}
