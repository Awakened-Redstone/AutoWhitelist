package com.awakenedredstone.autowhitelist.commands;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.bot.Bot;
import com.awakenedredstone.autowhitelist.database.SQLite;
import com.awakenedredstone.autowhitelist.util.MemberPlayer;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;

import java.util.Collection;
import java.util.List;

public class AutoWhitelistCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("auto-whitelist").requires((source) -> {
            return source.hasPermissionLevel(4);
        }).then(CommandManager.literal("reload").executes((source) -> {
            executeReload(source.getSource());
            return 0;
        })).then(CommandManager.literal("remove").then(CommandManager.argument("target", StringArgumentType.word()).suggests((commandContext, suggestionsBuilder) -> {
            List<GameProfile> players = new SQLite().getPlayers();
            return CommandSource.suggestMatching(players.stream().map(GameProfile::getName), suggestionsBuilder);
        }).executes((commandContext) -> {
            return executeAdd((ServerCommandSource) commandContext.getSource(), StringArgumentType.getString(commandContext, "target"));
        }))));
    }

    private static int executeAdd(ServerCommandSource source, String target) {
        MemberPlayer member = new SQLite().getMemberByNick(target);
        GameProfile profile = member.getProfile();
        boolean success = new SQLite().removeMemberByNick(target);
        if (success)
            AutoWhitelist.removePlayer(profile);
        if (success) {
            source.sendFeedback(new LiteralText(String.format("Removed %s from the database. Whitelist has been updated.", target)), true);
            return 1;
        } else {
            source.sendFeedback(new LiteralText(String.format("Failed to remove %s from the database.", target)), true);
            return 0;
        }
    }

    public static void executeReload(ServerCommandSource source) {
        AutoWhitelist.logger.warn("Reloading configurations, please wait.");
        source.sendFeedback(new LiteralText("Reloading AutoWhitelist configurations, please wait."), true);
        AutoWhitelist.config.loadConfigs();
        Bot.getInstance().reloadBot(source);
    }
}
