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

   /* private void updateWhitelist() {
        List<String> ids = new SQLite().getIds();
        List<ExtendedGameProfile> memberList = new SQLite().getMembers();
        Guild guild = jda.getGuildById(serverId);
        if (guild == null) {
            AutoWhitelist.logger.error("Failed to get discord server, got null");
            return;
        }
        guild.loadMembers().onSuccess(members -> {
            List<Member> users = members.stream().filter(member -> ids.contains(member.getId())).collect(Collectors.toList());

            for (Member user : users) {

                List<ExtendedGameProfile> players = memberList.stream().filter(player -> user.getId().equals(player.getDiscordId())).collect(Collectors.toList());
                ExtendedGameProfile player = players.get(0);

                List<String> roles = getMemberRoles();
                List<Role> userRoles = user.getRoles().stream().filter((role) -> roles.contains(role.getId())).collect(Collectors.toList());
                if (userRoles.size() >= 1) {
                    int higher = 0;
                    Role best = null;
                    for (Role role : userRoles) {
                        if (role.getPosition() > higher) {
                            higher = role.getPosition();
                            best = role;
                        }
                    }
                    if (best == null) {
                        AutoWhitelist.logger.error("Failed to get best tier role!");
                        return;
                    }
                    for (Map.Entry<String, JsonElement> entry : AutoWhitelist.config.getConfigs().get("whitelist").getAsJsonObject().entrySet()) {
                        JsonArray jsonArray = entry.getValue().getAsJsonArray();
                        for (JsonElement value : jsonArray) {
                            if (value.getAsString().equals(best.getId())) {
                                if (ids.contains(user.getId()) && !player.getTeam().equals(entry.getKey())) {
                                    try {
                                        new SQLite().updateData(user.getId(), getUsername(player.getId().toString()), player.getId().toString(), entry.getKey());
                                    } catch (IOException e) {
                                        AutoWhitelist.logger.error("Failed to get username!", e);
                                        return;
                                    }
                                }
                            }
                        }
                    }
                } else if (!AutoWhitelist.server.getPlayerManager().isOperator(player)) {
                    new SQLite().removeMemberById(player.getDiscordId());
                    AutoWhitelist.removePlayer(player);
                }
            }
        });
    }*/

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
                        return !Collections.disjoint(v.getRoles().stream().map(Role::getId).collect(Collectors.toList()), new ArrayList<>(whitelistDataMap.keySet()));
                    }).get();
                    List<String> memberIds = members.stream().map(ISnowflake::getId).collect(Collectors.toList());

                    List<ExtendedGameProfile> invalidPlayers = whitelist.getEntries().stream().map(entry -> {
                        ((ServerConfigEntryMixin<?>) entry).callGetKey();
                        try {
                            return (ExtendedGameProfile) ((ServerConfigEntryMixin<?>) entry).getKey();
                        } catch (ClassCastException exception) {
                            return null;
                        }
                    }).filter(Objects::nonNull).filter(entry -> !memberIds.contains(entry.getDiscordId())).collect(Collectors.toList());

                    if (!invalidPlayers.isEmpty()) {
                        for (ExtendedGameProfile invalidPlayer : invalidPlayers) {
                            AutoWhitelist.removePlayer(invalidPlayer);
                        }
                    }

                    for (Member member : members) {
                        String highestRole = member.getRoles().stream().map(Role::getId).filter(whitelistDataMap::containsKey).collect(Collectors.toList()).get(0);
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
