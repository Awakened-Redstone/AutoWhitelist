package com.awakenedredstone.autowhitelist.commands.permission;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;
//? if >=1.21.11 {
import net.minecraft.server.permissions.PermissionLevel;
//?}
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public record CommandPermission(@NotNull /*? if <1.21.11 {*/ /*int*//*?} else {*/ PermissionLevel /*?}*/ vanillaPermission, @NotNull String luckpermsPermission) {
    /**
     * Create a command permission that is usable by all players by default. <br/>
     * Previously known as command permission level 0
     *
     * @param permission The luckperms permission string
     * @return a command permission available to all by default and the provided permission string
     */
    public static CommandPermission all(String permission) {
        return new CommandPermission(/*? if <1.21.11 {*/ /*0 *//*?} else {*/ PermissionLevel.ALL /*?}*/, permission);
    }

    /**
     * Create a command permission that is usable only by moderators by default. <br/>
     * Previously known as command permission level 1
     *
     * @param permission The luckperms permission string
     * @return a command permission available to moderators by default and the provided permission string
     */
    public static CommandPermission moderators(String permission) {
        return new CommandPermission(/*? if <1.21.11 {*/ /*1 *//*?} else {*/ PermissionLevel.MODERATORS /*?}*/, permission);
    }

    /**
     * Create a command permission that is usable only by game masters by default. <br/>
     * Previously known as command permission level 2
     *
     * @param permission The luckperms permission string
     * @return a command permission available to game masters by default and the provided permission string
     */
    public static CommandPermission gameMasters(String permission) {
        return new CommandPermission(/*? if <1.21.11 {*/ /*2 *//*?} else {*/ PermissionLevel.GAMEMASTERS /*?}*/, permission);
    }

    /**
     * Create a command permission that is usable only by admins by default. <br/>
     * Previously known as command permission level 3
     *
     * @param permission The luckperms permission string
     * @return a command permission available to admins by default and the provided permission string
     */
    public static CommandPermission admins(String permission) {
        return new CommandPermission(/*? if <1.21.11 {*/ /*3 *//*?} else {*/ PermissionLevel.ADMINS /*?}*/, permission);
    }

    /**
     * Create a command permission that is usable only by owners by default. <br/>
     * Previously known as command permission level 4
     *
     * @param permission The luckperms permission string
     * @return a command permission available to owners by default and the provided permission string
     */
    public static CommandPermission owners(String permission) {
        return new CommandPermission(/*? if <1.21.11 {*/ /*4 *//*?} else {*/ PermissionLevel.OWNERS /*?}*/, permission);
    }

    /**
     * Creates a permission check predicate
     *
     * @return the permission check predicate using the Permission API
     */
    public Predicate<CommandSourceStack> check() {
        return source -> Permissions.check(source, luckpermsPermission, vanillaPermission);
    }
}
