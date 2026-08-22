package com.awakenedredstone.autowhitelist.discord.interaction.commands.admin.viewlink.chat;

import com.awakenedredstone.autowhitelist.discord.interaction.commands.LinkInfoCommand;
import com.awakenedredstone.autowhitelist.discord.interaction.commands.api.impl.ChatInputSubCommand;
import com.awakenedredstone.autowhitelist.server.profile.PlayerProfile;
import com.awakenedredstone.autowhitelist.server.whitelist.WhitelistHandler;
import com.awakenedredstone.autowhitelist.server.whitelist.link.LinkingWhitelist;
import com.awakenedredstone.autowhitelist.util.Optioning;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Member;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.rest.util.Permission;
import net.minecraft.server.players.StoredUserEntry;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

public class ViewLinkUserSubCommand extends ChatInputSubCommand<ViewLinkChatCommand> {
    public ViewLinkUserSubCommand(@NotNull ViewLinkChatCommand parent) {
        super(parent, "user");

        this.options.add(
          ApplicationCommandOptionData.builder()
            .name("user")
            .description(argumentDescription("user"))
            .type(ApplicationCommandOption.Type.USER.getValue())
            .required(true)
            .build()
        );
    }

    @Override
    public @NotNull Publisher<?> execute(@NotNull ChatInputInteractionEvent event, @NonNull List<ApplicationCommandInteractionOption> options) {
        var user = Optioning.getOptionAsMember(options, "user").orElseThrow();

        return event.deferReply().then(user.flatMap(member -> LinkInfoCommand.execute(event, member)));
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
