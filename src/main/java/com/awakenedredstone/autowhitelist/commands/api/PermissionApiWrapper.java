package com.awakenedredstone.autowhitelist.commands.api;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandSource;
import org.jetbrains.annotations.NotNull;

public class PermissionApiWrapper {

    static boolean check(@NotNull CommandSource source, @NotNull String permission, boolean defaultValue) {
        return Permissions.check(source, permission, defaultValue);
    }

    static boolean check(@NotNull CommandSource source, @NotNull String permission, int defaultRequiredLevel) {
        return Permissions.check(source, permission, defaultRequiredLevel);
    }
}
