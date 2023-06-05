package com.awakenedredstone.autowhitelist.discord;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.config.EntryData;
import com.awakenedredstone.autowhitelist.util.ExtendedGameProfile;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelist;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelistEntry;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.util.List;
import java.util.Optional;

import static com.awakenedredstone.autowhitelist.discord.Bot.guild;
import static com.awakenedredstone.autowhitelist.util.Debugger.analyzeTimings;

public class DiscordDataProcessor implements Runnable {

    @Override
    public void run() {
        updateWhitelist();
    }

    public void updateWhitelist() {
        analyzeTimings("DiscordDataProcessor#updateWhitelist", () -> {
                    if (guild == null) return;

                    ExtendedWhitelist whitelist = (ExtendedWhitelist) AutoWhitelist.server.getPlayerManager().getWhitelist();

                    List<Member> members = guild.findMembers(v -> {
                        if (v.getUser().isBot()) return false;
                        return hasRole(v.getRoles());
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
                        String role = getTopRole(member.getRoles()).get();
                        List<ExtendedGameProfile> profiles = whitelist.getProfilesFromDiscordId(member.getId());
                        if (profiles.isEmpty()) continue;
                        if (profiles.size() > 1) {
                            AutoWhitelist.LOGGER.warn("Duplicate entries of Discord user with id {}. All of them will be removed.", member.getId());
                            profiles.forEach(whitelist::remove);
                            continue;
                        }

                        ExtendedGameProfile profile = profiles.get(0);
                        if (!profile.getRole().equals(role)) {
                            EntryData entry = AutoWhitelist.whitelistDataMap.get(role);
                            whitelist.add(new ExtendedWhitelistEntry(profile.withRole(role)));

                            entry.assertSafe();
                            entry.updateUser(profile);
                        }
                    }
                });
        analyzeTimings("AutoWhitelist#updateWhitelist", AutoWhitelist::updateWhitelist);
    }

    private boolean hasRole(List<Role> roles) {
        for (Role r : roles)
            if (AutoWhitelist.whitelistDataMap.containsKey(r.getId())) return true;

        return false;
    }

    private Optional<String> getTopRole(List<Role> roles) {
        for (Role r : roles)
            if (AutoWhitelist.whitelistDataMap.containsKey(r.getId())) return Optional.of(r.getId());

        return Optional.empty();
    }
}
