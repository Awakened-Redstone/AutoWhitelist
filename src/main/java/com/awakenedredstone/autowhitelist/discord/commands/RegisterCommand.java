package com.awakenedredstone.autowhitelist.discord.commands;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.discord.BotHelper;
import com.awakenedredstone.autowhitelist.discord.api.command.CommandManager;
import com.awakenedredstone.autowhitelist.discord.api.command.DiscordCommandSource;
import com.awakenedredstone.autowhitelist.discord.api.text.TranslatableText;
import com.awakenedredstone.autowhitelist.mixin.ServerConfigEntryMixin;
import com.awakenedredstone.autowhitelist.util.ExtendedGameProfile;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelist;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelistEntry;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.awakenedredstone.autowhitelist.discord.Bot.whitelistDataMap;
import static com.awakenedredstone.autowhitelist.discord.BotHelper.*;
import static com.awakenedredstone.autowhitelist.util.Debugger.analyzeTimings;

public class RegisterCommand {
    public static void register(CommandDispatcher<DiscordCommandSource> dispatcher) {
        LiteralCommandNode<DiscordCommandSource> literalCommandNode = dispatcher.register(CommandManager.literal("register").requires(DiscordCommandSource::isFromGuild)
                .then(CommandManager.argument("minecraft_username", StringArgumentType.word()).executes((source) -> {
                    if (source.getSource().getType() == DiscordCommandSource.CommandType.SLASH_COMMAND) {
//                        ((SlashCommandInteractionEvent) source.getSource().getEvent()).deferReply().queue(m -> {
//                            execute(source.getSource(), StringArgumentType.getString(source, "minecraft_username"));
//                            m.deleteOriginal().queue();
//                        });
                    } else {
                        execute(source.getSource(), StringArgumentType.getString(source, "minecraft_username"));
                    }
                    return 0;
                })));

//        CommandDataImpl command = new CommandDataImpl("register", new com.awakenedredstone.autowhitelist.discord.api.text.TranslatableText("command.description.register").getString());
//        command.addOptions(new OptionData(OptionType.STRING, "username", new com.awakenedredstone.autowhitelist.discord.api.text.TranslatableText("command.description.register.username").getString()));
//        jda.upsertCommand(command).queue();
    }

