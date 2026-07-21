package com.awakenedredstone.autowhitelist.discord.message.responses;

import com.awakenedredstone.autowhitelist.discord.DiscordClientHolder;
import com.awakenedredstone.autowhitelist.discord.message.MessageBuilder;
import com.awakenedredstone.autowhitelist.discord.message.ResponseMessage;
import com.awakenedredstone.autowhitelist.server.ServerDetails;
import com.awakenedredstone.autowhitelist.server.profile.PlayerProfile;
import com.awakenedredstone.autowhitelist.server.whitelist.WhitelistHandler;
import com.awakenedredstone.autowhitelist.server.whitelist.link.LinkingWhitelist;
import com.awakenedredstone.autowhitelist.util.data.ModData;
import com.awakenedredstone.autowhitelist.util.string.LinedStringBuilder;
import discord4j.common.GitProperties;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static com.awakenedredstone.autowhitelist.AutoWhitelist.id;

public class StatusCommandMessages {
    public static void init() {}

    /*public static final Identifier SERVER = ResponseMessage.<>register(id("status/server"), args -> {
        LinkingWhitelist whitelist = WhitelistHandler.getWhitelist();
        var entries = whitelist.getEntries();
        List<PlayerProfile> profiles = entries.stream().map(entry -> PlayerProfile.from(entry.getUser())).toList();
        List<PlayerProfile> managed = profiles.stream().filter(PlayerProfile::isLinked).toList();
        List<PlayerProfile> unmanaged = new ArrayList<>(profiles);
        unmanaged.removeAll(managed);

        LinedStringBuilder builder = new LinedStringBuilder();
        builder.appendLine("Total whitelisted players: ", profiles.size());
        builder.appendLine("Managed players: ", managed.size());
        builder.appendLine("Unmanaged players: ", unmanaged.size());

        return List.of(
          MessageBuilder.translated("discord.autowhitelist.response.fail.not_found.title")
        );
    });

    public static final Identifier WHITELIST = ResponseMessage.register(id("status/whitelist"), args -> {
        LinkingWhitelist whitelist = WhitelistHandler.getWhitelist();
        var entries = whitelist.getEntries();
        List<PlayerProfile> profiles = entries.stream().map(entry -> PlayerProfile.from(entry.getUser())).toList();
        List<PlayerProfile> managed = profiles.stream().filter(PlayerProfile::isLinked).toList();
        List<PlayerProfile> unmanaged = new ArrayList<>(profiles);
        unmanaged.removeAll(managed);

        LinedStringBuilder builder = new LinedStringBuilder();
        builder.appendLine("Total whitelisted players: ", profiles.size());
        builder.appendLine("Managed players: ", managed.size());
        builder.appendLine("Unmanaged players: ", unmanaged.size());

        return List.of(
          MessageBuilder.translated("discord.autowhitelist.response.fail.not_found.title")
        );
    });

    public static final Identifier BOT = ResponseMessage.register(id("status/bot"), args -> {
        LinedStringBuilder builder = new LinedStringBuilder();
        Properties properties = GitProperties.getProperties();
        builder.appendLine()
          .append(properties.getProperty(GitProperties.APPLICATION_NAME, "Discord4J"))
          .append(" version: ")
          .append(properties.getProperty(GitProperties.APPLICATION_VERSION, "unknown"));
        builder.appendLine("Client: ", DiscordClientHolder.getCurrent().getClient());
        builder.appendLine("Guild: ", DiscordClientHolder.getCurrent().getGuild());
        builder.appendLine("Current task: ", DiscordClientHolder.getCurrent());
        builder.appendLine("Pending tasks: ", DiscordClientHolder.BOT_SERVICE.getPendingTaskCount());

        return List.of(
          MessageBuilder.translated("discord.autowhitelist.response.fail.not_found.title")
        );
    });

    public static final Identifier MINECRAFT = ResponseMessage.register(id("status/minecraft"), args -> {
        LinedStringBuilder builder = new LinedStringBuilder();
        builder.appendLine("Minecraft version: ", SharedConstants.getCurrentVersion().*REMOVE_THIS//*? if <=1.21.5 {*REMOVE_THIS//**REMOVE_THIS//*getName*REMOVE_THIS//**REMOVE_THIS//*?} else {*REMOVE_THIS//*name*REMOVE_THIS//*?}*REMOVE_THIS//*());
        builder.appendLine("Java version: ", Runtime.version());
        builder.appendLine("Mod loader: ", ServerDetails.getPlatformName());
        if (FabricLoader.getInstance().isModLoaded("connectormod")) {
            builder.appendLine("Connector version: ", ModData.getVersion("connectormod"));
        }
        builder.appendLine("Loader version: ", ServerDetails.getLoaderVersion());
        builder.appendLine("Mod version: ", ModData.getVersion("autowhitelist"));
        builder.appendLine("Luckperms version: ", ModData.getVersion("luckperms"));

        return List.of(
          MessageBuilder.translated("discord.autowhitelist.response.fail.not_found.title")
        );
    });

    public static final Identifier CONFIG = ResponseMessage.register(id("status/config"), args -> {
        LinedStringBuilder builder = new LinedStringBuilder();
        builder.appendLine("Minecraft version: ", SharedConstants.getCurrentVersion().*REMOVE_THIS//*? if <=1.21.5 {*REMOVE_THIS//**REMOVE_THIS//*getName*REMOVE_THIS//**REMOVE_THIS//*?} else {*REMOVE_THIS//*name*REMOVE_THIS//*?}*REMOVE_THIS//*());
        builder.appendLine("Java version: ", Runtime.version());
        builder.appendLine("Mod loader: ", ServerDetails.getPlatformName());
        if (FabricLoader.getInstance().isModLoaded("connectormod")) {
            builder.appendLine("Connector version: ", ModData.getVersion("connectormod"));
        }
        builder.appendLine("Loader version: ", ServerDetails.getLoaderVersion());
        builder.appendLine("Mod version: ", ModData.getVersion("autowhitelist"));
        builder.appendLine("Luckperms version: ", ModData.getVersion("luckperms"));
        builder.appendLine("Player Roles version: ", ModData.getVersion("player_roles"));

        return List.of(
          MessageBuilder.translated("discord.autowhitelist.response.fail.not_found.title")
        );
    });*/
}
