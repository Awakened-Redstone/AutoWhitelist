package com.awakenedredstone.autowhitelist.discord.interaction.commands.admin.showlink.chat;

import com.awakenedredstone.autowhitelist.discord.interaction.commands.LinkInfoCommand;
import com.awakenedredstone.autowhitelist.discord.interaction.commands.api.impl.ChatInputSubCommand;
import com.awakenedredstone.autowhitelist.server.profile.PlayerProfile;
import com.awakenedredstone.autowhitelist.server.whitelist.WhitelistHandler;
import com.awakenedredstone.autowhitelist.server.whitelist.link.LinkingWhitelist;
import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Member;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.rest.util.Permission;
import net.minecraft.server.players.StoredUserEntry;
import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

public class ShowLinkUserSubCommand extends ChatInputSubCommand<ShowLinkChatCommand> {
    public ShowLinkUserSubCommand(@NotNull ShowLinkChatCommand parent) {
        super(parent, "user");

        this.options.add(
          ApplicationCommandOptionData.builder()
            .name("username")
            .description(argumentDescription("username"))
            .autocomplete(true)
            .type(ApplicationCommandOption.Type.STRING.getValue())
            .required(true)
            .build()
        );
    }

    @Override
    public @NotNull Publisher<?> execute(@NotNull ChatInputInteractionEvent event) {
        String username = event.getOptionAsString("username").orElseThrow();

        return LinkInfoCommand.execute(event, username);
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
