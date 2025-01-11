package com.awakenedredstone.autowhitelist.entry;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.util.Stonecutter;
import com.awakenedredstone.autowhitelist.util.Texts;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;

public class CommandEntry extends BaseEntry {
    public static final Identifier ID = AutoWhitelist.id("execute_command");
    public static final /*? if <1.20.5 {*//*Codec*//*?} else {*/MapCodec/*?}*/<CommandEntry> CODEC = Stonecutter.entryCodec(instance -> instance.group(
        Codec.STRING.listOf().fieldOf("roles").forGetter(BaseEntry::getRoles),
        Identifier.CODEC.fieldOf("type").forGetter(BaseEntry::getType),
        Keys.CODEC.fieldOf("execute").forGetter(command -> new Keys(command.addCommand, command.removeCommand))
      ).apply(instance, CommandEntry::new)
    );
    private final String addCommand;
    private final String removeCommand;

    protected CommandEntry(List<String> roles, Identifier type, Keys executeKeys) {
        super(type, roles);
        this.addCommand = executeKeys.onAdd();
        this.removeCommand = executeKeys.onRemove();
    }

    @Override
    public <T extends GameProfile> void registerUser(T profile) {
        if (StringUtils.isBlank(addCommand)) return;
        AutoWhitelist.getServer().getCommandManager().executeWithPrefix(AutoWhitelist.getCommandSource(), Texts.playerPlaceholder(addCommand, profile.getName()).getString());
    }

    @Override
    public <T extends GameProfile> void removeUser(T profile) {
        if (StringUtils.isBlank(removeCommand)) return;
        AutoWhitelist.getServer().getCommandManager().executeWithPrefix(AutoWhitelist.getCommandSource(), Texts.playerPlaceholder(removeCommand, profile.getName()).getString());
    }

    @Override
    public void assertValid() {
        var root = AutoWhitelist.getServer().getCommandManager().getDispatcher().getRoot();
        String addCmdStart = addCommand.split(" ", 2)[0];
        if (root.getChild(addCmdStart) == null && !StringUtils.isBlank(addCmdStart)) {
            if (addCmdStart.startsWith("/")) {
                AutoWhitelist.LOGGER.warn("You don't need a slash at the start of the command, found on add command \"{}\"", addCmdStart);
            }
            throw new AssertionError(String.format("The command \"%s\" does not exist!", addCmdStart));
        }
        checkCommand(removeCommand.split(" ", 2)[0]);
    }

    private void checkCommand(String command) {
        var root = AutoWhitelist.getServer().getCommandManager().getDispatcher().getRoot();
        var child = root.getChild(command);
        if (child == null && StringUtils.isNotBlank(command)) {
            if (command.startsWith("/")) {
                AutoWhitelist.LOGGER.warn("You don't need a slash at the start of the command, found on command \"{}\"", command);
            }
            throw new AssertionError(String.format("The command \"%s\" does not exist!", command));
        } else if (child != null && !child.canUse(AutoWhitelist.getCommandSource())) {
            throw new AssertionError(String.format("AutoWhitelist does not have enough permission to execute the command \"%s\"!", command));
        }
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
    public boolean equals(BaseEntry otherEntry) {
        CommandEntry other = (CommandEntry) otherEntry;
        return Objects.equals(addCommand, other.addCommand) && Objects.equals(removeCommand, other.removeCommand);
    }
}
