package com.awakenedredstone.autowhitelist.discord;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.concurrent.NamedThreadFactory;
import com.awakenedredstone.autowhitelist.concurrent.SingleTaskExecutor;
import com.awakenedredstone.autowhitelist.concurrent.Stoppable;
import com.awakenedredstone.autowhitelist.concurrent.atomic.LateFinal;
import com.awakenedredstone.autowhitelist.concurrent.atomic.Lazy;
import com.awakenedredstone.autowhitelist.discord.interaction.buttons.RemoveLinkButton;
import com.awakenedredstone.autowhitelist.discord.interaction.commands.LinkCommand;
import com.awakenedredstone.autowhitelist.discord.interaction.commands.LinkInfoCommand;
import com.awakenedredstone.autowhitelist.discord.interaction.commands.admin.viewlink.chat.ViewLinkChatCommand;
import com.awakenedredstone.autowhitelist.discord.interaction.commands.api.InteractionHandler;
import com.awakenedredstone.autowhitelist.discord.store.DynamicRetriever;
import com.awakenedredstone.autowhitelist.discord.util.Reactor;
import com.awakenedredstone.autowhitelist.entry.api.RoleEntryMap;
import com.awakenedredstone.autowhitelist.server.whitelist.WhitelistHandler;
import com.awakenedredstone.autowhitelist.util.LoggingUtil;
import com.awakenedredstone.autowhitelist.util.object.DataFlow;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.EventDispatcher;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.shard.MemberRequestFilter;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.service.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

// TODO: cleanup
public class DiscordClientHolder implements Runnable, Stoppable {
    public static final Logger LOGGER = LoggerFactory.getLogger(DiscordClientHolder.class);
    public static final SingleTaskExecutor<DiscordClientHolder> BOT_SERVICE = new SingleTaskExecutor<>(
      new NamedThreadFactory(
        ignored -> "Discord Bot",
        thread -> thread.setUncaughtExceptionHandler((t, e) -> LOGGER.error("Unhandled exception in bot thread {}", t, e))
      )
    );

    private static boolean migrateCommands = false;

    private final Lazy<ScheduledExecutorService> activityUpdateExecutor = new Lazy<>(Executors::newSingleThreadScheduledExecutor);
    private final LateFinal<GatewayDiscordClient> client = new LateFinal<>();
    private final LateFinal<Guild> guild = new LateFinal<>();
    private final LateFinal<ScopedValue<DynamicRetriever.Mode>> retrieverScope = new LateFinal<>();
    private final StatusTracker statusTracker = new StatusTracker();

    public DiscordClientHolder() {
        statusTracker.on(Task.FETCH_GUILD, () -> { RoleEntryMap.reload(AutoWhitelist.config().whitelist.allow); WhitelistHandler.revalidateEntries(); });
        statusTracker.on(Task.FETCH_GUILD, () -> removeOldCommands(getGuild().getId().asLong()));
    }

    public static void queue() {
        if (BOT_SERVICE.getPendingTaskCount() > 0) {
            LOGGER.warn("Refusing to queue bot, a thread is already queued!", new RuntimeException());
            return;
        }

        BOT_SERVICE.submit(new DiscordClientHolder());
    }

    public static DiscordClientHolder getCurrent() {
        return BOT_SERVICE.getCurrentTask();
    }

    public static Status status() {
        DiscordClientHolder current = getCurrent();
        return current == null ? Status.DISABLED : current.statusTracker.status;
    }

    public static boolean hasTask() {
        return status().ordinal() <= Status.STARTING.ordinal();
    }

    public static boolean isInitialized() {
        return status() == Status.RUNNING;
    }

    public static boolean hasGuild() {
        return isInitialized() || hasTask() && !getCurrent().statusTracker.tasks.contains(Task.FETCH_GUILD);
    }

    public static void migrateCommands() {
        migrateCommands = true;
    }

    public GatewayDiscordClient getClient() {
        return client.get();
    }

    public Guild getGuild() {
        return guild.get();
    }

    public ScopedValue<DynamicRetriever.Mode> getRetrieverScope() {
        return retrieverScope.get();
    }

