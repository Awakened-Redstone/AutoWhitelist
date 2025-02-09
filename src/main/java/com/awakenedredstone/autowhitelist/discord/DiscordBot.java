package com.awakenedredstone.autowhitelist.discord;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.config.AutoWhitelistConfig;
import com.awakenedredstone.autowhitelist.discord.api.GatewayIntents;
import com.awakenedredstone.autowhitelist.discord.command.InfoCommand;
import com.awakenedredstone.autowhitelist.discord.command.RegisterCommand;
import com.awakenedredstone.autowhitelist.discord.command.admin.ModifyCommand;
import com.awakenedredstone.autowhitelist.discord.command.admin.UserInfoCommand;
import com.awakenedredstone.autowhitelist.discord.events.CoreEvents;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;

//TODO: Improve/rework the bot class
public class DiscordBot extends Thread {
    public static final Logger LOGGER = LoggerFactory.getLogger("AutoWhitelist Bot");
    public static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newScheduledThreadPool(1, runnable -> {
        Thread thread = new Thread(runnable);
        thread.setName("AutoWhitelist Worker");
        thread.setDaemon(true); // Allow the server to shut down
        thread.setUncaughtExceptionHandler((t, e) -> LOGGER.error("Unhandled exception in worker thread", e));
        return thread;
    });
    public static EventWaiter eventWaiter;
    public static ScheduledFuture<?> scheduledUpdate;
    @Nullable private static JDA jda = null;
    @Nullable private static Guild guild = null;
    @Nullable private static DiscordBot instance;

    public DiscordBot() {
        super("AutoWhitelist Bot");
        this.setDaemon(true);
        this.setUncaughtExceptionHandler((t, e) -> LOGGER.error("Unhandled exception in bot thread", e));
        if (instance != null) {
            LOGGER.warn("Bot instance already exists, stopping the previous instance");
            instance.interrupt();
        }
        instance = this;
    }

    public static void stopBot(boolean force) {
        AutoWhitelist.LOGGER.info("Stopping scheduled events");
        if (scheduledUpdate != null) {
            scheduledUpdate.cancel(force);
            if (!force) {
                try {
                    scheduledUpdate.get();
                } catch (Throwable ignored) {/**/}
            }
            scheduledUpdate = null;
        }

        if (jda != null) {
            AutoWhitelist.LOGGER.info("Stopping the bot");
            if (force) jda.shutdownNow();
            else jda.shutdown();
            AutoWhitelist.LOGGER.info("Bot stopped");
        }
        jda = null;
        setGuild(null);
    }

    @Nullable
    public static DiscordBot getInstance() {
        return instance;
    }

    @NotNull
    public static DiscordBot getInstanceSafe() {
        if (instance == null) {
            throw new NullPointerException("Bot instance is null, expected bot to be running, please open a bug report!");
        }
        return instance;
    }

    @NotNull
    public static JDA getJda() {
        if (jda == null) {
            throw new NullPointerException("Bot is null, expected bot to exist, please open a bug report!");
        }

        return jda;
    }

    public static void setJda(@Nullable JDA jda) {
        DiscordBot.jda = jda;
    }

    public static boolean botExists() {
        return jda != null;
    }

    public static @Nullable Guild getGuild() {
        return guild;
    }

    public static void setGuild(@Nullable Guild guild) {
        DiscordBot.guild = guild;
    }

    public static void startInstance() {
        if (instance == null) {
            new DiscordBot().start();
        } else {
            instance.execute();
        }
    }

    public void restartBot() {
        if (scheduledUpdate != null) {
            scheduledUpdate.cancel(false);
            try {
                scheduledUpdate.get();
            } catch (Throwable ignored) {/**/}
        }
        if (jda != null) jda.shutdown();

        execute();
    }

    private boolean validateConfigs() {
        if (StringUtils.isBlank(AutoWhitelist.CONFIG.token)) {
            LOGGER.error("Empty bot token, please review your configurations");
            return false;
        }
        if (AutoWhitelist.CONFIG.token.equalsIgnoreCase("DO NOT SHARE THE BOT TOKEN")) {
            LOGGER.error("Unset bot token, please review your configurations");
            return false;
        }
        if (AutoWhitelist.CONFIG.discordServerId <= 0) {
            LOGGER.error("Invalid discord server id, please review your configurations");
            return false;
        }
        return true;
    }

