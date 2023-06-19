package com.awakenedredstone.autowhitelist.commands;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.commands.api.Permission;
import com.awakenedredstone.autowhitelist.discord.Bot;
import com.awakenedredstone.autowhitelist.mixin.ServerConfigEntryMixin;
import com.awakenedredstone.autowhitelist.util.ExtendedGameProfile;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelist;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.WhitelistEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.stream.Stream;

import static com.awakenedredstone.autowhitelist.util.Debugger.analyzeTimings;

public class AutoWhitelistCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("autowhitelist")
                .requires(Permission.require("autowhitelist.command", 3))
                .then(CommandManager.literal("reload")
                        .executes(context -> executeReload(context.getSource()))
                        .then(CommandManager.literal("bot")
                                .executes(context -> executeSpecificReload(context.getSource(), ReloadableObjects.BOT))
                        ).then(CommandManager.literal("config")
                                .executes(context -> executeSpecificReload(context.getSource(), ReloadableObjects.CONFIG))
                        ).then(CommandManager.literal("cache")
                                .executes(context -> executeSpecificReload(context.getSource(), ReloadableObjects.CACHE))
                        )
                ).then(CommandManager.literal("entries")
                        .executes(context -> executeEntries(context.getSource()))
                ).then(CommandManager.literal("info")
                        .executes(context -> executeInfo(context.getSource()))
                )
        );
    }

    public static int executeInfo(ServerCommandSource source) {
        String text = "Mod information:" +
                "    Bot: " + (Bot.jda == null ? "offline" : "online");
        return Bot.jda != null ? 1 : 0;
    }

    public static int executeEntries(ServerCommandSource source) {
        if (source.getPlayer() != null) {
            source.getPlayer().sendMessage(Text.literal("Loading info..."), true);
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
            source.getPlayer().sendMessage(Text.literal(""), true);
        }

        source.sendFeedback(() -> Text.literal(list.toString()), false);
        return 0;
    }

    public static int executeReload(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("Reloading AutoWhitelist configurations, please wait."), true);

        analyzeTimings("Configs#load", AutoWhitelist.CONFIG::load);
        analyzeTimings("AutoWhitelist#loadWhitelistCache", AutoWhitelist::loadWhitelistCache);
        source.sendFeedback(() -> Text.literal("Restarting bot, please wait."), true);
        analyzeTimings("Bot#reloadBot", () -> Bot.getInstance().reloadBot(source));

        return 0;
    }

    public static int executeSpecificReload(ServerCommandSource source, ReloadableObjects type) {
        switch (type) {
            case BOT -> {
                source.sendFeedback(() -> Text.literal("Restarting bot, please wait."), true);
                analyzeTimings("Bot#reloadBot", () -> Bot.getInstance().reloadBot(source));
            }
            case CONFIG -> {
                source.sendFeedback(() -> Text.literal("Reloading configurations."), true);
                analyzeTimings("Configs#load", AutoWhitelist.CONFIG::load);
            }
            case CACHE -> {
                source.sendFeedback(() -> Text.literal("Reloading cache."), true);
                analyzeTimings("AutoWhitelist#loadWhitelistCache", AutoWhitelist::loadWhitelistCache);
            }
        }

        return 0;
    }

    private enum ReloadableObjects {
        BOT,
        CONFIG,
        CACHE
    }
}
