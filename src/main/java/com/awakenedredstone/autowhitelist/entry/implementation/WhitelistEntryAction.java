package com.awakenedredstone.autowhitelist.entry.implementation;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.entry.BaseEntryAction;
import com.awakenedredstone.autowhitelist.util.Stonecutter;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedPlayerProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.Identifier;

import java.util.List;

public class WhitelistEntryAction extends BaseEntryAction {
    public static final Identifier ID = AutoWhitelist.id("whitelist");
    public static final /*? if <1.20.5 {*//*Codec*//*?} else {*/MapCodec/*?}*/<WhitelistEntryAction> CODEC = Stonecutter.entryCodec(instance ->
      instance.group(
        Codec.STRING.listOf().fieldOf("roles").forGetter(BaseEntryAction::getRoles),
        Identifier.CODEC.fieldOf("type").forGetter(BaseEntryAction::getType)
        ).apply(instance, WhitelistEntryAction::new)
    );

    protected WhitelistEntryAction(List<String> roles, Identifier type) {
        super(type, roles);
    }

    @Override
    public void registerUser(ExtendedPlayerProfile profile) {
        // Nothing to do here
    }

    @Override
    public void removeUser(ExtendedPlayerProfile profile) {
        // Nothing to do here
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public String toString() {
        return "WhitelistEntry{}";
    }

    @Override
    public boolean equals(BaseEntryAction otherEntry) {
        return true;
    }
}
