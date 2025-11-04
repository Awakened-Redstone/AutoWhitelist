package com.awakenedredstone.autowhitelist.discord.old.api;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

public interface ReplyCallback {
    void sendMessage(MessageCreateData message);
    void editMessage(MessageEditData messageData);

    abstract class InteractionReplyCallback implements ReplyCallback {
        protected final AtomicReference<CompletableFuture<InteractionHook>> acknowledgment = new AtomicReference<>();
        public final AtomicReference<CompletableFuture<Message>> executingTask = new AtomicReference<>();
        protected final AtomicReference<Consumer<InteractionHook>> pendingTask = new AtomicReference<>();
        protected final AtomicReference<InteractionHook> originalMessage = new AtomicReference<>();

        public void sendMessage(MessageCreateData messageData) {
            acknowledgment.set(acknowledge());

            // Execute the pending task after the acknowledgment
            acknowledgment.get().thenAccept(message -> {
                originalMessage.set(message);
                // Only execute if there is a pending task
                Consumer<InteractionHook> hookConsumer = pendingTask.get();
                if (hookConsumer != null) {
                    hookConsumer.accept(message);
                }
            });
        }

        protected abstract CompletableFuture<InteractionHook> acknowledge();

        @Override
        public void editMessage(MessageEditData messageData) {
            submitEdit(interactionHook -> interactionHook.editOriginal(messageData));
        }

        public <R extends RestAction<Message>> void submitEdit(Function<InteractionHook, R> messageFunction) {
            // Wait for the acknowledgement to complete before editing
            if (!acknowledgment.get().isDone()) {
                // Set the task to execute once the interaction is acknowledged
                pendingTask.set(hook -> executingTask.set(CompletableFuture.completedFuture(messageFunction.apply(originalMessage.get()).complete())));
            } else {
                // Cancel any executing task to avoid spamming Discord's API
                if (executingTask.get() != null) {
                    executingTask.get().cancel(true);
                }

                // The interaction was already acknowledged, so execute the task now
                executingTask.set(CompletableFuture.completedFuture(messageFunction.apply(originalMessage.get()).complete()));
            }
        }
    }

    class DefaultInteractionReplyCallback extends InteractionReplyCallback {
        private final SlashCommandEvent event;

        public DefaultInteractionReplyCallback(SlashCommandEvent event) {
            this.event = event;
        }

        public void sendMessage(MessageCreateData messageData) {
            if (messageData == null) {
                acknowledgment.set(acknowledge());
            } else {
                acknowledgment.set(CompletableFuture.completedFuture(event.reply(messageData).setEphemeral(AutoWhitelist.CONFIG.ephemeralReplies).complete()));
            }

            // Execute the pending task after the acknowledgment
            acknowledgment.get().thenAccept(message -> {
                originalMessage.set(message);
                // Only execute if there is a pending task
                Consumer<InteractionHook> hookConsumer = pendingTask.get();
                if (hookConsumer != null) {
                    hookConsumer.accept(message);
                }
            });
        }

        @Override
        public CompletableFuture<InteractionHook> acknowledge() {
            return CompletableFuture.completedFuture(event.deferReply(AutoWhitelist.CONFIG.ephemeralReplies).complete());
        }
    }
}
