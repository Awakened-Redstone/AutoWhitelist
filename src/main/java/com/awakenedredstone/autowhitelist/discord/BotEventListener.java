package com.awakenedredstone.autowhitelist.discord;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.discord.commands.RegisterCommand;
import com.awakenedredstone.autowhitelist.discord.commands.developer.BotStatusCommand;
import com.awakenedredstone.autowhitelist.discord.commands.developer.ServerStatusCommand;
import com.awakenedredstone.autowhitelist.discord.commands.developer.StatusCommand;
import com.awakenedredstone.autowhitelist.mixin.ServerConfigEntryMixin;
import com.awakenedredstone.autowhitelist.util.ExtendedGameProfile;
import com.awakenedredstone.autowhitelist.util.FailedToUpdateWhitelistException;
import com.awakenedredstone.autowhitelist.util.InvalidTeamNameException;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelist;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelistEntry;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.examples.command.PingCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.awakenedredstone.autowhitelist.discord.Bot.*;
import static com.awakenedredstone.autowhitelist.util.Debugger.analyzeTimings;

public class BotEventListener extends ListenerAdapter {

    @Override
    public void onReady(@NotNull ReadyEvent e) {
        AutoWhitelist.LOGGER.info("Bot started. Parsing registered users.");

        CommandClientBuilder builder = new CommandClientBuilder();
        builder.setPrefix(prefix);
        builder.setOwnerId("387745099204919297");
        builder.setCoOwnerIds(AutoWhitelist.getConfigData().owners);
        builder.setHelpConsumer(generateHelpConsumer());
        builder.addCommands(
                new RegisterCommand(),
                new PingCommand(),

                //Developer commands
                new ServerStatusCommand(),
                new BotStatusCommand(),
                new StatusCommand()
        );
        CommandClient command = builder.build();

        jda.addEventListener(command);

        if (scheduledUpdate != null) {
            scheduledUpdate.cancel(false);
            try {
                scheduledUpdate.get();
            } catch (Exception ignored) {
            }
        }
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduledUpdate = scheduler.scheduleWithFixedDelay(new DiscordDataProcessor(), 0, updateDelay, TimeUnit.SECONDS);
    }

