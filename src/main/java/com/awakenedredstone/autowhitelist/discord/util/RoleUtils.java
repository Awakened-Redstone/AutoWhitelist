package com.awakenedredstone.autowhitelist.discord.util;

import com.awakenedredstone.autowhitelist.discord.DiscordClientHolder;
import com.awakenedredstone.autowhitelist.discord.old.DiscordBot;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Role;
import discord4j.core.util.OrderUtil;

import java.util.List;
import java.util.Optional;

public class RoleUtils {
    public static Optional<Role> getRoleFromString(String roleString) {
        if (!DiscordClientHolder.hasTask()) {
            return Optional.empty();
        }

        Guild guild = DiscordClientHolder.getCurrent().guild.get();

        if (roleString.charAt(0) != '@') {
            return Optional.ofNullable(guild.getRoleById(Snowflake.of(roleString)).block());
        }

        if (roleString.equalsIgnoreCase("@everyone")) {
            return Optional.ofNullable(guild.getEveryoneRole().block());
        }

        String roleSearch = roleString.substring(1);

        List<Role> roles = guild.getRoles().transform(OrderUtil::orderRoles).collectList().block();
        if (roles == null) return Optional.empty();

        return roles.reversed().stream().filter(role -> role.getName().equalsIgnoreCase(roleSearch)).findFirst();

    }
}
