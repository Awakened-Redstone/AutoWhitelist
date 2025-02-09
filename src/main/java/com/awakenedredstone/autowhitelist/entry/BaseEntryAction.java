package com.awakenedredstone.autowhitelist.entry;

import blue.endless.jankson.JsonObject;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedGameProfile;
import com.mojang.serialization.Codec;
/*? if >=1.20.5 {*/import com.mojang.serialization.MapCodec;/*?}*/
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * The entry action used when whitelisting a user
 */
public abstract class BaseEntryAction {
    private static final Map<Identifier, /*? if <1.20.5 {*//*Codec*//*?} else {*/MapCodec/*?}*/<? extends BaseEntryAction>> ENTRIES = new HashMap<>();
    public static final Codec<BaseEntryAction> CODEC = Identifier.CODEC.dispatch(BaseEntryAction::getType, ENTRIES::get);
    private static final Map<Identifier, BiFunction<Byte, JsonObject, JsonObject>> DATA_FIXERS = new HashMap<>();
    protected final Logger LOGGER;
    private final Set<String> roles = new HashSet<>(1);
    private final Identifier type;

    protected BaseEntryAction(Identifier type, List<String> roles) {
        this.type = type;
        this.roles.addAll(roles);
        LOGGER = LoggerFactory.getLogger(this.getClass());
    }

    /**
     * Register the new entry type to the registry to allow usage of it in the mod config<br/>
     * The codec must follow the default style, roles and type must be at root, and any extra parameter
     * for the execution of the task must be inside execute and named in such a way where it's action in
     * the task can be easily assumed by its name
     */
    public static void register(Identifier id, /*? if <1.20.5 {*//*Codec*//*?} else {*/MapCodec/*?}*/<? extends BaseEntryAction> codec) {
        ENTRIES.putIfAbsent(id, codec);
    }

    /**
     * The data fixer system is still experimental and may heavily change in the future
     **/
    @ApiStatus.Experimental
    public static void addDataFixer(Identifier id, BiFunction<Byte, JsonObject, JsonObject> fixer) {
        DATA_FIXERS.put(id, fixer);
    }

    /**
     * @return the identifier of this action
     */
    public Identifier getType() {
        return type;
    }

    @ApiStatus.Experimental
    public static Map<Identifier, BiFunction<Byte, JsonObject, JsonObject>> getDataFixers() {
        return Map.copyOf(DATA_FIXERS);
    }

    /**
     * @return the roles linked to the entry using this action
     */
    public List<String> getRoles() {
        return List.copyOf(roles);
    }

    /**
     * Used to verify that the entry action can be run without further issues.
     * Implement it to validate the options.
     * In case something is wrong log the fault as an error message and return false
     * @return whenever the action is valid and can be executed as expected
     */
    public abstract boolean isValid();

    /**
     * Executes the actions for when a user is added to the entry this action is from<br/>
     * Examples:
     * <ul>
     * <li>When the user is added to the whitelist</li>
     * <li>When the user role changes to one that uses a different entry</li>
     * </ul>
     * @param profile The game profile of the user being added to the entry this action is from
     */
    public abstract void registerUser(ExtendedGameProfile profile);

    /**
     * Executes the actions for when a user is removed from the entry this action is from<br/>
     * Examples:
     * <ul>
     * <li>When the user is removed to the whitelist</li>
     * <li>When the user role changes to one that uses a different entry</li>
     * </ul>
     * @param profile The game profile of the user being removed from the entry this action is from
     */
    public abstract void removeUser(ExtendedGameProfile profile);

    /**
     * When overriding this, remember to execute the removal of the old entry action
     * @param profile The game profile of the user that is being updated
     * @param oldEntry The old entry action the user was on, or null if none was found
     */
    public void updateUser(ExtendedGameProfile profile, @Nullable BaseEntryAction oldEntry) {
        if (oldEntry != null) {
            oldEntry.removeUser(profile);
        }
        registerUser(profile);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof BaseEntryAction baseEntryAction)) return false;
        if (o.getClass() != this.getClass()) return false;

        if (Objects.equals(getRoles(), baseEntryAction.getRoles()) && Objects.equals(getType(), baseEntryAction.getType())) {
            return equals(baseEntryAction);
        }

        return false;
    }

    /**
     * Used to compare if the entries are the same
     * Used to compare the extra parameters
     * @param otherEntry Always the same class as the one extending this, the other entry being compared to
     * @return if both entries match
     */
    @ApiStatus.OverrideOnly
    public abstract boolean equals(BaseEntryAction otherEntry);

    /**
     * Required to allow clear logging of the entry actions
     * Must follow the standard of ClassName{field1='value1',field2=value2}
     * @return The entry action as a string, following the standard of ClassName{field=value}
     */
    @Override
    public abstract String toString();
}


