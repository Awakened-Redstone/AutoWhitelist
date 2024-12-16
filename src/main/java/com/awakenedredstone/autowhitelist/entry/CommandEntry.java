package com.awakenedredstone.autowhitelist.entry;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.util.DynamicPlaceholders;
import com.awakenedredstone.autowhitelist.util.Stonecutter;
import com.awakenedredstone.autowhitelist.util.Texts;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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
        AutoWhitelist.getServer().getCommandManager()./*? if >=1.19 {*/executeWithPrefix/*?} else {*//*execute*//*?}*/(AutoWhitelist.getCommandSource(), Texts.playerPlaceholder(addCommand, profile.getName()).getString());
    }

    @Override
    public <T extends GameProfile> void removeUser(T profile) {
        AutoWhitelist.getServer().getCommandManager()./*? if >=1.19 {*/executeWithPrefix/*?} else {*//*execute*//*?}*/(AutoWhitelist.getCommandSource(), Texts.playerPlaceholder(removeCommand, profile.getName()).getString());
    }

    @Override
    public <T extends GameProfile> boolean shouldUpdate(T profile) {
        return false;
    }

    @Override
    public void assertSafe() {
        var root = AutoWhitelist.getServer().getCommandManager().getDispatcher().getRoot();
        String addCmdStart = addCommand.split(" ", 2)[0];
        if (root.getChild(addCmdStart) == null && !StringUtils.isBlank(addCmdStart)) {
            if (addCmdStart.startsWith("/")) {
                AutoWhitelist.LOGGER.warn("You don't need a slash at the start of the command, found on {}", addCmdStart);
            }
            throw new AssertionError(String.format("Add command \"%s\" does not exist!", addCmdStart));
        }
        String removeCmdStart = addCommand.split(" ", 2)[0];
        if (root.getChild(removeCmdStart) == null && !StringUtils.isBlank(removeCmdStart)) {
            if (removeCmdStart.startsWith("/")) {
                AutoWhitelist.LOGGER.warn("You don't need a slash at the start of the command, found on {}", removeCmdStart);
            }
            throw new AssertionError(String.format("Remove command \"%s\" does not exist!", removeCmdStart));
        }
    }

    @Override
    public void purgeInvalid() {/**/}

    protected record Keys(String onAdd, String onRemove) {
        public static final Codec<Keys> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("on_add").forGetter(Keys::onAdd),
            Codec.STRING.fieldOf("on_remove").forGetter(Keys::onRemove)
          ).apply(instance, Keys::new)
        );
    }
}
