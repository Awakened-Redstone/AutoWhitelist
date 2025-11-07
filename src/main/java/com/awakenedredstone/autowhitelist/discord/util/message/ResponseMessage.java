package com.awakenedredstone.autowhitelist.discord.util.message;

import discord4j.core.object.component.TopLevelMessageComponent;
import discord4j.core.spec.InteractionReplyEditSpec;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ResponseMessage {
    private static final Map<Identifier, MessageBuilder> BUILDERS = new HashMap<>();

    public static void register(Identifier id, MessageBuilder builder) {
        BUILDERS.putIfAbsent(id, builder);
    }

    /**
     * The return is for editing an interaction reply as it is expected that
     * it is an interaction reply and the reply was deferred
     *
     * @return the reply edit spec
     */
    public static InteractionReplyEditSpec buildSpec(Identifier id, Object... args) {
        MessageBuilder messageBuilder = BUILDERS.get(id);
        if (messageBuilder == null) {
            throw new NullPointerException("Message builder %s does not exist".formatted(id));
        }

        return InteractionReplyEditSpec.builder().addAllComponents(messageBuilder.build(args)).build();
    }

    @FunctionalInterface
    public interface MessageBuilder {
        List<TopLevelMessageComponent> build(Object... args);
    }
}
