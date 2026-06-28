package com.awakenedredstone.autowhitelist.discord.interaction.commands.admin.userlink.remove;

import com.awakenedredstone.autowhitelist.discord.interaction.commands.admin.userlink.UserLinkCommand;
import com.awakenedredstone.autowhitelist.discord.interaction.commands.api.impl.ChatInputSubCommandGroup;
import org.jetbrains.annotations.NotNull;

public class UserLinkRemoveSubCommandGroup extends ChatInputSubCommandGroup<UserLinkCommand> {
    public UserLinkRemoveSubCommandGroup(@NotNull UserLinkCommand parent) {
        super(parent, "remove");

        this.subCommands.add(new UserLinkRemoveUsernameSubCommand(parent));
        this.subCommands.add(new UserLinkRemoveUserSubCommand(parent));
    }


}
