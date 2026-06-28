package com.awakenedredstone.moondust.config.api;

import com.awakenedredstone.moondust.jankson.annotation.Comment;
import com.awakenedredstone.moondust.jankson.annotation.SkipNameFormat;

import java.util.HashMap;
import java.util.Map;

public abstract class ConfigValues {
    @SkipNameFormat
    @Comment("DO NOT CHANGE, MODIFYING THIS VALUE WILL BREAK THE CONFIGURATION FILE")
    public int CONFIG_VERSION = -1;

    public abstract static class Codec extends ConfigValues implements CodecSpecs {
        private final Map<String, com.mojang.serialization.Codec<?>> codecs = new HashMap<>();

        protected <T> void register(String field, com.mojang.serialization.Codec<T> codec) {
            codecs.put(field, codec);
        }

        public Map<String, com.mojang.serialization.Codec<?>> getCodecs() {
            return codecs;
        }
    }
}
