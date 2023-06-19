package com.awakenedredstone.autowhitelist.config;

import blue.endless.jankson.Comment;
import io.wispforest.owo.config.annotation.*;
import net.dv8tion.jda.api.entities.Activity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
@Config(name = "autowhitelist", wrapperName = "Configs")
public class ConfigData {

    @Comment("No touchy!")
    public boolean devVersion;
    @Comment("When enabled it will keep a cache of previous registered users and will use it to automatically add the user back (if they have the proper role)")
    public boolean enableWhitelistCache = true;
    @RangeConstraint(min = 10, max = 300)
    @Comment("The period the mod looks for outdated and invalid entries, this is an extra action to guarantee everything is updated")
    public short updatePeriod = 60;
    @PredicateConstraint("idConstraint")
    @Comment("A list of ids to allow users to use the debug commands")
    public List<String> admins = new ArrayList<>();
    @Comment("The activity shown on the bot status")
    public BotActivity botActivityType = BotActivity.PLAYING;
    @Comment("The bot command prefix")
    public String prefix = "np!";
    @Comment("Your bot token. Never share it, anyone with it has full control of the bot")
    public String token = "DO NOT SHARE THE BOT TOKEN";
    @RegexConstraint("\\d+")
    public String clientId = "";
    @RegexConstraint("\\d+")
    public String discordServerId = "";
    @Hook
    @Comment("The whitelist entry settings, please refer to the documentation to set them up")
    public List<EntryData> entries = new ArrayList<>();

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
            return activityType == null ? null : Activity.of(activityType, Text.translatable("bot.activity.message").getString());
        }
    }

    public static boolean idConstraint(List<String> roles) {
        return roles.stream().allMatch(v -> Pattern.compile("\\d+").matcher(v).matches());
    }
}
