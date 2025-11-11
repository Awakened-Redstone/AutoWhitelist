package com.awakenedredstone.autowhitelist.entry.implementation.luckperms;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.entry.BaseEntryAction;
import com.awakenedredstone.autowhitelist.stonecutter.Stonecutter;
import com.awakenedredstone.autowhitelist.whitelist.override.LinkedPlayerProfile;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.Node;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class LuckpermsEntryAction extends BaseEntryAction {
    protected LuckpermsEntryAction(Identifier type, List<String> roles) {
        super(type, roles);
    }

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
    public void registerUser(LinkedPlayerProfile profile) {
        getUser(profile).whenComplete((user, throwable) -> {
            // Add the LuckPerms group/permission to the user
            user.data().add(getNode());

            // Save the user to LuckPerms
            getUserManager().saveUser(user);
        });
    }

    @Override
    public void removeUser(LinkedPlayerProfile profile) {
        getUser(profile).whenComplete((user, throwable) -> {
            // Remove the LuckPerms group/permission from the user
            user.data().remove(getNode());

            // Save the user to LuckPerms
            getUserManager().saveUser(user);
        });
    }

    protected CompletableFuture<User> getUser(/*$ WhitelistProfile >>*/net.minecraft.server.PlayerConfigEntry profile) {
        UserManager userManager = getUserManager();
        CompletableFuture<User> future;
        if (AutoWhitelist.getServer().getPlayerManager().getPlayer(Stonecutter.profileId(profile)) == null) {
            future = userManager.loadUser(Stonecutter.profileId(profile));
        } else {
            future = CompletableFuture.completedFuture(userManager.getUser(Stonecutter.profileId(profile)));
        }
        return future;
    }

    protected abstract Node getNode();
}
