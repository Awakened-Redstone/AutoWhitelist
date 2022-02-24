package com.awakenedredstone.autowhitelist.discord.events;

import com.awakenedredstone.autowhitelist.discord.Bot;
import net.dv8tion.jda.api.events.ShutdownEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;

import static com.awakenedredstone.autowhitelist.discord.Bot.scheduledUpdate;

public class JdaEvents {

    @SubscribeEvent
    public void onShutdown(ShutdownEvent e) {}
}
