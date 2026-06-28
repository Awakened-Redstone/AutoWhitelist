package com.awakenedredstone.autowhitelist.entry;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.entry.api.ActionType;
import com.awakenedredstone.autowhitelist.entry.api.EntryAction;
import com.awakenedredstone.autowhitelist.entry.api.ActionFields;
import com.awakenedredstone.autowhitelist.server.ServerDetails;
import com.awakenedredstone.autowhitelist.server.profile.PlayerProfile;
import com.awakenedredstone.autowhitelist.util.string.Texts;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.commands.CommandSourceStack;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class CommandEntryAction extends EntryAction<CommandEntryAction.Fields> {
    protected CommandEntryAction(ActionType<Fields> type, CommandEntryAction.Fields fields) {
        super(type, fields);
    }

    private CommandSourceStack commandSource() {
        return ServerDetails.getCurrent().createCommandSource("AutoWhitelist", AutoWhitelist.config().whitelist.commandPermissionLevel);
    }

    @Override
    public void onAdd(PlayerProfile profile) {
        for (String command : fields.onAdd) {
            if (StringUtils.isBlank(command)) continue;
            ServerDetails.getServer().getCommands().performPrefixedCommand(commandSource(), Texts.playerPlaceholder(command, profile).getString());
        }
    }

    @Override
    public void onRemove(PlayerProfile profile) {
        for (String command : fields.onRemove) {
            if (StringUtils.isBlank(command)) continue;
            ServerDetails.getServer().getCommands().performPrefixedCommand(commandSource(), Texts.playerPlaceholder(command, profile).getString());
        }
    }

    @Override
    public boolean validate(boolean early) {
        return checkCommands(fields.onAdd, early) && checkCommands(fields.onRemove, early);
    }

    private boolean checkCommands(List<String> commands, boolean early) {
        for (String command : commands) {
            command = command.split(" ", 2)[0];
            if (StringUtils.isNotBlank(command)) return true;

            var root = ServerDetails.getServer().getCommands().getDispatcher().getRoot();
            var child = root.getChild(command);
            if (child == null) {
                if (command.startsWith("/")) {
                    logger.warn("You don't need a slash at the start of the command, found on {}", command);
                }
                logger.error("The command [{}] does not exist!", command);
                return false;
            } else {
                var permissionLevel = early ? AutoWhitelist.earlyConfigPermissionLevel : AutoWhitelist.config().whitelist.commandPermissionLevel;
                if (!child.canUse(ServerDetails.getCurrent().createCommandSource("AutoWhitelist", permissionLevel))) {
                    logger.error("AutoWhitelist does not have enough permission to execute the command {}", command);
                    return false;
                }
            }
        }

        return true;
    }

    public record Fields(List<String> onAdd, List<String> onRemove) implements ActionFields {
        private static final Codec<List<String>> COMMANDS_CODEC = Codec.withAlternative(Codec.STRING.listOf(), Codec.STRING.flatXmap(string -> DataResult.success(List.of(string)), list -> DataResult.success(list.getFirst())));

        public static final Codec<Fields> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            COMMANDS_CODEC.fieldOf("on_add").forGetter(Fields::onAdd),
            COMMANDS_CODEC.fieldOf("on_remove").forGetter(Fields::onRemove)
          ).apply(instance, Fields::new)
        );
    }
}
