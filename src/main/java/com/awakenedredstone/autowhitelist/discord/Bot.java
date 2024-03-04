package com.awakenedredstone.autowhitelist.discord;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.config.ConfigData;
import com.awakenedredstone.autowhitelist.discord.api.GatewayIntents;
import com.awakenedredstone.autowhitelist.discord.command.InfoCommand;
import com.awakenedredstone.autowhitelist.discord.command.RegisterCommand;
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
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.ServerCommandSource;
/*? if >=1.19 {*/
import net.minecraft.text.Text;
/*?} else {*//*
import net.minecraft.text.LiteralText;
*//*?} */
import org.apache.commons.lang3.StringUtils;
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
public class Bot extends Thread {
    public static final Logger LOGGER = LoggerFactory.getLogger("AutoWhitelist Bot");
    public static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newScheduledThreadPool(1);
    public static EventWaiter eventWaiter;
    public static ScheduledFuture<?> scheduledUpdate;
    public static JDA jda = null;
    public static Guild guild = null;
    private static Bot instance;

    public Bot() {
        super("AutoWhitelist Bot");
        this.setDaemon(true);
        this.setUncaughtExceptionHandler(new net.minecraft.util.logging.UncaughtExceptionHandler(AutoWhitelist.LOGGER));
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
                } catch (Exception ignored) {/**/}
            }
            scheduledUpdate = null;
        }

        if (force) EXECUTOR_SERVICE.shutdownNow();
        else EXECUTOR_SERVICE.shutdown();

        if (jda != null) {
            AutoWhitelist.LOGGER.info("Stopping the bot");
            if (force) jda.shutdownNow();
            else jda.shutdown();
            AutoWhitelist.LOGGER.info("Bot stopped");
        }
        jda = null;
        guild = null;
    }

    public static Bot getInstance() {
        return instance;
    }

    public static void startInstance() {
        if (instance == null) {
            new Bot().start();
        } else {
            instance.execute();
        }
    }

    public void reloadBot(ServerCommandSource source) {
        if (scheduledUpdate != null) {
            scheduledUpdate.cancel(false);
            try {
                scheduledUpdate.get();
            } catch (Exception ignored) {/**/}
        }
        if (jda != null) jda.shutdown();

        source.sendFeedback(/*? if >=1.20 {*/() ->/*?} */ /*? if >=1.19 {*/Text.literal/*?} else {*//*new LiteralText*//*?}*/("Discord bot starting."), true);

        execute();
    }

    private boolean validateConfigs() {
        if (StringUtils.isBlank(AutoWhitelist.CONFIG.token)) {
            LOGGER.error("Invalid bot token, please review your configurations.");
            return false;
        }
        if (StringUtils.isBlank(AutoWhitelist.CONFIG.discordServerId)) {
            LOGGER.error("Invalid discord server id, please review your configurations.");
            return false;
        }
        try {
            Long.parseLong(AutoWhitelist.CONFIG.discordServerId);
        } catch (NumberFormatException e) {
            LOGGER.error("Discord server id is not a valid 32-bit integer, please review your configurations.");
            return false;
        }
        for (String admin : AutoWhitelist.CONFIG.admins) {
            try {
                Long.parseLong(admin);
            } catch (NumberFormatException e) {
                LOGGER.error("Invalid admin id: \"{}\" is not a valid 32-bit integer, please review your configurations.", admin);
                return false;
            }
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
        guild = null;

        try {
            CommandClientBuilder commandBuilder = new CommandClientBuilder()
              .setPrefix(AutoWhitelist.CONFIG.prefix)
              .setOwnerId("0")
              .setHelpConsumer(helpConsumer())
              .addCommands(
                RegisterCommand.INSTANCE.new TextCommand()
              ).addSlashCommands(
                RegisterCommand.INSTANCE.new SlashCommand(),
                new InfoCommand()
              ).setActivity(null);

            if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
                commandBuilder.setOwnerId("387745099204919297").forceGuildOnly("387760260166844418");
            }

            CommandClient commands = commandBuilder.build();


            JDABuilder builder = JDABuilder.createDefault(AutoWhitelist.CONFIG.token);
            //builder.setEventManager(new AnnotatedEventManager());
            builder.addEventListeners(new CoreEvents(), commands);
            builder.enableIntents(GatewayIntents.BASIC);
            builder.setMemberCachePolicy(MemberCachePolicy.ALL);
            jda = builder.build();

            if (AutoWhitelist.CONFIG.botActivityType != ConfigData.BotActivity.NONE) {
                jda.getPresence().setActivity(AutoWhitelist.CONFIG.botActivityType.getActivity());
            }
        } catch (InvalidTokenException e) {
            AutoWhitelist.LOGGER.error("Invalid bot token, please review your configurations.");
        } catch (Throwable e) {
            AutoWhitelist.LOGGER.error("An unexpected error occurred while starting the bot", e);
        }
    }

    @Override
    public void interrupt() {
        stopBot(true);
        instance = null;
        super.interrupt();
    }

    private Consumer<CommandEvent> helpConsumer() {
        return (event) -> {
            EmbedBuilder builder = new EmbedBuilder().setAuthor(jda.getSelfUser().getName(), "https://discord.com", jda.getSelfUser().getAvatarUrl());
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
