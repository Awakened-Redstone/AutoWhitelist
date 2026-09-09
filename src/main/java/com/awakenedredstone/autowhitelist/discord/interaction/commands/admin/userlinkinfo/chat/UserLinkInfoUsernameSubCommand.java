package com.awakenedredstone.autowhitelist.discord.interaction.commands.admin.userlinkinfo.chat;

import com.awakenedredstone.autowhitelist.discord.interaction.commands.LinkInfoCommand;
import com.awakenedredstone.autowhitelist.discord.interaction.commands.api.impl.ChatInputSubCommand;
import com.awakenedredstone.autowhitelist.server.profile.PlayerProfile;
import com.awakenedredstone.autowhitelist.server.whitelist.WhitelistHandler;
import com.awakenedredstone.autowhitelist.server.whitelist.link.LinkingWhitelist;
import com.awakenedredstone.autowhitelist.util.Optioning;
import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import net.minecraft.server.players.StoredUserEntry;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.reactivestreams.Publisher;

import java.util.List;
import java.util.Objects;

public class UserLinkInfoUsernameSubCommand extends ChatInputSubCommand<UserLinkInfoChatCommand> {
    public UserLinkInfoUsernameSubCommand(@NotNull UserLinkInfoChatCommand parent) {
        super(parent, "username");

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
    public @NotNull Publisher<?> execute(@NotNull ChatInputInteractionEvent event, @NonNull List<ApplicationCommandInteractionOption> options) {
        var username = Optioning.getOptionAsString(options, "username").orElseThrow();

        return event.deferReply().then(LinkInfoCommand.execute(event, username));
    }

    @Override
    public @NotNull Publisher<?> onChatInput(@NotNull ChatInputAutoCompleteEvent event, @NonNull List<ApplicationCommandInteractionOption> options) {
        LinkingWhitelist whitelist = WhitelistHandler.getWhitelist();
        String typing = event.getFocusedOption().getValue()
          .map(ApplicationCommandInteractionOptionValue::asString)
          .orElse("");

        List<ApplicationCommandOptionChoiceData> usernames = whitelist.getEntries().stream()
          .map(StoredUserEntry::getUser)
          .filter(Objects::nonNull)
          .map(PlayerProfile::name)
          .filter(name -> name.contains(typing))
          .map(name -> (ApplicationCommandOptionChoiceData) ApplicationCommandOptionChoiceData.builder().name(name).value(name).build())
          .toList();

        return event.respondWithSuggestions(usernames);
    }
}
