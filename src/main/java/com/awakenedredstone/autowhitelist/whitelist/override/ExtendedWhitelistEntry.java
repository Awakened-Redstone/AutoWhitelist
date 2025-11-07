package com.awakenedredstone.autowhitelist.whitelist.override;

import com.google.gson.JsonObject;
import net.minecraft.server.WhitelistEntry;

public class ExtendedWhitelistEntry extends WhitelistEntry {

    public ExtendedWhitelistEntry(ExtendedPlayerProfile profile) {
        super(profile);
    }

    public ExtendedWhitelistEntry(JsonObject json) {
        super(ExtendedPlayerProfile.read(json));
    }

    public ExtendedPlayerProfile getProfile() {
        return (ExtendedPlayerProfile) getKey();
    }

    /*? if <1.21.9 {*//*
    @Override
    protected void write(JsonObject json) {
        ExtendedPlayerProfile profile = (ExtendedPlayerProfile) getKey();
        if (profile != null) {
            profile.write(json);
        }
    }
    *//*?}*/
}
