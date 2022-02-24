package com.awakenedredstone.autowhitelist.discord.api.command;

import com.awakenedredstone.autowhitelist.discord.api.BotHelper;
import com.awakenedredstone.autowhitelist.discord.api.AutoWhitelistAPI;
import com.awakenedredstone.autowhitelist.discord.api.text.LiteralText;
import com.awakenedredstone.autowhitelist.discord.api.text.Text;
import com.mojang.brigadier.ResultConsumer;
import com.mojang.brigadier.context.CommandContext;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.Event;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class DiscordCommandSource {
    private final User user;
    @Nullable private final Member member;
    private final Message message;
    private final MessageChannel channel;
    private final CommandType type;
    private final Event event;

    private final ResultConsumer<DiscordCommandSource> resultConsumer;

    public DiscordCommandSource(User user, Message message, MessageChannel channel, CommandType type, Event event) {
        this(user, null, message, channel, type, event);
    }

    public DiscordCommandSource(Member member, Message message, MessageChannel channel, CommandType type, Event event) {
        this(member.getUser(), member, message, channel, type, event);
    }

    public DiscordCommandSource(User user, @Nullable Member member, Message message, MessageChannel channel, CommandType type, Event event) {
        this.user = user;
        this.member = member;
        this.message = message;
        this.channel = channel;
        this.resultConsumer = (context, success, result) -> {};
        this.type = type;
        this.event = event;
    }

    public void sendFeedback(Text message) {
        BotHelper.sendSimpleMessage(channel, message);
    }

    public void sendError(Text message) {
        BotHelper.sendFeedbackMessage(channel, new LiteralText(" "), message, BotHelper.MessageType.ERROR);
    }

    public void onCommandComplete(CommandContext<DiscordCommandSource> context, boolean success, int result) {
        if (this.resultConsumer != null) {
            this.resultConsumer.onCommandComplete(context, success, result);
        }

    }

    public User getUser() {
        return user;
    }

    @Nullable
    public Member getMember() {
        return member;
    }

    public Message getMessage() {
        return message;
    }

    public <T extends MessageChannel> T getChannel() {
        return (T)channel;
    }

    public AutoWhitelistAPI getApi() {
        return AutoWhitelistAPI.INSTANCE;
    }

    public boolean isFromGuild() {
        return member != null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (DiscordCommandSource) obj;
        return Objects.equals(this.user, that.user) &&
                Objects.equals(this.message, that.message) &&
                Objects.equals(this.channel, that.channel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, message, channel);
    }

    @Override
    public String toString() {
        return "DiscordCommandSource[" +
                "user=" + user + ", " +
                "message=" + message + ", " +
                "channel=" + channel + ']';
    }

    public CommandType getType() {
        return type;
    }

    public Event getEvent() {
        return event;
    }

    public enum CommandType {
        MESSAGE,
        SLASH_COMMAND
    }
}
