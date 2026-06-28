package com.awakenedredstone.autowhitelist.config;

import com.awakenedredstone.moondust.jankson.annotation.Comment;
import com.awakenedredstone.moondust.config.api.CodecSpecs;
import com.awakenedredstone.moondust.jankson.annotation.NameFormat;
import com.awakenedredstone.autowhitelist.entry.Entry;
import com.awakenedredstone.autowhitelist.util.string.TimeParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
//? if >=1.21.11 {
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;
//?}
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@NameFormat(NameFormat.Case.SNAKE_CASE)
public class WhitelistConfig extends CodecSpecs.Simple {
    public WhitelistConfig() {
        register("lock_time", Codec.STRING.flatXmap(WhitelistConfig::timeConstraint, WhitelistConfig::timeConstraint));
        register("allow", Entry.CODEC.listOf());
        //? if >=1.21.11 {
        register(
          "command_permission_level",
          Codec.withAlternative(PermissionLevel.CODEC, PermissionLevel.INT_CODEC).xmap(LevelBasedPermissionSet::forLevel, LevelBasedPermissionSet::level)
        );
        //?} else {
        // register("command_permission_level", Codec.intRange(0, 4));
        //?}
    }

    @Comment("""
      The time the bot will lock a whitelist entry after it is added or updated, use -1 to lock all entries forever
      Changes to this value will only apply to new entries, except for permanent lock which is immediate and global
      Check the documentation for more details on how the format works""")
    public String lockTime = "1d";

    @Comment("""
      The permission level used for command entries. This limits what commands the mod can run, you likely don't need to change this.
      Check https://minecraft.wiki/w/Permission_level for more about permission levels.""")
    //? if <1.21.11 {
    /*public int commandPermissionLevel = 3;
    *///?} else {
    public LevelBasedPermissionSet commandPermissionLevel = LevelBasedPermissionSet.ADMIN;
    //?}

    @Comment("The whitelist allow settings, please refer to the documentation to set them up")
    public List<Entry> allow = new ArrayList<>();

    @SuppressWarnings("unused")
    public static DataResult<String> timeConstraint(@NotNull String timeString) {
        if (timeString.equals("-1")) return DataResult.success(timeString);
        int time = TimeParser.parseTime(timeString);
        if (time >= 0) return DataResult.success(timeString);

        return DataResult.error(() -> "Invalid time format");
    }

    public long lockTime() {
        if (lockTime.trim().equals("-1")) return -1;
        int time = TimeParser.parseTime(lockTime);
        return System.currentTimeMillis() + (time * 1000L);
    }
}
