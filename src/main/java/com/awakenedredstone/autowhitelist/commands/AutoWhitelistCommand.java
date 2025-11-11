package com.awakenedredstone.autowhitelist.commands;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.commands.api.Permission;
import com.awakenedredstone.autowhitelist.data.DefaultTranslationsDataProvider;
import com.awakenedredstone.autowhitelist.discord.old.DiscordBot;
import com.awakenedredstone.autowhitelist.util.LinedStringBuilder;
import com.awakenedredstone.autowhitelist.util.ModData;
import com.awakenedredstone.autowhitelist.stonecutter.Stonecutter;
import com.awakenedredstone.autowhitelist.util.TimeParser;
import com.awakenedredstone.autowhitelist.whitelist.override.LinkedPlayerProfile;
import com.awakenedredstone.autowhitelist.whitelist.override.LinkingWhitelist;
import com.jagrosh.jdautilities.commons.JDAUtilitiesInfo;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.dv8tion.jda.api.JDAInfo;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.SharedConstants;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.ServerConfigEntry;
import net.minecraft.server.WhitelistEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.literal;

public class AutoWhitelistCommand {
    public static final Logger LOGGER = LoggerFactory.getLogger(AutoWhitelistCommand.class);

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
          literal("autowhitelist")
            .requires(Permission.require("autowhitelist.command", 3))
            .then(
              literal("dump")
                .then(
                  literal("stats")
                    .executes(AutoWhitelistCommand::getInfo)
                )
                .then(
                  literal("status")
                    .executes(AutoWhitelistCommand::getInfo)
                )
                .then(
                  literal("info")
                    .executes(AutoWhitelistCommand::getInfo)
                ).then(
                  literal("config")
                    .executes(context -> {
                        context.getSource().sendFeedback(() -> Text.literal(AutoWhitelist.CONFIG.toString()), false);
                        return 0;
                    })
                ).then(
                  literal("mods")
                    .executes(context -> {
                        List<ModContainer> mods = new ArrayList<>(FabricLoader.getInstance().getAllMods());
                        mods.sort(Comparator.comparing(o -> o.getMetadata().getName()));
                        LinedStringBuilder builder = new LinedStringBuilder("Detected ", mods.size(), " mods:");

                        for (ModContainer mod : mods) {
                            ModMetadata modMeta = mod.getMetadata();
                            builder.appendLine(modMeta.getName()).append(" - ").append(modMeta.getVersion().getFriendlyString());
                        }

                        context.getSource().sendFeedback(() -> Text.literal(builder.toString()), false);
                        return mods.size();
                    })
                )
            ).then(
              literal("reload")
                .then(
                  literal("bot")
                    .executes(context -> {
                        ServerCommandSource source = context.getSource();
                        source.sendFeedback(() -> Text.literal("Restarting bot, please wait."), true);
                        DiscordBot.getInstanceSafe().restartBot();
                        source.sendFeedback(() -> Text.literal("The bot is now starting."), true);
                        return 0;
                    })
                ).then(
                  literal("config")
                    .executes(context -> {
                        ServerCommandSource source = context.getSource();
                        source.sendFeedback(() -> Text.literal("Reloading configurations."), true);
                        AutoWhitelist.CONFIG.load();
                        return 0;
                    })
                ).then(
                  literal("cache")
                    .executes(context -> {
                        ServerCommandSource source = context.getSource();
                        source.sendFeedback(() -> Text.literal("Reloading cache."), true);
                        AutoWhitelist.loadWhitelistCache();
                        return 0;
                    })
                )
            ).then(
              literal("list")
                .executes(context -> executeList(context.getSource()))
            ).then(
              literal("fix-duplicate-commands")
                .executes(context -> {
                    ServerCommandSource source = context.getSource();
                    source.sendFeedback(() -> Text.literal("Fixing commands..."), false);
                    CompletableFuture.runAsync(() -> {
                        List<Command> commands = DiscordBot.getJda().retrieveCommands().complete();
                        List<String> toRemove = List.of("register", "info", "userinfo", "modify");
                        commands.stream().filter(command -> toRemove.contains(command.getName())).map(ISnowflake::getId)
                          .forEach(id -> {
                              AutoWhitelist.LOGGER.debug("Removing command with id {}", id);
                              //noinspection ResultOfMethodCallIgnored
                              DiscordBot.getJda().deleteCommandById(id).submit();
                          });
                    });

                    return 0;
                })
            ).then(
              literal("create-translations-datapack")
                .executes(context -> {
                    ServerCommandSource source = context.getSource();
                    source.sendFeedback(() -> Text.literal("Creating datapack"), false);

                    Path path = source.getServer().getSavePath(WorldSavePath.DATAPACKS).resolve("autowhitelist_translations");
                    FabricDataOutput output = new FabricDataOutput(FabricLoader.getInstance().getModContainer(AutoWhitelist.MOD_ID).get(), path, true);
                    DataProvider provider = new DefaultTranslationsDataProvider(output);
                    provider.run(DataWriter.UNCACHED).whenComplete((o, throwable) -> {
                        try {
                            Files.writeString(path.resolve("pack.mcmeta"), "{\"pack\": {\"pack_format\": 34,\"description\": \"\"}}");
                        } catch (IOException e) {
                            source.sendError(Text.literal("Failed to create pack.mcmeta for \"autowhitelist_translations\""));
                            LOGGER.error("Failed to create pack.mcmeta for \"autowhitelist_translations\"", e);
                            return;
                        }

                        source.sendFeedback(() -> Text.literal("Created datapack autowhitelist_translations"), false);
                    });

                    return 0;
                })
            )
        );
    }

    public static int executeList(ServerCommandSource source) {
        if (source.getPlayer() != null) {
            source.getPlayer().sendMessage(Text.literal("Loading info..."), true);
        }

        Collection<? extends WhitelistEntry> entries = ((LinkingWhitelist) source.getServer().getPlayerManager().getWhitelist()).getEntries();

        List</*$ WhitelistProfile {*/net.minecraft.server.PlayerConfigEntry/*$}*/> profiles = entries.stream()
          .map(ServerConfigEntry::getKey)
          .filter(profile -> !(profile instanceof LinkedPlayerProfile))
          .toList();

        MutableText list = Text.literal("");
        if (!profiles.isEmpty()) {
            list.append("Vanilla whitelist:");
            profiles.forEach(player -> list.append("\n").append("    ").append(Stonecutter.profileName(player)));
        }

        List<LinkedPlayerProfile> extendedProfiles = entries.stream()
          .map(entry -> entry.getKey() instanceof LinkedPlayerProfile profile ? profile : null)
          .filter(Objects::nonNull)
          .toList();

        if (!extendedProfiles.isEmpty()) {
            if (!list.getString().isEmpty()) list.append("\n");
            list.append("Automated whitelist:");
            Guild guild = DiscordBot.getGuild();
            if (guild != null) {
                for (LinkedPlayerProfile profile : extendedProfiles) {
                    list.append("\n").append("    ").append(Stonecutter.profileName(profile));
                    list.append(Text.literal(" - ").formatted(Formatting.DARK_GRAY));

                    Member member = guild.getMemberById(profile.getDiscordId());
                    if (member == null) {
                        list.append(Text.literal("Invalid member").formatted(Formatting.RED));
                    } else {
                        list.append(Text.literal(member.getUser().getName()).formatted(Formatting.GRAY));
                    }

                    list.append(Text.literal(" (").formatted(Formatting.DARK_GRAY));
                    Role role = guild.getRoleById(profile.getRole());
                    if (role == null) {
                        list.append(Text.literal("Invalid role").formatted(Formatting.RED));
                    } else {
                        list.append(Text.literal("@" + role.getName()).formatted(Formatting.GRAY));
                    }
                    list.append(Text.literal(")").formatted(Formatting.DARK_GRAY));
                }
            } else {
                list.append("\n").append("    ").append(Text.literal("Failed to get guild!").formatted(Formatting.RED));
            }
        }

        if (source.getPlayer() != null) {
            source.getPlayer().sendMessage(Text.literal(""), true);
        }

        source.sendFeedback(() -> list, false);
        return extendedProfiles.size();
    }

    private static String getPlatformName() {
        String loaderName = getLoaderName();
        if (FabricLoader.getInstance().isModLoaded("connectormod")) {
            return loaderName + " - Via Connector";
        }

        return loaderName;
    }

    private static String getLoaderName() {
        return switch (AutoWhitelist.getServer().getServerModName()) {
            case "fabric" -> "Fabric";
            case "quilt" -> "Quilt";
            case "forge" -> "Forge";
            case "neoforge" -> "NeoForge";
            default -> "Unknown";
        };
    }

    private static String getLoaderVersion() {
        return switch (AutoWhitelist.getServer().getServerModName()) {
            case "fabric" -> ModData.getVersion("fabricloader");
            case "quilt" -> ModData.getVersion("quilt_loader");
            case "forge" -> ModData.getVersion("forge");
            case "neoforge" -> ModData.getVersion("neoforge");
            default -> "unknown";
        };
    }

    private static int getInfo(CommandContext<ServerCommandSource> context) {
        context.getSource().sendFeedback(() -> Text.literal("Generating data dump..."), false);
        PlayerManager playerManager = AutoWhitelist.getServer().getPlayerManager();

        CompletableFuture.runAsync(() -> {
            boolean canConfigLoad;
            LinedStringBuilder dump = new LinedStringBuilder().append(" ");
            dump.appendLine("==== AutoWhitelist data dump ====");
            dump.appendLine("Minecraft:");
            dump.appendLine("  Minecraft version: ", SharedConstants.getGameVersion()./*? if <=1.21.5 {*//*getName*//*?} else {*/name/*?}*/());
            dump.appendLine("  Java version: ", Runtime.version());
            dump.appendLine("  Mod loader: ", getPlatformName());
            if (FabricLoader.getInstance().isModLoaded("connectormod")) {
                dump.appendLine("  Connector version: ", ModData.getVersion("connectormod"));
            }
            dump.appendLine("  Loader version: ", getLoaderVersion());
            dump.appendLine("  Mod version: ", ModData.getVersion("autowhitelist"));
            dump.appendLine("  Total whitelisted players: ", playerManager.getWhitelistedNames().length);
            dump.appendLine("  Luckperms version: ", ModData.getVersion("luckperms"));

            dump.appendLine();
            dump.appendLine("AutoWhitelist:");
            dump.appendLine("  Config:");
            dump.appendLine("    Total entries: ", AutoWhitelist.CONFIG.entries.size());
            dump.appendLine("    Config exists: ", AutoWhitelist.CONFIG.configExists());
            dump.appendLine("    Config loads: ", canConfigLoad = AutoWhitelist.CONFIG.canLoad());
            if (!canConfigLoad) {
                dump.append(" <-- BAD CONFIG! Check the logs for the error cause");
            }
            dump.appendLine("    Lock time: ", TimeParser.parseTime(AutoWhitelist.CONFIG.lockTime));
            dump.appendLine("  Bot:");
            dump.appendLine("    JDA version: ", JDAInfo.VERSION);
            dump.appendLine("    Chewtils version: ", JDAUtilitiesInfo.VERSION);
            dump.appendLine("    Bot status: ", DiscordBot.botExists() ? "online" : "offline");
            if (DiscordBot.botExists()) {
                dump.appendLine("    Gateway ping: ", DiscordBot.getJda().getGatewayPing());
                dump.appendLine("    Rest ping: ", DiscordBot.getJda().getRestPing().complete());
            }

            context.getSource().sendFeedback(() -> Text.literal(dump.toString()), false);
        });

        return 0;
    }
}
