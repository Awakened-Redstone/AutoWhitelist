package com.awakenedredstone.autowhitelist.discord.events;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.discord.DiscordDataProcessor;
import com.awakenedredstone.autowhitelist.mixin.ServerConfigEntryMixin;
import com.awakenedredstone.autowhitelist.util.ExtendedGameProfile;
import com.awakenedredstone.autowhitelist.util.FailedToUpdateWhitelistException;
import com.awakenedredstone.autowhitelist.util.InvalidTeamNameException;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelist;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelistEntry;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.awakenedredstone.autowhitelist.discord.Bot.*;
import static com.awakenedredstone.autowhitelist.util.Debugger.analyzeTimings;

public class CoreEvents {

    @SubscribeEvent
    public void onReady(ReadyEvent e) {
        AutoWhitelist.LOGGER.info("Finishing setup.");
        /*if (AutoWhitelist.getConfigData().enableSlashCommands) {
            List<Command> commands = jda.retrieveCommands().complete();
            AutoWhitelistAPI.dispatcher().getRoot().getChildren().forEach(command -> {
                if (commands.stream().map(Command::getName).noneMatch(slashCommand -> slashCommand.equalsIgnoreCase(command.getName()))) {
                    registerCommands(command);
                }
            });
        } else {
            jSlashCommandDatada.retrieveCommands().complete().forEach(command -> jda.deleteCommandById(command.getId()).queue());
        }*/

        if (scheduledUpdate != null) {
            scheduledUpdate.cancel(false);
            try {
                scheduledUpdate.get();
            } catch (Exception ignored) {
            }
        }
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        AutoWhitelist.LOGGER.info("Parsing registered users.");
        scheduledUpdate = scheduler.scheduleWithFixedDelay(new DiscordDataProcessor(), 0, updateDelay, TimeUnit.SECONDS);
        AutoWhitelist.LOGGER.info("Load complete.");
    }

    @SubscribeEvent
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
        }).map(v -> {
            ((ServerConfigEntryMixin<?>) v).callGetKey();
            return (ExtendedGameProfile) ((ServerConfigEntryMixin<?>) v).getKey();
        }).forEach(players::add);
        if (players.size() > 1) {
            AutoWhitelist.LOGGER.error("Found more than one registered user with same discord id: {}", user.getId(), new FailedToUpdateWhitelistException("Could not update the whitelist, found multiple"));
            return;
        } else if (players.size() == 0) return;
        ExtendedGameProfile player = players.get(0);

        if (!AutoWhitelist.server.getPlayerManager().isOperator(player)) {
            AutoWhitelist.removePlayer(player);
        }
    }

    @SubscribeEvent
    public void onGuildMemberRoleAdd(@NotNull GuildMemberRoleAddEvent e) {
        updateUser(e.getMember(), e.getRoles());
    }

    @SubscribeEvent
    public void onGuildMemberRoleRemove(@NotNull GuildMemberRoleRemoveEvent e) {
        updateUser(e.getMember(), e.getRoles());
    }

    private void updateUser(Member member, List<Role> roles) {
        analyzeTimings("BotEventListener#updateUser", () -> {
            if (Collections.disjoint(roles.stream().map(Role::getId).toList(), new ArrayList<>(whitelistDataMap.keySet()))) {
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

            List<String> validRoles = member.getRoles().stream().map(Role::getId).filter(whitelistDataMap::containsKey).toList();
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

    private void registerCommands(CommandNode<?> command) {
        AutoWhitelist.LOGGER.info(String.valueOf(command.getRedirect() != null));
        if (command.getRedirect() != null) {
            return;
        }
//        CommandDataImpl commandData = new CommandDataImpl(command.getName().toLowerCase(), new TranslatableText("command.description." + command.getName()).getString());
        boolean required = command.getCommand() == null;
        command.getChildren().forEach(node -> {
            if (node instanceof LiteralCommandNode) {
//                commandData.addOptions(new OptionData(OptionType.SUB_COMMAND, node.getName(), new TranslatableText("command.description." + command.getName() + "." + node.getName()).getString(), required));
            }
            else if (node instanceof ArgumentCommandNode node1) {
                OptionType type;
                ArgumentType<?> type1 = node1.getType();
                if (type1 instanceof BoolArgumentType) {
                    type = OptionType.BOOLEAN;
                } else if (type1 instanceof DoubleArgumentType || type1 instanceof FloatArgumentType) {
                    type = OptionType.NUMBER;
                } else if (type1 instanceof IntegerArgumentType || type1 instanceof LongArgumentType) {
                    type = OptionType.INTEGER;
                } else {
                    type = OptionType.STRING;
                }
//                commandData.addOptions(new OptionData(type, node.getName(), new TranslatableText("command.description." + command.getName() + "." + node.getName()).getString(), required));
            }

        });
//        jda.upsertCommand(commandData).queue();
    }
}
