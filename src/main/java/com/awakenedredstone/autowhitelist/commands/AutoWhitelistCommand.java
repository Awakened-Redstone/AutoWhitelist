package com.awakenedredstone.autowhitelist.commands;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.commands.api.Permission;
import com.awakenedredstone.autowhitelist.debug.DebugFlags;
import com.awakenedredstone.autowhitelist.discord.DiscordBot;
import com.awakenedredstone.autowhitelist.util.LinedStringBuilder;
import com.awakenedredstone.autowhitelist.util.ModData;
import com.awakenedredstone.autowhitelist.util.TimeParser;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedGameProfile;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelist;
import com.jagrosh.jdautilities.commons.JDAUtilitiesInfo;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.dv8tion.jda.api.JDAInfo;
import net.dv8tion.jda.api.entities.Role;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.SharedConstants;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.ServerConfigEntry;
import net.minecraft.server.WhitelistEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class AutoWhitelistCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
          literal("autowhitelist")
            .requires(Permission.require("autowhitelist.command", 3))
            .then(
              literal("dump")
                .then(
                  literal("stats")
                    .executes(context -> {
                        context.getSource().sendFeedback(() -> Text.literal("Generating data dump..."), false);
                        PlayerManager playerManager = AutoWhitelist.getServer().getPlayerManager();

                        CompletableFuture.runAsync(() -> {
                            boolean canConfigLoad;
                            LinedStringBuilder dump = new LinedStringBuilder().append(" ");
                            dump.appendLine("==== AutoWhitelist data dump ====");
                            dump.appendLine("Minecraft:");
                            dump.appendLine("  Minecraft version: ", SharedConstants.getGameVersion().getName());
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
                    })
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
              literal("debug")
                .then(
                  literal("trackEntryError")
                    .then(
                      argument("enable", BoolArgumentType.bool())
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            DebugFlags.trackEntryError = BoolArgumentType.getBool(context, "enable");

                            source.sendFeedback(() -> Text.literal("Updated debug flag"), true);

                            return 0;
                        })
                    )
                )
            )
        );
    }

    public static int executeList(ServerCommandSource source) {
        if (source.getPlayer() != null) {
            source.getPlayer().sendMessage(Text.literal("Loading info..."), true);
        }

        Collection<? extends WhitelistEntry> entries = ((ExtendedWhitelist) source.getServer().getPlayerManager().getWhitelist()).getEntries();

        List<GameProfile> profiles = entries.stream()
          .map(ServerConfigEntry::getKey)
          .filter(profile -> !(profile instanceof ExtendedGameProfile))
          .toList();

        MutableText list = Text.literal("");
        if (!profiles.isEmpty()) {
            list.append("Vanilla whitelist:");
            profiles.forEach(player -> list.append("\n").append("    ").append(player.getName()));
        }

        List<ExtendedGameProfile> extendedProfiles = entries.stream()
          .map(entry -> entry.getKey() instanceof ExtendedGameProfile profile ? profile : null)
          .filter(Objects::nonNull)
          .toList();

        if (!extendedProfiles.isEmpty()) {
            if (!list.getString().isEmpty()) list.append("\n");
            list.append("Automated whitelist:");
            extendedProfiles.forEach(player -> {
                list.append("\n").append("    ").append(player.getName()).append(Text.literal(" - ").formatted(Formatting.DARK_GRAY));
                if (DiscordBot.getGuild() != null) {
                    Role role = DiscordBot.getGuild().getRoleById(player.getRole());
                    if (role == null) {
                        list.append(Text.literal("Invalid role").formatted(Formatting.RED));
                    } else {
                        list.append(Text.literal("@" + role.getName()).formatted(Formatting.GRAY));
                    }
                } else {
                    list.append(Text.literal("Guild is missing").formatted(Formatting.RED));
                }
              }
            );
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
}
