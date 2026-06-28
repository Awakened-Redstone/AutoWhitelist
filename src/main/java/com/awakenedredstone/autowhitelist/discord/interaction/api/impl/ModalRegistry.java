package com.awakenedredstone.autowhitelist.discord.interaction.api.impl;

import com.awakenedredstone.autowhitelist.discord.interaction.api.EventHandler;
import com.awakenedredstone.autowhitelist.discord.interaction.api.SimpleHandlerRegistry;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;

public class ModalRegistry extends SimpleHandlerRegistry<ModalSubmitInteractionEvent> {
    @Override
    protected EventHandler<ModalSubmitInteractionEvent, ?> getHandler(ModalSubmitInteractionEvent event) {
        return handlers.get(event.getCustomId());
    }
}
