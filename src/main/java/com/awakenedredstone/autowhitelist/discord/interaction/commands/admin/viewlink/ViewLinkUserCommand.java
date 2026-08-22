package com.awakenedredstone.autowhitelist.discord.interaction.commands.admin.viewlink;

import com.awakenedredstone.autowhitelist.discord.interaction.commands.LinkInfoCommand;
import com.awakenedredstone.autowhitelist.discord.interaction.commands.api.AbstractApplicationCommand;
import com.awakenedredstone.autowhitelist.discord.message.MessageUtils;
import com.awakenedredstone.autowhitelist.discord.util.DiscordData;
import discord4j.core.event.domain.interaction.UserInteractionEvent;
import discord4j.core.object.command.ApplicationCommand;
import discord4j.core.object.command.ApplicationCommandContexts;
import discord4j.core.object.entity.Member;
import discord4j.rest.util.Permission;
import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.Optional;

public class ViewLinkUserCommand extends AbstractApplicationCommand<UserInteractionEvent> {
    public ViewLinkUserCommand() {
        super("user_viewlink", ApplicationCommand.Type.USER);

        this.setPermissions(Permission.KICK_MEMBERS);
        this.contexts = new ApplicationCommandContexts[]{ApplicationCommandContexts.GUILD};
    }

    @Override
    public @NotNull Publisher<?> execute(@NotNull UserInteractionEvent event) {
        Optional<Member> schrodingerMember = DiscordData.getMember(event.getTargetUser());
        if (schrodingerMember.isEmpty()) return Mono.empty();

        return event.deferReply().withEphemeral(MessageUtils.ephemeral())
          .then(LinkInfoCommand.execute(event, schrodingerMember.get()));
    }
}
