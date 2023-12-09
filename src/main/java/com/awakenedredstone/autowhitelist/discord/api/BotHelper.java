package com.awakenedredstone.autowhitelist.discord.api;

import com.awakenedredstone.autowhitelist.discord.Bot;
import com.awakenedredstone.autowhitelist.discord.api.util.Markdown;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.minecraft.text.Text;

import java.awt.Color;
import java.util.concurrent.TimeUnit;

@Deprecated
public class BotHelper {
    public static void sendFeedbackMessage(MessageChannel channel, Text title, Text message) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor(Bot.jda.getSelfUser().getName(), "https://discord.com", Bot.jda.getSelfUser().getAvatarUrl());
        embedBuilder.setTitle(title.getString());
        embedBuilder.setDescription(message.getString());
        embedBuilder.setFooter(Text.translatable("command.feedback.message.signature").getString());
        MessageCreateAction messageAction = channel.sendMessageEmbeds(embedBuilder.build());
        messageAction.queue();
    }

    public static void sendFeedbackMessage(MessageChannel channel, Text title, Text message, MessageType type) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor(Bot.jda.getSelfUser().getName(), "https://discord.com", Bot.jda.getSelfUser().getAvatarUrl());
        embedBuilder.setTitle(title.getString());
        embedBuilder.setDescription(message.getString());
        embedBuilder.setFooter(Text.translatable("command.feedback.message.signature").getString());
        embedBuilder.setColor(type.hexColor);
        MessageCreateAction messageAction = channel.sendMessageEmbeds(embedBuilder.build());
        messageAction.queue();
    }

    public static void sendTempFeedbackMessage(MessageChannel channel, Text title, Text message, int seconds) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor(Bot.jda.getSelfUser().getName(), "https://discord.com", Bot.jda.getSelfUser().getAvatarUrl());
        embedBuilder.setTitle(title.getString());
        embedBuilder.setDescription(message.getString());
        embedBuilder.setFooter(String.format("This message will be deleted %s seconds after being sent.", seconds));
        MessageCreateAction messageAction = channel.sendMessageEmbeds(embedBuilder.build());
        messageAction.queue(m -> m.delete().queueAfter(seconds, TimeUnit.SECONDS));
    }

    public static MessageCreateData generateFeedbackMessage(Text title, Text message) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor(Bot.jda.getSelfUser().getName(), "https://discord.com", Bot.jda.getSelfUser().getAvatarUrl());
        embedBuilder.setTitle(title.getString());
        embedBuilder.setDescription(message.getString());
        embedBuilder.setFooter(Text.translatable("command.feedback.message.signature").getString());
        return new MessageCreateBuilder().setEmbeds(embedBuilder.build()).build();
    }

    public static MessageCreateData generateFeedbackMessage(Text title, Text message, MessageType type) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor(Bot.jda.getSelfUser().getName(), "https://discord.com", Bot.jda.getSelfUser().getAvatarUrl());
        embedBuilder.setTitle(Markdown.formatText(title));
        embedBuilder.setDescription(Markdown.formatText(message));
        embedBuilder.setFooter(Text.translatable("command.feedback.message.signature").getString());
        embedBuilder.setColor(type.hexColor);
        return new MessageCreateBuilder().setEmbeds(embedBuilder.build()).build();
    }

    public static void sendSimpleMessage(MessageChannel channel, Text message) {
        MessageCreateAction messageAction = channel.sendMessage(Markdown.formatText(message));
        messageAction.queue();
    }

    public static void sendTempSimpleMessage(MessageChannel channel, Text message, int seconds) {
        MessageCreateAction messageAction = channel.sendMessage(Markdown.formatText(message));
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

        public final int hexColor;

        MessageType(Color hexColor) {
            this.hexColor = hexColor.getRGB();
        }

        MessageType(int hexColor) {
            this.hexColor = hexColor;
        }
    }
}
