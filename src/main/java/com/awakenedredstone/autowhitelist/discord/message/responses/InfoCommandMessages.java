package com.awakenedredstone.autowhitelist.discord.message.responses;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.discord.interaction.buttons.RemoveLinkButton;
import com.awakenedredstone.autowhitelist.discord.message.MessageBuilder;
import com.awakenedredstone.autowhitelist.discord.message.ResponseMessage;
import com.awakenedredstone.autowhitelist.discord.message.responses.ResponseTypes.*;
import com.awakenedredstone.autowhitelist.server.whitelist.WhitelistHandler;
import com.awakenedredstone.autowhitelist.util.string.Texts;
import discord4j.core.object.component.*;
import net.minecraft.resources.Identifier;

import java.util.List;

import static com.awakenedredstone.autowhitelist.AutoWhitelist.id;

public class InfoCommandMessages {
    /// The user has no linked account
    public static final Identifier USER_UNREGISTERED = ResponseMessage.<EventMemberDirect>register(id("info", "user_unregistered"), (event, member, direct) -> {
        String direction = direct ? "direct" : "indirect";

        if (!WhitelistHandler.qualifies(member)) {
            return List.of(
              MessageBuilder.translated("discord.autowhitelist.response.info.user_unqualified.%s.title".formatted(direction)),
              MessageBuilder.translated("discord.autowhitelist.response.info.user_unqualified.%s.description".formatted(direction))
            );
        }

        return List.of(
          MessageBuilder.translated("discord.autowhitelist.response.info.user_unregistered.%s.title".formatted(direction)),
          MessageBuilder.translated("discord.autowhitelist.response.info.user_unregistered.%s.description".formatted(direction)),
          ActionRow.of(
            Button.primary(
              // TODO: Implement register modal
              id("register_" + direction).toString(),
              Texts.translated("discord.autowhitelist.button." + "register_" + direction)
            ).disabled()
          )
        );
    });

    /// There is no whitelist entry or saved link for the given username
    public static final Identifier USERNAME_NOT_FOUND = ResponseMessage.<EventUsername>register(id("info", "username_not_found"), (event, username) -> {
        return List.of(
          MessageBuilder.translated("discord.autowhitelist.response.info.not_found.title"),
          MessageBuilder.translated("discord.autowhitelist.response.info.not_found.description")
        );
    });

    /// Show the link and whitelist details for the profile
    public static final Identifier DETAILS_WHITELISTED = ResponseMessage.<EventPlayerProfileDirect>register(id("info", "details/whitelisted"), (event, profile, direct) -> {
        String direction = direct ? "direct" : "indirect";

        return List.of(
          Section.of(
            Thumbnail.of(UnfurledMediaItem.of(AutoWhitelist.config().vanity.playerRenderer.formatted(profile.getSkinId()))),
            MessageBuilder.translated("discord.autowhitelist.response.info.details/whitelisted.%s.title".formatted(direction)),
            MessageBuilder.translated(
              "discord.autowhitelist.response.info.details/whitelisted.%s.description.%s".formatted(direction, profile.isLinked() ? "linked" : "unlinked"),
              profile.name(),
              profile.id(),
              profile.discordId(),
              profile.role(),
              profile.lockedUntil() == -1 ? 999999999999L : profile.lockedUntil() / 1000
            )
          ),
          ActionRow.of(
            // TODO: Implement edit modal
            Button.secondary(id("edit_link").toString(), Texts.translated("discord.autowhitelist.button.edit_link_" + direction)).disabled(),
            RemoveLinkButton.create(direct)
          )
        );
    });

    /// Show the link and whitelist details for the profile
    public static final Identifier DETAILS_CACHED = ResponseMessage.<EventPlayerProfileDirect>register(id("info", "details/cached"), (event, profile, direct) -> {
        String direction = direct ? "direct" : "indirect";

        var qualifies = WhitelistHandler.qualifies(profile);

        return List.of(
          Section.of(
            Thumbnail.of(UnfurledMediaItem.of(AutoWhitelist.config().vanity.playerRenderer.formatted(profile.getSkinId()))),
            MessageBuilder.translated("discord.autowhitelist.response.info.details/cached.%s.title".formatted(direction)),
            MessageBuilder.translated(
              "discord.autowhitelist.response.info.details/cached.%s.description.%s".formatted(direction, profile.isLinked() ? "linked" : "unlinked"),
              qualifies,
              profile.name(),
              profile.id(),
              profile.discordId()
            )
          ),
          ActionRow.of(
            // TODO: Implement edit modal
            Button.secondary(id("edit_link").toString(), Texts.translated("discord.autowhitelist.button.edit_link_" + direction)).disabled(),
            RemoveLinkButton.create(direct)
          )
        );
    });
}
