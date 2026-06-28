package com.awakenedredstone.autowhitelist.discord.interaction.buttons;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.discord.interaction.commands.api.ButtonInteraction;
import com.awakenedredstone.autowhitelist.server.whitelist.WhitelistHandler;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.component.*;
import discord4j.core.object.entity.Member;
import discord4j.discordjson.json.MessageInteractionData;
import discord4j.discordjson.json.UserData;
import discord4j.rest.util.AllowedMentions;
import net.minecraft.resources.Identifier;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.List;

public class RemoveLinkButton extends ButtonInteraction {
    public static final Identifier ID = AutoWhitelist.id("remove_link");

    public RemoveLinkButton() {
        super(ID);
    }

    @Override
    public Publisher<?> execute(ButtonInteractionEvent event) {
        Interaction interaction = event.getInteraction();
        Member interactionUser = interaction.getMember().orElseThrow(AssertionError::new);
        long interactionUserId = interactionUser.getId().asLong();

        MessageInteractionData originalInteraction = interaction.getData().message().get().interaction().get();
        UserData originalUser = originalInteraction.user();
        long originalUserId = originalUser.id().asLong();

        if (interactionUserId != originalUserId) {
            // TODO: translation
            return event.reply("You do not have permission to do this operation").withEphemeral(true);
        }

        // TODO: require confirmation
        return Mono.fromRunnable(() -> WhitelistHandler.unlink(interactionUser))
          .then(event.edit().withAllowedMentions(AllowedMentions.suppressAll()).withComponents(disableButtons(event.getMessage().get().getComponents())))
          // TODO: translation
          .then(event.createFollowup("Link successfully removed").withEphemeral(AutoWhitelist.config().discord.ephemeralReplies));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T extends BaseMessageComponent> List<T> disableButtons(List<T> components) {
        return components.stream().map(component -> {
            if (component instanceof ActionRow actionRow) {
                return (T) ActionRow.of(disableButtons((List) actionRow.getChildren()));
            }

            if (component instanceof Button button) {
                return (T) button.disabled();
            }

            return component;
        }).toList();
    }

    /// @deprecated TODO: Move to a factory
    @Deprecated(forRemoval = true)
    public static Button create(boolean direct) {
        return Button.danger(ID.toString(), ButtonInteraction.translatedName(ID, direct ? "_direct" : "_indirect"));
    }
}
