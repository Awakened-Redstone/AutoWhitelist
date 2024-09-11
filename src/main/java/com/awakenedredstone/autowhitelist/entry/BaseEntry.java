package com.awakenedredstone.autowhitelist.entry;

import blue.endless.jankson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
/*? if >=1.20.5 {*/import com.mojang.serialization.MapCodec;/*?}*/
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

public abstract class BaseEntry {
    private static final Map<Identifier, /*? if <1.20.5 {*//*Codec*//*?} else {*/MapCodec/*?}*/<? extends BaseEntry>> ENTRIES = new HashMap<>();
    public static final Codec<BaseEntry> CODEC = Identifier.CODEC.dispatch(BaseEntry::getType, ENTRIES::get);
    private static final Map<Identifier, BiFunction<Byte, JsonObject, JsonObject>> DATA_FIXERS = new HashMap<>();
    private final Set<String> roles = new HashSet<>();
    private final Identifier type;

    protected BaseEntry(Identifier type, List<String> roles) {
        this.type = type;
        this.roles.addAll(roles);
    }

    public static void register(Identifier id, /*? if <1.20.5 {*//*Codec*//*?} else {*/MapCodec/*?}*/<? extends BaseEntry> data) {
        ENTRIES.putIfAbsent(id, data);
    }

    /**
     * The data fixer system is still in development and may heavily change in the future
     **/
    @ApiStatus.Experimental
    public static void addDataFixer(Identifier id, BiFunction<Byte, JsonObject, JsonObject> fixer) {
        DATA_FIXERS.put(id, fixer);
    }

    public Identifier getType() {
        return type;
    }

    public static Map<Identifier, BiFunction<Byte, JsonObject, JsonObject>> getDataFixers() {
        return Map.copyOf(DATA_FIXERS);
    }

    public abstract void assertSafe();

    public abstract <T extends GameProfile> void registerUser(T profile);

    public abstract <T extends GameProfile> void removeUser(T profile);

    /**
     * <b>When overriding this, remember to execute the removal of the old entry to avoid bugs<b/>
     */
    public <T extends GameProfile> void updateUser(T profile, @Nullable BaseEntry oldEntry) {
        if (oldEntry != null) {
            oldEntry.removeUser(profile);
        }
        registerUser(profile);
    }

    public abstract <T extends GameProfile> boolean shouldUpdate(T profile);

    public abstract void purgeInvalid();

    public List<String> getRoles() {
        return List.copyOf(roles);
    }
}


