package com.awakenedredstone.autowhitelist.discord.commands;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.config.EntryData;
import com.awakenedredstone.autowhitelist.discord.BotHelper;
import com.awakenedredstone.autowhitelist.discord.api.command.CommandManager;
import com.awakenedredstone.autowhitelist.discord.api.command.DiscordCommandSource;
import com.awakenedredstone.autowhitelist.mixin.ServerConfigEntryMixin;
import com.awakenedredstone.autowhitelist.util.ExtendedGameProfile;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelist;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelistEntry;
import com.awakenedredstone.autowhitelist.whitelist.WhitelistCacheEntry;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.awakenedredstone.autowhitelist.discord.BotHelper.*;
import static com.awakenedredstone.autowhitelist.util.Debugger.analyzeTimings;

public class RegisterCommand {
    public static void register(CommandDispatcher<DiscordCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("register").requires(DiscordCommandSource::isFromGuild)
                .then(CommandManager.argument("minecraft_username", StringArgumentType.word())
                        .executes((source) -> {
                            execute(source.getSource(), StringArgumentType.getString(source, "minecraft_username"));
                            return 0;
                        })
                )
        );
    }

    protected static void execute(DiscordCommandSource source, String username) {
        analyzeTimings("RegisterCommand#execute", () -> {
            MessageChannelUnion channel = source.getChannel();
            Member member = source.getMember();
            if (member == null) return;

            sendTempFeedbackMessage(source.getChannel(), Text.translatable("command.feedback.received.title"),
                    Text.translatable("command.feedback.received.message"), 10);

            String id = member.getId();
            List<Role> roles = member.getRoles();

            boolean accepted = !Collections.disjoint(roles.stream().map(Role::getId).toList(), new ArrayList<>(AutoWhitelist.whitelistDataMap.keySet()));
            if (accepted) {
                MinecraftServer server = AutoWhitelist.server;
                ExtendedWhitelist whitelist = (ExtendedWhitelist) server.getPlayerManager().getWhitelist();

                boolean hasAccountWhitelisted = whitelist.getEntries().stream().map(entry -> {
                    try {
                        return ((ExtendedGameProfile) ((ServerConfigEntryMixin<?>) entry).getKey()).getDiscordId();
                    } catch (ClassCastException exception) {
                        return null;
                    }
                }).filter(Objects::nonNull).anyMatch(id_ -> id_.equals(id));
                if (hasAccountWhitelisted) {
                    BotHelper.sendFeedbackMessage(channel, Text.translatable("command.register.already_registered.title"),
                            Text.translatable("command.register.already_registered.message"), MessageType.WARNING);
                    return;
                }

                String highestRole = roles.stream().map(Role::getId).filter(AutoWhitelist.whitelistDataMap::containsKey).findFirst().get();
                EntryData entry = AutoWhitelist.whitelistDataMap.get(highestRole);
                try {
                    entry.assertSafe();
                } catch (Exception e) {
                    BotHelper.sendFeedbackMessage(channel, Text.translatable("command.fail.title"),
                            Text.translatable("command.fatal.exception", e.getMessage()), MessageType.ERROR);
                    AutoWhitelist.LOGGER.error("Failed to whitelist user, tried to assert entry but got an exception", e);
                    return;
                }

                if (username.isEmpty()) {
                    BotHelper.sendFeedbackMessage(channel, Text.translatable("command.few_args.title"),
                            Text.translatable("command.few_args.message"), MessageType.WARNING);
                    return;
                }
                String[] args = username.split(" ");
                if (args.length > 1) {
                    BotHelper.sendFeedbackMessage(channel, Text.translatable("command.too_many_args.title"),
                            Text.translatable("command.too_many_args.message"), MessageType.WARNING);
                    return;
                }
                String arg = args[0];

                {
                    if (arg.length() > 16) {
                        sendFeedbackMessage(source.getChannel(), Text.translatable("command.register.invalid_username.title"),
                                Text.translatable("command.register.invalid_username.message.too_long"), MessageType.WARNING);
                        return;
                    } else if (arg.length() < 3) {
                        sendFeedbackMessage(source.getChannel(), Text.translatable("command.register.invalid_username.title"),
                                Text.translatable("command.register.invalid_username.message.too_short"), MessageType.WARNING);
                        return;
                    }
                }
                GameProfile profile = server.getUserCache().findByName(arg).orElse(null);
                if (profile == null) {
                    BotHelper.sendFeedbackMessage(channel, Text.translatable("command.fail.title"),
                            Text.translatable("command.register.fail.account_data", arg), BotHelper.MessageType.ERROR);
                    return;
                }
                ExtendedGameProfile extendedProfile = new ExtendedGameProfile(profile.getId(), profile.getName(), highestRole, id);
                if (AutoWhitelist.server.getPlayerManager().getUserBanList().contains(extendedProfile)) {
                    BotHelper.sendFeedbackMessage(channel, Text.translatable("command.register.player_banned.title"),
                            Text.translatable("command.register.player_banned.message"), BotHelper.MessageType.ERROR);
                    return;
                }
                boolean whitelisted = whitelist.isAllowed(extendedProfile);
                if (whitelisted) {
                    BotHelper.sendFeedbackMessage(channel, Text.translatable("command.register.username_already_registered.title"),
                            Text.translatable("command.register.username_already_registered.message"), BotHelper.MessageType.ERROR);
                } else {
                    MessageCreateData message = BotHelper.generateFeedbackMessage(Text.translatable("command.register.last_steps.title"),
                            Text.translatable("command.register.last_steps.message"), BotHelper.MessageType.INFO);
                    MessageCreateAction feedbackMessage = channel.sendMessage(message);
                    feedbackMessage.queue(message_ -> {
                        whitelist.add(new ExtendedWhitelistEntry(extendedProfile));
                        if (!AutoWhitelist.CONFIG.enableWhitelistCache()) {
                            AutoWhitelist.WHITELIST_CACHE.add(new WhitelistCacheEntry(extendedProfile));
                        }
                        entry.registerUser(profile);
                        message_.editMessage(BotHelper.generateEditFeedbackMessage(Text.translatable("command.register.success.title"),
                                Text.translatable("command.register.success.message"), BotHelper.MessageType.SUCCESS)).queue();
                    });
                }
            } else {
                BotHelper.sendFeedbackMessage(channel, Text.translatable("command.register.fail.not_allowed.title"),
                        Text.translatable("command.register.fail.not_allowed.message"), MessageType.NORMAL);
            }
        });
    }
}
