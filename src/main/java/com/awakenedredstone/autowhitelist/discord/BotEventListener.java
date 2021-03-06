package com.awakenedredstone.autowhitelist.discord;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.awakenedredstone.autowhitelist.discord.Bot.*;

public class BotEventListener extends ListenerAdapter {

    //
    @Override
    public void onReady(@NotNull ReadyEvent e) {
        AutoWhitelist.logger.info("Bot started. Parsing registered users.");
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
        scheduledUpdate = scheduler.scheduleWithFixedDelay(this::updateWhitelist, 0, updateDelay, TimeUnit.SECONDS);
    }
}
