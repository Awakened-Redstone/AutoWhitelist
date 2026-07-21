package com.awakenedredstone.autowhitelist.jsonrpc.internalapi;

import com.awakenedredstone.autowhitelist.discord.DiscordClientHolder;
import com.awakenedredstone.autowhitelist.server.profile.PlayerProfile;
import com.awakenedredstone.autowhitelist.server.whitelist.WhitelistHandler;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.jsonrpc.JsonRpcLogger;
import net.minecraft.server.jsonrpc.internalapi.MinecraftAllowListServiceImpl;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.notifications.NotificationManager;

import java.util.Optional;

public class LinkedAllowListService extends MinecraftAllowListServiceImpl {
    //? if >=26.2 {
    public LinkedAllowListService(NotificationManager notificationManager, JsonRpcLogger jsonrpcLogger) {
        super(notificationManager, jsonrpcLogger);
    }
    //? } else {
    /*public LinkedAllowListService(DedicatedServer server, JsonRpcLogger jsonrpcLogger) {
        super(server, jsonrpcLogger);
    }
    */ //? }

    public boolean register(PlayerProfile profile, ClientInfo clientInfo) {
        if (profile.discordId() == null) return false;

        this.jsonrpcLogger.log(clientInfo, "Register player '{}' to AutoWhitelist", profile);

        Member member = DiscordClientHolder.getCurrent().getGuild().getMemberById(Snowflake.of(profile.discordId())).block();
        return WhitelistHandler.register(server /*? if >=26.2 { */() /*?} */, member, () -> Optional.of(profile), true).map(response -> {
            if (response.success()) {
                this.jsonrpcLogger.log(clientInfo, "Successfully registered player '{}' to the allowlist", response.args()[0]);
            }

            return response.success();
        }).blockOptional().orElse(false);
    }
}
