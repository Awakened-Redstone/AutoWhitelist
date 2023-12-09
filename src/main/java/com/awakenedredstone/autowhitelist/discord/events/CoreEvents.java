package com.awakenedredstone.autowhitelist.discord.events;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.config.EntryData;
import com.awakenedredstone.autowhitelist.discord.Bot;
import com.awakenedredstone.autowhitelist.discord.BotHelper;
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

public class CoreEvents {

    @SubscribeEvent
    public void onReady(ReadyEvent e) {
        Bot.guild = Bot.jda.getGuildById(AutoWhitelist.CONFIG.discordServerId);
        AutoWhitelist.LOGGER.info("Finishing setup.");
        if (Bot.scheduledUpdate != null) {
            Bot.scheduledUpdate.cancel(false);
            try {
                Bot.scheduledUpdate.get();
            } catch (Exception ignored) {/**/}
        }

        AutoWhitelist.LOGGER.info("Parsing registered users.");
        Bot.scheduledUpdate = Bot.EXECUTOR_SERVICE.scheduleWithFixedDelay(new DiscordDataProcessor(), 0, AutoWhitelist.CONFIG.updatePeriod, TimeUnit.SECONDS);
        AutoWhitelist.LOGGER.info("Load complete.");
    }

    @SubscribeEvent
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent e) {
        User user = e.getUser();
        ExtendedWhitelist whitelist = (ExtendedWhitelist) AutoWhitelist.getServer().getPlayerManager().getWhitelist();

        List<ExtendedGameProfile> players = whitelist.getEntries().stream()
            .filter(entry -> ((ServerConfigEntryMixin<?>) entry).getKey() instanceof ExtendedGameProfile)
            .filter(entry -> user.getId().equals(((ExtendedGameProfile) ((ServerConfigEntryMixin<?>) entry).getKey()).getDiscordId()))
            .map(v -> (ExtendedGameProfile) ((ServerConfigEntryMixin<?>) v).getKey())
            .toList();

        if (players.size() > 1) {
            AutoWhitelist.LOGGER.error("Found more than one registered user with same discord id: {}", user.getId(), new FailedToUpdateWhitelistException("Could not update the whitelist, found multiple"));
            return;
        } else if (players.isEmpty()) return;
        ExtendedGameProfile player = players.get(0);

        if (!AutoWhitelist.getServer().getPlayerManager().isOperator(player)) {
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
        Optional<String> roleOptional = getTopRole(BotHelper.getRolesForMember(member));

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
        EntryData entry = AutoWhitelist.ENTRY_MAP_CACHE.get(role);
        ExtendedGameProfile profile = profiles.get(0);
        if (!profile.getRole().equals(role)) {
            whitelist.add(new ExtendedWhitelistEntry(profile.withRole(role)));

            entry.assertSafe();
            entry.updateUser(profile);
        }
    }

    private Optional<String> getTopRole(List<Role> roles) {
        for (Role r : roles) {
            if (whitelistDataMap.containsKey(r.getId())) {
                return Optional.of(r.getId());
            }
        }

        return Optional.empty();
    }
}
