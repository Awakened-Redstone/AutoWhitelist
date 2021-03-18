package com.awakenedredstone.autowhitelist.commands;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.discord.Bot;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;

import static com.awakenedredstone.autowhitelist.util.Debugger.analyzeTimings;

public class AutoWhitelistCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("auto-whitelist").requires((source) -> {
            return source.hasPermissionLevel(4);
        }).then((CommandManager.literal("reload").executes((source) -> {
            executeReload(source.getSource());
            return 0;
        })).then(CommandManager.literal("bot").executes((source) -> {
            executeSpecificReload(source.getSource(), ReloadableObjects.BOT);
            return 0;
        })).then(CommandManager.literal("config").executes((source) -> {
            executeSpecificReload(source.getSource(), ReloadableObjects.CONFIG);
            return 0;
        })).then(CommandManager.literal("translations").executes((source) -> {
            executeSpecificReload(source.getSource(), ReloadableObjects.TRANSLATIONS);
            return 0;
        }))));
    }

    public static void executeReload(ServerCommandSource source) {
        source.sendFeedback(new LiteralText("Reloading AutoWhitelist configurations, please wait."), true);

        analyzeTimings("Config#loadConfigs", AutoWhitelist.config::loadConfigs);
        analyzeTimings("AutoWhitelist#reloadTranslations", AutoWhitelist::reloadTranslations);
        source.sendFeedback(new LiteralText("Restarting bot, please wait."), true);
        analyzeTimings("Bot#reloadBot", () -> Bot.getInstance().reloadBot(source));
    }

    public static void executeSpecificReload(ServerCommandSource source, ReloadableObjects type) {
        switch (type) {
            case BOT:
                source.sendFeedback(new LiteralText("Restarting bot, please wait."), true);
                analyzeTimings("Bot#reloadBot", () -> Bot.getInstance().reloadBot(source));
                break;
            case CONFIG:
                source.sendFeedback(new LiteralText("Reloading configurations."), true);
                analyzeTimings("Config#loadConfigs", AutoWhitelist.config::loadConfigs);
                break;
            case TRANSLATIONS:
                source.sendFeedback(new LiteralText("Reloading translations."), true);
                analyzeTimings("AutoWhitelist#reloadTranslations", AutoWhitelist::reloadTranslations);
                break;
        }
    }

    private enum ReloadableObjects {
        BOT,
        CONFIG,
        TRANSLATIONS
    }
}
