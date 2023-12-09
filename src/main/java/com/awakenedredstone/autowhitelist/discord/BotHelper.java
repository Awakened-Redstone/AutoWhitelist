package com.awakenedredstone.autowhitelist.discord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import net.minecraft.text.Text;

import java.awt.Color;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class BotHelper extends Bot {

    public static void sendFeedbackMessage(MessageChannel channel, Text title, Text message) {
        MessageCreateAction messageAction = channel.sendMessage(generateFeedbackMessage(title, message));
        messageAction.queue();
    }

    public static void sendFeedbackMessage(MessageChannel channel, Text title, Text message, MessageType type) {
        MessageCreateAction messageAction = channel.sendMessage(generateFeedbackMessage(title, message, type));
        messageAction.queue();
    }

    public static void sendTempFeedbackMessage(MessageChannel channel, Text title, Text message, int seconds) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor(jda.getSelfUser().getName(), "https://discord.com", jda.getSelfUser().getAvatarUrl());
        embedBuilder.setTitle(title.getString());
        embedBuilder.setDescription(message.getString());
        embedBuilder.setFooter(String.format("This message will be deleted %s seconds after being sent.", seconds));
        MessageCreateAction messageAction = channel.sendMessage(new MessageCreateBuilder().setEmbeds(embedBuilder.build()).build());
        messageAction.queue(m -> m.delete().queueAfter(seconds, TimeUnit.SECONDS));
    }

    public static MessageCreateData generateFeedbackMessage(Text title, Text message) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor(jda.getSelfUser().getName(), "https://discord.com", jda.getSelfUser().getAvatarUrl());
        embedBuilder.setTitle(title.getString());
        embedBuilder.setDescription(message.getString());
        embedBuilder.setFooter(Text.translatable("command.feedback.message.signature").getString());
        return new MessageCreateBuilder().setEmbeds(embedBuilder.build()).build();
    }

    public static MessageCreateData generateFeedbackMessage(Text title, Text message, MessageType type) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor(jda.getSelfUser().getName(), "https://discord.com", jda.getSelfUser().getAvatarUrl());
        embedBuilder.setTitle(title.getString());
        embedBuilder.setDescription(message.getString());
        embedBuilder.setFooter(Text.translatable("command.feedback.message.signature").getString());
        embedBuilder.setColor(type.hexColor);
        return new MessageCreateBuilder().addEmbeds(embedBuilder.build()).build();
    }

    public static MessageEditData generateEditFeedbackMessage(Text title, Text message, MessageType type) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor(jda.getSelfUser().getName(), "https://discord.com", jda.getSelfUser().getAvatarUrl());
        embedBuilder.setTitle(title.getString());
        embedBuilder.setDescription(message.getString());
        embedBuilder.setFooter(Text.translatable("command.feedback.message.signature").getString());
        embedBuilder.setColor(type.hexColor);
        return new MessageEditBuilder().setEmbeds(embedBuilder.build()).build();
    }

    public static void sendSimpleMessage(MessageChannel channel, Text message) {
        MessageCreateAction messageAction = channel.sendMessage(message.getString());
        messageAction.queue();
    }

    public static void sendTempSimpleMessage(MessageChannel channel, Text message, int seconds) {
        MessageCreateAction messageAction = channel.sendMessage(message.getString());
        messageAction.queue(m -> m.delete().queueAfter(seconds, TimeUnit.SECONDS));
    }

    public static List<Role> getRolesForMember(Member member) {
        List<Role> roles = new ArrayList<>(member.getRoles());
        roles.add(member.getGuild().getPublicRole());
        return roles;
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
