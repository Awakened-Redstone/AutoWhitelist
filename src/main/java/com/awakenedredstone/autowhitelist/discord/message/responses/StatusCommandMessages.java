package com.awakenedredstone.autowhitelist.discord.message.responses;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.discord.DiscordClientHolder;
import com.awakenedredstone.autowhitelist.discord.message.MessageBuilder;
import com.awakenedredstone.autowhitelist.discord.message.ResponseMessage;
import com.awakenedredstone.autowhitelist.server.ServerDetails;
import com.awakenedredstone.autowhitelist.server.profile.LinkedNameAndId;
import com.awakenedredstone.autowhitelist.server.profile.PlayerProfile;
import com.awakenedredstone.autowhitelist.server.whitelist.WhitelistHandler;
import com.awakenedredstone.autowhitelist.server.whitelist.link.LinkingWhitelist;
import com.awakenedredstone.autowhitelist.util.data.ModData;
import com.awakenedredstone.autowhitelist.util.string.LinedStringBuilder;
import discord4j.common.GitProperties;
import discord4j.core.object.component.TextDisplay;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.StoredUserEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static com.awakenedredstone.autowhitelist.AutoWhitelist.id;

public class StatusCommandMessages {
    public static void init() {}

    public static final Identifier MINECRAFT = ResponseMessage.<ResponseTypes.Simple>register(id("status/minecraft"), () -> {
        LinedStringBuilder builder = new LinedStringBuilder();
        builder.appendLine("Minecraft version: ", SharedConstants.getCurrentVersion()./*? if <=1.21.5 {*//*getName*//*?} else {*/name/*?}*/());
        builder.appendLine("Java version: ", Runtime.version());
        builder.appendLine("Mod loader: ", ServerDetails.getPlatformName());
        if (FabricLoader.getInstance().isModLoaded("connectormod")) {
            builder.appendLine("Connector version: ", ModData.getVersion("connectormod"));
        }
        builder.appendLine("Loader version: ", ServerDetails.getLoaderVersion());
        // I'm using the class name as a type name, so I'm just taking the simple name
        builder.appendLine("Server type: ", ServerDetails.getServer().getClass().getSimpleName());
        builder.appendLine("Mod version: ", ModData.getVersion("autowhitelist"));
        builder.appendLine("Luckperms version: ", ModData.getVersion("luckperms"));
        builder.appendLine("Player roles version: ", ModData.getVersion("player_roles"));

        return List.of(
          TextDisplay.of(builder.toString())
        );
    });

    public static final Identifier NETWORKING = ResponseMessage.<ResponseTypes.Simple>register(id("status/networking"), () -> {
        // TODO
        MinecraftServer server = ServerDetails.getServer();

        LinedStringBuilder builder = new LinedStringBuilder();
        builder.appendLine("Networking: ", server.getConnection().running ? "Running" : "Not running");
        builder.appendLine("Connections: ", server.getConnection().getConnections().size());

        return List.of(
          TextDisplay.of(builder.toString())
        );
    });

    public static final Identifier WHITELIST = ResponseMessage.<ResponseTypes.Simple>register(id("status/whitelist"), () -> {
        MinecraftServer server = ServerDetails.getServer();
        LinkingWhitelist whitelist = WhitelistHandler.getWhitelist();
        var entries = whitelist.getEntries();
        List<PlayerProfile> profiles = entries.stream().map(entry -> PlayerProfile.from(entry.getUser())).toList();
        List<PlayerProfile> managed = profiles.stream().filter(PlayerProfile::isLinked).toList();
        List<PlayerProfile> unmanaged = new ArrayList<>(profiles);
        unmanaged.removeAll(managed);

        LinedStringBuilder builder = new LinedStringBuilder();
        builder.appendLine("Whitelist enabled: ", server.isUsingWhitelist());
        builder.appendLine("Whitelist enforced: ", server.isEnforceWhitelist());
        builder.appendLine("Total whitelisted players: ", entries.size());
        builder.appendLine("Managed players: ", managed.size());
        builder.appendLine("Unmanaged players: ", unmanaged.size());

        return List.of(
          TextDisplay.of(builder.toString())
        );
    });

    public static final Identifier CACHE = ResponseMessage.<ResponseTypes.Simple>register(id("status/cache"), () -> {
        MinecraftServer server = ServerDetails.getServer();
        var whitelist = WhitelistHandler.getWhitelist();
        var cache = WhitelistHandler.getCache();
        var entries = cache.getEntries();
        List<LinkedNameAndId> names = entries.stream().map(StoredUserEntry::getUser).toList();
        List<PlayerProfile> whitelisted = names.stream().filter(whitelist::isWhiteListed).map(PlayerProfile::from).toList();
        List<PlayerProfile> cached = names.stream().map(PlayerProfile::from).collect(Collectors.toList());
        cached.removeAll(whitelisted);

        LinedStringBuilder builder = new LinedStringBuilder();
        builder.appendLine("Whitelist enabled: ", server.isUsingWhitelist());
        builder.appendLine("Whitelist enforced: ", server.isEnforceWhitelist());
        builder.appendLine("Total cached players: ", entries.size());
        builder.appendLine("In whitelist: ", whitelisted.size());
        builder.appendLine("Cache only: ", cached.size());

        return List.of(
          TextDisplay.of(builder.toString())
        );
    });

    public static final Identifier BOT = ResponseMessage.<ResponseTypes.Simple>register(id("status/bot"), () -> {
        LinedStringBuilder builder = new LinedStringBuilder();
        Properties properties = GitProperties.getProperties();
        builder.appendLine()
          .append(properties.getProperty(GitProperties.APPLICATION_NAME, "Discord4J"))
          .append(" version: ")
          .append(properties.getProperty(GitProperties.APPLICATION_VERSION, "unknown"));
        builder.appendLine("Status: ", DiscordClientHolder.status().name().toLowerCase());
        if (DiscordClientHolder.hasClient()) {
            DiscordClientHolder holder = DiscordClientHolder.getCurrent();
            var tasks = holder.tasks();
            builder.appendLine("Startup tasks: ");
            if (tasks.isEmpty()) {
                builder.append("none");
            } else {
                builder.append("(", tasks.size(), ")");
                builder.append(Arrays.toString(tasks.toArray()).toLowerCase());
            }
            // FIXME
            builder.appendLine("Client: ", holder.getClient());
            builder.appendLine("Guild: ", holder.getGuild());
            builder.appendLine("Current service task: ", holder);
            builder.appendLine("Queued service tasks: ", DiscordClientHolder.BOT_SERVICE.getPendingTaskCount());
        }

        return List.of(
          TextDisplay.of(builder.toString())
        );
    });

    public static final Identifier CONFIG = ResponseMessage.<ResponseTypes.Simple>register(id("status/config"), () -> {
        LinedStringBuilder builder = new LinedStringBuilder();
        builder.appendLine("Config exists: ", AutoWhitelist.CONFIG.configExists());
        builder.appendLine("Can read: ", AutoWhitelist.CONFIG.tryRead().isSuccess());
        builder.appendLine("Config version: ", AutoWhitelist.config().CONFIG_VERSION);
        builder.appendLine("Bot enabled: ", AutoWhitelist.config().discord.token != null);
        builder.appendLine("Guyser support: ", AutoWhitelist.config().guyserSupport.name().toLowerCase());
        // builder.appendLine("Multilink enabled: ", AutoWhitelist.config().multilink != null);

        return List.of(
          TextDisplay.of(builder.toString())
        );
    });
}
