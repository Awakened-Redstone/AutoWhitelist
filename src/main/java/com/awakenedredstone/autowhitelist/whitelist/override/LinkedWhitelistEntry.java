package com.awakenedredstone.autowhitelist.whitelist.override;

import com.google.gson.JsonObject;
import net.minecraft.server.WhitelistEntry;

public class LinkedWhitelistEntry extends WhitelistEntry {

    public LinkedWhitelistEntry(LinkedPlayerProfile profile) {
        super(profile);
    }

    public LinkedWhitelistEntry(JsonObject json) {
        super(LinkedPlayerProfile.read(json));
    }

    @Override
    public LinkedPlayerProfile getKey() {
        return (LinkedPlayerProfile) super.getKey();
    }

    public boolean isLocked() {
        return getKey().isLocked();
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
