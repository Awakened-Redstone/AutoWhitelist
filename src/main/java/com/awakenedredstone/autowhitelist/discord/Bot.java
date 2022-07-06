package com.awakenedredstone.autowhitelist.discord;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.discord.api.AutoWhitelistAPI;
import com.awakenedredstone.autowhitelist.discord.api.text.TranslatableText;
import com.awakenedredstone.autowhitelist.discord.commands.RegisterCommand;
import com.awakenedredstone.autowhitelist.discord.commands.developer.BotStatusCommand;
import com.awakenedredstone.autowhitelist.discord.commands.developer.ServerStatusCommand;
import com.awakenedredstone.autowhitelist.discord.commands.developer.StatusCommand;
import com.awakenedredstone.autowhitelist.discord.commands.development.TestCommand;
import com.awakenedredstone.autowhitelist.discord.commands.system.HelpCommand;
import com.awakenedredstone.autowhitelist.discord.commands.system.PingCommand;
import com.awakenedredstone.autowhitelist.discord.events.CoreEvents;
import com.awakenedredstone.autowhitelist.discord.events.GatewayEvents;
import com.awakenedredstone.autowhitelist.discord.events.JdaEvents;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.hooks.AnnotatedEventManager;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import javax.security.auth.login.LoginException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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
    private static String token = AutoWhitelist.getConfigData().token;
    private static String clientId = AutoWhitelist.getConfigData().clientId;
    public static String serverId = AutoWhitelist.getConfigData().discordServerId;
    public static String prefix = AutoWhitelist.getConfigData().prefix;
    public static long updateDelay = AutoWhitelist.getConfigData().whitelistScheduledVerificationSeconds;
    public static Map<String, String> whitelistDataMap = new HashMap<>();

    public static void stopBot(boolean force) {
        if (scheduledUpdate != null) {
            AutoWhitelist.LOGGER.info("Stopping scheduled events");
            scheduledUpdate.cancel(force);
            if (!force) {
                try {
                    scheduledUpdate.get();
                } catch (Exception ignored) {/**/}
            }
            scheduledUpdate = null;
        }

        if (!executorService.isShutdown()) executorService.shutdown();

        if (jda != null) {
            AutoWhitelist.LOGGER.info("Stopping the bot");
            if (force) jda.shutdownNow();
            else jda.shutdown();
            AutoWhitelist.LOGGER.info("Bot stopped");

            jda = null;
        }
    }

    public static Bot getInstance() {
        return instance;
    }

    public void reloadBot(ServerCommandSource source) {
        whitelistDataMap.clear();
        AutoWhitelist.getConfigData().whitelist.forEach((v, k) -> k.forEach(id_ -> whitelistDataMap.put(id_, v)));
        token = AutoWhitelist.getConfigData().token;
        clientId = AutoWhitelist.getConfigData().clientId;
        serverId = AutoWhitelist.getConfigData().discordServerId;
        prefix = AutoWhitelist.getConfigData().prefix;
        updateDelay = AutoWhitelist.getConfigData().whitelistScheduledVerificationSeconds;
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
            AutoWhitelistAPI.INSTANCE = new AutoWhitelistAPI();
            if (whitelistDataMap.isEmpty())
                AutoWhitelist.getConfigData().whitelist.forEach((v, k) -> k.forEach(id_ -> whitelistDataMap.put(id_, v)));
            JDABuilder builder = JDABuilder.createDefault(token);
            builder.setEventManager(new AnnotatedEventManager());
            builder.addEventListeners(new JdaEvents());
            builder.addEventListeners(new CoreEvents());
            builder.addEventListeners(new GatewayEvents());
            builder.enableIntents(GatewayIntent.GUILD_MEMBERS);
            builder.setMemberCachePolicy(MemberCachePolicy.ALL);
            jda = builder.build();

            try {
                jda.getPresence().setActivity(Activity.of(Activity.ActivityType.valueOf(new TranslatableText("bot.activity.type").getString().toUpperCase().replace("PLAYING", "DEFAULT")), new TranslatableText("bot.activity.message").getString()));
            } catch (IllegalArgumentException |
                     NullPointerException e) { //TODO: remove the replace once JDA is updated to 5.x.x
                AutoWhitelist.LOGGER.error("Failed to set bot activity, the activity type {} is not valid.", new TranslatableText("bot.activity.type").getString().toUpperCase(), e);
            }

            TestCommand.register(AutoWhitelistAPI.dispatcher());
            HelpCommand.register(AutoWhitelistAPI.dispatcher());
            PingCommand.register(AutoWhitelistAPI.dispatcher());
            BotStatusCommand.register(AutoWhitelistAPI.dispatcher());
            ServerStatusCommand.register(AutoWhitelistAPI.dispatcher());
            StatusCommand.register(AutoWhitelistAPI.dispatcher());
            RegisterCommand.register(AutoWhitelistAPI.dispatcher());

            instance = this;
        } catch (LoginException e) {
            AutoWhitelist.LOGGER.error("Failed to start bot, please verify the token.");
        } catch (Exception e) {
            AutoWhitelist.LOGGER.error("Failed to start bot!", e);
        }
    }
}
