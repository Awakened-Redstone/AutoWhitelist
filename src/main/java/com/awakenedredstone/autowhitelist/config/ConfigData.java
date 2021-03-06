package com.awakenedredstone.autowhitelist.config;

import java.util.List;
import java.util.Map;

public class ConfigData {

    public long whitelistAutoUpdateDelaySeconds;
    public String prefix;
    public String token;
    public String applicationId;
    public String discordServerId;
    public Map<String, List<String>> whitelist;
}
