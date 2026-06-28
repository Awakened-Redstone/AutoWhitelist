package com.awakenedredstone.autowhitelist.entry.playerroles;

import com.awakenedredstone.autowhitelist.entry.api.ActionType;
import com.awakenedredstone.autowhitelist.entry.api.EntryAction;
import net.minecraft.resources.Identifier;

public class PlayerRolesEntryActions {
    public static final ActionType<RoleEntryAction.Fields> ROLE = EntryAction.register(Identifier.fromNamespaceAndPath("player_roles", "role"), RoleEntryAction.Fields.CODEC, RoleEntryAction::new);

    public static void init() {}
}
