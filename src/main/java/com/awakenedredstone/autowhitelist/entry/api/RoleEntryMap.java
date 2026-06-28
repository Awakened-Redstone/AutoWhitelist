package com.awakenedredstone.autowhitelist.entry.api;

import com.awakenedredstone.autowhitelist.discord.DiscordClientHolder;
import com.awakenedredstone.autowhitelist.discord.util.RoleUtils;
import com.awakenedredstone.autowhitelist.entry.Entry;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Role;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RoleEntryMap {
    public static final Logger LOGGER = LoggerFactory.getLogger(RoleEntryMap.class);
    private static final Map<String, Entry> ROLE_ACTION_MAP = new HashMap<>();

    public static void clear() {
        ROLE_ACTION_MAP.clear();
    }

    public static void register(String role, Entry action) {
        ROLE_ACTION_MAP.put(role, action);
    }

    public static void register(Role role, Entry action) {
        register(role.getId().asString(), action);
    }

    @NotNull
    public static Entry get(String role) {
        Entry action = ROLE_ACTION_MAP.get(role);
        if (action == null) {
            throw new NullPointerException("Tried to get action from role but got null!");
        }

        return action;
    }

    @NotNull
    public static Entry get(Role role) {
        return get(role.getId().asString());
    }

    @Nullable
    public static Entry getNullable(String role) {
        return ROLE_ACTION_MAP.get(role);
    }

    @Nullable
    public static Entry getNullable(Role role) {
        return getNullable(role.getId().asString());
    }

    public static boolean containsRole(String roleId) {
        return ROLE_ACTION_MAP.containsKey(roleId);
    }

    public static boolean containsRole(Role role) {
        return containsRole(role.getId().asString());
    }

    public static boolean containsRole(Snowflake roleId) {
        return containsRole(roleId.asString());
    }

    public static void reload(List<Entry> entries) {
        if (!DiscordClientHolder.hasGuild()) {
            LOGGER.warn("Tried to reload entry map while no guild is available, too early?");
            return;
        }

        RoleEntryMap.clear();
        for (Entry entry : entries) {
            for (String roleString : entry.roles()) {
                Optional<Role> perhapsRole = RoleUtils.getRoleFromString(roleString);
                if (perhapsRole.isPresent()) {
                    RoleEntryMap.register(perhapsRole.get(), entry);
                } else {
                    LOGGER.warn("Invalid role \"{}\", ignoring", roleString);
                }
            }
        }
    }
}
