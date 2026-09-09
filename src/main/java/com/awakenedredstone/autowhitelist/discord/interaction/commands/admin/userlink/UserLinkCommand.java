package com.awakenedredstone.autowhitelist.discord.interaction.commands.admin.userlink;

import com.awakenedredstone.autowhitelist.discord.interaction.commands.admin.userlink.remove.UserLinkRemoveSubCommandGroup;
import com.awakenedredstone.autowhitelist.discord.interaction.commands.api.impl.ChatInputApplicationCommand;
import com.awakenedredstone.autowhitelist.discord.message.responses.ModifyCommandMessages;
import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.rest.util.Permission;
import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

public class UserLinkCommand extends ChatInputApplicationCommand {
    public UserLinkCommand() {
        super("userlink", "admin");

        this.setPermissions(Permission.KICK_MEMBERS);

        this.options.add(new UserLinkModifySubCommand(this).asOption());
        this.options.add(new UserLinkRemoveSubCommandGroup(this).asOption());
    }

    @Override
    public @NotNull Publisher<?> execute(@NotNull ChatInputInteractionEvent event) {
        var res = this.executeSubCommand(event, event.getOptions());
        if (res == Mono.empty()) {
            return Mono.error(new IllegalArgumentException("Invalid input, no handler found!"));
        }

        return res;
    }

    @Override
    public @NotNull Publisher<?> onChatInput(@NotNull ChatInputAutoCompleteEvent event) {
        return this.forwardChatInput(event, event.getOptions());
    }

    static {
        ModifyCommandMessages.init();
    }
}
