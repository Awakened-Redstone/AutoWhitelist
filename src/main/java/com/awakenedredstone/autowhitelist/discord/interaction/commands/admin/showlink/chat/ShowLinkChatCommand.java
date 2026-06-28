package com.awakenedredstone.autowhitelist.discord.interaction.commands.admin.showlink.chat;

import com.awakenedredstone.autowhitelist.discord.interaction.commands.api.impl.ChatInputApplicationCommand;
import com.awakenedredstone.autowhitelist.server.profile.PlayerProfile;
import com.awakenedredstone.autowhitelist.server.whitelist.WhitelistHandler;
import com.awakenedredstone.autowhitelist.server.whitelist.link.LinkingWhitelist;
import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.Member;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.rest.util.Permission;
import net.minecraft.server.players.StoredUserEntry;
import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

public class ShowLinkChatCommand extends ChatInputApplicationCommand {
    public ShowLinkChatCommand() {
        super("link-info");

        this.options.add(new ShowLinkUserSubCommand(this).asOption());
        this.options.add(new ShowLinkUsernameSubCommand(this).asOption());
    }

    @Override
    public @NotNull Publisher<?> execute(@NotNull ChatInputInteractionEvent event) {
        return Mono.empty();
    }

    @Override
    public @NotNull Publisher<?> onChatInput(@NotNull ChatInputAutoCompleteEvent event) {
        Member invoker = event.getInteraction().getMember().orElseThrow();

        if (!invoker.getBasePermissions().blockOptional().orElseThrow().contains(Permission.MANAGE_MESSAGES)) {
            return Mono.empty();
        }

        LinkingWhitelist whitelist = WhitelistHandler.getWhitelist();
        String typing = event.getFocusedOption().getValue()
          .map(ApplicationCommandInteractionOptionValue::asString)
          .orElse("");

        List<ApplicationCommandOptionChoiceData> usernames = whitelist.getEntries().stream()
          .map(StoredUserEntry::getUser)
          .filter(Objects::nonNull)
          .map(PlayerProfile::name)
          .filter(name -> name.contains(typing))
          .map(name -> (ApplicationCommandOptionChoiceData) ApplicationCommandOptionChoiceData.builder().name("username").value(name).build())
          .toList();

        return event.respondWithSuggestions(usernames);
    }
}
