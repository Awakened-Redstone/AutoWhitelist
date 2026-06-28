package com.awakenedredstone.autowhitelist.entry;

import com.awakenedredstone.autowhitelist.entry.api.ActionType;
import com.awakenedredstone.autowhitelist.entry.api.EntryAction;
import com.awakenedredstone.autowhitelist.entry.luckperms.LuckpermsEntryActions;
import com.awakenedredstone.autowhitelist.entry.playerroles.PlayerRolesEntryActions;
import com.awakenedredstone.autowhitelist.util.data.ModData;
import net.minecraft.resources.Identifier;

public class EntryActions {
    public static final ActionType<CommandEntryAction.Fields> EXECUTE_COMMAND = EntryAction.register(Identifier.withDefaultNamespace("execute_command"), CommandEntryAction.Fields.CODEC, CommandEntryAction::new);

    public static void init() {}

    static {
        ModData.ifModLoaded("luckperms", LuckpermsEntryActions::init);
        ModData.ifModLoaded("player_roles", PlayerRolesEntryActions::init);
    }
}
