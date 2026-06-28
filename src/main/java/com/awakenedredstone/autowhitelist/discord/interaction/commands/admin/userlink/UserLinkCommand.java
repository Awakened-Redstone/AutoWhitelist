package com.awakenedredstone.autowhitelist.discord.interaction.commands.admin.userlink;

import com.awakenedredstone.autowhitelist.discord.interaction.commands.admin.userlink.remove.UserLinkRemoveSubCommandGroup;
import com.awakenedredstone.autowhitelist.discord.interaction.commands.api.impl.ChatInputApplicationCommand;
import com.awakenedredstone.autowhitelist.discord.message.MessageUtils;
import com.awakenedredstone.autowhitelist.discord.message.ResponseMessage;
import com.awakenedredstone.autowhitelist.discord.message.responses.ModifyCommandMessages;
import com.awakenedredstone.autowhitelist.discord.util.DiscordData;
import com.awakenedredstone.autowhitelist.server.profile.ProfileFetcher;
import com.awakenedredstone.autowhitelist.server.whitelist.WhitelistHandler;
import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.Member;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.rest.util.Permission;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@SuppressWarnings({"BlockingMethodInNonBlockingContext", "DuplicatedCode"})
public class UserLinkCommand extends ChatInputApplicationCommand {
    public UserLinkCommand() {
        super("userlink", "admin");

        this.permissions = new Permission[]{Permission.KICK_MEMBERS};

        this.options.add(new UserLinkModifySubCommand(this).asOption());
        this.options.add(new UserLinkRemoveSubCommandGroup(this).asOption());
    }

    public @NotNull Publisher<?> execute(@NotNull ChatInputInteractionEvent event, @NotNull Member member, @NotNull String input, boolean geyser) {
        ProfileFetcher profileFetcher = geyser ? ProfileFetcher.bedrockFetcher(input) : ProfileFetcher.javaFetcher(input);

        return WhitelistHandler.register(member, profileFetcher, true)
          .flatMap(response -> event.editReply(
          ResponseMessage.buildEditSpec(
            response.replyId().withPrefix("modify/"),
            ArrayUtils.insert(0, response.args(), input, geyser)
          )
        ));
    }

    @Override
    public @NotNull Publisher<?> execute(@NotNull ChatInputInteractionEvent event) {
        Member member = DiscordData.getMember(event.getOptionAsUser("user")).orElseThrow();

        @NotNull String input = event.getOptionAsString("username").orElseThrow();
        boolean geyser = event.getOptionAsString("account_type").orElse("java").equalsIgnoreCase("bedrock");

        event.deferReply().withEphemeral(MessageUtils.ephemeral()).block();
        return this.execute(event, member, input, geyser);
    }

    @Override
    public @NotNull Publisher<?> onChatInput(@NotNull ChatInputAutoCompleteEvent event) {
        var userOption = event.getOption("user");
        if (userOption.isEmpty()) return Mono.empty();

        var schrodingerUser = userOption.get().getValue().map(ApplicationCommandInteractionOptionValue::asUser);

        if (schrodingerUser.isEmpty()) {
            return Mono.empty();
        }

        Optional<Member> schrodingerMember = DiscordData.getMember(schrodingerUser.get());
        if (schrodingerMember.isEmpty()) {
            return Mono.empty();
        }

        Member member = schrodingerMember.get();

        return event.respondWithSuggestions(List.of(
          ApplicationCommandOptionChoiceData.builder().name("username").value(member.getUsername()).build(),
          ApplicationCommandOptionChoiceData.builder().name("username").value(member.getDisplayName().replace(" ", "")).build()
        ));
    }

    static {
        ModifyCommandMessages.init();
    }
}
