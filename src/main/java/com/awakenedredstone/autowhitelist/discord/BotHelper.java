package com.awakenedredstone.autowhitelist.discord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.messages.*;
/*? if <1.19 {*/
import net.minecraft.text.TranslatableText;
/*?}*/
import net.minecraft.text.Text;

import java.awt.Color;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

//TODO: Rewrite for cleaner code and better uses
public class BotHelper extends Bot {
    @Deprecated(since = "1.0.0-beta.1", forRemoval = true)
    public static void sendFeedbackMessage(MessageChannel channel, Text title, Text message) {
        MessageCreateAction messageAction = channel.sendMessage(generateFeedbackMessage(title, message));
        messageAction.queue();
    }

    @Deprecated(since = "1.0.0-beta.1", forRemoval = true)
    public static void sendFeedbackMessage(MessageChannel channel, Text title, Text message, MessageType type) {
        MessageCreateAction messageAction = channel.sendMessage(generateFeedbackMessage(title, message, type));
        messageAction.queue();
    }

    @Deprecated(since = "1.0.0-beta.1", forRemoval = true)
    public static void sendTempFeedbackMessage(MessageChannel channel, Text title, Text message, int seconds) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor(jda.getSelfUser().getName(), "https://discord.com", jda.getSelfUser().getAvatarUrl());
        embedBuilder.setTitle(title.getString());
        embedBuilder.setDescription(message.getString());
        embedBuilder.setFooter(String.format("This message will be deleted %s seconds after being sent.", seconds));
        MessageCreateAction messageAction = channel.sendMessage(new MessageCreateBuilder().setEmbeds(embedBuilder.build()).build());
        messageAction.queue(m -> m.delete().queueAfter(seconds, TimeUnit.SECONDS));
    }

    @Deprecated(since = "1.0.0-beta.1", forRemoval = true)
    public static MessageCreateData generateFeedbackMessage(Text title, Text message) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor(jda.getSelfUser().getName(), "https://discord.com", jda.getSelfUser().getAvatarUrl());
        embedBuilder.setTitle(title.getString());
        embedBuilder.setDescription(message.getString());
        embedBuilder.setFooter(/*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("command.feedback.message.signature").getString());
        return new MessageCreateBuilder().setEmbeds(embedBuilder.build()).build();
    }

    @Deprecated(since = "1.0.0-beta.1", forRemoval = true)
    public static MessageCreateData generateFeedbackMessage(Text title, Text message, MessageType type) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor(jda.getSelfUser().getName(), "https://discord.com", jda.getSelfUser().getAvatarUrl());
        embedBuilder.setTitle(title.getString());
        embedBuilder.setDescription(message.getString());
        embedBuilder.setFooter(/*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("command.feedback.message.signature").getString());
        embedBuilder.setColor(type.hexColor);
        return new MessageCreateBuilder().addEmbeds(embedBuilder.build()).build();
    }

    @Deprecated(since = "1.0.0-beta.1", forRemoval = true)
    public static MessageEditData generateEditFeedbackMessage(Text title, Text message, MessageType type) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor(jda.getSelfUser().getName(), "https://discord.com", jda.getSelfUser().getAvatarUrl());
        embedBuilder.setTitle(title.getString());
        embedBuilder.setDescription(message.getString());
        embedBuilder.setFooter(/*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("command.feedback.message.signature").getString());
        embedBuilder.setColor(type.hexColor);
        return new MessageEditBuilder().setEmbeds(embedBuilder.build()).build();
    }

    @Deprecated(since = "1.0.0-beta.1", forRemoval = true)
    public static void sendSimpleMessage(MessageChannel channel, Text message) {
        MessageCreateAction messageAction = channel.sendMessage(message.getString());
        messageAction.queue();
    }

    @Deprecated(since = "1.0.0-beta.1", forRemoval = true)
    public static void sendTempSimpleMessage(MessageChannel channel, Text message, int seconds) {
        MessageCreateAction messageAction = channel.sendMessage(message.getString());
        messageAction.queue(m -> m.delete().queueAfter(seconds, TimeUnit.SECONDS));
    }

    public static List<Role> getRolesForMember(Member member) {
        List<Role> roles = new ArrayList<>(member.getRoles());
        roles.add(member.getGuild().getPublicRole());
        return roles;
    }

    public static <T extends MessageData> T buildEmbedMessage(boolean edit, MessageEmbed... embeds) {
        if (edit) {
            return (T) new MessageEditBuilder().setEmbeds(embeds).build();
        } else {
            return (T) new MessageCreateBuilder().setEmbeds(embeds).build();
        }
    }

    public static String formatDiscordTimestamp(long timestamp) {
        return "<t:" + (timestamp / 1000) + ":R>";
    }

    public static class Feedback {
        public static EmbedBuilder defaultEmbed(Text title, Text message) {
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setAuthor(jda.getSelfUser().getName(), "https://discord.com", jda.getSelfUser().getAvatarUrl());
            embedBuilder.setTitle(title.getString());
            embedBuilder.setDescription(message.getString());
            embedBuilder.setFooter(/*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("command.feedback.message.signature").getString());
            return embedBuilder;
        }

        @Deprecated
        public static MessageEmbed buildEmbed(Text title, Text message) {
            return defaultEmbed(title, message).build();
        }

        public static MessageEmbed buildEmbed(Text title, Text message, MessageType type) {
            return defaultEmbed(title, message).setColor(type.hexColor).build();
        }
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
