package com.awakenedredstone.autowhitelist.util;

import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/*? if >=1.20.5 {*/
import com.mojang.serialization.MapCodec;
 /*?} else {*/
/*import com.mojang.serialization.Codec;
*//*?}*/

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
}
