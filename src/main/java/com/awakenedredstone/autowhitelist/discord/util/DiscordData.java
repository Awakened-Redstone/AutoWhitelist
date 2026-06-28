package com.awakenedredstone.autowhitelist.discord.util;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import reactor.core.publisher.Mono;

import java.util.Optional;

public class DiscordData {
    public static Optional<Member> getMember(Mono<User> userMono) {
        return userMono.blockOptional().map(user -> user.asMember(Snowflake.of(AutoWhitelist.config().discord.guildId)).block());
    }
}
