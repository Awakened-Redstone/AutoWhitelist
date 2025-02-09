package com.awakenedredstone.autowhitelist.mixin.jackson;

import blue.endless.jankson.impl.POJODeserializer;
import blue.endless.jankson.impl.serializer.DeserializerFunctionPool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = POJODeserializer.class, remap = false)
public interface POJODeserializerAccessor {
    @Invoker
    static <B> DeserializerFunctionPool<B> callDeserializersFor(Class<B> targetClass) {
        throw new IllegalStateException("Mixin failed to apply!");
    }
}
