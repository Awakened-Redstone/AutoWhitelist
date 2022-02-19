package com.awakenedredstone.autowhitelist.discord.api;

import net.dv8tion.jda.api.requests.GatewayIntent;

import java.util.Arrays;
import java.util.List;

public class GatewayIntents {
    public static final List<GatewayIntent> BASIC = Arrays.asList(
            GatewayIntent.GUILD_MEMBERS,
            GatewayIntent.GUILD_EMOJIS,
            GatewayIntent.GUILD_BANS,
            GatewayIntent.GUILD_MESSAGES,
            GatewayIntent.DIRECT_MESSAGES,
            GatewayIntent.GUILD_MESSAGE_REACTIONS);
}
