package com.awakenedredstone.autowhitelist.config;

import com.awakenedredstone.autowhitelist.discord.DiscordClientHolder;
import com.awakenedredstone.moondust.config.api.ConfigSettings;
import com.awakenedredstone.moondust.config.api.datafixer.JsonPorting;
import com.awakenedredstone.autowhitelist.entry.api.EntryAction;
import com.awakenedredstone.autowhitelist.util.JvmViolations;
import com.awakenedredstone.moondust.jankson.*;
import com.awakenedredstone.moondust.jankson.element.*;
import com.awakenedredstone.moondust.jankson.element.primitive.JsonPrimitive;
import com.awakenedredstone.moondust.jankson.element.primitive.JsonText;
//? if >=1.21.11 {
import net.minecraft.server.permissions.PermissionLevel;
//?}
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class AutoWhitelistConfigSettings extends ConfigSettings<AutoWhitelistConfig> {
    /**
     * Catalog of the config versions:
     * <ol>
     * <li>Initial config</li>
     * <li>Major refactor, use snake_case and renaming</li>
     * <li>Added `cache_discord_data`</li>
     * <li>Doesn't exist</li>
     * <li>Added `command_permission_level`</li>
     * <li>Removed `admins` and `prefix`, renamed `update_period` to `periodic_check_delay`</li>
     * <li>Major rewrite</li>
     * <ol/>
     */
    public AutoWhitelistConfigSettings() {
        super("autowhitelist", AutoWhitelistConfig.class,
          7,
          Jankson.builder()
            .registerCodec(EntryAction.class, JvmViolations.unsafeCast(EntryAction.CODEC))
            .build()
        );

        addDataFixer(7, jsonObject -> {
            if (jsonObject.getInt("CONFIG_VERSION", 0) < 6) {
                throw new IllegalStateException("Updating to AutoWhitelist 2.0 requires a config version of 6, please run to the latest 1.x first!");
            }

            // $schema                  Deprecated and removed, almost no panel editor supports it
            // enable_whitelist_cache   Deprecated and removed, always enabled now
            // periodic_check_delay     Deprecated and removed, it's now fully event based
            // cache_discord_data       Deprecated and removed, it poses no practical difference or benefits

            AutoWhitelistConfig defaultConfig = new AutoWhitelistConfig();
            JsonObject config = (JsonObject) this.getInterpreter().toJson(defaultConfig);
            JsonPorting porting = new JsonPorting(jsonObject, config);

            // Discord config key
            var discord = porting.getTarget("discord");
            discord.copy("token");
            discord.migrate("guild_id", "discord_server_id");
            discord.migrate("ephemeral_replies", "ephemeral_replies");
            portPresence(discord);

            // Whitelist config key
            var whitelist = porting.getTarget("whitelist");
            whitelist.copy("lock_time");
            //? if <1.21.11 {
            /*whitelist.copy("command_permission_level");
            *///?} else {
            int permissionLevel = whitelist.source().getInt("command_permission_level", 3);
            whitelist.target().put("command_permission_level", new JsonText(PermissionLevel.byId(permissionLevel).getSerializedName()));
            //?}
            whitelist.target().put("allow", updateEntries(Objects.requireNonNull(jsonObject.get(JsonArray.class, "entries"))));

            DiscordClientHolder.migrateCommands();

            return config;
        });
    }

    private void portPresence(JsonPorting discord) {
        var presence = discord.getTarget("presence");

        JsonElement activityType = Objects.requireNonNull(discord.source().get("bot_activity_type"));
        if (activityType instanceof JsonPrimitive<?> primitive) {
            String string = primitive.asString();
            if (string.equalsIgnoreCase("DONT_CHANGE")) {
                discord.target().put("presence", JsonNull.INSTANCE);
                return;
            }

            var presenceActivity = presence.getTarget("activity");
            presenceActivity.target().put("type", string.equalsIgnoreCase("CLEAR") ? JsonNull.INSTANCE : activityType);
            presenceActivity.migrate("text", "bot_activity_text");
        } else {
            throw new IllegalStateException("status is not set to a string, can not process!");
        }
    }

    private JsonArray updateEntries(JsonArray original) {
        var entries = new JsonArray();
        for (JsonElement jsonElement : original) {
            if (!(jsonElement instanceof JsonObject object)) continue;

            entries.add(updateEntry(object));
        }

        return entries;
    }

    private @NotNull JsonObject updateEntry(@NotNull JsonObject original) {
        var entry = new JsonObject();

        entry.put("roles", Objects.requireNonNull(original.get("roles")));
        entry.put("actions", updateAction(original));

        return entry;
    }

    private JsonObject updateAction(@NotNull JsonObject original) {
        var action = new JsonObject();

        JsonElement typeElement = Objects.requireNonNull(original.get("type"));
        String type = ((JsonPrimitive<?>) typeElement).asString();

        JsonObject execute = original.getObject("execute");
        switch (type) {
            case "autowhitelist:luckperms/group" -> action.put("type", new JsonText("luckperms:group"));
            case "autowhitelist:luckperms/permission" -> action.put("type", new JsonText("luckperms:permission"));
            case "autowhitelist:execute_command" -> action.put("type", new JsonText("minecraft:execute_command"));
            case "autowhitelist:team" -> {
                action.put("type", new JsonText("minecraft:execute_command"), new InnerEntry.Meta("Migrated from \"autowhitelist:team\"", null));
                String team = ((JsonText) Objects.requireNonNull(Objects.requireNonNull(execute).get("associate_team"))).asString();
                execute = new JsonObject();
                execute.put("on_add", new JsonText("team join %s {player}".formatted(team)));
                execute.put("on_remove", new JsonText("team leave {player}"));
                action.put("execute", execute);
            }
            default -> action.put("type", typeElement);
        }

        if (execute != null) {
            action.put("execute", execute);
        }

        return action;
    }
}
