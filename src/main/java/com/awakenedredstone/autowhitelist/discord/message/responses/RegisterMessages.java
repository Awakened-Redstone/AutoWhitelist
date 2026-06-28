package com.awakenedredstone.autowhitelist.discord.message.responses;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import net.minecraft.resources.Identifier;

/**
 * The base ids for the responses to the registration process, this is just the base ids,
 * with the messages being added by other more specific classes
 * @see RegisterCommandMessages
 * @see ModifyCommandMessages
 */
public class RegisterMessages {
    /// The user doesn't qualify for being added to the whitelist due to not having any of the required roles
    public static final Identifier UNQUALIFIED = AutoWhitelist.id("fail/unqualified");

    /// The user's entry is locked and can not be modified at this moment
    public static final Identifier LOCKED = AutoWhitelist.id("fail/locked");

    /// It was not possible to find an account for the given username/UUID
    public static final Identifier NOT_FOUND = AutoWhitelist.id("fail/not_found");

    /// The user already had the provided account whitelisted and nothing has changed
    public static final Identifier NOTHING_CHANGED = AutoWhitelist.id("fail/nothing_changed");

    /// The provided account is banned and can not be whitelisted
    public static final Identifier BANNED = AutoWhitelist.id("fail/banned");

    /// The provided account is already linked and can not be added again
    public static final Identifier ALREADY_LINKED = AutoWhitelist.id("fail/linked");

    /// The provided account is already whitelisted and can not be added again
    public static final Identifier ALREADY_WHITELISTED = AutoWhitelist.id("fail/whitelisted");

    /// One of actions that would be run for the user's role is broken and whitelisting was aborted to avoid issues
    public static final Identifier BROKEN_ACTION = AutoWhitelist.id("fail/broken_action");

    /// Failed to add the user to the whitelist, they were already in it, likely race condition, no actions were run
    public static final Identifier ERROR_WHILE_ADDING = AutoWhitelist.id("fail/error_while_adding");

    /// The user has successfully been added to the whitelist and all actions were run
    public static final Identifier REGISTERED = AutoWhitelist.id("success/registered");
}
