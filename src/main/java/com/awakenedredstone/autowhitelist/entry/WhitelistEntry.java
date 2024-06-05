package com.awakenedredstone.autowhitelist.entry;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WhitelistEntry extends BaseEntry {
    public static final Identifier ID = AutoWhitelist.id("whitelist");
    public static final MapCodec<WhitelistEntry> CODEC = RecordCodecBuilder.mapCodec(instance ->
      instance.group(
        Codec.STRING.listOf().fieldOf("roles").forGetter(BaseEntry::getRoles),
        Identifier.CODEC.fieldOf("type").forGetter(BaseEntry::getType)
        ).apply(instance, WhitelistEntry::new)
    );

    protected WhitelistEntry(List<String> roles, Identifier type) {
        super(type, roles);
    }

    @Override
    public <T extends GameProfile> void registerUser(T profile) {
        // Nothing to do here
    }

    @Override
    public <T extends GameProfile> void removeUser(T profile) {
        // Nothing to do here
    }

    @Override
    public <T extends GameProfile> void updateUser(T profile, @Nullable BaseEntry entry) {
        // Nothing to do here
    }

    @Override
    public <T extends GameProfile> boolean shouldUpdate(T profile) {
        return false;
    }

    @Override
    public void assertSafe() {
        // Nothing to do here
    }

    @Override
    public void purgeInvalid() {/**/}
}
