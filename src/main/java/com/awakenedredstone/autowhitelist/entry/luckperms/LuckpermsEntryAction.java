package com.awakenedredstone.autowhitelist.entry.luckperms;

import com.awakenedredstone.autowhitelist.entry.api.ActionType;
import com.awakenedredstone.autowhitelist.entry.api.EntryAction;
import com.awakenedredstone.autowhitelist.entry.api.ActionFields;
import com.awakenedredstone.autowhitelist.server.ServerDetails;
import com.awakenedredstone.autowhitelist.server.profile.PlayerProfile;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.Node;

import java.util.concurrent.CompletableFuture;

public abstract class LuckpermsEntryAction<T extends ActionFields> extends EntryAction<T> {
    protected LuckpermsEntryAction(ActionType<T> type, T fields, Builder<T> builder) {
        super(type, fields);
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
    public void onAdd(PlayerProfile profile) {
        getUser(profile).whenComplete((user, throwable) -> {
            // Add the LuckPerms group/permission to the user
            user.data().add(getNode());

            // Save the user to LuckPerms
            getUserManager().saveUser(user);
        });
    }

    @Override
    public void onRemove(PlayerProfile profile) {
        getUser(profile).whenComplete((user, throwable) -> {
            // Remove the LuckPerms group/permission from the user
            user.data().remove(getNode());

            // Save the user to LuckPerms
            getUserManager().saveUser(user);
        });
    }

    protected CompletableFuture<User> getUser(PlayerProfile profile) {
        UserManager userManager = getUserManager();
        CompletableFuture<User> future;
        if (ServerDetails.getServer().getPlayerList().getPlayer(profile.id()) == null) {
            future = userManager.loadUser(profile.id());
        } else {
            future = CompletableFuture.completedFuture(userManager.getUser(profile.id()));
        }
        return future;
    }

    protected abstract Node getNode();
}
