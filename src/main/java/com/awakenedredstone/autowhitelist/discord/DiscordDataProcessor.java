package com.awakenedredstone.autowhitelist.discord;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.config.ConfigData;
import com.awakenedredstone.autowhitelist.database.SQLite;
import com.awakenedredstone.autowhitelist.util.MemberPlayer;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.awakenedredstone.autowhitelist.discord.Bot.*;

public class DiscordDataProcessor {

    private void updateWhitelist() {
        List<String> ids = new SQLite().getIds();
        List<MemberPlayer> memberList = new SQLite().getMembers();
        Guild guild = jda.getGuildById(serverId);
        if (guild == null) {
            AutoWhitelist.logger.error("Failed to get discord server, got null");
            return;
        }
        guild.loadMembers().onSuccess(members -> {
            List<Member> users = members.stream().filter(member -> ids.contains(member.getId())).collect(Collectors.toList());

            for (Member user : users) {

                List<MemberPlayer> players = memberList.stream().filter(player -> user.getId().equals(player.getUserId())).collect(Collectors.toList());
                MemberPlayer player = players.get(0);

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
                                        new SQLite().updateData(user.getId(), getUsername(player.getProfile().getId().toString()), player.getProfile().getId().toString(), entry.getKey());
                                    } catch (IOException e) {
                                        AutoWhitelist.logger.error("Failed to get username!", e);
                                        return;
                                    }
                                }
                            }
                        }
                    }
                } else if (!AutoWhitelist.server.getPlayerManager().isOperator(player.getProfile())) {
                    new SQLite().removeMemberById(player.getUserId());
                    AutoWhitelist.removePlayer(player.getProfile());
                }
            }
        });
    }
}
