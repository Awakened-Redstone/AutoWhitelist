package com.awakenedredstone.autowhitelist.discord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
/*? if <1.19 {*//*
import net.minecraft.text.TranslatableText;
*//*?}*/
import net.minecraft.text.Text;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

//TODO: Rewrite for cleaner code and better uses (1/2 - Clean up)
public class BotHelper extends Bot {
    public static List<Role> getRolesForMember(Member member) {
        List<Role> roles = new ArrayList<>(member.getRoles());
        roles.add(member.getGuild().getPublicRole());
        return roles;
    }

    @SuppressWarnings("unchecked")
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
            embedBuilder.setFooter(/*? if >=1.19 {*/Text.translatable/*?} else {*//*new TranslatableText*//*?}*/("command.feedback.message.signature").getString());
            return embedBuilder;
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
