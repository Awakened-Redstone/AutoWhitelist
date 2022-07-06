package com.awakenedredstone.autowhitelist.commands;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.discord.Bot;
import com.awakenedredstone.autowhitelist.mixin.ServerConfigEntryMixin;
import com.awakenedredstone.autowhitelist.util.ExtendedGameProfile;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelist;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelistEntry;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.WhitelistEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static com.awakenedredstone.autowhitelist.util.Debugger.analyzeTimings;

public class AutoWhitelistCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("autowhitelist").requires((source) -> source.hasPermissionLevel(4))
                .then(CommandManager.literal("reload").executes(context ->
                                executeReload(context.getSource()))
                        .then(CommandManager.literal("bot").executes(context ->
                                executeSpecificReload(context.getSource(), ReloadableObjects.BOT)))
                        .then(CommandManager.literal("config").executes(context ->
                                executeSpecificReload(context.getSource(), ReloadableObjects.CONFIG)))
                        .then(CommandManager.literal("translations").executes(context ->
                                executeSpecificReload(context.getSource(), ReloadableObjects.TRANSLATIONS))))
                .then(CommandManager.literal("info").executes(context ->
                        executeInfo(context.getSource())))
        );
    }

    public static int executeInfo(ServerCommandSource source) {
        if (source.getPlayer() != null) {
            source.getPlayer().sendMessage(Text.literal("Loading info..."), true);
        }

        Collection<? extends WhitelistEntry> entries = ((ExtendedWhitelist) source.getServer().getPlayerManager().getWhitelist()).getEntries();

        Stream<GameProfile> profiles = entries.stream().map(v -> {
            ((ServerConfigEntryMixin<?>) v).callGetKey();
            return (GameProfile) ((ServerConfigEntryMixin<?>) v).getKey();
        });

        StringBuilder list = new StringBuilder();
        list.append("Non-managed players:\n");
        profiles.filter(profile -> !(profile instanceof ExtendedGameProfile)).forEach(player -> list.append("    ").append(player.getName()).append("\n"));

        profiles = entries.stream().map(v -> {
            ((ServerConfigEntryMixin<?>) v).callGetKey();
            return (GameProfile) ((ServerConfigEntryMixin<?>) v).getKey();
        });

        list.append("\n");
        list.append("Managed players:\n");
        profiles.filter(profile -> profile instanceof ExtendedGameProfile).forEach(player -> list.append("    ").append(player.getName()).append("\n"));

        if (source.getPlayer() != null) {
            source.getPlayer().sendMessage(Text.literal(""), true);
        }

        source.sendFeedback(Text.literal(list.toString()), false);
        return 0;
    }

    public static int executeReload(ServerCommandSource source) {
        source.sendFeedback(Text.literal("Reloading AutoWhitelist configurations, please wait."), true);

        analyzeTimings("Config#loadConfigs", AutoWhitelist.config::loadConfigs);
        analyzeTimings("AutoWhitelist#reloadTranslations", AutoWhitelist::reloadTranslations);
        source.sendFeedback(Text.literal("Restarting bot, please wait."), true);
        analyzeTimings("Bot#reloadBot", () -> Bot.getInstance().reloadBot(source));

        return 0;
    }

    public static int executeSpecificReload(ServerCommandSource source, ReloadableObjects type) {
        switch (type) {
            case BOT -> {
                source.sendFeedback(Text.literal("Restarting bot, please wait."), true);
                analyzeTimings("Bot#reloadBot", () -> Bot.getInstance().reloadBot(source));
            }
            case CONFIG -> {
                source.sendFeedback(Text.literal("Reloading configurations."), true);
                analyzeTimings("Config#loadConfigs", AutoWhitelist.config::loadConfigs);
            }
            case TRANSLATIONS -> {
                source.sendFeedback(Text.literal("Reloading translations."), true);
                analyzeTimings("AutoWhitelist#reloadTranslations", AutoWhitelist::reloadTranslations);
            }
        }

        return 0;
    }

    private enum ReloadableObjects {
        BOT,
        CONFIG,
        TRANSLATIONS
    }
}