    protected static void execute(DiscordCommandSource source, String username) {
        analyzeTimings("RegisterCommand#execute", () -> {
            MessageChannel channel = source.getChannel();
            Member member = source.getMember();
            if (member == null) return;

            sendTempFeedbackMessage(source.getChannel(), new TranslatableText("command.feedback.received.title").getMinecraftText(),
                    new TranslatableText("command.feedback.received.message").getMinecraftText(), 10);

            String id = member.getId();
            List<Role> roles = member.getRoles();

            boolean accepted = !Collections.disjoint(roles.stream().map(Role::getId).toList(), new ArrayList<>(whitelistDataMap.keySet()));
            if (accepted) {
                MinecraftServer server = AutoWhitelist.server;
                ExtendedWhitelist whitelist = (ExtendedWhitelist) server.getPlayerManager().getWhitelist();

                boolean hasAccountWhitelisted = whitelist.getEntries().stream().map(entry -> {
                    ((ServerConfigEntryMixin<?>) entry).callGetKey();
                    try {
                        return ((ExtendedGameProfile) ((ServerConfigEntryMixin<?>) entry).getKey()).getDiscordId();
                    } catch (ClassCastException exception) {
                        return null;
                    }
                }).filter(Objects::nonNull).anyMatch(id_ -> id_.equals(id));

                if (hasAccountWhitelisted) {
                    BotHelper.sendFeedbackMessage(channel, new TranslatableText("command.register.already_registered.title").getMinecraftText(),
                            new TranslatableText("command.register.already_registered.message").getMinecraftText(), MessageType.WARNING);
                    return;
                }

                String highestRole = roles.stream().map(Role::getId).filter(whitelistDataMap::containsKey).toList().get(0);
                String teamName = whitelistDataMap.get(highestRole);
                Team team = server.getScoreboard().getTeam(teamName);
                if (team == null) {
                    BotHelper.sendFeedbackMessage(channel, new TranslatableText("command.fail.title").getMinecraftText(),
                            new TranslatableText("command.register.fatal.null_team", teamName).getMinecraftText(), BotHelper.MessageType.FATAL);
                    return;
                }

                if (username.isEmpty()) {
                    BotHelper.sendFeedbackMessage(channel, new TranslatableText("command.few_args.title").getMinecraftText(),
                            new TranslatableText("command.few_args.message").getMinecraftText(), MessageType.WARNING);
                    return;
                }
                String[] args = username.split(" ");
                if (args.length > 1) {
                    BotHelper.sendFeedbackMessage(channel, new TranslatableText("command.too_many_args.title").getMinecraftText(),
                            new TranslatableText("command.too_many_args.message").getMinecraftText(), MessageType.WARNING);
                    return;
                }
                String arg = args[0];

                {
                    if (arg.length() > 16) {
                        sendFeedbackMessage(source.getChannel(), new TranslatableText("command.register.invalid_username.title").getMinecraftText(),
                                new TranslatableText("command.register.invalid_username.message.too_long").getMinecraftText(), MessageType.WARNING);
                        return;
                    } else if (arg.length() < 3) {
                        sendFeedbackMessage(source.getChannel(), new TranslatableText("command.register.invalid_username.title").getMinecraftText(),
                                new TranslatableText("command.register.invalid_username.message.too_short").getMinecraftText(), MessageType.WARNING);
                        return;
                    }
                }
                {

                }
                GameProfile profile = server.getUserCache().findByName(arg).orElse(null);
                if (profile == null) {
                    BotHelper.sendFeedbackMessage(channel, new TranslatableText("command.fail.title").getMinecraftText(),
                            new TranslatableText("command.register.fail.account_data", arg).getMinecraftText(), BotHelper.MessageType.ERROR);
                    return;
                }
                ExtendedGameProfile extendedProfile = new ExtendedGameProfile(profile.getId(), profile.getName(), teamName, id);
                boolean whitelisted;
                whitelisted = whitelist.isAllowed(extendedProfile);
                if (!whitelisted)
                    whitelisted = server.getPlayerManager().getWhitelist().isAllowed(new GameProfile(profile.getId(), arg));
                if (whitelisted) {
                    BotHelper.sendFeedbackMessage(channel, new TranslatableText("command.register.username_already_registered.title").getMinecraftText(),
                            new TranslatableText("command.register.username_already_registered.title").getMinecraftText(), BotHelper.MessageType.ERROR);
                } else {
                    Message message = BotHelper.generateFeedbackMessage(new TranslatableText("command.register.last_steps.title").getMinecraftText(),
                            new TranslatableText("command.register.last_steps.message").getMinecraftText(), BotHelper.MessageType.INFO);
                    MessageAction feedbackMessage = channel.sendMessage(message);
                    feedbackMessage.queue(message_ -> {
                        whitelist.add(new ExtendedWhitelistEntry(extendedProfile));
                        server.getScoreboard().addPlayerToTeam(profile.getName(), team);
                        message_.editMessage(BotHelper.generateFeedbackMessage(new TranslatableText("command.register.success.title").getMinecraftText(),
                                new TranslatableText("command.register.success.message").getMinecraftText(), BotHelper.MessageType.SUCCESS)).queue();
                    });
                }
            } else {
                BotHelper.sendFeedbackMessage(channel, new TranslatableText("command.register.fail.not_allowed.title").getMinecraftText(),
                        new TranslatableText("command.register.fail.not_allowed.message").getMinecraftText(), MessageType.NORMAL);
            }
        });
    }
}
