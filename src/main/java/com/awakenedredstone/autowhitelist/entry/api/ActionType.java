package com.awakenedredstone.autowhitelist.entry.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

public record ActionType<T extends ActionFields>(Identifier id, MapCodec<T> fieldsCodec, EntryAction.Builder<T> builder) {
    private static final Map<Identifier, MapCodec<EntryAction<?>>> MEMO = new HashMap<>();

    public ActionType(Identifier id, Codec<T> fieldsCodec, EntryAction.Builder<T> builder) {
        this(id, fieldsCodec.fieldOf("execute"), builder);
    }

    @SuppressWarnings("unchecked")
    public MapCodec<? extends EntryAction<?>> actionCodec() {
        return MEMO.computeIfAbsent(id, ignored -> RecordCodecBuilder.mapCodec(instance -> instance.group(
            fieldsCodec.forGetter(entryAction -> (T) entryAction.getFields())
          ).apply(instance, fields -> builder.build(this, fields))
        ));
    }
}
