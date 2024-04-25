package com.awakenedredstone.autowhitelist.config;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.mixin.ServerConfigEntryMixin;
import com.awakenedredstone.autowhitelist.util.DynamicPlaceholders;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelist;
import com.mojang.authlib.GameProfile;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.WhitelistEntry;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class EntryData {
    private static final Map<String, EntryData> entryTypes = new HashMap<>();
    private final List<String> roleIds = new ArrayList<>();
    protected boolean onLogin = false;

    public static EntryData deserialize(String type, JsonObject data) {
        EntryData entry = entryTypes.get(String.valueOf(type)).deserialize(data);
        entry.populate(data);
        return entry;
    }

    public static void register(EntryData data) {
        entryTypes.putIfAbsent(data.getType(), data);
    }

    public abstract String getType();

    public abstract void assertSafe();

    public abstract <T extends GameProfile> void registerUser(T profile);

    public abstract <T extends GameProfile> void removeUser(T profile);

    public abstract <T extends GameProfile> void updateUser(T profile);

    public abstract <T extends GameProfile> boolean shouldUpdate(T profile);

    public abstract void purgeInvalid();

    public abstract EntryData deserialize(JsonObject data);

    public abstract JsonObject serialize();

    public List<String> getRoleIds() {
        return List.copyOf(roleIds);
    }

    public boolean isOnLogin() {
        return onLogin;
    }

    protected JsonPrimitive getPrimitive(String key, JsonObject data) {
        return (JsonPrimitive) data.get(key);
    }

    public void populate(JsonObject data) {
        JsonArray roles = (JsonArray) data.get("roleIds");
        if (roles == null) throw new AssertionError("The roleIds array is missing!");
        roleIds.addAll(roles.stream().map(v -> ((JsonPrimitive) v).asString()).toList());

        JsonElement onLogin = data.get("onLogin");
        if (onLogin != null) {
            this.onLogin = ((JsonPrimitive) onLogin).asBoolean(false);
        }
    }

    public static class Team extends EntryData {
        private final String team;

        public Team(String team) {
            this.team = team;
        }

        @Override
        public String getType() {
            return "TEAM";
        }

        @Override
        public void assertSafe() {
            if (AutoWhitelist.getServer().getScoreboard().getTeam(team) == null) {
                throw new AssertionError(String.format("The team \"%s\" does not exist!", team));
            }
        }

        @Override
        public void purgeInvalid() {
            Scoreboard scoreboard = AutoWhitelist.getServer().getScoreboard();
            net.minecraft.scoreboard.Team serverTeam = scoreboard.getTeam(team);
            if (serverTeam == null) {
                AutoWhitelist.LOGGER.error("Could not check for invalid players on team \"{}\", could not find the team", team);
                return;
            }
            PlayerManager playerManager = AutoWhitelist.getServer().getPlayerManager();
            ExtendedWhitelist whitelist = (ExtendedWhitelist) playerManager.getWhitelist();
            Collection<? extends WhitelistEntry> entries = whitelist.getEntries();
            List<GameProfile> profiles = entries.stream().map(v -> (GameProfile) ((ServerConfigEntryMixin<?>) v).getKey()).toList();
            List<String> invalidPlayers = serverTeam.getPlayerList().stream().filter(player -> {
                GameProfile profile = profiles.stream().filter(v -> v.getName().equals(player)).findFirst().orElse(null);
                if (profile == null) return true;
                return !whitelist.isAllowed(profile);
            }).toList();
            /*? if >=1.20.3 {*/
            invalidPlayers.forEach(player -> scoreboard.removeScoreHolderFromTeam(player, serverTeam));
            /*?} else {*//*
            invalidPlayers.forEach(player -> scoreboard.removePlayerFromTeam(player, serverTeam));
            *//*?} */
        }

        @Override
        public <T extends GameProfile> void registerUser(T profile) {
            net.minecraft.scoreboard.Team serverTeam = AutoWhitelist.getServer().getScoreboard().getTeam(team);
            /*? if >=1.20.3 {*/
            AutoWhitelist.getServer().getScoreboard().addScoreHolderToTeam(profile.getName(), serverTeam);
            /*?} else {*//*
            AutoWhitelist.getServer().getScoreboard().addPlayerToTeam(profile.getName(), serverTeam);
            *//*?} */
        }

        @Override
        public <T extends GameProfile> void removeUser(T profile) {
            /*? if >=1.20.3 {*/
            AutoWhitelist.getServer().getScoreboard().clearTeam(profile.getName());
            /*?} else {*//*
            AutoWhitelist.getServer().getScoreboard().clearPlayerTeam(profile.getName());
            *//*?} */
        }

        @Override
        public <T extends GameProfile> void updateUser(T profile) {
            registerUser(profile);
        }

        @Override
        public <T extends GameProfile> boolean shouldUpdate(T profile) {
            ServerScoreboard scoreboard = AutoWhitelist.getServer().getScoreboard();
            net.minecraft.scoreboard.Team serverTeam = scoreboard.getTeam(team);

            return scoreboard.getScoreHolderTeam(profile.getName()) != serverTeam;
        }

        @Override
        public EntryData deserialize(JsonObject data) {
            return new Team(getPrimitive("team", data).asString());
        }

        @Override
        public JsonObject serialize() {
            JsonObject json = new JsonObject();
            json.put("team", new JsonPrimitive(team));
            return json;
        }
    }

    public static class Command extends EntryData {
        private final String addCommand;
        private final String removeCommand;

        public Command(String addCommand, String removeCommand) {
            this.addCommand = addCommand;
            this.removeCommand = removeCommand;
        }

        @Override
        public String getType() {
            return "COMMAND";
        }

        @Override
        public <T extends GameProfile> void registerUser(T profile) {
            AutoWhitelist.getServer().getCommandManager()./*? if >=1.19 {*/executeWithPrefix/*?} else {*//*execute*//*?}*/(AutoWhitelist.getCommandSource(), DynamicPlaceholders.parseText(addCommand, profile.getName()).getString());
        }

        @Override
        public <T extends GameProfile> void removeUser(T profile) {
            AutoWhitelist.getServer().getCommandManager()./*? if >=1.19 {*/executeWithPrefix/*?} else {*//*execute*//*?}*/(AutoWhitelist.getCommandSource(), DynamicPlaceholders.parseText(removeCommand, profile.getName()).getString());
        }

        @Override
        public <T extends GameProfile> void updateUser(T profile) {
            removeUser(profile);
            registerUser(profile);
        }

        @Override
        public <T extends GameProfile> boolean shouldUpdate(T profile) {
            return false;
        }

        @Override
        public void assertSafe() {
            var root = AutoWhitelist.getServer().getCommandManager().getDispatcher().getRoot();
            String addCmdStart = addCommand.split(" ", 2)[0];
            if (root.getChild(addCmdStart) == null && !StringUtils.isBlank(addCmdStart)) {
                if (addCmdStart.startsWith("/")) {
                    AutoWhitelist.LOGGER.warn("You don't need a slash at the start of the command");
                }
                throw new AssertionError(String.format("Add command \"%s\" does not exist!", addCmdStart));
            }
            String removeCmdStart = addCommand.split(" ", 2)[0];
            if (root.getChild(removeCmdStart) == null && !StringUtils.isBlank(removeCmdStart)) {
                if (removeCmdStart.startsWith("/")) {
                    AutoWhitelist.LOGGER.warn("You don't need a slash at the start of the command");
                }
                throw new AssertionError(String.format("Remove command \"%s\" does not exist!", removeCmdStart));
            }
        }

        @Override
        public void purgeInvalid() {/**/}

        @Override
        public EntryData deserialize(JsonObject data) {
            return new Command(getPrimitive("addCommand", data).asString(), getPrimitive("removeCommand", data).asString());
        }

        @Override
        public JsonObject serialize() {
            JsonObject json = new JsonObject();
            json.put("addCommand", new JsonPrimitive(addCommand));
            json.put("removeCommand", new JsonPrimitive(removeCommand));
            return json;
        }
    }

    public static class Whitelist extends EntryData {
        @Override
        public String getType() {
            return "WHITELIST";
        }

        @Override
        public <T extends GameProfile> void registerUser(T profile) {
            // Nothing to do here
        }

        @Override
        public <T extends GameProfile> void removeUser(T profile) {
            // Nothing to do here
        }

        @Override
        public <T extends GameProfile> void updateUser(T profile) {
            // Nothing to do here
        }

        @Override
        public <T extends GameProfile> boolean shouldUpdate(T profile) {
            return false;
        }

        @Override
        public void assertSafe() {
            // Nothing to do here
        }

        @Override
        public void purgeInvalid() {/**/}

        @Override
        public EntryData deserialize(JsonObject data) {
            return new Whitelist();
        }

        @Override
        public JsonObject serialize() {
            return new JsonObject();
        }
    }
}


