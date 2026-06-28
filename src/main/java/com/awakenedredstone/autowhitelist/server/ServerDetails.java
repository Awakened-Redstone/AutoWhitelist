package com.awakenedredstone.autowhitelist.server;

import com.awakenedredstone.autowhitelist.util.data.ModData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.players.UserNameToIdResolver;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record ServerDetails(MinecraftServer server) {
    public static final Logger LOGGER = LoggerFactory.getLogger(ServerDetails.class);
    private static @Nullable ServerDetails instance;

    public static @NotNull ServerDetails getCurrent() {
        if (instance == null) {
            throw new IllegalStateException("Tried to access server too early or too late, no server is available!");
        }

        return instance;
    }

    public static @NotNull MinecraftServer getServer() {
        return getCurrent().server;
    }

    public ServerLevel spawnWorld() {
        return /*? if <1.21.9 {*/ /*server.overworld(); *//*?} else {*/ server.findRespawnDimension() /*?}*/;
    }

    public CommandSourceStack createCommandSource(String name, /*? if <1.21.11 {*//*int*//*?} else {*/LevelBasedPermissionSet/*?}*/ permissionLevel) {
        return new CommandSourceStack(server, Vec3.ZERO, Vec2.ZERO, spawnWorld(), permissionLevel, name, Component.literal(name), server, null);
    }

    //? if <1.21.9 {
    /*public static UserCache getUserCache() {
        return ServerDetails.getServer().getUserCache();
    }
    *///?} else {
    public static UserNameToIdResolver getUserCache() {
        return ServerDetails.getServer().services().nameToIdCache();
    }
    //?}

    private static void setServer(@NotNull MinecraftServer server) {
        if (ServerDetails.instance != null) {
            throw new IllegalStateException("Can not override server details with another one!");
        }

        ServerDetails.instance = new ServerDetails(server);
    }

    private static void unsetServer() {
        if (ServerDetails.instance == null) {
            LOGGER.warn("Tried to unload server when there was none. Something may have gone horribly wrong!");
        }

        ServerDetails.instance = null;
    }

    public static void registerEvents() {
        ServerLifecycleEvents.SERVER_STARTING.register(ServerDetails::setServer);
        ServerLifecycleEvents.SERVER_STOPPED.register(_ -> unsetServer());
    }

    public static String getPlatformName() {
        String loaderName = getLoaderName();
        if (FabricLoader.getInstance().isModLoaded("connectormod")) {
            return loaderName + " - Via Connector";
        }

        return loaderName;
    }

    public static String getLoaderName() {
        return switch (getServer().getServerModName()) {
            case "fabric" -> "Fabric";
            case "quilt" -> "Quilt";
            case "forge" -> "Forge";
            case "neoforge" -> "NeoForge";
            default -> "Other (%s)".formatted(ServerDetails.getServer().getServerModName());
        };
    }

    public static String getLoaderVersion() {
        return switch (getServer().getServerModName()) {
            case "fabric" -> ModData.getVersion("fabricloader");
            case "quilt" -> ModData.getVersion("quilt_loader");
            case "forge" -> ModData.getVersion("forge");
            case "neoforge" -> ModData.getVersion("neoforge");
            default -> "unknown";
        };
    }
}
