package com.awakenedredstone.autowhitelist.discord.message.responses;

import com.awakenedredstone.autowhitelist.discord.message.MessageBuilder;
import com.awakenedredstone.autowhitelist.discord.message.ResponseMessage;
import discord4j.core.object.component.Container;
import discord4j.core.object.component.Separator;
import discord4j.rest.util.Color;
import net.minecraft.resources.Identifier;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.List;

import static com.awakenedredstone.autowhitelist.AutoWhitelist.id;

public class CommonResponseMessages {
    /// The command has failed catastrophically, notify the user about the failure
    public static final Identifier INTERACTION_CRASH = ResponseMessage.<ResponseTypes.InteractionCrash<?>>register(
      id("interaction/crash"), (interaction, event, throwable) -> {
          var outputStream = new ByteArrayOutputStream();
          try (var writer = new PrintWriter(outputStream)) {
              throwable.printStackTrace(writer);
          }

          return List.of(
            MessageBuilder.translated("discord.autowhitelist.response.error.crash.title"),
            Container.of(
              Color.of(212, 4, 4),
              MessageBuilder.translated(
                "discord.autowhitelist.response.error.crash.description",
                throwable.toString()
              )
            )
          );
      }
    );
}
