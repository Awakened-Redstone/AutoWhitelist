package com.awakenedredstone.autowhitelist.discord.events;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.config.EntryData;
import com.awakenedredstone.autowhitelist.discord.DiscordDataProcessor;
import com.awakenedredstone.autowhitelist.mixin.ServerConfigEntryMixin;
import com.awakenedredstone.autowhitelist.util.ExtendedGameProfile;
import com.awakenedredstone.autowhitelist.util.FailedToUpdateWhitelistException;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelist;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelistEntry;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.awakenedredstone.autowhitelist.AutoWhitelist.whitelistDataMap;
import static com.awakenedredstone.autowhitelist.discord.Bot.*;
import static com.awakenedredstone.autowhitelist.discord.BotHelper.getRolesForMember;
import static com.awakenedredstone.autowhitelist.util.Debugger.analyzeTimings;

public class CoreEvents {

    @SubscribeEvent
    public void onReady(ReadyEvent e) {
        guild = jda.getGuildById(AutoWhitelist.CONFIG.discordServerId);
        AutoWhitelist.LOGGER.info("Finishing setup.");
        if (scheduledUpdate != null) {
            scheduledUpdate.cancel(false);
            try {
                scheduledUpdate.get();
            } catch (Exception ignored) {/**/}
        }

        AutoWhitelist.LOGGER.info("Parsing registered users.");
        scheduledUpdate = executorService.scheduleWithFixedDelay(new DiscordDataProcessor(), 0, AutoWhitelist.CONFIG.updatePeriod, TimeUnit.SECONDS);
        AutoWhitelist.LOGGER.info("Load complete.");
    }

    @SubscribeEvent
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent e) {
        User user = e.getUser();
        ExtendedWhitelist whitelist = (ExtendedWhitelist) AutoWhitelist.server.getPlayerManager().getWhitelist();

        List<ExtendedGameProfile> players = whitelist.getEntries().stream()
            .filter(entry -> ((ServerConfigEntryMixin<?>) entry).getKey() instanceof ExtendedGameProfile)
            .filter(entry -> user.getId().equals(((ExtendedGameProfile) ((ServerConfigEntryMixin<?>) entry).getKey()).getDiscordId()))
            .map(v -> (ExtendedGameProfile) ((ServerConfigEntryMixin<?>) v).getKey())
            .toList();

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
        updateUser(e.getMember());
    }

    @SubscribeEvent
    public void onGuildMemberRoleRemove(@NotNull GuildMemberRoleRemoveEvent e) {
        updateUser(e.getMember());
    }

    private void updateUser(Member member) {
        analyzeTimings("BotEventListener#updateUser", () -> {
            Optional<String> roleOptional = getTopRole(getRolesForMember(member));

            ExtendedWhitelist whitelist = (ExtendedWhitelist) AutoWhitelist.server.getPlayerManager().getWhitelist();

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
            EntryData entry = whitelistDataMap.get(role);
            ExtendedGameProfile profile = profiles.get(0);
            if (!profile.getRole().equals(role)) {
                whitelist.add(new ExtendedWhitelistEntry(profile.withRole(role)));

                entry.assertSafe();
                entry.updateUser(profile);
            }
        });
    }

    private boolean hasRole(List<Role> roles) {
        for (Role r : roles)
            if (whitelistDataMap.containsKey(r.getId())) return true;

        return false;
    }

    private Optional<String> getTopRole(List<Role> roles) {
        for (Role r : roles)
            if (whitelistDataMap.containsKey(r.getId())) return Optional.of(r.getId());

        return Optional.empty();
    }
}
