package com.awakenedredstone.autowhitelist.discord;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.mixin.ServerConfigEntryMixin;
import com.awakenedredstone.autowhitelist.util.ExtendedGameProfile;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelist;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelistEntry;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.awakenedredstone.autowhitelist.discord.Bot.*;
import static com.awakenedredstone.autowhitelist.util.Debugger.analyzeTimings;

public class DiscordDataProcessor implements Runnable {

    @Override
    public void run() {
        updateWhitelist();
    }

    public void updateWhitelist() {
        analyzeTimings("DiscordDataProcessor#updateWhitelist", () -> {
                    Guild guild = jda.getGuildById(serverId);
                    if (guild == null) {
                        return;
                    }

                    ExtendedWhitelist whitelist = (ExtendedWhitelist) AutoWhitelist.server.getPlayerManager().getWhitelist();

                    List<Member> members = guild.findMembers(v -> {
                        if (v.getUser().isBot()) return false;
                        return !Collections.disjoint(v.getRoles().stream().map(Role::getId).toList(), new ArrayList<>(whitelistDataMap.keySet()));
                    }).get();
                    List<String> memberIds = members.stream().map(ISnowflake::getId).toList();

                    List<ExtendedGameProfile> invalidPlayers = whitelist.getEntries().stream().map(entry -> {
                        ((ServerConfigEntryMixin<?>) entry).callGetKey();
                        try {
                            return (ExtendedGameProfile) ((ServerConfigEntryMixin<?>) entry).getKey();
                        } catch (ClassCastException exception) {
                            return null;
                        }
                    }).filter(Objects::nonNull).filter(entry -> !memberIds.contains(entry.getDiscordId())).toList();

                    if (!invalidPlayers.isEmpty()) {
                        for (ExtendedGameProfile invalidPlayer : invalidPlayers) {
                            AutoWhitelist.removePlayer(invalidPlayer);
                        }
                    }

                    for (Member member : members) {
                        String highestRole = member.getRoles().stream().map(Role::getId).filter(whitelistDataMap::containsKey).toList().get(0);
                        String teamName = whitelistDataMap.get(highestRole);
                        List<ExtendedGameProfile> profiles = whitelist.getFromDiscordId(member.getId());
                        if (profiles.isEmpty()) continue;
                        if (profiles.size() > 1) {
                            AutoWhitelist.LOGGER.warn("Duplicate entries of Discord user with id {}. All of them will be removed.", member.getId());
                            profiles.forEach(profile -> whitelist.remove(new ExtendedWhitelistEntry(new ExtendedGameProfile(profile.getId(), profile.getName(), profile.getTeam(), profile.getDiscordId()))));
                            continue;
                        }
                        ExtendedGameProfile profile = profiles.get(0);
                        if (!profile.getTeam().equals(teamName)) {
                            whitelist.remove(profile);
                            whitelist.add(new ExtendedWhitelistEntry(new ExtendedGameProfile(profile.getId(), profile.getName(), teamName, profile.getDiscordId())));
                        }
                    }
                });
        analyzeTimings("AutoWhitelist#updateWhitelist", AutoWhitelist::updateWhitelist);
    }
}
