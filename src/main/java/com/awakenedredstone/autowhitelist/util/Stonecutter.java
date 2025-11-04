package com.awakenedredstone.autowhitelist.util;

import com.mojang.authlib.GameProfile;

import net.minecraft.server.MinecraftServer;
/*? if >=1.21.9 {*/
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.util.NameToIdCache;
/*?} else {*//*
import net.minecraft.util.UserCache;
*//*?}*/

import java.util.UUID;

/**
 * Utility class to move common components that have stonecutter comments to make the code cleaner
 */
public class Stonecutter {
    public static String profileName(GameProfile profile) {
        return /*? if <1.21.9 {*/ /*profile.getName() *//*?} else {*/ profile.name() /*?}*/;
    }

    public static UUID profileId(GameProfile profile) {
        return /*? if <1.21.9 {*/ /*profile.getId() *//*?} else {*/ profile.id() /*?}*/;
    }

    //? if <1.21.9 {
    /*public static UserCache getUserCache(MinecraftServer server) {
        return server.getUserCache();
    }
    *///?} else {
    public static NameToIdCache getUserCache(MinecraftServer server) {
        return server.getApiServices().nameToIdCache();
    }
    //?}

    //? if >=1.21.9 {
    public static String profileName(PlayerConfigEntry profile) {
        return profile.name();
    }

    public static UUID profileId(PlayerConfigEntry profile) {
        return profile.id();
    }
    //?}
}
