package com.awakenedredstone.autowhitelist.config;

import com.awakenedredstone.autowhitelist.util.string.Texts;
import com.awakenedredstone.autowhitelist.util.string.TimeParser;
import com.awakenedredstone.moondust.jankson.annotation.Comment;
import com.awakenedredstone.moondust.jankson.annotation.Nullable;
import com.awakenedredstone.moondust.config.api.CodecSpecs;
import com.awakenedredstone.moondust.jankson.annotation.NameFormat;
import com.awakenedredstone.autowhitelist.util.object.DataFlow;
import com.awakenedredstone.moondust.jankson.annotation.Secret;
import com.mojang.serialization.Codec;
import discord4j.core.object.presence.*;

@NameFormat(NameFormat.Case.SNAKE_CASE)
public class DiscordConfig extends CodecSpecs.Simple {
    public DiscordConfig() {
        final var checker = Codec.checkRange(0L, Long.MAX_VALUE);
        register("guild_id", Codec.LONG.flatXmap(checker, checker));
    }

    @Nullable
    @Secret
    @Comment("""
      Your application's bot token. Set to null to disable the bot.
      NEVER SHARE A BOT TOKEN! Anyone with it has full control of the application's bot!""")
    public String token = "DO NOT SHARE THE BOT TOKEN";

    @Comment("""
      The ID of the guild (aka server) that the application should check the roles of.
      The application must have been added to the guild.""")
    public long guildId = 0;

    @Comment("When enabled, all interactions and slash commands will be ephemeral, meaning only the user can see the response.")
    public boolean ephemeralReplies = true;

    @Nullable
    @Comment("The bot presence. Set it to null if it is handled by another application.")
    public PresenceConfig presence = new PresenceConfig();

    @NameFormat(NameFormat.Case.SNAKE_CASE)
    public static class PresenceConfig {
        @Comment("The bot status")
        public Status status = Status.ONLINE;

        @Nullable
        @Comment("The bot activity. Set to null to show nothing.")
        public PresenceActivity activity = new PresenceActivity();

        public ClientPresence build() {
            return ClientPresence.of(status, DataFlow.nullableF(activity, PresenceActivity::build));
        }
    }

    @NameFormat(NameFormat.Case.SNAKE_CASE)
    public static class PresenceActivity {
        @Comment("The activity type shown on the bot status")
        public Activity.Type type = Activity.Type.PLAYING;

        @Nullable
        @Comment("The time interval of which the activity is updated. Setting it to null only updates on bot start/restart")
        public String updateInterval = null;

        @Comment("The text shown on the bot activity status")
        public String text = "on the Member Server";

        @Nullable
        @Comment("The url used on activity types that accepts it")
        public String url = null;

        public ClientActivity build() {
            String activityText = Texts.placeholder(text).getString();
            return ClientActivity.of(type, activityText, url);
        }

        public long updateInterval() {
            if (updateInterval == null || updateInterval.trim().equals("-1")) return -1;
            return TimeParser.parseTime(updateInterval);
        }
    }
}
