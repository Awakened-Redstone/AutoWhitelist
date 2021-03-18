package com.awakenedredstone.autowhitelist.discord;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.lang.TranslatableText;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.util.logging.UncaughtExceptionHandler;

import javax.security.auth.login.LoginException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

public class Bot implements Runnable {
    private static Bot instance;

    static ScheduledFuture<?> scheduledUpdate;

    public static JDA jda = null;
    private static String token = AutoWhitelist.getConfigData().token;
    private static String clientId = AutoWhitelist.getConfigData().clientId;
    public static String serverId = AutoWhitelist.getConfigData().discordServerId;
    public static String prefix = AutoWhitelist.getConfigData().prefix;
    public static long updateDelay = AutoWhitelist.getConfigData().whitelistScheduledVerificationSeconds;
    public static Map<String, String> whitelistDataMap = new HashMap<>();

    public static void stopBot() {
        if (scheduledUpdate != null) {
            scheduledUpdate.cancel(false);
            try {
                scheduledUpdate.get();
            } catch (Exception ignored) {
            }
        }
        if (jda != null) jda.shutdown();
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
            } catch (Exception ignored) {
            }
        }
        if (jda != null) jda.shutdown();

        Thread thread = new Thread(new Bot());
        thread.setName("AutoWhitelist Bot");
        thread.setUncaughtExceptionHandler(new UncaughtExceptionHandler(AutoWhitelist.LOGGER));
        thread.setDaemon(true);
        thread.start();

        source.sendFeedback(new LiteralText("Discord bot starting."), true);
    }

    public void run() {
        init();
    }

    private void init() {
        try {
            if (whitelistDataMap.isEmpty()) AutoWhitelist.getConfigData().whitelist.forEach((v, k) -> k.forEach(id_ -> whitelistDataMap.put(id_, v)));
            jda = JDABuilder.createDefault(token).enableIntents(GatewayIntent.GUILD_MEMBERS).setMemberCachePolicy(MemberCachePolicy.ALL).build();
            jda.addEventListener(new BotEventListener());
            try {
                jda.getPresence().setActivity(Activity.of(Activity.ActivityType.valueOf(new TranslatableText("bot.activity.type").getString().toUpperCase()), new TranslatableText("bot.activity.message").getString()));
            } catch (IllegalArgumentException | NullPointerException e) {
                AutoWhitelist.LOGGER.error("Failed to set bot activity, the chosen activity type value is not valid.");
            }
            instance = this;
        } catch (LoginException e) {
            AutoWhitelist.LOGGER.error("Failed to start bot, please verify the token.");
        }
    }
}
