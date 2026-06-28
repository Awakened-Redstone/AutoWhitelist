package com.awakenedredstone.autowhitelist.discord.interaction.api.impl;

import com.awakenedredstone.autowhitelist.discord.interaction.api.EventHandler;
import com.awakenedredstone.autowhitelist.discord.interaction.api.SimpleHandlerRegistry;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;

public class ButtonRegistry extends SimpleHandlerRegistry<ButtonInteractionEvent> {
    @Override
    protected EventHandler<ButtonInteractionEvent, ?> getHandler(ButtonInteractionEvent event) {
        return handlers.get(event.getCustomId());
    }
}
