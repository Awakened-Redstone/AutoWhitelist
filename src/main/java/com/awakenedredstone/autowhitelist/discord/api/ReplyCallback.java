package com.awakenedredstone.autowhitelist.discord.api;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public interface ReplyCallback {

    void sendMessage(MessageCreateData message);
    void editMessage(MessageEditData messageData);

    abstract class InteractionReplyCallback implements ReplyCallback {
        protected boolean isFirstRun = true;
        protected CompletableFuture<?> lastTask;
        protected Consumer<?> editConsumer;
        protected Object lastMessage;

        public void sendMessage(MessageCreateData messageData) {
            acknowledge();
        }

        public abstract void acknowledge();

        @Override
        public void editMessage(MessageEditData messageData) {
            editMessage((v) -> {
                if (v instanceof Message message) {
                    return message.editMessage(messageData);
                } else if (v instanceof InteractionHook hook) {
                    return hook.editOriginal(messageData);
                } else {
                    throw new IllegalStateException("Unknown message type: " + v.getClass().getName());
                }
            });
        }

        public <T, R extends RestAction<?>> void editMessage(Function<T, R> messageFunction) {
            if (isFirstRun) {
                editConsumer = (T v) -> {
                    lastTask = messageFunction.apply(v).submit();
                    editConsumer = null;
                };

                ((CompletableFuture<T>) lastTask).thenAccept((T o) -> {

                    isFirstRun = false;
                    lastMessage = o;
                    lastTask = null;
                    if (editConsumer != null) {
                        ((Consumer<T>) editConsumer).accept(o);
                    }
                });
                return;
            }

            if (lastTask != null) {
                lastTask.cancel(true);
            }
            lastTask = messageFunction.apply((T) lastMessage).submit();
        }
    }
}
