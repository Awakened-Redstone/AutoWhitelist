package com.awakenedredstone.autowhitelist.entry.implementation;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.entry.BaseEntryAction;
import com.awakenedredstone.autowhitelist.util.Stonecutter;
import com.awakenedredstone.autowhitelist.util.Texts;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedPlayerProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;

public class CommandEntryAction extends BaseEntryAction {
    public static final Identifier ID = AutoWhitelist.id("execute_command");
    public static final MapCodec<CommandEntryAction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.STRING.listOf().fieldOf("roles").forGetter(BaseEntryAction::getRoles),
        Identifier.CODEC.fieldOf("type").forGetter(BaseEntryAction::getType),
        Keys.CODEC.fieldOf("execute").forGetter(command -> new Keys(command.addCommand, command.removeCommand))
      ).apply(instance, CommandEntryAction::new)
    );
    private final String addCommand;
    private final String removeCommand;

    protected CommandEntryAction(List<String> roles, Identifier type, Keys executeKeys) {
        super(type, roles);
        this.addCommand = executeKeys.onAdd();
        this.removeCommand = executeKeys.onRemove();
    }

    @Override
    public void registerUser(ExtendedPlayerProfile profile) {
        if (StringUtils.isBlank(addCommand)) return;
        AutoWhitelist.getServer().getCommandManager().executeWithPrefix(AutoWhitelist.getCommandSource(), Texts.playerPlaceholder(addCommand, Stonecutter.profileName(profile)).getString());
    }

    @Override
    public void removeUser(ExtendedPlayerProfile profile) {
        if (StringUtils.isBlank(removeCommand)) return;
        AutoWhitelist.getServer().getCommandManager().executeWithPrefix(AutoWhitelist.getCommandSource(), Texts.playerPlaceholder(removeCommand, Stonecutter.profileName(profile)).getString());
    }

    @Override
    public boolean isValid() {
        return checkCommand(addCommand.split(" ", 2)[0]) &&
               checkCommand(removeCommand.split(" ", 2)[0]);
    }

    private boolean checkCommand(String command) {
        var root = AutoWhitelist.getServer().getCommandManager().getDispatcher().getRoot();
        var child = root.getChild(command);
        if (child == null && StringUtils.isNotBlank(command)) {
            if (command.startsWith("/")) {
                LOGGER.warn("You don't need a slash at the start of the command, found on command \"{}\"", command);
            }
            LOGGER.error("The command \"{}\" does not exist!", command);
            return false;
        } else if (child != null && !child.canUse(AutoWhitelist.getCommandSource())) {
            LOGGER.error("AutoWhitelist does not have enough permission to execute the command \"{}\"!", command);
            return false;
        }

        return true;
    }

    protected record Keys(String onAdd, String onRemove) {
        public static final Codec<Keys> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("on_add").forGetter(Keys::onAdd),
            Codec.STRING.fieldOf("on_remove").forGetter(Keys::onRemove)
          ).apply(instance, Keys::new)
        );
    }

    @Override
    public String toString() {
        return "CommandEntry{" +
               "addCommand='" + addCommand + '\'' +
               ", removeCommand='" + removeCommand + '\'' +
               '}';
    }

    @Override
    public boolean equals(BaseEntryAction otherEntry) {
        CommandEntryAction other = (CommandEntryAction) otherEntry;
        return Objects.equals(addCommand, other.addCommand) && Objects.equals(removeCommand, other.removeCommand);
    }
}
