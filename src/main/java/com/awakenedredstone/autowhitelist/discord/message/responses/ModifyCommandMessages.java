package com.awakenedredstone.autowhitelist.discord.message.responses;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.discord.message.MessageBuilder;
import com.awakenedredstone.autowhitelist.discord.message.Prefix;
import com.awakenedredstone.autowhitelist.discord.message.ResponseMessage;
import com.awakenedredstone.autowhitelist.discord.message.responses.ResponseTypes.*;
import com.awakenedredstone.autowhitelist.util.data.UUIDUtil;
import com.awakenedredstone.prechecks.RequireFieldsFrom;
import discord4j.core.object.component.Section;
import discord4j.core.object.component.Thumbnail;
import discord4j.core.object.component.UnfurledMediaItem;
import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.List;

/**
 * The registration of the messages for the modify command
 *
 * @see RegisterMessages
 */
@SuppressWarnings("unused")
@Prefix("modify")
@RequireFieldsFrom(RegisterMessages.class)
public class ModifyCommandMessages {
    public static void init() {
    }

    /**
     * @see RegisterMessages#UNQUALIFIED
     */
    public static final Identifier UNQUALIFIED = ResponseMessage.<RegisterEmpty>register(RegisterMessages.UNQUALIFIED, (input, geyser) -> List.of(
      MessageBuilder.translated("discord.autowhitelist.response.modify.fail.unqualified.title"),
      MessageBuilder.translated("discord.autowhitelist.response.modify.fail.unqualified.body")
    ));

    /// This should never trigger, the lock is bypassed on admin commands
    public static final Identifier LOCKED = ResponseMessage.<RegisterPlayerProfile>register(RegisterMessages.LOCKED, (input, geyser, profile) -> List.of(
      MessageBuilder.translated("discord.autowhitelist.response.error.impossible", "admin_modify.response.locked")
    ));

    /**
     * @see RegisterMessages#NOT_FOUND
     */
    public static final Identifier NOT_FOUND = ResponseMessage.<RegisterEmpty>register(RegisterMessages.NOT_FOUND, (input, geyser) -> {
        var type = UUIDUtil.isValidUuid(input) ? "UUID" : "username";
        var edition = geyser ? "Bedrock" : "Java";
        return List.of(
          MessageBuilder.translated("discord.autowhitelist.response.modify.fail.not_found.title"),
          MessageBuilder.translated("discord.autowhitelist.response.modify.fail.not_found.description", edition, type, input)
        );
    });

    /**
     * @see RegisterMessages#NOTHING_CHANGED
     */
    public static final Identifier NOTHING_CHANGED = ResponseMessage.<RegisterEmpty>register(RegisterMessages.NOTHING_CHANGED, (input, geyser) -> List.of(
      MessageBuilder.translated("discord.autowhitelist.response.modify.fail.nothing_changed.title"),
      MessageBuilder.translated("discord.autowhitelist.response.modify.fail.nothing_changed.description")
    ));

    /**
     * @see RegisterMessages#BANNED
     */
    public static final Identifier BANNED = ResponseMessage.<RegisterBanEntry>register(RegisterMessages.BANNED, (input, geyser, entry) -> List.of(
      MessageBuilder.translated("discord.autowhitelist.response.modify.fail.banned.title"),
      MessageBuilder.translated("discord.autowhitelist.response.modify.fail.banned.description")
    ));

    /**
     * @see RegisterMessages#ALREADY_LINKED
     */
    public static final Identifier ALREADY_LINKED = ResponseMessage.<RegisterEmpty>register(RegisterMessages.ALREADY_LINKED, (input, geyser) -> List.of(
      MessageBuilder.translated("discord.autowhitelist.response.modify.fail.linked.title"),
      MessageBuilder.translated("discord.autowhitelist.response.modify.fail.linked.description")
    ));

    /**
     * @see RegisterMessages#ALREADY_WHITELISTED
     */
    public static final Identifier ALREADY_WHITELISTED = ResponseMessage.<RegisterEmpty>register(RegisterMessages.ALREADY_WHITELISTED, (input, geyser) -> List.of(
      MessageBuilder.translated("discord.autowhitelist.response.modify.fail.whitelisted.title"),
      MessageBuilder.translated("discord.autowhitelist.response.modify.fail.whitelisted.description")
    ));

    /**
     * @see RegisterMessages#BROKEN_ACTION
     */
    public static final Identifier BROKEN_ACTION = ResponseMessage.<RegisterRoleAction>register(RegisterMessages.BROKEN_ACTION, (input, geyser, role, action) -> List.of(
      MessageBuilder.translated("discord.autowhitelist.response.modify.fail.broken_action.title"),
      MessageBuilder.translated("discord.autowhitelist.response.modify.fail.broken_action.description", role.getId().asLong(), action.getType().id(), action.toString())
    ));

    /**
     * @see RegisterMessages#ERROR_WHILE_ADDING
     */
    public static final Identifier ERROR_WHILE_ADDING = ResponseMessage.<RegisterPlayerProfile>register(RegisterMessages.ERROR_WHILE_ADDING, (input, geyser, profile) -> List.of(
      MessageBuilder.translated("discord.autowhitelist.response.modify.fail.error_while_adding.title"),
      MessageBuilder.translated("discord.autowhitelist.response.modify.fail.error_while_adding.description")
    ));

    /**
     * @see RegisterMessages#REGISTERED
     */
    public static final Identifier REGISTERED = ResponseMessage.<RegisterPlayerProfile>register(RegisterMessages.REGISTERED, (input, geyser, profile) -> Collections.singletonList(
      Section.of(
        Thumbnail.of(UnfurledMediaItem.of(AutoWhitelist.config().vanity.playerRenderer.formatted(profile.getSkinId()))),
        MessageBuilder.translated("discord.autowhitelist.response.modify.success.registered.title"),
        MessageBuilder.translated("discord.autowhitelist.response.modify.success.registered.description")
      )
    ));
}
