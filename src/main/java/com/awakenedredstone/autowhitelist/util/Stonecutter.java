package com.awakenedredstone.autowhitelist.util;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/*? if >=1.20.5 {*/
import com.mojang.serialization.MapCodec;
/*?} else {*/
/*import com.mojang.serialization.Codec;
*//*?}*/

import net.minecraft.server.MinecraftServer;
/*? if >=1.21.9 {*/
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.util.NameToIdCache;
/*?}*/
import net.minecraft.util.UserCache;

import java.util.UUID;
import java.util.function.Function;

/**
 * Utility class to move common components that have stonecutter comments to make the code cleaner
 */
public class Stonecutter {
    public static <O> /*? if <1.20.5 {*//*Codec*//*?} else {*/MapCodec/*?}*/<O> entryCodec(final Function<RecordCodecBuilder.Instance<O>, ? extends App<RecordCodecBuilder.Mu<O>, O>> builder) {
        return RecordCodecBuilder./*? if <1.20.5 {*//*create*//*?} else {*/mapCodec/*?}*/(builder);
    }

    public static <R> R getOrThrowDataResult(DataResult<R> dataResult) {
        return dataResult.getOrThrow(/*? if <1.20.5 {*//*false, s -> {}*//*?}*/);
    }

    public static String profileName(GameProfile profile) {
        return /*? if <1.21.9 {*/ /*Stonecutter.profileName(profile) *//*?} else {*/ profile.name() /*?}*/;
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
