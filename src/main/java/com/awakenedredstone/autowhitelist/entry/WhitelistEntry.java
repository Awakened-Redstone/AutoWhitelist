package com.awakenedredstone.autowhitelist.entry;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.util.Stonecutter;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WhitelistEntry extends BaseEntry {
    public static final Identifier ID = AutoWhitelist.id("whitelist");
    public static final /*? if <1.20.5 {*//*Codec*//*?} else {*/MapCodec/*?}*/<WhitelistEntry> CODEC = Stonecutter.entryCodec(instance ->
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
