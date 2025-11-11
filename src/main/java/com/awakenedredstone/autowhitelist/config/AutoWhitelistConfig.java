package com.awakenedredstone.autowhitelist.config;

import blue.endless.jankson.Comment;
import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.Constants;
import com.awakenedredstone.autowhitelist.config.source.ConfigHandler;
import com.awakenedredstone.autowhitelist.config.source.annotation.NameFormat;
import com.awakenedredstone.autowhitelist.config.source.annotation.PredicateConstraint;
import com.awakenedredstone.autowhitelist.config.source.annotation.RangeConstraint;
import com.awakenedredstone.autowhitelist.config.source.annotation.SkipNameFormat;
import com.awakenedredstone.autowhitelist.entry.BaseEntryAction;
import com.awakenedredstone.autowhitelist.entry.implementation.CommandEntryAction;
import com.awakenedredstone.autowhitelist.entry.implementation.VanillaTeamEntryAction;
import com.awakenedredstone.autowhitelist.entry.implementation.WhitelistEntryAction;
import com.awakenedredstone.autowhitelist.entry.implementation.luckperms.GroupEntryAction;
import com.awakenedredstone.autowhitelist.entry.implementation.luckperms.PermissionEntryAction;
import com.awakenedredstone.autowhitelist.entry.serialization.JanksonOps;
import com.awakenedredstone.autowhitelist.util.JanksonBuilder;
import com.awakenedredstone.autowhitelist.util.JsonUtil;
import com.awakenedredstone.autowhitelist.util.TimeParser;
import com.google.common.base.CaseFormat;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static com.awakenedredstone.autowhitelist.AutoWhitelist.DATA_FIXER_LOGGER;

@SuppressWarnings("CanBeFinal")
@NameFormat(NameFormat.Case.SNAKE_CASE)
public class AutoWhitelistConfig extends ConfigHandler {
    public AutoWhitelistConfig() {
        super("autowhitelist", Constants.CONFIG_VERSION, JanksonBuilder.buildJankson(builder -> {
            builder.registerDeserializer(JsonObject.class, BaseEntryAction.class, (jsonObject, marshaller) -> BaseEntryAction.CODEC.parse(JanksonOps.INSTANCE, jsonObject).getOrThrow());
            builder.registerSerializer(BaseEntryAction.class, (entryData, marshaller) -> BaseEntryAction.CODEC.encodeStart(JanksonOps.INSTANCE, entryData).getOrThrow());
        }));
    }

    @SuppressWarnings("unused")
    @SkipNameFormat
    @Comment("The JSON schema for the config, this is for text editors to show syntax highlighting, do not change it")
    public String $schema = Constants.CONFIG_SCHEMA;

    @Comment("When enabled, it will keep a cache of previous registered users and will use it to automatically add the user back (if they have the proper role)")
    public boolean enableWhitelistCache = true;

    @RangeConstraint(min = 10, max = 600)
    @Comment("""
      The delay (in seconds) for checking for invalid and outdated entries. This ensures everything is up to date in case the bot misses an event
      [Min: 10, Max: 600]""")
    public short periodicCheckDelay = 60;

    @Comment("The activity type shown on the bot status, must be in UPPERCASE")
    public BotActivity botActivityType = BotActivity.PLAYING;

    @Comment("The text shown on the bot activity status")
    public String botActivityText = "on the Member Server";

    @PredicateConstraint("timeConstraint")
    @Comment("""
      The time the bot will lock a whitelist entry after it is added or updated, use -1 to lock all entries forever
      Changes to this value will only apply to new entries, except for permanent lock which is immediate and global
      Check the documentation for more details on how the format works""")
    public String lockTime = "1d";

    @Comment("Your bot's token. Never share it, anyone with it has full control of the bot")
    public String token = "DO NOT SHARE THE BOT TOKEN";

    @RangeConstraint(min = 0, max = Long.MAX_VALUE)
    public long discordServerId = 0;

    @Comment("When enabled, all interactions and slash commands will be ephemeral, meaning only the user can see the response.")
    public boolean ephemeralReplies = true;

    @Comment("When enabled, the bot will cache the data of the users on discord, this reduces response time, but may cause higher memory usage on large discord servers.")
    public boolean cacheDiscordData = true;

    @RangeConstraint(min = 0, max = 4)
    @Comment("""
      The permission level used for command entries. This limits what commands the mod can run, you likely don't need to change this.
      Check https://minecraft.wiki/w/Permission_level for more about permission levels.""")
    public int commandPermissionLevel = 3;

    @Comment("The whitelist entry settings, please refer to the documentation to set them up")
    public List<BaseEntryAction> entries = new ArrayList<>();



    @SuppressWarnings("unused")
    public static boolean timeConstraint(String timeString) {
        if (timeString.equals("-1")) return true;
        int time = TimeParser.parseTime(timeString);
        return time >= 0;
    }

