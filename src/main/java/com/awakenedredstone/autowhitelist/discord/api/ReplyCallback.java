package com.awakenedredstone.autowhitelist.discord.api;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public interface ReplyCallback {
    void sendMessage(MessageCreateData message);
    void editMessage(MessageEditData messageData);

    class DefaultInteractionReplyCallback extends InteractionReplyCallback {
        private final SlashCommandEvent event;

        public DefaultInteractionReplyCallback(SlashCommandEvent event) {
            this.event = event;
        }

        public void sendMessage(MessageCreateData messageData) {
            if (messageData == null) {
                acknowledgment = acknowledge();
            } else {
                acknowledgment = event.reply(messageData).setEphemeral(AutoWhitelist.CONFIG.ephemeralReplies).submit();
            }

            // Execute the pending task after the acknowledgment
            acknowledgment.thenAccept(message -> {
                originalMessage = message;
                // Only execute if there is a pending task
                if (pendingTask != null) {
                    pendingTask.accept(message);
                }
            });
        }

        @Override
        public CompletableFuture<InteractionHook> acknowledge() {
            return event.deferReply(AutoWhitelist.CONFIG.ephemeralReplies).submit();
        }
    }

    abstract class InteractionReplyCallback implements ReplyCallback {
        protected CompletableFuture<InteractionHook> acknowledgment;
        public CompletableFuture<Message> executingTask;
        protected Consumer<InteractionHook> pendingTask;
        protected InteractionHook originalMessage;

        public void sendMessage(MessageCreateData messageData) {
            acknowledgment = acknowledge();

            // Execute the pending task after the acknowledgment
            acknowledgment.thenAccept(message -> {
                originalMessage = message;
                // Only execute if there is a pending task
                if (pendingTask != null) {
                    pendingTask.accept(message);
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
            if (!acknowledgment.isDone()) {
                // Set the task to execute once the interaction is acknowledged
                pendingTask = hook -> executingTask = messageFunction.apply(originalMessage).submit();
            } else {
                // Cancel any executing task to avoid spamming Discord's API
                if (executingTask != null) {
                    executingTask.cancel(true);
                }

                // The interaction was already acknowledged, so execute the task now
                executingTask = messageFunction.apply(originalMessage).submit();
            }
        }
    }
}
