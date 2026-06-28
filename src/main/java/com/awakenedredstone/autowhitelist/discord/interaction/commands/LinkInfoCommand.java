package com.awakenedredstone.autowhitelist.discord.interaction.commands;

import com.awakenedredstone.autowhitelist.discord.interaction.commands.api.impl.ChatInputApplicationCommand;
import com.awakenedredstone.autowhitelist.discord.message.MessageUtils;
import com.awakenedredstone.autowhitelist.discord.message.ResponseMessage;
import com.awakenedredstone.autowhitelist.discord.message.responses.InfoCommandMessages;
import com.awakenedredstone.autowhitelist.server.profile.PlayerProfile;
import com.awakenedredstone.autowhitelist.server.whitelist.WhitelistHandler;
import com.awakenedredstone.autowhitelist.server.whitelist.cache.WhitelistCacheEntry;
import com.awakenedredstone.autowhitelist.server.whitelist.link.LinkingWhitelist;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import net.minecraft.server.players.UserWhiteListEntry;
import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

public class LinkInfoCommand extends ChatInputApplicationCommand {
    public LinkInfoCommand() {
        super("linkinfo");
    }

    @SuppressWarnings("OptionalIsPresent") // Using ifPresent is definitely not easier to read for this method
    public static @NotNull Mono<Message> execute(ApplicationCommandInteractionEvent event, Member target) {
        LinkingWhitelist whitelist = WhitelistHandler.getWhitelist();
        String discordId = target.getId().asString();
        var direct = target.equals(event.getInteraction().getMember().orElse(null));

        var profile = whitelist.fromDiscordId(discordId).map(UserWhiteListEntry::getUser).map(PlayerProfile::from);
        if (profile.isPresent()) {
            return event.editReply(ResponseMessage.buildEditSpec(InfoCommandMessages.DETAILS_WHITELISTED, event, profile.get(), direct));
        }

        profile = whitelist.getCache().fromDiscordId(discordId).map(WhitelistCacheEntry::getUser).map(PlayerProfile::from);
        if (profile.isPresent()) {
            return event.editReply(ResponseMessage.buildEditSpec(InfoCommandMessages.DETAILS_CACHED, event, profile.get(), direct));
        }

        return event.editReply(ResponseMessage.buildEditSpec(InfoCommandMessages.USER_UNREGISTERED, event, target, direct));
    }

    @SuppressWarnings("OptionalIsPresent") // Using ifPresent is definitely not easier to read for this method
    public static @NotNull Mono<Message> execute(ApplicationCommandInteractionEvent event, String username) {
        LinkingWhitelist whitelist = WhitelistHandler.getWhitelist();

        var profile = whitelist.fromName(username).map(UserWhiteListEntry::getUser).map(PlayerProfile::from);
        if (profile.isPresent()) {
            return event.editReply(ResponseMessage.buildEditSpec(InfoCommandMessages.DETAILS_WHITELISTED, event, profile.get(), false));
        }

        profile = whitelist.getCache().fromName(username).map(WhitelistCacheEntry::getUser).map(PlayerProfile::from);
        if (profile.isPresent()) {
            return event.editReply(ResponseMessage.buildEditSpec(InfoCommandMessages.DETAILS_CACHED, event, profile.get(), false));
        }

        return event.editReply(ResponseMessage.buildEditSpec(InfoCommandMessages.USERNAME_NOT_FOUND, event, username));
    }

    @Override
    public @NotNull Publisher<?> execute(@NotNull ChatInputInteractionEvent event) {
        Member invoker = event.getInteraction().getMember().orElseThrow();

        return event.deferReply().withEphemeral(MessageUtils.ephemeral()).then(LinkInfoCommand.execute(event, invoker));
    }
}
