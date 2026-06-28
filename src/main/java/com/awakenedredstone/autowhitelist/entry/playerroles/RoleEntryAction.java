package com.awakenedredstone.autowhitelist.entry.playerroles;

import com.awakenedredstone.autowhitelist.entry.api.ActionFields;
import com.awakenedredstone.autowhitelist.entry.api.ActionType;
import com.awakenedredstone.autowhitelist.entry.api.EntryAction;
import com.awakenedredstone.autowhitelist.server.ServerDetails;
import com.awakenedredstone.autowhitelist.server.profile.PlayerProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.gegy.roles.config.PlayerRolesConfig;
import dev.gegy.roles.store.PlayerRoleManager;

public class RoleEntryAction extends EntryAction<RoleEntryAction.Fields> {
    protected RoleEntryAction(ActionType<Fields> type, Fields fields) {
        super(type, fields);
    }

    @Override
    public boolean validate(boolean early) {
        if (PlayerRolesConfig.get().get(fields.role) == null) {
            logger.error("Invalid role!");
            return false;
        }

        return true;
    }

    @Override
    public void onAdd(PlayerProfile profile) {
        var role = PlayerRolesConfig.get().get(fields.role);
        PlayerRoleManager.get().updateRoles(ServerDetails.getServer(), profile.id(), set -> set.add(role));
    }

    @Override
    public void onRemove(PlayerProfile profile) {
        var role = PlayerRolesConfig.get().get(fields.role);
        PlayerRoleManager.get().updateRoles(ServerDetails.getServer(), profile.id(), set -> set.remove(role));
    }

    public record Fields(String role) implements ActionFields {
        public static final Codec<Fields> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("role").forGetter(Fields::role)
          ).apply(instance, Fields::new)
        );
    }
}
