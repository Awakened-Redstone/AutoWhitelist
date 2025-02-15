package com.awakenedredstone.autowhitelist.discord.events;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.entry.BaseEntryAction;
import com.awakenedredstone.autowhitelist.discord.DiscordBot;
import com.awakenedredstone.autowhitelist.discord.DiscordBotHelper;
import com.awakenedredstone.autowhitelist.discord.PeriodicWhitelistChecker;
import com.awakenedredstone.autowhitelist.entry.RoleActionMap;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedGameProfile;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelist;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelistEntry;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class CoreEvents extends ListenerAdapter {
    @Override
    public void onGenericEvent(@NotNull GenericEvent event) {
        if (DiscordBot.eventWaiter != null && !DiscordBot.eventWaiter.isShutdown()) {
            DiscordBot.eventWaiter.onEvent(event);
        }
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        if (!DiscordBot.botExists()) {
            AutoWhitelist.LOGGER.error("The bot was marked as ready but it doesn't exist, refusing to proceed");
            return;
        }

        if (AutoWhitelist.getServer() == null) {
            AutoWhitelist.LOGGER.error("The bot was ready while the server was non existent, refusing to proceed");
            if (DiscordBot.getInstance() != null) {
                DiscordBot.getInstance().interrupt();
            }
            return;
        }

        if (DiscordBot.scheduledUpdate != null) {
            DiscordBot.scheduledUpdate.cancel(false);
            try {
                DiscordBot.scheduledUpdate.get();
            } catch (Throwable ignored) {/**/}
        }

        DiscordBot.setGuild(DiscordBot.getJda().getGuildById(AutoWhitelist.CONFIG.discordServerId));
        if (DiscordBot.getGuild() == null) {
            AutoWhitelist.LOGGER.error("Could not find the guild with id {}", AutoWhitelist.CONFIG.discordServerId);
            return;
        }

        try {
            DiscordBot.scheduledUpdate = DiscordBot.EXECUTOR_SERVICE.scheduleWithFixedDelay(new PeriodicWhitelistChecker(), 0, AutoWhitelist.CONFIG.updatePeriod, TimeUnit.SECONDS);
            AutoWhitelist.LOGGER.info("Load complete.");
        } catch (Throwable e) {
            AutoWhitelist.LOGGER.error("Failed to schedule the periodic whitelist checker", e);
        }

        DiscordBot.eventWaiter = new EventWaiter();

        AutoWhitelist.updateEntryMap(AutoWhitelist.CONFIG.entries);
    }

    @Override
    public void onShutdown(@NotNull ShutdownEvent event) {
        if (DiscordBot.scheduledUpdate != null) {
            DiscordBot.scheduledUpdate.cancel(false);
            try {
                DiscordBot.scheduledUpdate.get();
            } catch (Throwable ignored) {/**/}
            DiscordBot.scheduledUpdate = null;
        }
        if (DiscordBot.eventWaiter != null && !DiscordBot.eventWaiter.isShutdown()) {
            DiscordBot.eventWaiter.shutdown();
            DiscordBot.eventWaiter = null;
        }
        DiscordBot.setJda(null);
        DiscordBot.setGuild(null);
    }

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent e) {
        User user = e.getUser();
        ExtendedWhitelist whitelist = (ExtendedWhitelist) AutoWhitelist.getServer().getPlayerManager().getWhitelist();

        List<ExtendedGameProfile> players = whitelist.getEntries().stream()
            .filter(entry -> entry.getKey() instanceof ExtendedGameProfile)
            .filter(entry -> user.getId().equals(((ExtendedGameProfile) entry.getKey()).getDiscordId()))
            .map(entry -> (ExtendedGameProfile) entry.getKey())
            .toList();

        if (players.size() > 1) {
            AutoWhitelist.LOGGER.error("Found more than one registered user with same discord id: {}", user.getId(), new IllegalStateException("Could not update the whitelist, found more than one entry with the same discord id."));
            return;
        } else if (players.isEmpty()) return;
        ExtendedGameProfile player = players.get(0);

        if (!AutoWhitelist.getServer().getPlayerManager().isOperator(player)) {
            AutoWhitelist.removePlayer(player);
        }
    }

    @Override
    public void onGuildMemberRoleAdd(@NotNull GuildMemberRoleAddEvent e) {
        AutoWhitelist.LOGGER.debug("User \"{}\" gained the role(s) \"{}\"", e.getMember().getEffectiveName(), String.join(", ", e.getRoles().stream().map(Role::getName).toList()));
        updateUser(e.getMember());
    }

    @Override
    public void onGuildMemberRoleRemove(@NotNull GuildMemberRoleRemoveEvent e) {
        AutoWhitelist.LOGGER.debug("User \"{}\" lost the role(s) \"{}\"", e.getMember().getEffectiveName(), String.join(", ", e.getRoles().stream().map(Role::getName).toList()));
        updateUser(e.getMember());
    }

    private void updateUser(Member member) {
        AutoWhitelist.LOGGER.debug("Updating entry for {}", member.getEffectiveName());
        Optional<Role> role = DiscordBotHelper.getHighestEntryRole(DiscordBotHelper.getRolesForMember(member));

        ExtendedWhitelist whitelist = (ExtendedWhitelist) AutoWhitelist.getServer().getPlayerManager().getWhitelist();

        List<ExtendedGameProfile> profiles = whitelist.getProfilesFromDiscordId(member.getId());
        if (profiles.isEmpty()) return;
        if (profiles.size() > 1) {
            AutoWhitelist.LOGGER.warn("Duplicate entries of Discord user with id {}. All of them will be removed.", member.getId());
            profiles.forEach(whitelist::remove);
            return;
        }

        ExtendedGameProfile profile = profiles.get(0);
        if (role.isEmpty()) {
            AutoWhitelist.removePlayer(profile);
            return;
        }

        BaseEntryAction entry = RoleActionMap.get(role.get());
        BaseEntryAction oldEntry = RoleActionMap.getNullable(profile.getRole());
        if (!profile.getRole().equals(role.get().getId())) {
            if (oldEntry != null && !oldEntry.isValid()) {
                AutoWhitelist.LOGGER.error("Failed to validate old entry {}! Could not update user whitelist for {}", oldEntry, member.getEffectiveName());
                return;
            }

            if (!entry.isValid()) {
                AutoWhitelist.LOGGER.error("Failed to validate new entry {}! Could not update user whitelist for {}", entry, member.getEffectiveName());
                return;
            }

            whitelist.add(new ExtendedWhitelistEntry(profile.withRole(role.get())));
            entry.updateUser(profile, oldEntry);
        }

        if (AutoWhitelist.getServer().getPlayerManager().isWhitelistEnabled()) {
            AutoWhitelist.getServer().kickNonWhitelistedPlayers(AutoWhitelist.getServer().getCommandSource());
        }
    }
}
