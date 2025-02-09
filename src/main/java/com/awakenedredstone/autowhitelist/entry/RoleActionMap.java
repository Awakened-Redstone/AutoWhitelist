package com.awakenedredstone.autowhitelist.entry;

import net.dv8tion.jda.api.entities.Role;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class RoleActionMap {
    private static final Map<String, BaseEntryAction> ROLE_ACTION_MAP = new HashMap<>();

    public static void clear() {
        ROLE_ACTION_MAP.clear();
    }

    public static void register(String role, BaseEntryAction action) {
        ROLE_ACTION_MAP.put(role, action);
    }

    public static void register(Role role, BaseEntryAction action) {
        register(role.getId(), action);
    }

    @NotNull
    public static BaseEntryAction get(String role) {
        BaseEntryAction action = ROLE_ACTION_MAP.get(role);
        if (action == null) {
            throw new NullPointerException("Tried to get action from role but got null!");
        }

        return action;
    }

    @NotNull
    public static BaseEntryAction get(Role role) {
        return get(role.getId());
    }

    @Nullable
    public static BaseEntryAction getNullable(String role) {
        return ROLE_ACTION_MAP.get(role);
    }

    @Nullable
    public static BaseEntryAction getNullable(Role role) {
        return getNullable(role.getId());
    }

    public static boolean containsRole(String role) {
        return ROLE_ACTION_MAP.containsKey(role);
    }

    public static boolean containsRole(Role role) {
        return containsRole(role.getId());
    }
}
