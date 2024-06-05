package com.awakenedredstone.autowhitelist.config;

import blue.endless.jankson.*;
import blue.endless.jankson.annotation.SerializedName;
import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.Constants;
import com.awakenedredstone.autowhitelist.config.source.ConfigHandler;
import com.awakenedredstone.autowhitelist.config.source.annotation.*;
import com.awakenedredstone.autowhitelist.entry.BaseEntry;
import com.awakenedredstone.autowhitelist.entry.CommandEntry;
import com.awakenedredstone.autowhitelist.entry.TeamEntry;
import com.awakenedredstone.autowhitelist.entry.WhitelistEntry;
import com.awakenedredstone.autowhitelist.entry.luckperms.GroupEntry;
import com.awakenedredstone.autowhitelist.entry.luckperms.PermissionEntry;
import com.awakenedredstone.autowhitelist.entry.serialization.JanksonOps;
import com.awakenedredstone.autowhitelist.util.JanksonBuilder;
import com.awakenedredstone.autowhitelist.util.TimeParser;
import com.google.common.base.CaseFormat;
import net.dv8tion.jda.api.entities.Activity;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
/*? if >=1.19 {*/
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/*?} else {*/
/*import net.minecraft.text.TranslatableText;
*//*?}*/

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@NameFormat(NameFormat.Case.SNAKE_CASE)
public class AutoWhitelistConfig extends ConfigHandler {
    public AutoWhitelistConfig() {
        super("autowhitelist", JanksonBuilder.buildJankson(builder -> {
            builder.registerDeserializer(JsonObject.class, BaseEntry.class, (jsonObject, marshaller) -> BaseEntry.CODEC.parse(JanksonOps.INSTANCE, jsonObject).getOrThrow(/*? if <1.20.5 {*//*false, s -> {}*//*?}*/));
            builder.registerSerializer(BaseEntry.class, (entryData, marshaller) -> BaseEntry.CODEC.encodeStart(JanksonOps.INSTANCE, entryData).getOrThrow(/*? if <1.20.5 {*//*false, s -> {}*//*?}*/));
        }));
    }

    @SkipNameFormat
    @Comment("The JSON schema for the config, this is for text editors to show syntax highlighting, do not change it")
    public String $schema = Constants.CONFIG_SCHEMA;

    @SkipNameFormat
    @Comment("DO NOT CHANGE, MODIFYING THIS VALUE WILL BREAK THE CONFIGURATION FILE")
    public byte CONFIG_VERSION = Constants.CONFIG_VERSION;
    @Comment("When enabled, it will keep a cache of previous registered users and will use it to automatically add the user back (if they have the proper role)")
    public boolean enableWhitelistCache = true;
    @RangeConstraint(min = 10, max = 300)
    @Comment("The period the mod looks for outdated and invalid entries, this is an extra action to guarantee everything is updated")
    public short updatePeriod = 60;
    @PredicateConstraint("idConstraint")
    @Comment("A list of ids to allow users to use the debug commands")
    public List<Long> admins = new ArrayList<>();
    @Comment("The activity type shown on the bot status")
    public BotActivity botActivityType = BotActivity.PLAYING;
    @Comment("The bot command prefix [DEPRECATED]")
    public String prefix = "np!";
    @PredicateConstraint("timeConstraint")
    @Comment("The time in seconds the bot will lock a whitelist entry after it is added or updated, use -1 to disable changing the linked username")
    public String lockTime = "1d";
    @Comment("Your bot token. Never share it, anyone with it has full control of the bot")
    public String token = "DO NOT SHARE THE BOT TOKEN";
    @RegexConstraint("\\d+")
    public long discordServerId = 0;
    @Comment("When enabled, all interactions and slash commands will be ephemeral, meaning only the user can see the response.")
    public boolean ephemeralReplies = true;
    @Comment("The whitelist entry settings, please refer to the documentation to set them up")
    public List<BaseEntry> entries = new ArrayList<>();

    @SuppressWarnings("unused")
    public static boolean idConstraint(List<Long> roles) {
        return roles.stream().allMatch(v -> v >= 0);
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
          .replace(token, "[HIDDEN]")
          .replace(String.valueOf(discordServerId), "[HIDDEN]");
    }

