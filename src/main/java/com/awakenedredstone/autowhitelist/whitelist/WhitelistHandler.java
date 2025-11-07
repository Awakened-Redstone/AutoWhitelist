package com.awakenedredstone.autowhitelist.whitelist;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.whitelist.override.ExtendedWhitelist;
import discord4j.core.object.entity.Member;

public class WhitelistHandler {
    public static boolean whitelistUser(Member user) {

    }

    public boolean qualifies(Member user) {

    }

    public static ExtendedWhitelist getWhitelist() {
        return (ExtendedWhitelist) AutoWhitelist.getServer().getPlayerManager().getWhitelist();
    }
}
