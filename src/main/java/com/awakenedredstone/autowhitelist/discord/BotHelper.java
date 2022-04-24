package com.awakenedredstone.autowhitelist.discord;

import com.awakenedredstone.autowhitelist.discord.api.text.TranslatableText;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.minecraft.text.Text;

import java.awt.*;
import java.util.concurrent.TimeUnit;

public class BotHelper extends Bot {

    public static void sendFeedbackMessage(MessageChannel channel, Text title, Text message) {
        MessageAction messageAction = channel.sendMessage(generateFeedbackMessage(title, message));
        messageAction.queue();
    }

    public static void sendFeedbackMessage(MessageChannel channel, Text title, Text message, MessageType type) {
        MessageAction messageAction = channel.sendMessage(generateFeedbackMessage(title, message, type));
        messageAction.queue();
    }

    public static void sendTempFeedbackMessage(MessageChannel channel, Text title, Text message, int seconds) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor(jda.getSelfUser().getName(), "https://discord.com", jda.getSelfUser().getAvatarUrl());
        embedBuilder.setTitle(title.getString());
        embedBuilder.setDescription(message.getString());
        embedBuilder.setFooter(String.format("This message will be deleted %s seconds after being sent.", seconds));
        MessageAction messageAction = channel.sendMessage(new MessageBuilder().setEmbeds(embedBuilder.build()).build());
        messageAction.queue(m -> m.delete().queueAfter(seconds, TimeUnit.SECONDS));
    }

    public static Message generateFeedbackMessage(Text title, Text message) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor(jda.getSelfUser().getName(), "https://discord.com", jda.getSelfUser().getAvatarUrl());
        embedBuilder.setTitle(title.getString());
        embedBuilder.setDescription(message.getString());
        embedBuilder.setFooter(new TranslatableText("command.feedback.message.signature").getString());
        return new MessageBuilder(embedBuilder.build()).build();
    }

    public static Message generateFeedbackMessage(Text title, Text message, MessageType type) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor(jda.getSelfUser().getName(), "https://discord.com", jda.getSelfUser().getAvatarUrl());
        embedBuilder.setTitle(title.getString());
        embedBuilder.setDescription(message.getString());
        embedBuilder.setFooter(new TranslatableText("command.feedback.message.signature").getString());
        embedBuilder.setColor(type.hexColor);
        return new MessageBuilder(embedBuilder.build()).build();
    }

    public static void sendSimpleMessage(MessageChannel channel, Text message) {
        MessageAction messageAction = channel.sendMessage(message.getString());
        messageAction.queue();
    }

    public static void sendTempSimpleMessage(MessageChannel channel, Text message, int seconds) {
        MessageAction messageAction = channel.sendMessage(message.getString());
        messageAction.queue(m -> m.delete().queueAfter(seconds, TimeUnit.SECONDS));
    }

    public enum MessageType {
        DEBUG(new Color(19, 40, 138)),
        NORMAL(Role.DEFAULT_COLOR_RAW),
        INFO(new Color(176, 154, 15)),
        SUCCESS(new Color(50, 134, 25)),
        WARNING(new Color(208, 102, 21)),
        ERROR(new Color(141, 29, 29)),
        FATAL(new Color(212, 4, 4));

        private final int hexColor;

        MessageType(Color hexColor) {
            this.hexColor = hexColor.getRGB();
        }

        MessageType(int hexColor) {
            this.hexColor = hexColor;
        }
    }
}
