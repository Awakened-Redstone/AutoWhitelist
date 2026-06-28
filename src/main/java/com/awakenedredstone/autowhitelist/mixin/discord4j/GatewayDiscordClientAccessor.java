package com.awakenedredstone.autowhitelist.mixin.discord4j;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.retriever.EntityRetriever;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GatewayDiscordClient.class)
public interface GatewayDiscordClientAccessor {
    @Accessor EntityRetriever getEntityRetriever();
}