    @Override
    public void load() {
        if (!configExists()) {
            super.load();
            return;
        }

        try {
            JsonObject config = this.getInterpreter().load(Files.readString(this.getFileLocation(), StandardCharsets.UTF_8));
            byte configVersion = config.getByte("CONFIG_VERSION", Constants.CONFIG_VERSION);
            if (configVersion == 1) {
                LOGGER.info("Updating config file from v{} to v{}", configVersion, Constants.CONFIG_VERSION);
                JsonArray entries = config.get(JsonArray.class, "entries");
                if (entries != null) {
                    List<BaseEntry> entryList = new ArrayList<>();
                    for (JsonElement jsonElement : entries) {
                        JsonObject entryData = (JsonObject) jsonElement;
                        String oldType = entryData.get(String.class, "type");
                        Identifier newType;
                        if (oldType != null) {
                            // Don't use "case null, default ->" as the code has to work on Java 17 too
                            newType = switch (oldType) {
                                case "WHITELIST" -> WhitelistEntry.ID;
                                case "COMMAND" -> CommandEntry.ID;
                                case "TEAM" -> TeamEntry.ID;
                                case "LUCKPERMS_GROUP" -> GroupEntry.ID;
                                case "LUCKPERMS_PERMISSION" -> PermissionEntry.ID;
                                default -> null;
                            };
                        } else {
                            LOGGER.warn("Could not get type of an entry, the invalid entry was removed!");
                            continue;
                        }

                        if (newType == null) {
                            LOGGER.warn("Unknown entry type [{}], can not update to new config, the invalid entry was removed!", oldType);
                            continue;
                        }

                        LOGGER.debug("Updating entry name from {} to {}", oldType, newType);

                        entryData.remove("type");
                        entryData.put("type", Identifier.CODEC.encodeStart(JanksonOps.INSTANCE, newType).getOrThrow(/*? if <1.20.5 {*//*false, s -> {}*//*?}*/));

                        JsonArray rolesArray = entryData.get(JsonArray.class, "roleIds");

                        if (rolesArray == null) {
                            rolesArray = new JsonArray();
                        }

                        entryData.put("roles", rolesArray);
                        entryData.remove("roleIds");

                        entryData = BaseEntry.getDataFixers().get(newType).apply(configVersion, entryData);

                        entryList.add(BaseEntry.CODEC.parse(JanksonOps.INSTANCE, entryData).getOrThrow(/*? if <1.20.5 {*//*false, s -> {}*//*?}*/));
                    }

                    entries.clear();
                    for (BaseEntry entry : entryList) {
                        AutoWhitelist.LOGGER.debug("Encoding {}", entry.getType());
                        entries.add(BaseEntry.CODEC.encodeStart(JanksonOps.INSTANCE, entry).getOrThrow(/*? if <1.20.5 {*//*false, s -> {}*//*?}*/));
                    }

                    config.remove("entries");
                    config.put("entries", entries);
                }

                JsonObject newConfig = new JsonObject();

                config.forEach((key, jsonElement) -> {
                    String comment;
                    try {
                        Field field = this.getClass().getField(key);
                        if (field.isAnnotationPresent(Comment.class)) {
                            comment = field.getAnnotation(Comment.class).value();
                        } else {
                            comment = null;
                        }
                    } catch (NoSuchFieldException e) {
                        comment = null;
                    }

                    if (key.equals("CONFIG_VERSION")) {
                        newConfig.put(key, jsonElement, comment);
                        return;
                    }

                    if (key.equals("admins")) {
                        JsonArray newAdmins = new JsonArray();
                        for (JsonElement element : (JsonArray) jsonElement) {
                            try {
                                newAdmins.add(new JsonPrimitive(Long.parseLong(((JsonPrimitive) element).asString())));
                            } catch (Throwable e) {
                                LOGGER.warn("Invalid user Id: {}, the value was removed", jsonElement);
                            }
                        }
                        jsonElement = newAdmins;
                    }

                    if (key.equals("discordServerId")) {
                        try {
                            jsonElement = new JsonPrimitive(Long.parseLong(((JsonPrimitive) jsonElement).asString()));
                        } catch (Throwable e) {
                            LOGGER.warn("Invalid user Id: {}, replacing with default", jsonElement);
                            jsonElement = new JsonPrimitive(0L);
                        }
                    }

                    String newKey = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key);
                    LOGGER.debug("Updating {} to {}", key, newKey);

                    newConfig.put(newKey, jsonElement, comment);
                });

                config = newConfig;
            }

            if (configVersion != Constants.CONFIG_VERSION) {
                JsonObject newJson = new JsonObject();
                String comment;
                try {
                    Field field = this.getClass().getField("$schema");
                    if (field.isAnnotationPresent(Comment.class)) {
                        comment = field.getAnnotation(Comment.class).value();
                    } else {
                        comment = null;
                    }
                } catch (NoSuchFieldException e) {
                    comment = null;
                }

                newJson.put("$schema", new JsonPrimitive(Constants.CONFIG_SCHEMA), comment);
                JsonObject finalConfig = config;
                config.forEach((key, jsonElement) -> {
                    if (key.equals("$schema")) return;
                    newJson.put(key, jsonElement, finalConfig.getComment(key));
                });
                newJson.put("CONFIG_VERSION", new JsonPrimitive(Constants.CONFIG_VERSION));

                LOGGER.info("Saving updated config");
                this.save(newJson);
            }
        } catch (Throwable e) {
            LOGGER.error("The config updater crashed!", e);
        }

        super.load();
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