    // TODO: fully handle shutdown during initialization
    private void execute() {
        String token = AutoWhitelist.config().discord.token;
        if (token == null) return;
        if (token.equals("DO NOT SHARE THE BOT TOKEN")) {
            LOGGER.warn("The bot token has not been set, aborting initialization");
            return;
        }

        DiscordClient baseClient;
        try {
            baseClient = DiscordClient.create(token);
        } catch (IllegalArgumentException e) {
            statusTracker.fail();
            LOGGER.error(e.getMessage());
            return;
        }

        GatewayDiscordClient client = baseClient
          .gateway()
          .setEnabledIntents(IntentSet.of(Intent.GUILDS, Intent.GUILD_MEMBERS))
          .setEntityRetrievalStrategy(gateway -> {
              var retriever = DynamicRetriever.common(gateway);
              this.retrieverScope.setAndLock(retriever.mode);
              return retriever;
          })
          .setMemberRequestFilter(MemberRequestFilter.none())
          .setEventDispatcher(EventDispatcher.replaying())
          .login()
          .onErrorResume(ClientException.isStatusCode(401), e -> {
              LOGGER.error("Failed to authenticate with Discord {}", LoggingUtil.getErrorResponseMessage(e));
              return Mono.empty();
          }).block();

        if (client == null) return;

        this.client.setAndLock(client);
        statusTracker.complete(Task.CLIENT_SET);

        EventHandler.handleEvents(client);

        long guildId = AutoWhitelist.config().discord.guildId;
        statusTracker.track(Task.FETCH_GUILD,
          // TODO: Handle 404
          client.getGuildById(Snowflake.of(guildId)).doOnSuccess(guild -> {
              if (guild == null) throw new NoSuchElementException("No guild with ID %s found".formatted(guildId));
              this.guild.setAndLock(guild);
          }).flatMapMany(guild -> {
              var entries = WhitelistHandler.getCache().getEntries().stream().map(entry -> Snowflake.of(entry.getUser().getDiscordId())).collect(Collectors.toSet());
              return guild.requestMembers(entries);
          })
        );

        InteractionHandler interactionHandler = new InteractionHandler();
        // User commands
        interactionHandler.registerCommand(new LinkCommand());
        interactionHandler.registerCommand(new LinkInfoCommand());
        // Admin commands
        // commandRegistry.register(new StatusCommand());
        // commandRegistry.register(new WhitelistCommand());
        interactionHandler.registerCommand(new ViewLinkChatCommand());
        // commandRegistry.register(new LinkInfoUserCommand());
        // Buttons
        interactionHandler.registerButton(new RemoveLinkButton());

        statusTracker.on(Task.FETCH_GUILD, () -> statusTracker.track(Task.INTERACTION_HANDLER, interactionHandler.postCommands(client, getGuild().getId().asLong())));

        if (statusTracker.isFailed()) {
            return;
        }

        initPresence();

        EventHandler.listen(client, ApplicationCommandInteractionEvent.class, interactionHandler::onCommand);
        EventHandler.listen(client, ChatInputAutoCompleteEvent.class, interactionHandler::onChatInput);
        EventHandler.listen(client, ButtonInteractionEvent.class, interactionHandler::onComponent);

        statusTracker.complete(Task.METHOD_END);
        client.onDisconnect().block();
        LOGGER.debug("Discord bot disconnected");

        if (activityUpdateExecutor.isInitialized()) {
            LOGGER.info("Waiting for presence updater to stop");
            activityUpdateExecutor.get().close();
        }

        LOGGER.info("Discord bot shutdown complete, closing thread");
    }

    private void initPresence() {
        var presence = AutoWhitelist.config().discord.presence;
        if (presence == null) return;
        if (presence.activity == null) {
            updatePresence();
            return;
        }

        long interval = presence.activity.updateInterval();
        if (interval == -1) {
            updatePresence();
            return;
        }

        // TODO: cancel future
        activityUpdateExecutor.get().scheduleAtFixedRate(this::updatePresence, 0, interval, TimeUnit.SECONDS);
    }

    private void updatePresence() {
        var presence = AutoWhitelist.config().discord.presence;
        if (presence == null) return;
        getClient().updatePresence(presence.build()).subscribe();
    }

    private void removeOldCommands(long guildId) {
        if (!migrateCommands) {
            statusTracker.complete(Task.MIGRATE_COMMANDS);
            return;
        }

        GatewayDiscordClient client = this.getClient();

        LOGGER.info("Removing old commands");
        migrateCommands = false;

        long applicationId = client.rest().getApplicationId().blockOptional().orElseThrow();
        ApplicationService applicationService = client.rest().getApplicationService();
        var commands = Set.of("register", "info", "modify", "userinfo");

        statusTracker.track(Task.MIGRATE_COMMANDS,
          applicationService.getGuildApplicationCommands(applicationId, guildId)
            .filter(command -> commands.contains(command.name()))
            .flatMap(command -> applicationService.deleteGuildApplicationCommand(applicationId, guildId, command.id().asLong()))
        );
    }

