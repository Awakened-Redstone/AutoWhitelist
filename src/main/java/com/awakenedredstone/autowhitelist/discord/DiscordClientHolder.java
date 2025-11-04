package com.awakenedredstone.autowhitelist.discord;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.concurrent.NamedThreadFactory;
import com.awakenedredstone.autowhitelist.concurrent.SingleTaskExecutor;
import com.awakenedredstone.autowhitelist.concurrent.atomic.LockingAtomicReference;
import com.awakenedredstone.autowhitelist.discord.commands.api.CommandRegistry;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

public class DiscordClientHolder implements Runnable {
    public static final Logger LOGGER = LoggerFactory.getLogger(DiscordClientHolder.class);
    public static final SingleTaskExecutor<DiscordClientHolder> BOT_SERVICE = new SingleTaskExecutor<>(
      new NamedThreadFactory(
        ignored -> "Discord Bot",
        thread -> thread.setUncaughtExceptionHandler((t, e) -> LOGGER.error("Unhandled exception in bot thread {}", t, e))
      )
    );

    public final LockingAtomicReference<GatewayDiscordClient> client = new LockingAtomicReference<>();
    public final LockingAtomicReference<Guild> guild = new LockingAtomicReference<>();
    private boolean failed = false;

    public static void queueBotInit() {
        if (BOT_SERVICE.getPendingTaskCount() > 0) {
            LOGGER.warn("Refusing to queue bot, a thread is already queued!", new RuntimeException());
            return;
        }

        BOT_SERVICE.submit(new DiscordClientHolder());
    }

    public static DiscordClientHolder getCurrent() {
        return BOT_SERVICE.getCurrentTask();
    }

    public static boolean hasTask() {
        DiscordClientHolder current = getCurrent();
        return current != null && !current.failed;
    }

    private void execute() {
        GatewayDiscordClient client = DiscordClient.create(AutoWhitelist.CONFIG.token)
          .gateway()
          .setEnabledIntents(IntentSet.of(Intent.GUILD_MEMBERS))
          .login()
          .blockOptional()
          .orElseThrow();

        this.client.setAndLock(client);

        CommandRegistry registry = new CommandRegistry();

        this.guild.setAndLock(client.getGuildById(Snowflake.of(AutoWhitelist.CONFIG.discordServerId)).block());

        client.on(ApplicationCommandInteractionEvent.class, event -> Mono.fromRunnable(() -> registry.execute(event))).subscribe();

        client.onDisconnect().block();
        LOGGER.info("Discord bot disconnected, closing thread");
    }

    @Override
    public void run() {
        try {
            execute();
        } catch (Exception e) {
            this.failed = true;
            GatewayDiscordClient client = this.client.get();
            if (client != null) {
                client.logout().block();
            }

            throw e;
        }
    }
}
