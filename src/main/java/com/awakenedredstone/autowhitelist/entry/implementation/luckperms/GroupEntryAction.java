package com.awakenedredstone.autowhitelist.entry.implementation.luckperms;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.entry.BaseEntryAction;
import com.awakenedredstone.autowhitelist.util.Stonecutter;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;

public class GroupEntryAction extends LuckpermsEntryAction {
    public static final Identifier ID = AutoWhitelist.id("luckperms/group");
    public static final /*? if <1.20.5 {*//*Codec*//*?} else {*/MapCodec/*?}*/<GroupEntryAction> CODEC = Stonecutter.entryCodec(instance ->
      instance.group(
        Codec.STRING.listOf().fieldOf("roles").forGetter(BaseEntryAction::getRoles),
        Identifier.CODEC.fieldOf("type").forGetter(BaseEntryAction::getType),
        Codec.STRING.fieldOf("group").codec().fieldOf("execute").forGetter(group -> group.group)
      ).apply(instance, GroupEntryAction::new)
    );
    private final String group;

    public GroupEntryAction(List<String> roles, Identifier type, String group) {
        super(type, roles);
        this.group = group;
    }

    @Override
    protected Node getNode() {
        return InheritanceNode.builder(group).build();
    }

    @Override
    public boolean isValid() {
        if (StringUtils.isBlank(group)) {
            LOGGER.error("Group can not be blank!");
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "GroupEntry{" +
               "group='" + group + '\'' +
               '}';
    }

    @Override
    public boolean equals(BaseEntryAction otherEntry) {
        GroupEntryAction other = (GroupEntryAction) otherEntry;
        return Objects.equals(group, other.group);
    }
}
