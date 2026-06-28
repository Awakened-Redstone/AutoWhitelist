package com.awakenedredstone.moondust.config.api;

import com.mojang.serialization.Codec;

import java.util.HashMap;
import java.util.Map;

public interface CodecSpecs {
    Map<String, Codec<?>> getCodecs();

    abstract class Simple implements CodecSpecs {
        private final Map<String, Codec<?>> codecs = new HashMap<>();

        protected <T> void register(String field, Codec<T> codec) {
            codecs.put(field, codec);
        }

        public Map<String, Codec<?>> getCodecs() {
            return codecs;
        }
    }
}
