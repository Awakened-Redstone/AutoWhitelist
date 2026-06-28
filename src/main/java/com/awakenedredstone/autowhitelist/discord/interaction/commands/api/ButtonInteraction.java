package com.awakenedredstone.autowhitelist.discord.interaction.commands.api;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import org.reactivestreams.Publisher;

public abstract class ButtonInteraction implements DeferrableInteraction<ButtonInteractionEvent> {
    public final Identifier id;

    protected ButtonInteraction(Identifier id) {
        this.id = id;
    }

    public abstract Publisher<?> execute(ButtonInteractionEvent event);

    public static String nameKey(Identifier id) {
        return nameKey(id, "");
    }

    public static String nameKey(Identifier id, String suffix) {
        return "discord.%s.button.%s".formatted(id.getNamespace(), id.getPath() + suffix);
    }

    public static MutableComponent nameComponent(Identifier id, Object... args) {
        return nameComponent(id, "", args);
    }

    public static MutableComponent nameComponent(Identifier id, String suffix, Object... args) {
        return Component.translatable(nameKey(id, suffix), args);
    }

    public static String translatedName(Identifier id, Object... args) {
        return nameComponent(id, args).getString();
    }

    public static String translatedName(Identifier id, String suffix, Object... args) {
        return nameComponent(id, suffix, args).getString();
    }
}
