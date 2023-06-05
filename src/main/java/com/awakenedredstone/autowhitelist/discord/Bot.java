package com.awakenedredstone.autowhitelist.discord;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.discord.api.DiscordBrigadierHelper;
import com.awakenedredstone.autowhitelist.discord.api.GatewayIntents;
import com.awakenedredstone.autowhitelist.discord.commands.RegisterCommand;
import com.awakenedredstone.autowhitelist.discord.commands.debug.BotStatusCommand;
import com.awakenedredstone.autowhitelist.discord.commands.debug.ServerStatusCommand;
import com.awakenedredstone.autowhitelist.discord.commands.debug.StatusCommand;
import com.awakenedredstone.autowhitelist.discord.commands.development.TestCommand;
import com.awakenedredstone.autowhitelist.discord.commands.system.HelpCommand;
import com.awakenedredstone.autowhitelist.discord.commands.system.PingCommand;
import com.awakenedredstone.autowhitelist.discord.events.CoreEvents;
import com.awakenedredstone.autowhitelist.discord.events.GatewayEvents;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.hooks.AnnotatedEventManager;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

public class Bot extends Thread {
    public Bot() {
        super("AutoWhitelist Bot");
        this.setDaemon(true);
        this.setUncaughtExceptionHandler(new net.minecraft.util.logging.UncaughtExceptionHandler(AutoWhitelist.LOGGER));
    }

    private static Bot instance;

    public static ScheduledFuture<?> scheduledUpdate;
    public static final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    public static JDA jda = null;
    public static Guild guild = null;

    public static void stopBot(boolean force) {
        AutoWhitelist.LOGGER.info("Stopping scheduled events");
        if (scheduledUpdate != null) {
            scheduledUpdate.cancel(force);
            if (!force) {
                try {
                    scheduledUpdate.get();
                } catch (Exception ignored) {/**/}
            }
            scheduledUpdate = null;
        }

        if (force) executorService.shutdownNow();
        else executorService.shutdown();

        if (jda != null) {
            AutoWhitelist.LOGGER.info("Stopping the bot");
            if (force) jda.shutdownNow();
            else jda.shutdown();
            AutoWhitelist.LOGGER.info("Bot stopped");
        }
        jda = null;
        guild = null;
        instance = null;
    }

    public static Bot getInstance() {
        return instance;
    }

    public void reloadBot(ServerCommandSource source) {
        if (scheduledUpdate != null) {
            scheduledUpdate.cancel(false);
            try {
                scheduledUpdate.get();
            } catch (Exception ignored) {/**/}
        }
        if (jda != null) jda.shutdown();

        source.sendFeedback(Text.literal("Discord bot starting."), true);

        //noinspection CallToThreadRun
        run();
    }

    public void run() {
        try {
            jda = null;
            guild = null;
            instance = null;
            DiscordBrigadierHelper.INSTANCE = new DiscordBrigadierHelper();
            JDABuilder builder = JDABuilder.createDefault(AutoWhitelist.CONFIG.token());
            builder.setEventManager(new AnnotatedEventManager());
            builder.addEventListeners(new CoreEvents());
            builder.addEventListeners(new GatewayEvents());
            builder.enableIntents(GatewayIntents.BASIC);
            builder.setMemberCachePolicy(MemberCachePolicy.ALL);
            jda = builder.build();

            jda.getPresence().setActivity(AutoWhitelist.CONFIG.botActivityType().getActivity());

            TestCommand.register(DiscordBrigadierHelper.dispatcher());
            HelpCommand.register(DiscordBrigadierHelper.dispatcher());
            PingCommand.register(DiscordBrigadierHelper.dispatcher());
            BotStatusCommand.register(DiscordBrigadierHelper.dispatcher());
            ServerStatusCommand.register(DiscordBrigadierHelper.dispatcher());
            StatusCommand.register(DiscordBrigadierHelper.dispatcher());
            RegisterCommand.register(DiscordBrigadierHelper.dispatcher());

            instance = this;
        } catch (InvalidTokenException e) {
            AutoWhitelist.LOGGER.error("Invalid bot token, please review your configurations.");
        } catch (Exception e) {
            AutoWhitelist.LOGGER.error("Failed to start bot!", e);
        }
    }
}