    @Override
    public void run() {
        try {
            execute();
        } catch (Exception e) {
            this.statusTracker.fail();
            LOGGER.error("An exception occurred when running the bot!", e);
        } catch (Throwable e) {
            this.statusTracker.fail();
            LOGGER.error("A critical error occurred when running the bot!", e);
            // Forward the exception, dropping a Throwable may not lead to good things
            throw e;
        }
    }

    public void shutdown() {
        LOGGER.info("Shutting down bot thread");
        statusTracker.close();
    }

    private void logout() {
        if (!client.isLocked()) return;

        getClient().logout()
          .doOnError(t -> LOGGER.error("Unexpected error while disconnecting the bot, a full restart is recommended", t))
          .doOnSuccess(_ -> LOGGER.debug("Bot logged out"))
          .subscribe();
    }

    @SuppressWarnings("SameParameterValue")
    private class StatusTracker {
        private transient Status status = Status.STARTING;
        private final Set<Task> tasks;
        private final Map<Task, Disposable> pending = new HashMap<>();
        private final Map<Task, SubTasks> listeners = new HashMap<>();

        private StatusTracker() {
            this.tasks = new HashSet<>(Set.of(Task.values()));
        }

        private void complete(Task task) {
            if (isFailed()) throw new IllegalStateException("Tried to complete a task on a failed thread");
            if (isInitialized()) throw new IllegalStateException("Tried to complete a task on a fully initialized thread");
            if (isClosed()) return; // If it was closed we just ignore

            if (!tasks.remove(task)) throw new IllegalArgumentException("Tried to complete an already completed task");
            pending.remove(task);
            DataFlow.nullableC(listeners.remove(task), SubTasks::run);
            LOGGER.debug("Completed task {} ({} remaining)", task, tasks.size());
            if (!tasks.isEmpty()) return;

            LOGGER.info("AutoWhitelist bot fully initialized");
            status = Status.RUNNING;
        }

        private void track(Task task, Mono<?> mono) {
            if (!isAlive()) return;
            pending.put(task, mono.doOnError(_ -> fail()).thenEmpty(Reactor.elastic(() -> complete(task))).subscribe());
        }

        private void track(Task task, Flux<?> mono) {
            if (!isAlive()) return;
            pending.put(task, mono.doOnError(_ -> fail()).thenEmpty(Reactor.elastic(() -> complete(task))).subscribe());
        }

        private void on(Task task, Runnable handler) {
            if (!tasks.contains(task)) {
                // Assume it as already completed
                // Run the handler immediately to avoid locking due to a race condition
                handler.run();
                return;
            }

            listeners.computeIfAbsent(task, _ -> new SubTasks()).add(handler);
        }

        private boolean isAlive() {
            return status.isOrHigher(Status.STARTING);
        }

        private boolean isInitialized() {
            return isStatus(Status.RUNNING);
        }

        private boolean isFailed() {
            return isStatus(Status.CRASHED);
        }

        private boolean isClosed() {
            return isStatus(Status.OFFLINE);
        }

        private boolean isStatus(Status status) {
            return this.status == status;
        }

        private void fail() {
            if (isFailed()) throw new IllegalStateException("Bot thread is already crashed");
            if (isClosed()) throw new IllegalStateException("Bot thread is closed");

            LOGGER.warn("Discord bot crashed, closing connection");
            status = Status.CRASHED;
            clear();
        }

        private void close() {
            if (isFailed()) throw new IllegalStateException("Bot thread is crashed");
            if (isClosed()) throw new IllegalStateException("Bot thread is already closed");

            status = Status.OFFLINE;
            clear();
        }

        private void clear() {
            pending.forEach((_, disposable) -> disposable.dispose());
            pending.clear();
            listeners.clear();
            logout();
        }

        private static class SubTasks {
            private final List<Runnable> tasks = new ArrayList<>();

            public void add(Runnable task) {
                tasks.add(task);
            }

            public void run() {
                tasks.forEach(Runnable::run);
            }
        }
    }

    public enum Task {
        CLIENT_SET,
        FETCH_GUILD,
        MIGRATE_COMMANDS,
        INTERACTION_HANDLER,
        METHOD_END,
    }

    public enum Status {
        RUNNING,
        STARTING,
        CRASHED,
        OFFLINE,
        DISABLED;

        private boolean isOrHigher(Status status) {
            return ordinal() <= status.ordinal();
        }

        private boolean isOrLower(Status status) {
            return this.ordinal() >= status.ordinal();
        }
    }
}
