package com.awakenedredstone.autowhitelist.commands.api;

import net.fabricmc.loader.api.FabricLoader;
//? if >=1.21.11 {
import net.minecraft.command.permission.PermissionCheck;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.server.command.CommandManager;
//?}
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Predicate;

public class Permission {

    private static Predicate<ServerCommandSource> tryPermissionApi(Predicate<ServerCommandSource> predicate, boolean defaultValue) {
        return FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0") ? predicate : v -> defaultValue;
    }

    private static Predicate<ServerCommandSource> tryPermissionApi(Predicate<ServerCommandSource> predicate, int defaultRequiredLevel) {
        return FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0") ? predicate : hasPermissionLevel(defaultRequiredLevel);
    }

    public static @NotNull Predicate<ServerCommandSource> require(@NotNull String permission, boolean defaultValue) {
        Objects.requireNonNull(permission, "permission");
        return tryPermissionApi(source -> PermissionApiWrapper.check(source, permission, defaultValue), defaultValue);
    }

    public static @NotNull Predicate<ServerCommandSource> require(@NotNull String permission, int defaultRequiredLevel) {
        Objects.requireNonNull(permission, "permission");
        return tryPermissionApi(source -> PermissionApiWrapper.check(source, permission, defaultRequiredLevel), defaultRequiredLevel);
    }

    private static Predicate<ServerCommandSource> hasPermissionLevel(int level) {
        //? if >=1.21.11 {
        var permission = new PermissionCheck.Require(new net.minecraft.command.permission.Permission.Level(PermissionLevel.fromLevel(level)));
        return CommandManager.requirePermissionLevel(permission);
        //?} else {
        /*return source -> source.hasPermissionLevel(level);
        *///?}
    }
}
