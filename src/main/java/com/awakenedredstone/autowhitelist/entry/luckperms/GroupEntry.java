package com.awakenedredstone.autowhitelist.entry.luckperms;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.entry.BaseEntry;
import com.awakenedredstone.autowhitelist.util.Stonecutter;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;

public class GroupEntry extends LuckpermsEntry {
    public static final Identifier ID = AutoWhitelist.id("luckperms/group");
    public static final /*? if <1.20.5 {*//*Codec*//*?} else {*/MapCodec/*?}*/<GroupEntry> CODEC = Stonecutter.entryCodec(instance ->
      instance.group(
        Codec.STRING.listOf().fieldOf("roles").forGetter(BaseEntry::getRoles),
        Identifier.CODEC.fieldOf("type").forGetter(BaseEntry::getType),
        Codec.STRING.fieldOf("group").codec().fieldOf("execute").forGetter(group -> group.group)
      ).apply(instance, GroupEntry::new)
    );
    private final String group;

    public GroupEntry(List<String> roles, Identifier type, String group) {
        super(type, roles);
        this.group = group;
    }

    @Override
    protected Node getNode() {
        return InheritanceNode.builder(group).build();
    }

    @Override
    public void assertValid() {
        if (StringUtils.isBlank(group)) {
            throw new IllegalArgumentException("Group can not be blank!");
        }
    }

    @Override
    public String toString() {
        return "GroupEntry{" +
               "group='" + group + '\'' +
               '}';
    }

    @Override
    public boolean equals(BaseEntry otherEntry) {
        GroupEntry other = (GroupEntry) otherEntry;
        return Objects.equals(group, other.group);
    }
}
