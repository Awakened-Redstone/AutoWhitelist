package com.awakenedredstone.autowhitelist.discord.interaction.commands.api;

import com.awakenedredstone.autowhitelist.discord.message.ResponseMessage;
import com.awakenedredstone.autowhitelist.discord.message.responses.CommonResponseMessages;
import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import discord4j.core.object.component.TopLevelMessageComponent;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Publisher;

import java.util.List;

public interface DeferrableInteraction<T extends DeferrableInteractionEvent> {
    // @NotNull Publisher<?> execute(@NotNull T event);

    default @NotNull Publisher<?> onError(@NotNull T event, @NotNull Throwable exception) {
        List<TopLevelMessageComponent> components = ResponseMessage.buildComponents(CommonResponseMessages.COMMAND_FATAL, this, event, exception);

        return event.createFollowup(InteractionFollowupCreateSpec.builder().addAllComponents(components).build());
    }
}
