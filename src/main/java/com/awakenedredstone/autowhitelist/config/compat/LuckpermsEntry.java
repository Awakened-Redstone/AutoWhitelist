package com.awakenedredstone.autowhitelist.config.compat;

import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.config.EntryData;
import com.awakenedredstone.autowhitelist.config.EntryType;
import com.mojang.authlib.GameProfile;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeEqualityPredicate;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.PermissionNode;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class LuckpermsEntry extends EntryData {
    /**
     * Obtain a reference of the LuckPerms userManager.
     *
     * @return An instance of the LuckPerms userManager.
     */
    private UserManager getUserManager() {
        LuckPerms luckPerms = LuckPermsProvider.get();
        return luckPerms.getUserManager();
    }

    @Override
    public <T extends GameProfile> void registerUser(T profile) {
        getUser(profile).whenComplete((user, throwable) -> {
            // Add the LuckPerms group/permission to the user
            user.data().add(getNode());

            // Save the user to LuckPerms
            getUserManager().saveUser(user);
        });
    }

    @Override
    public <T extends GameProfile> void removeUser(T profile) {
        getUser(profile).whenComplete((user, throwable) -> {
            // Remove the LuckPerms group/permission from the user
            user.data().remove(getNode());

            // Save the user to LuckPerms
            getUserManager().saveUser(user);
        });
    }

    @Override
    public <T extends GameProfile> void updateUser(T profile) {
        removeUser(profile);
        registerUser(profile);
    }

    @Override
    public <T extends GameProfile> boolean shouldUpdate(T profile) {
        try {
            User user = getUser(profile).get(1, TimeUnit.SECONDS);
            return user.data().contains(getNode(), NodeEqualityPredicate.EXACT).asBoolean();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            AutoWhitelist.LOGGER.error("Failed to get permission or group data", e);
            return false;
        }
    }

    protected CompletableFuture<User> getUser(GameProfile profile) {
        UserManager userManager = getUserManager();
        CompletableFuture<User> future;
        if (AutoWhitelist.server.getPlayerManager().getPlayer(profile.getId()) == null) {
            future = userManager.loadUser(profile.getId());
        } else {
            future = CompletableFuture.completedFuture(userManager.getUser(profile.getId()));
        }
        return future;
    }

    protected abstract Node getNode();

    public static class Permission extends LuckpermsEntry {
        private final String permission;

        public Permission(String permission) {
            this.permission = permission;
        }

        @Override
        protected Node getNode() {
            return PermissionNode.builder(permission).build();
        }

        @Override
        public EntryType getType() {
            return EntryType.LUCKPERMS_PERMISSION;
        }

        @Override
        public void assertSafe() {
            if (StringUtils.isBlank(permission)) {
                throw new IllegalArgumentException("Permission can not be blank!");
            }
        }

        @Override
        public void purgeInvalid() {/**/}

        @Override
        public EntryData deserialize(JsonObject data) {
            return new Permission(getPrimitive("permission", data).asString());
        }

        @Override
        public JsonObject serialize() {
            JsonObject json = new JsonObject();
            json.put("permission", new JsonPrimitive(permission));
            return json;
        }
    }

    public static class Group extends LuckpermsEntry {
        private final String group;

        public Group(String group) {
            this.group = group;
        }

        @Override
        protected Node getNode() {
            return InheritanceNode.builder(group).build();
        }

        @Override
        public EntryType getType() {
            return EntryType.LUCKPERMS_GROUP;
        }

        @Override
        public void assertSafe() {
            if (StringUtils.isBlank(group)) {
                throw new IllegalArgumentException("Group can not be blank!");
            }
        }

        @Override
        public <T extends GameProfile> boolean shouldUpdate(T profile) {
            return false;
        }

        @Override
        public void purgeInvalid() {/**/}

        @Override
        public EntryData deserialize(JsonObject data) {
            return new Group(getPrimitive("group", data).asString());
        }

        @Override
        public JsonObject serialize() {
            JsonObject json = new JsonObject();
            json.put("group", new JsonPrimitive(group));
            return json;
        }
    }
}
