/*
 * This file is part of Discord4J.
 *
 * Discord4J is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Discord4J is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Discord4J. If not, see <http://www.gnu.org/licenses/>.
 */
package com.awakenedredstone.autowhitelist.discord.store;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.ScheduledEventUser;
import discord4j.core.object.VoiceState;
import discord4j.core.object.automod.AutoModRule;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.retriever.EntityRetriever;
import discord4j.core.retriever.RestEntityRetriever;
import discord4j.core.retriever.StoreEntityRetriever;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

public class DynamicRetriever implements EntityRetriever {
    public final ScopedValue<Mode> mode = ScopedValue.newInstance();
    private final EntityRetriever first;
    private final EntityRetriever fallback;

    public DynamicRetriever(EntityRetriever first, EntityRetriever fallback) {
        this.first = first;
        this.fallback = fallback;
    }

    public static DynamicRetriever common(GatewayDiscordClient gateway) {
        return new DynamicRetriever(new StoreEntityRetriever(gateway), new RestEntityRetriever(gateway));
    }

    private <T> Flux<T> flux(Function<EntityRetriever, Flux<T>> mapper) {
        return mode.isBound() ? mapper.apply(mode.get().get(this)) : mapper.apply(first).switchIfEmpty(mapper.apply(fallback));
    }

    private <T> Mono<T> mono(Function<EntityRetriever, Mono<T>> mapper) {
        return mode.isBound() ? mapper.apply(mode.get().get(this)) : mapper.apply(first).switchIfEmpty(mapper.apply(fallback));
    }

    @Override
    public Mono<Channel> getChannelById(Snowflake channelId) {
        return mono(retriever -> retriever.getChannelById(channelId));
    }

    @Override
    public Mono<Guild> getGuildById(Snowflake guildId) {
        return mono(retriever -> retriever.getGuildById(guildId));
    }

    @Override
    public Mono<GuildSticker> getGuildStickerById(Snowflake guildId, Snowflake stickerId) {
        return mono(retriever -> retriever.getGuildStickerById(guildId, stickerId));
    }

    @Override
    public Mono<GuildEmoji> getGuildEmojiById(Snowflake guildId, Snowflake emojiId) {
        return mono(retriever -> retriever.getGuildEmojiById(guildId, emojiId));
    }

    @Override
    public Mono<Member> getMemberById(Snowflake guildId, Snowflake userId) {
        return mono(retriever -> retriever.getMemberById(guildId, userId));
    }

    @Override
    public Mono<Message> getMessageById(Snowflake channelId, Snowflake messageId) {
        return mono(retriever -> retriever.getMessageById(channelId, messageId));
    }

    @Override
    public Mono<Role> getRoleById(Snowflake guildId, Snowflake roleId) {
        return mono(retriever -> retriever.getRoleById(guildId, roleId));
    }

    @Override
    public Mono<User> getUserById(Snowflake userId) {
        return mono(retriever -> retriever.getUserById(userId));
    }

    @Override
    public Flux<Guild> getGuilds() {
        return flux(EntityRetriever::getGuilds);
    }

    @Override
    public Mono<User> getSelf() {
        return mono(EntityRetriever::getSelf);
    }

    @Override
    public Mono<Member> getSelfMember(Snowflake guildId) {
        return mono(retriever -> retriever.getSelfMember(guildId));
    }

    @Override
    public Flux<Member> getGuildMembers(Snowflake guildId) {
        return flux(retriever -> retriever.getGuildMembers(guildId));
    }

    @Override
    public Flux<GuildChannel> getGuildChannels(Snowflake guildId) {
        return flux(retriever -> retriever.getGuildChannels(guildId));
    }

    @Override
    public Flux<Role> getGuildRoles(Snowflake guildId) {
        return flux(retriever -> retriever.getGuildRoles(guildId));
    }

    @Override
    public Flux<GuildEmoji> getGuildEmojis(Snowflake guildId) {
        return flux(retriever -> retriever.getGuildEmojis(guildId));
    }

    @Override
    public Mono<StageInstance> getStageInstanceByChannelId(Snowflake channelId) {
        return mono(retriever -> retriever.getStageInstanceByChannelId(channelId));
    }

    @Override
    public Flux<GuildSticker> getGuildStickers(Snowflake guildId) {
        return flux(retriever -> retriever.getGuildStickers(guildId));
    }

    @Override
    public Mono<ThreadMember> getThreadMemberById(Snowflake threadId, Snowflake userId) {
        return mono(retriever -> retriever.getThreadMemberById(threadId, userId));
    }

    @Override
    public Flux<ThreadMember> getThreadMembers(Snowflake threadId) {
        return flux(retriever -> retriever.getThreadMembers(threadId));
    }

    @Override
    public Flux<AutoModRule> getGuildAutoModRules(Snowflake guildId) {
        return flux(retriever -> retriever.getGuildAutoModRules(guildId));
    }

    @Override
    public Mono<ScheduledEvent> getScheduledEventById(Snowflake guildId, Snowflake eventId) {
        return mono(retriever -> retriever.getScheduledEventById(guildId, eventId));
    }

    @Override
    public Flux<ScheduledEvent> getScheduledEvents(Snowflake guildId) {
        return flux(retriever -> retriever.getScheduledEvents(guildId));
    }

    @Override
    public Flux<ScheduledEventUser> getScheduledEventUsers(Snowflake guildId, Snowflake eventId) {
        return flux(retriever -> retriever.getScheduledEventUsers(guildId, eventId));
    }

    @Override
    public Mono<VoiceState> getVoiceStateById(Snowflake guildId, Snowflake userId) {
        return mono(retriever -> retriever.getVoiceStateById(guildId, userId));
    }

    public enum Mode {
        FIRST(retriever -> retriever.first),
        FALLBACK(retriever -> retriever.fallback);

        private final Function<DynamicRetriever, EntityRetriever> field;

        Mode(Function<DynamicRetriever, EntityRetriever> field) {
            this.field = field;
        }

        EntityRetriever get(DynamicRetriever retriever) {
            return field.apply(retriever);
        }
    }
}
