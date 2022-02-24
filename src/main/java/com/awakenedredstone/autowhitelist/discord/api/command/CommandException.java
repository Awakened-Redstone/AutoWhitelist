package com.awakenedredstone.autowhitelist.discord.api.command;

import com.awakenedredstone.autowhitelist.discord.api.text.Text;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

public class CommandException extends RuntimeException {
   private final Text message;

   public CommandException(Text message) {
      super(message.getString(), null, CommandSyntaxException.ENABLE_COMMAND_STACK_TRACES, CommandSyntaxException.ENABLE_COMMAND_STACK_TRACES);
      this.message = message;
   }

   public Text getTextMessage() {
      return this.message;
   }
}
