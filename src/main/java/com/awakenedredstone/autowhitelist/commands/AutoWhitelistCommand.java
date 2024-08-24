package com.awakenedredstone.autowhitelist.commands;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.commands.api.Permission;
import com.awakenedredstone.autowhitelist.debug.DebugFlags;
import com.awakenedredstone.autowhitelist.discord.DiscordBot;
import com.awakenedredstone.autowhitelist.mixin.ServerConfigEntryMixin;
import com.awakenedredstone.autowhitelist.util.Stonecutter;
import com.awakenedredstone.autowhitelist.util.TimeParser;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedGameProfile;
import com.awakenedredstone.autowhitelist.util.LinedStringBuilder;
import com.awakenedredstone.autowhitelist.util.ModData;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelist;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.dv8tion.jda.api.JDAInfo;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.SharedConstants;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.WhitelistEntry;
import net.minecraft.server.command.ServerCommandSource;
/*? if <1.19 {*/
/*import com.mojang.brigadier.exceptions.CommandSyntaxException;
*//*?}*/

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class AutoWhitelistCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
          literal("autowhitelist")
            .requires(Permission.require("autowhitelist.command", 3))
            .then(
              literal("dump")
                .executes(context -> {
                    context.getSource().sendFeedback(Stonecutter.feedbackText(Stonecutter.literalText("Generating data dump...")), false);
                    PlayerManager playerManager = AutoWhitelist.getServer().getPlayerManager();

                    CompletableFuture.runAsync(() -> {
                        boolean canConfigLoad;
                        LinedStringBuilder dump = new LinedStringBuilder().append(" ");
                        dump.appendLine("==== AutoWhitelist data dump ====");
                        dump.appendLine("Minecraft:");
                        dump.appendLine("  Minecraft version: ", SharedConstants.getGameVersion().getName());
                        dump.appendLine("  Java version: ", Runtime.version());
                        dump.appendLine("  Mod loader: ", getLoaderName());
                        dump.appendLine("  Loader version: ", getLoaderVersion());
                        dump.appendLine("  Mod version: ", ModData.getVersion("autowhitelist"));
                        dump.appendLine("  Total whitelisted players: ", playerManager.getWhitelistedNames().length);
                        dump.appendLine("  Luckperms version: ", ModData.getVersion("luckperms"));
                        Collection<ModContainer> mods = FabricLoader.getInstance().getAllMods().stream().filter(mod -> !isCoreMod(mod.getMetadata().getId())).toList();
                        dump.appendLine("  Found ", mods.size(), " other non \"core\" mods");

                        dump.appendLine();
                        dump.appendLine("AutoWhitelist:");
                        dump.appendLine("  Config:");
                        dump.appendLine("    Total entries: ", AutoWhitelist.CONFIG.entries.size());
                        dump.appendLine("    Config exists: ", AutoWhitelist.CONFIG.configExists());
                        dump.appendLine("    Is config valid: ", canConfigLoad = AutoWhitelist.CONFIG.canLoad());
                        if (!canConfigLoad) {
                            dump.append(" <-- BAD CONFIG! Check the logs for the error cause");
                        }
                        dump.appendLine("    Lock time: ", TimeParser.parseTime(AutoWhitelist.CONFIG.lockTime));
                        dump.appendLine("  Bot:");
                        dump.appendLine("    JDA version: ", JDAInfo.VERSION);
                        dump.appendLine("    Bot status: ", DiscordBot.jda == null ? "offline" : "online");
                        if (DiscordBot.jda != null) {
                            dump.appendLine("    Gateway ping: ", DiscordBot.jda.getGatewayPing());
                            dump.appendLine("    Rest ping: ", DiscordBot.jda.getRestPing().complete());
                        }

                        context.getSource().sendFeedback(Stonecutter.feedbackText(Stonecutter.literalText(dump.toString())), false);
                    });

                    return 0;
                }).then(
                  literal("config")
                    .executes(context -> {
                        context.getSource().sendFeedback(Stonecutter.feedbackText(Stonecutter.literalText(AutoWhitelist.CONFIG.toString())), false);
                        return 0;
                    })
                )
            ).then(
              literal("reload")
                .then(
                  literal("bot")
                    .executes(context -> {
                        ServerCommandSource source = context.getSource();
                        source.sendFeedback(Stonecutter.feedbackText(Stonecutter.literalText("Restarting bot, please wait.")), true);
                        DiscordBot.getInstance().reloadBot(source);
                        return 0;
                    })
                ).then(
                  literal("config")
                    .executes(context -> {
                        ServerCommandSource source = context.getSource();
                        source.sendFeedback(Stonecutter.feedbackText(Stonecutter.literalText("Reloading configurations.")), true);
                        AutoWhitelist.CONFIG.load();
                        return 0;
                    })
                ).then(
                  literal("cache")
                    .executes(context -> {
                        ServerCommandSource source = context.getSource();
                        source.sendFeedback(Stonecutter.feedbackText(Stonecutter.literalText("Reloading cache.")), true);
                        AutoWhitelist.loadWhitelistCache();
                        return 0;
                    })
                )
            ).then(
              literal("entries")
                .executes(context -> executeEntries(context.getSource()))
            ).then(
              literal("debug")
                .then(
                  literal("trackRoleChanges")
                    .then(
                      argument("enable", BoolArgumentType.bool())
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            DebugFlags.trackRoleChanges = BoolArgumentType.getBool(context, "enable");

                            source.sendFeedback(Stonecutter.feedbackText(Stonecutter.literalText("Updated debug flag")), true);
                            AutoWhitelist.loadWhitelistCache();

                            return 0;
                        })
                    )
                )
            )
        );
    }

    public static int executeEntries(ServerCommandSource source) /*? if <1.19 {*//*throws CommandSyntaxException*//*?}*/ {
        if (source.getPlayer() != null) {
            source.getPlayer().sendMessage(Stonecutter.literalText("Loading info..."), true);
        }

        Collection<? extends WhitelistEntry> entries = ((ExtendedWhitelist) source.getServer().getPlayerManager().getWhitelist()).getEntries();

        Stream<GameProfile> profiles = entries.stream().map(v -> (GameProfile) ((ServerConfigEntryMixin<?>) v).getKey());

        StringBuilder list = new StringBuilder();
        list.append("Non-managed players:\n");
        profiles.filter(profile -> !(profile instanceof ExtendedGameProfile)).forEach(player -> list.append("    ").append(player.getName()).append("\n"));

        profiles = entries.stream().map(v -> (GameProfile) ((ServerConfigEntryMixin<?>) v).getKey());

        list.append("\n");
        list.append("Managed players:\n");
        profiles.filter(profile -> profile instanceof ExtendedGameProfile).forEach(player -> list.append("    ").append(player.getName()).append("\n"));

        if (source.getPlayer() != null) {
            source.getPlayer().sendMessage(Stonecutter.literalText(""), true);
        }

        source.sendFeedback(Stonecutter.feedbackText(Stonecutter.literalText(list.toString())), false);
        return 1;
    }

    private static boolean isCoreMod(String modid) {
        return Pattern.compile("^fabric(-\\w+)+-v\\d$").matcher(modid).matches() || equals(modid, "java", "minecraft", "fabricloader", "autowhitelist",
          "placeholder-api", "server_translations_api", "packet_tweaker", "fabric-language-kotlin", "fabric-api", "fabric-api-base", "mixinextras");
    }

    private static String getLoaderName() {
        if (FabricLoader.getInstance().isModLoaded("connectormod")) {
            return "Forge - Via Connector";
        }

        return switch (AutoWhitelist.getServer().getServerModName()) {
            case "fabric" -> "Fabric";
            case "quilt" -> "Quilt";
            case "forge" -> "Forge";
            case "neoforge" -> "NeoForge";
            default -> "Unknown";
        };
    }

    private static String getLoaderVersion() {
        if (FabricLoader.getInstance().isModLoaded("connectormod")) {
            return ModData.getVersion("connectormod");
        }

        return switch (AutoWhitelist.getServer().getServerModName()) {
            case "fabric" -> ModData.getVersion("fabricloader");
            case "quilt" -> ModData.getVersion("quilt_loader");
            default -> "unknown";
        };
    }

    private static boolean equals(String stringA, String... stringB) {
        //return true if stringA equals to any of stringB
        for (String string : stringB) {
            if (stringA.equals(string)) return true;
        }
        return false;
    }
}
