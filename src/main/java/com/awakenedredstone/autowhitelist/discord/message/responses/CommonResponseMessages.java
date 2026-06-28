package com.awakenedredstone.autowhitelist.discord.message.responses;

import com.awakenedredstone.autowhitelist.discord.message.MessageBuilder;
import com.awakenedredstone.autowhitelist.discord.message.ResponseMessage;
import net.minecraft.resources.Identifier;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.List;

import static com.awakenedredstone.autowhitelist.AutoWhitelist.id;

public class CommonResponseMessages {
    /// The command has failed catastrophically, notify the user about the failure
    public static final Identifier COMMAND_FATAL = ResponseMessage.<ResponseTypes.CommandEventThrowable>register(
      id("command/fatal"), (command, event, throwable) -> {
          var outputStream = new ByteArrayOutputStream();
          try (var writer = new PrintWriter(outputStream)) {
              throwable.printStackTrace(writer);
          }

          return List.of(
            MessageBuilder.translated("discord.autowhitelist.response.error.title"),
            MessageBuilder.translated(
              "discord.autowhitelist.response.error.description",
              command,
              event,
              throwable.getClass().getCanonicalName(),
              throwable.getMessage(),
              outputStream.toString()
            )
          );
      }
    );
}
