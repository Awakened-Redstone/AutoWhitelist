package com.awakenedredstone.autowhitelist.discord.interaction.commands;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.LazyConstants;
import com.awakenedredstone.autowhitelist.discord.interaction.commands.api.impl.ChatInputApplicationCommand;
import com.awakenedredstone.autowhitelist.discord.message.MessageUtils;
import com.awakenedredstone.autowhitelist.discord.message.ResponseMessage;
import com.awakenedredstone.autowhitelist.discord.message.responses.RegisterCommandMessages;
import com.awakenedredstone.autowhitelist.server.profile.ProfileFetcher;
import com.awakenedredstone.autowhitelist.server.whitelist.WhitelistHandler;
import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

public class LinkCommand extends ChatInputApplicationCommand {
    public LinkCommand() {
        super("link");

        this.options.add(
          ApplicationCommandOptionData.builder()
            .name("username")
            .description(argumentDescription("username"))
            .autocomplete(true)
            .type(ApplicationCommandOption.Type.STRING.getValue())
            .required(true)
            .build()
        );

        if (AutoWhitelist.useGuyser()) {
            this.options.add(
              ApplicationCommandOptionData.builder()
                .name("edition")
                .description(argumentDescription("edition"))
                .type(ApplicationCommandOption.Type.STRING.getValue())
                .choices(
                  ApplicationCommandOptionChoiceData.builder()
                    .name(choice("edition", "java"))
                    .value("java")
                    .build(),
                  ApplicationCommandOptionChoiceData.builder()
                    .name(choice("edition", "bedrock"))
                    .value("bedrock")
                    .build()
                )
                .required(false)
                .build()
            );
        }
    }

    public @NotNull Mono<Message> execute(@NotNull ChatInputInteractionEvent event, @NotNull Member member, @NotNull String input, boolean geyser) {
        ProfileFetcher profileFetcher = geyser ? ProfileFetcher.bedrockFetcher(input) : ProfileFetcher.javaFetcher(input);

        return WhitelistHandler.register(member, profileFetcher, false).flatMap(response -> event.editReply(
          ResponseMessage.buildEditSpec(
            response.replyId().withPrefix("register/"),
            ArrayUtils.insert(0, response.args(), input, geyser)
          )
        ));
    }

    @Override
    public @NotNull Mono<Message> execute(@NotNull ChatInputInteractionEvent event) {
        Member member = event.getInteraction().getMember().orElseThrow();

        @NotNull String input = event.getOptionAsString("username").orElseThrow();
        boolean geyser = event.getOptionAsString("edition").orElse("java").equalsIgnoreCase("bedrock");

        return event.deferReply().withEphemeral(MessageUtils.ephemeral()).then(this.execute(event, member, input, geyser));
    }

    @Override
    public @NotNull Mono<Void> onChatInput(@NotNull ChatInputAutoCompleteEvent event) {
        Optional<Member> schrodingerMember = event.getInteraction().getMember();
        if (schrodingerMember.isEmpty()) {
            return Mono.empty();
        }

        Member member = schrodingerMember.get();

        return event.respondWithSuggestions(List.of(
          simpleOptionChoice(member.getUsername()),
          simpleOptionChoice(member.getDisplayName().replace(" ", ""))
        ));
    }

    static {
        RegisterCommandMessages.init();
    }
}
