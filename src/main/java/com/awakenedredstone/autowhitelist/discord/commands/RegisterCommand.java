package com.awakenedredstone.autowhitelist.discord.commands;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.discord.BotHelper;
import com.awakenedredstone.autowhitelist.mixin.ServerConfigEntryMixin;
import com.awakenedredstone.autowhitelist.util.ExtendedGameProfile;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelist;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelistEntry;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.mojang.authlib.GameProfile;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.LiteralText;
import com.awakenedredstone.autowhitelist.lang.TranslatableText;

import java.util.*;
import java.util.stream.Collectors;

import static com.awakenedredstone.autowhitelist.discord.Bot.whitelistDataMap;
import static com.awakenedredstone.autowhitelist.discord.BotHelper.*;
import static com.awakenedredstone.autowhitelist.util.Debugger.analyzeTimings;
import static net.dv8tion.jda.api.Permission.*;

public class RegisterCommand extends Command {

    public RegisterCommand() {
        this.name = "register";
        this.help = "Adds the informed minecraft account to the Member Server whitelist. [Members only]";
        this.category = new Category("Server integration");
        this.botPermissions = new Permission[]{MESSAGE_ATTACH_FILES, MESSAGE_HISTORY, MESSAGE_EMBED_LINKS, MESSAGE_READ, MESSAGE_WRITE, VIEW_CHANNEL};
        this.arguments = "<minecraft username>";
        this.guildOnly = true;
    }

    @Override
    protected void execute(CommandEvent e) {
        analyzeTimings("RegisterCommand#execute", () -> {
            MessageChannel channel = e.getChannel();
            Member member = e.getMember();
            if (member == null) return;

            sendTempFeedbackMessage(e.getChannel(), new TranslatableText("command.feedback.received.title"), new TranslatableText("command.feedback.received.message"), 7);

            String id = member.getId();
            List<Role> roles = member.getRoles();

            boolean accepted = !Collections.disjoint(roles.stream().map(Role::getId).collect(Collectors.toList()), new ArrayList<>(whitelistDataMap.keySet()));
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
                    BotHelper.sendFeedbackMessage(channel, new TranslatableText("command.register.already_registered.title"), new TranslatableText("command.register.already_registered.message"), MessageType.WARNING);
                    return;
                }

                String highestRole = roles.stream().map(Role::getId).filter(whitelistDataMap::containsKey).collect(Collectors.toList()).get(0);
                String teamName = whitelistDataMap.get(highestRole);
                Team team = server.getScoreboard().getTeam(teamName);
                if (team == null) {
                    BotHelper.sendFeedbackMessage(channel, new TranslatableText("command.fail.title"), new TranslatableText("command.register.fatal.null_team", teamName), BotHelper.MessageType.FATAL);
                    return;
                }

                String arguments = e.getArgs();
                if (arguments.isEmpty()) {
                    BotHelper.sendFeedbackMessage(channel, new TranslatableText("command.few_args.title"), new TranslatableText("command.few_args.message"), MessageType.WARNING);
                    return;
                }
                String[] args = arguments.split(" ");
                if (args.length > 1) {
                    BotHelper.sendFeedbackMessage(channel, new TranslatableText("command.too_many_args.title"), new TranslatableText("command.too_many_args.message"), MessageType.WARNING);
                    return;
                }
                String arg = args[0];

                {
                    if (arg.length() > 16) {
                        sendFeedbackMessage(e.getChannel(), new TranslatableText("command.register.invalid_username.title"), new TranslatableText("command.register.invalid_username.message.too_long"), MessageType.WARNING);
                        return;
                    } else if (arg.length() < 3) {
                        sendFeedbackMessage(e.getChannel(), new TranslatableText("command.register.invalid_username.title"), new TranslatableText("command.register.invalid_username.message.too_short"), MessageType.WARNING);
                        return;
                    }
                }
                {

                }
                GameProfile profile = server.getUserCache().findByName(arg).orElse(null);
                if (profile == null) {
                    BotHelper.sendFeedbackMessage(channel, new TranslatableText("command.fail.title"), new TranslatableText("command.register.fail.account_data", arg), BotHelper.MessageType.ERROR);
                    return;
                }
                ExtendedGameProfile extendedProfile = new ExtendedGameProfile(profile.getId(), profile.getName(), teamName, id);
                boolean whitelisted;
                whitelisted = whitelist.isAllowed(extendedProfile);
                if (!whitelisted)
                    whitelisted = server.getPlayerManager().getWhitelist().isAllowed(new GameProfile(profile.getId(), arg));
                if (whitelisted) {
                    BotHelper.sendFeedbackMessage(channel, new TranslatableText("command.register.username_already_registered.title"), new TranslatableText("command.register.username_already_registered.title"), BotHelper.MessageType.ERROR);
                } else {
                    Message message = BotHelper.generateFeedbackMessage(new TranslatableText("command.register.last_steps.title"), new LiteralText("command.register.last_steps.message"), BotHelper.MessageType.INFO);
                    MessageAction feedbackMessage = channel.sendMessage(message);
                    feedbackMessage.queue(message_ -> {
                        whitelist.add(new ExtendedWhitelistEntry(extendedProfile));
                        server.getScoreboard().addPlayerToTeam(profile.getName(), team);
                        message_.editMessage(BotHelper.generateFeedbackMessage(new TranslatableText("command.register.success.title"), new TranslatableText("command.register.success.message"), BotHelper.MessageType.SUCCESS)).queue();
                    });
                }
            } else {
                BotHelper.sendFeedbackMessage(channel, new TranslatableText("command.register.fail.not_allowed.title"), new TranslatableText("command.register.fail.not_allowed.message"), MessageType.NORMAL);
            }
        });
    }
}
