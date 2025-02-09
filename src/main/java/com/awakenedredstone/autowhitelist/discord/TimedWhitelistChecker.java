package com.awakenedredstone.autowhitelist.discord;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.entry.BaseEntryAction;
import com.awakenedredstone.autowhitelist.entry.RoleActionMap;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedGameProfile;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelist;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelistEntry;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class TimedWhitelistChecker implements Runnable {
    @Override
    public void run() {
        try {
            updateWhitelist();
        } catch (Throwable e) {
            AutoWhitelist.LOGGER.error("Failed to run the periodic whitelist check!", e);
        }
    }

    public void updateWhitelist() {
        if (DiscordBot.getGuild() == null) return;
        AutoWhitelist.LOGGER.debug("Reading trough whitelisted players");

        ExtendedWhitelist whitelist = (ExtendedWhitelist) AutoWhitelist.getServer().getPlayerManager().getWhitelist();

        // Get all users that qualify for whitelist
        List<Member> members = DiscordBot.getGuild().findMembers(member -> {
            if (member.getUser().isBot()) return false;
            return DiscordBotHelper.getHighestEntryRole(member).isPresent();
        }).get();

        List<String> memberIds = members.stream().map(ISnowflake::getId).toList();

        // Get a list of players that has no valid role to stay whitelisted
        List<ExtendedGameProfile> playersToRemove = whitelist.getEntries().stream()
          .filter(entry -> entry instanceof ExtendedWhitelistEntry)
          .map(entry -> ((ExtendedWhitelistEntry) entry).getProfile())
          .filter(profile -> !memberIds.contains(profile.getDiscordId()))
          .toList();

        // Remove players that shouldn't be whitelisted anymore
        if (!playersToRemove.isEmpty()) {
            AutoWhitelist.LOGGER.debug("Removing {} players that don't qualify", playersToRemove.size());
            for (ExtendedGameProfile profile : playersToRemove) {
                AutoWhitelist.LOGGER.debug("Removing entry for {}", profile.getName());
                AutoWhitelist.removePlayer(profile);
            }
        }

        for (Member member : members) {
            List<ExtendedGameProfile> profiles = whitelist.getProfilesFromDiscordId(member.getId());

            Optional<Role> highestRole = DiscordBotHelper.getHighestEntryRole(DiscordBotHelper.getRolesForMember(member));
            // Handle race condition, since a user can have the role changed between the list creation and this point
            if (highestRole.isEmpty()) {
                continue;
            }

            if (profiles.isEmpty()) continue;

            // Remove duplicates
            if (profiles.size() > 1) {
                AutoWhitelist.LOGGER.warn("Duplicate entries of Discord user with id {}. All of them will be removed.", member.getId());
                profiles.forEach(whitelist::remove);
                continue;
            }

            // Update profile to a new entry if needed
            ExtendedGameProfile profile = profiles.get(0);
            if (!profile.getRole().equals(highestRole.get().getId())) {
                AutoWhitelist.LOGGER.debug("Updating entry for {}", profile.getName());
                BaseEntryAction entry = RoleActionMap.get(highestRole.get());
                @Nullable BaseEntryAction oldEntry = RoleActionMap.getNullable(profile.getRole());
                if (oldEntry != null && !oldEntry.isValid()) {
                    AutoWhitelist.LOGGER.warn("Failed to validate old entry {}, could not update whitelist for {}", oldEntry, member.getEffectiveName());
                    continue;
                }
                if (!entry.isValid()) {
                    AutoWhitelist.LOGGER.warn("Failed to validate new entry {}, could not update whitelist for {}", entry, member.getEffectiveName());
                    continue;
                }
                whitelist.add(new ExtendedWhitelistEntry(profile.withRole(highestRole.get())));
                entry.updateUser(profile, oldEntry);
            }
        }

        if (AutoWhitelist.getServer().getPlayerManager().isWhitelistEnabled()) {
            AutoWhitelist.getServer().kickNonWhitelistedPlayers(AutoWhitelist.getServer().getCommandSource());
        }
    }
}
