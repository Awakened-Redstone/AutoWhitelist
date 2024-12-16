package com.awakenedredstone.autowhitelist.discord.events;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.debug.DebugFlags;
import com.awakenedredstone.autowhitelist.entry.BaseEntry;
import com.awakenedredstone.autowhitelist.discord.DiscordBot;
import com.awakenedredstone.autowhitelist.discord.DiscordBotHelper;
import com.awakenedredstone.autowhitelist.discord.DiscordDataProcessor;
import com.awakenedredstone.autowhitelist.mixin.ServerConfigEntryMixin;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedGameProfile;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelist;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelistEntry;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import net.dv8tion.jda.api.entities.IMentionable;
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
import org.jetbrains.annotations.Nullable;

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
        if (DiscordBot.jda == null) {
            AutoWhitelist.LOGGER.error("The bot was marked as ready but it doesn't exist, refusing to proceed");
            return;
        }

        if (AutoWhitelist.getServer() == null) {
            AutoWhitelist.LOGGER.error("The bot was ready while the server was null, refusing to proceed");
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

        AutoWhitelist.LOGGER.info("Bot is in {} guilds", DiscordBot.getJDASafe().getGuilds().size());

        DiscordBot.guild = DiscordBot.getJDASafe().getGuildById(AutoWhitelist.CONFIG.discordServerId);
        if (DiscordBot.guild == null) {
            AutoWhitelist.LOGGER.error("Could not find the guild with id {}", AutoWhitelist.CONFIG.discordServerId);
            return;
        }

        AutoWhitelist.LOGGER.info("Parsing registered users.");
        try {
            DiscordBot.scheduledUpdate = DiscordBot.EXECUTOR_SERVICE.scheduleWithFixedDelay(new DiscordDataProcessor(), 0, AutoWhitelist.CONFIG.updatePeriod, TimeUnit.SECONDS);
            AutoWhitelist.LOGGER.info("Load complete.");
        } catch (Throwable e) {
            AutoWhitelist.LOGGER.error("Failed to schedule the discord data processor on an interval", e);
        }

        DiscordBot.eventWaiter = new EventWaiter();

        if (AutoWhitelist.ENTRY_MAP_CACHE.isEmpty()) {
            for (BaseEntry newEntry : AutoWhitelist.CONFIG.entries) {
                for (String roleString : newEntry.getRoles()) {
                    Role role = DiscordBotHelper.getRoleFromString(roleString);
                    if (role != null) {
                        AutoWhitelist.ENTRY_MAP_CACHE.put(role.getId(), newEntry);
                    }
                }
            }
        }
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
        DiscordBot.jda = null;
        DiscordBot.guild = null;
    }

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent e) {
        User user = e.getUser();
        ExtendedWhitelist whitelist = (ExtendedWhitelist) AutoWhitelist.getServer().getPlayerManager().getWhitelist();

        List<ExtendedGameProfile> players = whitelist.getEntries().stream()
            .filter(entry -> ((ServerConfigEntryMixin<?>) entry).getKey() instanceof ExtendedGameProfile)
            .filter(entry -> user.getId().equals(((ExtendedGameProfile) ((ServerConfigEntryMixin<?>) entry).getKey()).getDiscordId()))
            .map(v -> (ExtendedGameProfile) ((ServerConfigEntryMixin<?>) v).getKey())
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
        if (DebugFlags.trackRoleChanges) {
            DebugFlags.LOGGER.info("User \"{}\" have gained the role(s) \"{}\"", e.getMember().getEffectiveName(), String.join(", ", e.getRoles().stream().map(Role::getName).toList()));
        }

        updateUser(e.getMember());
    }

    @Override
    public void onGuildMemberRoleRemove(@NotNull GuildMemberRoleRemoveEvent e) {
        if (DebugFlags.trackRoleChanges) {
            DebugFlags.LOGGER.info("User \"{}\" have lost the role(s) \"{}\"", e.getMember().getEffectiveName(), String.join(", ", e.getRoles().stream().map(Role::getName).toList()));
        }

        updateUser(e.getMember());
    }

    private void updateUser(Member member) {
        Optional<String> roleOptional = getTopRole(DiscordBotHelper.getRolesForMember(member));

        ExtendedWhitelist whitelist = (ExtendedWhitelist) AutoWhitelist.getServer().getPlayerManager().getWhitelist();

        List<ExtendedGameProfile> profiles = whitelist.getProfilesFromDiscordId(member.getId());
        if (profiles.isEmpty()) return;
        if (profiles.size() > 1) {
            AutoWhitelist.LOGGER.warn("Duplicate entries of Discord user with id {}. All of them will be removed.", member.getId());
            profiles.forEach(whitelist::remove);
            return;
        }

        if (roleOptional.isEmpty()) {
            ExtendedGameProfile profile = profiles.get(0);
            AutoWhitelist.removePlayer(profile);
            return;
        }
        String role = roleOptional.get();
        BaseEntry entry = AutoWhitelist.ENTRY_MAP_CACHE.get(role);
        ExtendedGameProfile profile = profiles.get(0);
        BaseEntry oldEntry = AutoWhitelist.ENTRY_MAP_CACHE.get(profile.getRole());
        if (!profile.getRole().equals(role)) {
            whitelist.add(new ExtendedWhitelistEntry(profile.withRole(role)));

            entry.assertSafe();
            entry.updateUser(profile, oldEntry);
        }

        if (AutoWhitelist.getServer().getPlayerManager().isWhitelistEnabled()) {
            AutoWhitelist.getServer().kickNonWhitelistedPlayers(AutoWhitelist.getServer().getCommandSource());
        }
    }

    private Optional<String> getTopRole(List<Role> roles) {
        for (Role r : roles) {
            if (AutoWhitelist.ENTRY_MAP_CACHE.containsKey(r.getId())) {
                return Optional.of(r.getId());
            }
        }

        return Optional.empty();
    }
}
