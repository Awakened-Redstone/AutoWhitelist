package com.awakenedredstone.autowhitelist.commands.api;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Predicate;

public class Permission {

    private static Predicate<ServerCommandSource> tryPermissionApi(Predicate<ServerCommandSource> predicate, boolean defaultValue) {
        return FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0") ? predicate : v -> defaultValue;
    }

    private static Predicate<ServerCommandSource> tryPermissionApi(Predicate<ServerCommandSource> predicate, int defaultRequiredLevel) {
        return FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0") ? predicate : source -> source.hasPermissionLevel(defaultRequiredLevel);
    }

    public static @NotNull Predicate<ServerCommandSource> require(@NotNull String permission, boolean defaultValue) {
        Objects.requireNonNull(permission, "permission");
        return tryPermissionApi(source -> PermissionApiWrapper.check(source, permission, defaultValue), defaultValue);
    }

    public static @NotNull Predicate<ServerCommandSource> require(@NotNull String permission, int defaultRequiredLevel) {
        Objects.requireNonNull(permission, "permission");
        return tryPermissionApi(source -> PermissionApiWrapper.check(source, permission, defaultRequiredLevel), defaultRequiredLevel);
    }
}