    @Override
    public void run() {
        execute();
    }

    public void execute() {
        if (!validateConfigs()) {
            LOGGER.error("Refusing to initiate the Discord bot, invalid configuration");
            return;
        }

        if (jda != null) {
            LOGGER.warn("Bot already running, stopping the previous instance");
            stopBot(false);
        }

        jda = null;
        setGuild(null);

        try {
            CommandClientBuilder commandBuilder = new CommandClientBuilder()
              .setOwnerId(0) // Why is this required ._.
              .setPrefix(AutoWhitelist.CONFIG.prefix)
              .setHelpConsumer(helpConsumer())
              .addSlashCommands(
                new RegisterCommand(),
                new InfoCommand(),
                new UserInfoCommand(),
                new ModifyCommand()
              ).setActivity(null);

            CommandClient commands = commandBuilder.build();

            JDABuilder builder = JDABuilder.createDefault(AutoWhitelist.CONFIG.token)
              .addEventListeners(new CoreEvents(), commands)
              .enableIntents(GatewayIntents.BASIC);

            if (AutoWhitelist.CONFIG.cacheDiscordData) {
                builder.setMemberCachePolicy(MemberCachePolicy.ALL);
            } else {
                builder.setMemberCachePolicy(MemberCachePolicy.NONE);
            }

            jda = builder.build();

            if (AutoWhitelist.CONFIG.botActivityType != AutoWhitelistConfig.BotActivity.DONT_CHANGE) {
                jda.getPresence().setActivity(AutoWhitelist.CONFIG.botActivityType.getActivity());
            }
        } catch (InvalidTokenException e) {
            AutoWhitelist.LOGGER.error("Invalid bot token, please review your configurations");
        } catch (Throwable e) {
            AutoWhitelist.LOGGER.error("An unexpected error occurred while starting the bot", e);
        }
    }

    @Override
    public void interrupt() {
        instance = null;
        stopBot(true);
        super.interrupt();
    }

    private Consumer<CommandEvent> helpConsumer() {
        return (event) -> {
            EmbedBuilder builder = new EmbedBuilder().setAuthor(getJda().getSelfUser().getName(), "https://discord.com", getJda().getSelfUser().getAvatarUrl());
            Command.Category category;
            List<MessageEmbed.Field> fields = new ArrayList<>();
            for (Command command : event.getClient().getCommands()) {
                if ((!command.isHidden() && !command.isOwnerCommand()) || event.isOwner()) {

                    String command_ = "\n`" +
                      event.getClient().getPrefix() +
                      (AutoWhitelist.CONFIG.prefix == null ? " " : "") +
                      command.getName() +
                      (command.getArguments() == null ? "" : " " + command.getArguments()) +
                      "` __ __ | __ __ " + command.getHelp();

                    category = command.getCategory();
                    fields.add(new MessageEmbed.Field(category == null ? "No Category" : category.getName(), command_, false));
                }
            }

            List<MessageEmbed.Field> mergedFields = new ArrayList<>();
            String commands = "";
            String lastName = "";
            for (MessageEmbed.Field field : fields) {
                if (Objects.equals(field.getName(), lastName)) {
                    commands += "\n" + field.getValue();
                    if (fields.get(fields.size() - 1) == field) {
                        mergedFields.add(new MessageEmbed.Field(lastName, commands, false));
                    }
                } else if (!commands.isEmpty()) {
                    mergedFields.add(new MessageEmbed.Field(lastName, commands, false));
                    commands = "";
                    commands += "\n" + field.getValue();
                    lastName = field.getName();
                } else if (fields.size() > 1) {
                    commands += field.getValue();
                    lastName = field.getName();
                } else {
                    mergedFields.add(new MessageEmbed.Field(field.getName(), field.getValue(), false));
                }
            }

            mergedFields.forEach(builder::addField);
            event.reply(builder.build());
        };
    }
}
