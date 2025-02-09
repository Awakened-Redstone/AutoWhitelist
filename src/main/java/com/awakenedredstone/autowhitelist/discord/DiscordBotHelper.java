package com.awakenedredstone.autowhitelist.discord;

import com.awakenedredstone.autowhitelist.entry.RoleActionMap;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.minecraft.text.Text;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

//TODO: Rewrite for cleaner code and better uses (1/2 - Clean up)
public class DiscordBotHelper extends DiscordBot {
    public static List<Role> getRolesForMember(Member member) {
        List<Role> roles = new ArrayList<>(member.getRoles());
        roles.add(member.getGuild().getPublicRole());
        return roles;
    }

    public static List<Role> getValidRolesForMember(Member member) {
        return getRolesForMember(member).stream().filter(RoleActionMap::containsRole).toList();
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

    public static Optional<Role> getRoleFromString(String roleString) {
        if (DiscordBot.getInstance() == null || DiscordBot.getGuild() == null) {
            return Optional.empty();
        }

        Optional<Role> role;
        if (roleString.charAt(0) == '@') {
            String roleSearch = roleString.equalsIgnoreCase("@everyone") ? roleString : roleString.substring(1);
            List<Role> roles = DiscordBot.getGuild().getRolesByName(roleSearch, true);
            if (!roles.isEmpty()) {
                role = Optional.of(roles.get(0));
            } else {
                role = Optional.empty();
            }
        } else {
            role = Optional.ofNullable(DiscordBot.getGuild().getRoleById(roleString));
        }

        return role;
    }

    public static Optional<Role> getHighestEntryRole(Member member) {
        return getHighestEntryRole(getRolesForMember(member));
    }

    public static Optional<Role> getHighestEntryRole(List<Role> roles) {
        for (Role role : roles) {
            if (RoleActionMap.containsRole(role)) {
                return Optional.of(role);
            }
        }

        return Optional.empty();
    }

    public static class Feedback {
        public static EmbedBuilder defaultEmbed(Text title, Text message) {
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle(title.getString());
            embedBuilder.setDescription(message.getString());
            embedBuilder.setFooter(Text.translatable("discord.command.feedback.message.signature").getString());
            return embedBuilder;
        }

        public static MessageEmbed buildEmbed(Text title, Text message, MessageType type) {
            return defaultEmbed(title, message).setColor(type.hexColor).build();
        }

        public static MessageEmbed buildEmbed(Text title, Text message) {
            return buildEmbed(title, message, MessageType.NORMAL);
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
