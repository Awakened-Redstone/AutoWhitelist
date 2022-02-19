package com.awakenedredstone.autowhitelist.discord.api.text;

import com.awakenedredstone.autowhitelist.discord.api.command.DiscordCommandSource;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.Nullable;

/**
 * A {@link Text} that needs to be parsed when it is loaded into the game.
 */
public interface ParsableText {
   MutableText parse(@Nullable DiscordCommandSource source, @Nullable User sender, int depth) throws CommandSyntaxException;
}