    private Consumer<CommandEvent> generateHelpConsumer() {
        return (event) -> {
            EmbedBuilder builder = new EmbedBuilder().setAuthor(jda.getSelfUser().getName(), "https://discord.com", jda.getSelfUser().getAvatarUrl());
            Command.Category category;
            List<MessageEmbed.Field> fields = new ArrayList<>();
            for (Command command : event.getClient().getCommands()) {
                if ((!command.isHidden() && !command.isOwnerCommand()) || event.isOwner()) {

                    String command_ = "\n`" +
                            event.getClient().getPrefix() +
                            (prefix == null ? " " : "") +
                            command.getName() +
                            (command.getArguments() == null ? "" : " " + command.getArguments()) +
                            "` \u200E \u200E | \u200E \u200E " + command.getHelp();

                    category = command.getCategory();
                    fields.add(new MessageEmbed.Field(category == null ? "No Category" : category.getName(), command_, false));
                }
            }

            List<MessageEmbed.Field> mergedFields = new ArrayList<>();
            String commands = "";
            String lastName = "";
            for (MessageEmbed.Field field : fields) {
                if (Objects.equals(field.getName(), lastName)) {
                    commands += "\n" + field.getValue();
                    if (fields.get(fields.size() - 1) == field) {
                        mergedFields.add(new MessageEmbed.Field(lastName, commands, false));
                    }
                } else if (!commands.isEmpty()) {
                    mergedFields.add(new MessageEmbed.Field(lastName, commands, false));
                    commands = "";
                    commands += "\n" + field.getValue();
                    lastName = field.getName();
                } else if (fields.size() > 1) {
                    commands += field.getValue();
                    lastName = field.getName();
                } else {
                    mergedFields.add(new MessageEmbed.Field(field.getName(), field.getValue(), false));
                }
            }

            mergedFields.forEach(builder::addField);
            event.reply(builder.build());
        };
    }

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent e) {
        User user = e.getUser();
        ExtendedWhitelist whitelist = (ExtendedWhitelist) AutoWhitelist.server.getPlayerManager().getWhitelist();

        List<ExtendedGameProfile> players = new ArrayList<>();
        whitelist.getEntries().stream().filter(entry -> {
            ((ServerConfigEntryMixin<?>) entry).callGetKey();
            try {
                return user.getId().equals(((ExtendedGameProfile) ((ServerConfigEntryMixin<?>) entry).getKey()).getDiscordId());
            } catch (ClassCastException exception) {
                return false;
            }
        }).findFirst().map(v -> {
            ((ServerConfigEntryMixin<?>) v).callGetKey();
            return (ExtendedGameProfile) ((ServerConfigEntryMixin<?>) v).getKey();
        }).ifPresent(players::add);
        if (players.size() > 1) {
            AutoWhitelist.LOGGER.error("Found more than one registered user with same discord id: {}", user.getId(), new FailedToUpdateWhitelistException("Could not update the whitelist, found multiple"));
            return;
        } else if (players.size() == 0) return;
        ExtendedGameProfile player = players.get(0);

        if (!AutoWhitelist.server.getPlayerManager().isOperator(player)) {
            AutoWhitelist.removePlayer(player);
        }
    }

    @Override
    public void onGuildMemberRoleAdd(@NotNull GuildMemberRoleAddEvent e) {
        updateUser(e.getMember(), e.getRoles());
    }

    @Override
    public void onGuildMemberRoleRemove(@NotNull GuildMemberRoleRemoveEvent e) {
        updateUser(e.getMember(), e.getRoles());
    }

    private void updateUser(Member member, List<Role> roles) {
        analyzeTimings("BotEventListener#updateUser", () -> {
            if (Collections.disjoint(roles.stream().map(Role::getId).collect(Collectors.toList()), new ArrayList<>(whitelistDataMap.keySet()))) {
                return;
            }

            ExtendedWhitelist whitelist = (ExtendedWhitelist) AutoWhitelist.server.getPlayerManager().getWhitelist();

            List<ExtendedGameProfile> profiles = whitelist.getFromDiscordId(member.getId());
            if (profiles.isEmpty()) return;
            if (profiles.size() > 1) {
                AutoWhitelist.LOGGER.warn("Duplicate entries of Discord user with id {}. All of them will be removed.", member.getId());
                profiles.forEach(profile -> whitelist.remove(new ExtendedWhitelistEntry(new ExtendedGameProfile(profile.getId(), profile.getName(), profile.getTeam(), profile.getDiscordId()))));
                return;
            }

            List<String> validRoles = member.getRoles().stream().map(Role::getId).filter(whitelistDataMap::containsKey).collect(Collectors.toList());
            if (validRoles.isEmpty()) {
                ExtendedGameProfile profile = profiles.get(0);
                AutoWhitelist.removePlayer(profile);
                return;
            }
            String highestRole = validRoles.get(0);
            String teamName = whitelistDataMap.get(highestRole);
            ExtendedGameProfile profile = profiles.get(0);
            if (!profile.getTeam().equals(teamName)) {
                whitelist.remove(profile);
                whitelist.add(new ExtendedWhitelistEntry(new ExtendedGameProfile(profile.getId(), profile.getName(), teamName, profile.getDiscordId())));

                Scoreboard scoreboard = AutoWhitelist.server.getScoreboard();
                Team team = scoreboard.getTeam(teamName);
                if (team == null) {
                    AutoWhitelist.LOGGER.error("Could not check team information of \"{}\", got \"null\" when trying to get \"net.minecraft.scoreboard.Team\" from \"{}\"", profile.getName(), profile.getTeam(), new InvalidTeamNameException("Tried to get \"net.minecraft.scoreboard.Team\" from \"" + profile.getTeam() + "\" but got \"null\"."));
                    return;
                }
                scoreboard.addPlayerToTeam(profile.getName(), team);
            }
        });
    }
}
