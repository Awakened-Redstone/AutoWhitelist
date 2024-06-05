package com.awakenedredstone.autowhitelist.discord;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.entry.BaseEntry;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedGameProfile;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelist;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelistEntry;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.util.List;
import java.util.Optional;

public class DiscordDataProcessor implements Runnable {

    @Override
    public void run() {
        updateWhitelist();
    }

    public void updateWhitelist() {
        if (DiscordBot.guild == null) return;

        ExtendedWhitelist whitelist = (ExtendedWhitelist) AutoWhitelist.getServer().getPlayerManager().getWhitelist();

        List<Member> members = DiscordBot.guild.findMembers(v -> {
            if (v.getUser().isBot()) return false;
            return hasRole(DiscordBotHelper.getRolesForMember(v));
        }).get();
        List<String> memberIds = members.stream().map(ISnowflake::getId).toList();

        List<ExtendedGameProfile> invalidPlayers = whitelist.getEntries().stream()
          .filter(entry -> entry instanceof ExtendedWhitelistEntry)
          .map(entry -> ((ExtendedWhitelistEntry) entry).getProfile())
          .filter(profile -> !memberIds.contains(profile.getDiscordId()))
          .toList();

        if (!invalidPlayers.isEmpty()) {
            for (ExtendedGameProfile invalidPlayer : invalidPlayers) {
                AutoWhitelist.removePlayer(invalidPlayer);
            }
        }

        for (Member member : members) {
            List<ExtendedGameProfile> profiles = whitelist.getProfilesFromDiscordId(member.getId());

            Optional<String> topRoleOptional = getTopRole(DiscordBotHelper.getRolesForMember(member));
            if (topRoleOptional.isEmpty()) {
                AutoWhitelist.LOGGER.error("Impossible case, the user {} has no valid role, but it passed to update. Please report this bug.", member.getId(), new IllegalStateException());
                profiles.forEach(whitelist::remove);
                continue;
            }

            String topRole = topRoleOptional.get();
            if (profiles.isEmpty()) continue;
            if (profiles.size() > 1) {
                AutoWhitelist.LOGGER.warn("Duplicate entries of Discord user with id {}. All of them will be removed.", member.getId());
                profiles.forEach(whitelist::remove);
                continue;
            }

            ExtendedGameProfile profile = profiles.get(0);
            if (!profile.getRole().equals(topRole)) {
                BaseEntry entry = AutoWhitelist.ENTRY_MAP_CACHE.get(topRole);
                BaseEntry oldEntry = AutoWhitelist.ENTRY_MAP_CACHE.get(profile.getRole());
                whitelist.add(new ExtendedWhitelistEntry(profile.withRole(topRole)));

                entry.assertSafe();
                entry.updateUser(profile, oldEntry);
            }
        }
        AutoWhitelist.updateWhitelist();
    }

    public static boolean hasRole(List<Role> roles) {
        for (Role role : roles) {
            if (AutoWhitelist.ENTRY_MAP_CACHE.containsKey(role.getId())) {
                return true;
            }
        }

        return false;
    }

    public static Optional<String> getTopRole(List<Role> roles) {
        for (Role role : roles) {
            if (AutoWhitelist.ENTRY_MAP_CACHE.containsKey(role.getId())) {
                return Optional.of(role.getId());
            }
        }

        return Optional.empty();
    }
}
