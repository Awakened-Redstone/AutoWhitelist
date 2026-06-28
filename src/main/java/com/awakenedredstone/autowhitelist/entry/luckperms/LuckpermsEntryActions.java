package com.awakenedredstone.autowhitelist.entry.luckperms;

import com.awakenedredstone.autowhitelist.entry.api.ActionType;
import com.awakenedredstone.autowhitelist.entry.api.EntryAction;
import net.minecraft.resources.Identifier;

public class LuckpermsEntryActions {
    public static final ActionType<PermissionEntryAction.Fields> PERMISSION = EntryAction.register(Identifier.fromNamespaceAndPath("luckperms", "permission"), PermissionEntryAction.Fields.CODEC, PermissionEntryAction::new);
    public static final ActionType<GroupEntryAction.Fields> GROUP = EntryAction.register(Identifier.fromNamespaceAndPath("luckperms", "group"), GroupEntryAction.Fields.CODEC, GroupEntryAction::new);

    public static void init() {}
}
