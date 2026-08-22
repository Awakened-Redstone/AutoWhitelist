package com.awakenedredstone.autowhitelist.mixin.discord4j;

import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.Function;

@Mixin(ApplicationCommandInteractionOptionValue.class)
public interface ApplicationCommandInteractionOptionValueAccessor {
    @Accessor @Nullable Long getGuildId();
    @Invoker <T> T callGetValueAs(String parsedTypeName, Function<String, T> parser, ApplicationCommandOption.Type... allowedTypes);
}
