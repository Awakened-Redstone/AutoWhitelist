package com.awakenedredstone.autowhitelist.discord.interaction.api;

import discord4j.core.event.domain.Event;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Blocking;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;

public abstract class SimpleHandlerRegistry<E extends Event> {
    public static final Logger LOGGER = LoggerFactory.getLogger(SimpleHandlerRegistry.class);

    protected final Map<String, EventHandler<E, ?>> handlers = new HashMap<>();

    public void register(Identifier id, EventHandler<E, ?> command) {
        handlers.put(id.toString(), command);
    }

    protected abstract EventHandler<E, ?> getHandler(E event);

    @Blocking
    public Publisher<?> execute(E event) {
        try {
            var handler = getHandler(event);
            if (handler != null) {
                return Mono.fromCallable(() -> handler.handle(event)).subscribeOn(Schedulers.boundedElastic());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to handle interaction!", e);
        }

        return Mono.empty();
    }
}
