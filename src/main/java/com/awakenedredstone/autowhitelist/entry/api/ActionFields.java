package com.awakenedredstone.autowhitelist.entry.api;

import com.mojang.serialization.Decoder;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.MapCodec;

public interface ActionFields {
    record Empty() implements ActionFields {
        public static final MapCodec<Empty> CODEC = MapCodec.of(Encoder.empty(), Decoder.unit(new Empty()));
    }
}
