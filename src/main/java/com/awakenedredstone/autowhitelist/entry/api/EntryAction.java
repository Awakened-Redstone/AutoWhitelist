package com.awakenedredstone.autowhitelist.entry.api;

import com.awakenedredstone.moondust.jankson.element.JsonObject;
import com.awakenedredstone.autowhitelist.WeakRegistries;
import com.awakenedredstone.autowhitelist.server.profile.PlayerProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * The entry action used when whitelisting a user
 */
public abstract class EntryAction<T extends ActionFields> {
    // TODO: maybe use a custom registry codec with a simpler error message
    public static final Codec<EntryAction<?>> CODEC = WeakRegistries.ACTION_REGISTRY.byNameCodec().dispatch(EntryAction::getType, ActionType::actionCodec);
    private static final Map<Identifier, BiFunction<Byte, JsonObject, JsonObject>> DATA_FIXERS = new HashMap<>();
    protected final Logger logger;
    private final ActionType<T> type;
    protected final T fields;

    protected EntryAction(ActionType<T> type, T fields) {
        this.type = type;
        this.logger = LoggerFactory.getLogger(this.getClass());
        this.fields = fields;
    }

    /**
     * Register the new entry type to the registry to allow usage of it in the mod config<br/>
     * The codec must follow the default style, roles and type must be at root, and any extra parameter
     * for the execution of the task must be inside execute and named in such a way where it's action in
     * the task can be easily assumed by its name
     *
     * @return the entry identifier
     */
    public static <T extends ActionFields> ActionType<T> register(Identifier id, Codec<T> codec, Builder<T> builder) {
        return Registry.register(WeakRegistries.ACTION_REGISTRY, id, new ActionType<>(id, codec, builder));
    }

    /**
     * Register the new entry type to the registry to allow usage of it in the mod config<br/>
     * The codec must follow the default style, roles and type must be at root, and any extra parameter
     * for the execution of the task must be inside execute and named in such a way where it's action in
     * the task can be easily assumed by its name
     *
     * @return the entry identifier
     */
    public static <T extends ActionFields> ActionType<T> register(Identifier id, MapCodec<T> codec, Builder<T> builder) {
        return Registry.register(WeakRegistries.ACTION_REGISTRY, id, new ActionType<T>(id, codec, builder));
    }

    /**
     * The data fixer system is still experimental and may heavily change in the future
     **/
    @ApiStatus.Experimental
    public static void addDataFixer(Identifier id, BiFunction<Byte, JsonObject, JsonObject> fixer) {
        DATA_FIXERS.put(id, fixer);
    }

    @ApiStatus.Experimental
    public static Map<Identifier, BiFunction<Byte, JsonObject, JsonObject>> getDataFixers() {
        return Map.copyOf(DATA_FIXERS);
    }

    public ActionType<T> getType() {
        return type;
    }

    /**
     * @return the fields class holding the action data
     */
    public T getFields() {
        return fields;
    }

    /**
     * Used to verify that the entry action can be run without further issues.
     * Implement it to validate the options.
     * In case something is wrong log the fault as an error message and return false
     *
     * @return whenever the action is valid and can be executed as expected
     */
    public boolean validate() {
        return validate(false);
    }

    /**
     * Used to verify that the entry action can be run without further issues.
     * Implement it to validate the options.
     * In case something is wrong log the fault as an error message and return false
     *
     * @param early set if the config is not loaded yet
     *
     * @return whenever the action is valid and can be executed as expected
     */
    public abstract boolean validate(boolean early);

    /**
     * Check if the action is valid and throw an assertion error if not.
     *
     * @throws AssertionError if the action is not valid
     */
    public void assertValid(boolean early) throws AssertionError {
        if (!validate(early)) throw new AssertionError("Failed to assert action %s".formatted(this));
    }

    /**
     * Executes the actions for when a user is added to the entry this action is from<br/>
     * Examples:
     * <ul>
     * <li>When the user is added to the whitelist</li>
     * <li>When the user role changes to one that uses a different entry</li>
     * </ul>
     *
     * @param profile The game profile of the user being added to the entry this action is from
     */
    public abstract void onAdd(PlayerProfile profile);

    /**
     * Executes the actions for when a user is removed from the entry this action is from<br/>
     * Examples:
     * <ul>
     * <li>When the user is removed to the whitelist</li>
     * <li>When the user role changes to one that uses a different entry</li>
     * </ul>
     *
     * @param profile The game profile of the user being removed from the entry this action is from
     */
    public abstract void onRemove(PlayerProfile profile);

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof EntryAction<?> other)) return false;

        return this.getType().equals(other.getType()) && this.getFields().equals(other.getFields());
    }

    /**
     * Required to allow clear logging of the entry actions
     *
     * @return The entry action as a string, following the standard of ClassName{field=value}
     */
    @Override
    public String toString() {
        return  this.getClass().getSimpleName() + '[' + "type=" + type.id() + ", fields=" + fields.toString() + ']';
    }

    @FunctionalInterface
    public interface Builder<T extends ActionFields> {
        EntryAction<T> build(ActionType<T> type, T fields);
    }
}