    public long lockTime() {
        if (lockTime.trim().equals("-1")) return -1;
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

            if (configVersion != Constants.CONFIG_VERSION) {
                DATA_FIXER_LOGGER.info("New config version available!");
                DATA_FIXER_LOGGER.info("Updating config from {} to {}", configVersion, Constants.CONFIG_VERSION);
            }

            switch (configVersion) {
                case 1: {
                    DATA_FIXER_LOGGER.info("Updating config file from v{} to v{}", configVersion, Constants.CONFIG_VERSION);
                    JsonArray entries = config.get(JsonArray.class, "entries");
                    if (entries != null) {
                        List<BaseEntryAction> entryList = new ArrayList<>();
                        for (JsonElement jsonElement : entries) {
                            JsonObject entryData = (JsonObject) jsonElement;
                            String oldType = entryData.get(String.class, "type");
                            Identifier newType;
                            if (oldType != null) {
                                // Don't use "case null, default ->" as the code has to work on Java 17 too
                                newType = switch (oldType) {
                                    case "WHITELIST" -> WhitelistEntryAction.ID;
                                    case "COMMAND" -> CommandEntryAction.ID;
                                    case "TEAM" -> VanillaTeamEntryAction.ID;
                                    case "LUCKPERMS_GROUP" -> GroupEntryAction.ID;
                                    case "LUCKPERMS_PERMISSION" -> PermissionEntryAction.ID;
                                    default -> null;
                                };
                            } else {
                                DATA_FIXER_LOGGER.warn("Could not get type of an entry, the invalid entry was removed!");
                                continue;
                            }

                            if (newType == null) {
                                DATA_FIXER_LOGGER.warn("Unknown entry type [{}], can not update to new config, the invalid entry was removed!", oldType);
                                continue;
                            }

                            DATA_FIXER_LOGGER.debug("Updating entry name from {} to {}", oldType, newType);

                            entryData.remove("type");
                            entryData.put("type", Identifier.CODEC.encodeStart(JanksonOps.INSTANCE, newType).getOrThrow());

                            JsonArray rolesArray = entryData.get(JsonArray.class, "roleIds");

                            if (rolesArray == null) {
                                rolesArray = new JsonArray();
                            }

                            entryData.put("roles", rolesArray);
                            entryData.remove("roleIds");

                            entryData = BaseEntryAction.getDataFixers().get(newType).apply(configVersion, entryData);

                            entryList.add(BaseEntryAction.CODEC.parse(JanksonOps.INSTANCE, entryData).getOrThrow());
                        }

                        entries.clear();
                        for (BaseEntryAction entry : entryList) {
                            AutoWhitelist.LOGGER.debug("Encoding {}", entry.getType());
                            entries.add(BaseEntryAction.CODEC.encodeStart(JanksonOps.INSTANCE, entry).getOrThrow());
                        }

                        config.remove("entries");
                        config.put("entries", entries);
                    }

                    JsonObject newConfig = new JsonObject();

                    config.forEach((key, jsonElement) -> {
                        String comment = getComment(key);

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
                                    DATA_FIXER_LOGGER.warn("Invalid user Id: {}, the value was removed", jsonElement);
                                }
                            }
                            jsonElement = newAdmins;
                        }

                        if (key.equals("discordServerId")) {
                            try {
                                jsonElement = new JsonPrimitive(Long.parseLong(((JsonPrimitive) jsonElement).asString()));
                            } catch (Throwable e) {
                                DATA_FIXER_LOGGER.warn("Invalid user Id: {}, replacing with default", jsonElement);
                                jsonElement = new JsonPrimitive(0L);
                            }
                        }

                        String newKey = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, key);
                        DATA_FIXER_LOGGER.debug("Updating {} to {}", key, newKey);

                        newConfig.put(newKey, jsonElement, comment);
                    });

                    config = newConfig;
                    configVersion = config.getByte("CONFIG_VERSION", Constants.CONFIG_VERSION);
                }
                case 2: {
                    String activityType = config.get(String.class, "bot_activity_type");
                    if (activityType != null) {
                        activityType = switch (activityType.toUpperCase()) {
                            case "NONE" -> "DONT_CHANGE";
                            case "RESET" -> "CLEAR";
                            default -> activityType;
                        };

                        config.put("bot_activity_type", new JsonPrimitive(activityType));
                    }
                }
            }

            if (configVersion < 3) {
                if (!config.containsKey("cache_discord_data")) {
                    config.put("cache_discord_data", new JsonPrimitive(cacheDiscordData), getComment("cacheDiscordData"));
                }
            }

            if (configVersion < 5) {
                config.put("command_permission_level", new JsonPrimitive(commandPermissionLevel), getComment("commandPermissionLevel"));
            }

            if (configVersion < 6) {
                config.remove("admins");
                config.remove("prefix");
                JsonUtil.rename(config, "update_period", "periodic_check_delay", getComment("periodicCheckDelay"));
            }

            if (configVersion != Constants.CONFIG_VERSION) {
                JsonObject newJson = new JsonObject();
                String comment = getComment("$schema");

                newJson.put("$schema", new JsonPrimitive(Constants.CONFIG_SCHEMA), comment);
                JsonObject finalConfig = config;
                config.forEach((key, jsonElement) -> {
                    if (key.equals("$schema")) return;
                    newJson.put(key, jsonElement, finalConfig.getComment(key));
                });
                newJson.put("CONFIG_VERSION", new JsonPrimitive(Constants.CONFIG_VERSION));

                DATA_FIXER_LOGGER.info("Saving updated config");
                this.save(newJson);
            }
        } catch (Exception e) {
            DATA_FIXER_LOGGER.error("The config updater crashed!", e);
        }

        super.load();
    }

    private @Nullable String getComment(String key) {
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
        return comment;
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    public enum BotActivity {
        DONT_CHANGE(null),
        CLEAR(null),
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
            return activityType == null ? null : Activity.of(getActivityType(), AutoWhitelist.CONFIG.botActivityText);
        }
    }
}
