package com.awakenedredstone.autowhitelist.discord.interaction.api;

import discord4j.core.event.domain.Event;
import org.reactivestreams.Publisher;

@FunctionalInterface
public interface EventHandler<E extends Event, T> {
    Publisher<T> handle(E event);
}
