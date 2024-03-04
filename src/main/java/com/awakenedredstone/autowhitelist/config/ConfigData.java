package com.awakenedredstone.autowhitelist.config;

import blue.endless.jankson.Comment;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import com.awakenedredstone.autowhitelist.config.annotation.PredicateConstraint;
import com.awakenedredstone.autowhitelist.config.annotation.RangeConstraint;
import com.awakenedredstone.autowhitelist.config.annotation.RegexConstraint;
import com.awakenedredstone.autowhitelist.util.JanksonBuilder;
import com.awakenedredstone.autowhitelist.util.TimeParser;
import net.dv8tion.jda.api.entities.Activity;
/*? if >=1.19 {*/
import net.minecraft.text.Text;
/*?} else {*//*
import net.minecraft.text.TranslatableText;
*//*?} */

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ConfigData extends Config {
    public ConfigData() {
        super("autowhitelist", JanksonBuilder.buildJankson(builder -> {
            //noinspection DataFlowIssue
            builder.registerDeserializer(JsonObject.class, EntryData.class, (jsonObject, m) -> EntryData.deserialize(((JsonPrimitive) jsonObject.get("type")).asString(), jsonObject));
            builder.registerSerializer(EntryData.class, (entryData, marshaller) -> {
                JsonObject json = entryData.serialize();
                json.put("type", marshaller.serialize(entryData.getType()));
                json.put("roleIds", marshaller.serialize(entryData.getRoleIds()));
                return json;
            });
        }));
    }

    @Comment("DO NOT CHANGE, CHANGING THIS WILL BREAK THE CONFIGURATION FILE")
    public byte CONFIG_VERSION = 1;
    @Comment("When enabled, it will keep a cache of previous registered users and will use it to automatically add the user back (if they have the proper role)")
    public boolean enableWhitelistCache = true;
    @RangeConstraint(min = 10, max = 300)
    @Comment("The period the mod looks for outdated and invalid entries, this is an extra action to guarantee everything is updated")
    public short updatePeriod = 60;
    @PredicateConstraint("idConstraint")
    @Comment("A list of ids to allow users to use the debug commands")
    public List<String> admins = new ArrayList<>();
    @Comment("The activity type shown on the bot status")
    public BotActivity botActivityType = BotActivity.PLAYING;
    @Comment("The bot command prefix")
    public String prefix = "np!";
    @PredicateConstraint("timeConstraint")
    @Comment("The time in seconds the bot will lock a whitelist entry after it is added or updated, use -1 to disable changing the linked username")
    public String lockTime = "1d";
    @Comment("Your bot token. Never share it, anyone with it has full control of the bot")
    public String token = "DO NOT SHARE THE BOT TOKEN";
    @RegexConstraint("\\d+")
    public String discordServerId = "";
    @Comment("When enabled, all interactions and slash commands will be ephemeral, meaning only the user can see the response.")
    public boolean ephemeralReplies = true;
    //@Hook
    @Comment("The whitelist entry settings, please refer to the documentation to set them up")
    public List<EntryData> entries = new ArrayList<>();

    @SuppressWarnings("unused")
    public static boolean idConstraint(List<String> roles) {
        return roles.stream().allMatch(v -> Pattern.compile("\\d+").matcher(v).matches());
    }

    @SuppressWarnings("unused")
    public static boolean timeConstraint(String timeString) {
        if (timeString.equals("-1")) return true;
        int time = TimeParser.parseTime(timeString);
        return time >= 0;
    }

    public long lockTime() {
        if (lockTime.equals("-1")) return -1;
        int time = TimeParser.parseTime(lockTime);
        return System.currentTimeMillis() + (time * 1000L);
    }

    public String toString() {
        return super.toString()
          .replace(token, "[REDACTED]")
          .replace(discordServerId, "[REDACTED]");
    }

    public enum BotActivity {
        NONE(null),
        RESET(null),
        PLAYING(Activity.ActivityType.PLAYING),
        STREAMING(Activity.ActivityType.STREAMING),
        LISTENING(Activity.ActivityType.LISTENING),
        WATCHING(Activity.ActivityType.WATCHING);

        private final Activity.ActivityType activityType;

        BotActivity(Activity.ActivityType activityType) {
            this.activityType = activityType;
        }

        public Activity.ActivityType getActivityType() {
            return activityType;
        }

        public Activity getActivity() {
            return activityType == null ? null : Activity.of(getActivityType(), /*? if >=1.19 {*/Text.translatable/*?} else {*//*new TranslatableText*//*?}*/("discord.bot.activity.message").getString());
        }
    }
}
