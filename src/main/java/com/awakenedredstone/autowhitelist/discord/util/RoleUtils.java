package com.awakenedredstone.autowhitelist.discord.util;

import com.awakenedredstone.autowhitelist.discord.DiscordClientHolder;
import com.awakenedredstone.autowhitelist.entry.api.RoleEntryMap;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.util.OrderUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class RoleUtils {
    public static final Comparator<Role> DESCENDING_ROLE_ORDER = OrderUtil.ROLE_ORDER.reversed();

    public static Mono<List<Role>> collectRoles(Flux<Role> flux) {
        return flux.sort(DESCENDING_ROLE_ORDER).collectList();
    }

    public static List<Role> getRolesForMember(Member member) {
        List<Role> roles = collectRoles(member.getRoles()).blockOptional().orElseGet(ArrayList::new);

        roles.add(DiscordClientHolder.getCurrent().getGuild().getEveryoneRole().blockOptional().orElseThrow());
        return roles;
    }

    public static List<Role> getValidRolesForMember(Member member) {
        return getRolesForMember(member).stream().filter(RoleEntryMap::containsRole).toList();
    }

    public static Optional<Role> getRoleFromString(String roleString) {
        if (!DiscordClientHolder.hasGuild()) return Optional.empty();

        Guild guild = DiscordClientHolder.getCurrent().getGuild();

        if (roleString.charAt(0) != '@') {
            return Optional.ofNullable(guild.getRoleById(Snowflake.of(roleString)).block());
        }

        if (roleString.equalsIgnoreCase("@everyone")) {
            return Optional.ofNullable(guild.getEveryoneRole().block());
        }

        String roleSearch = roleString.substring(1);

        List<Role> roles = collectRoles(guild.getRoles()).block();
        if (roles == null) return Optional.empty();

        return roles.stream().filter(role -> role.getName().equalsIgnoreCase(roleSearch)).findFirst();
    }

    public static Optional<Role> getHighestEntryRole(Member member) {
        return getHighestEntryRole(getRolesForMember(member));
    }

    public static Optional<Role> getHighestEntryRole(List<Role> roles) {
        for (Role role : roles) {
            if (RoleEntryMap.containsRole(role)) {
                return Optional.of(role);
            }
        }

        return Optional.empty();
    }
}
