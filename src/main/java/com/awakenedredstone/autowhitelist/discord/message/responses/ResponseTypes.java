package com.awakenedredstone.autowhitelist.discord.message.responses;

import com.awakenedredstone.autowhitelist.discord.interaction.commands.api.AbstractApplicationCommand;
import com.awakenedredstone.autowhitelist.discord.interaction.commands.api.DeferrableInteraction;
import com.awakenedredstone.autowhitelist.entry.api.EntryAction;
import com.awakenedredstone.autowhitelist.server.profile.PlayerProfile;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import discord4j.core.object.component.TopLevelMessageComponent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import net.minecraft.server.players.UserBanListEntry;

import java.util.List;

public interface ResponseTypes {
    interface EventMemberDirect { List<TopLevelMessageComponent> build(ApplicationCommandInteractionEvent event, Member member, boolean direct); }
    interface EventPlayerProfileDirect { List<TopLevelMessageComponent> build(ApplicationCommandInteractionEvent event, PlayerProfile profile, boolean direct); }
    interface EventUsername { List<TopLevelMessageComponent> build(ApplicationCommandInteractionEvent event, String username); }

    interface RegisterEmpty { List<TopLevelMessageComponent> build(String input, boolean geyser); }
    interface RegisterPlayerProfile { List<TopLevelMessageComponent> build(String input, boolean geyser, PlayerProfile profile); }
    interface RegisterBanEntry { List<TopLevelMessageComponent> build(String input, boolean geyser, UserBanListEntry entry); }
    interface RegisterRoleAction { List<TopLevelMessageComponent> build(String input, boolean geyser, Role role, EntryAction<?> entry); }

    interface InteractionCrash<T extends DeferrableInteractionEvent> {
        List<TopLevelMessageComponent> build(
          DeferrableInteraction<T> interaction,
          T event,
          Throwable exception
        );
    }
}
