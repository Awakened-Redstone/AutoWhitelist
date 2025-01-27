package com.awakenedredstone.autowhitelist.entry.luckperms;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.entry.BaseEntry;
import com.awakenedredstone.autowhitelist.util.Stonecutter;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.PermissionNode;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;

public class PermissionEntry extends LuckpermsEntry {
    public static final Identifier ID = AutoWhitelist.id("luckperms/permission");
    public static final /*? if <1.20.5 {*//*Codec*//*?} else {*/MapCodec/*?}*/<PermissionEntry> CODEC = Stonecutter.entryCodec(instance ->
      instance.group(
        Codec.STRING.listOf().fieldOf("roles").forGetter(BaseEntry::getRoles),
        Identifier.CODEC.fieldOf("type").forGetter(BaseEntry::getType),
        Codec.STRING.fieldOf("permission").codec().fieldOf("execute").forGetter(permission -> permission.permission)
      ).apply(instance, PermissionEntry::new)
    );
    private final String permission;

    public PermissionEntry(List<String> roles, Identifier type, String permission) {
        super(type, roles);
        this.permission = permission;
    }

    @Override
    protected Node getNode() {
        return PermissionNode.builder(permission).build();
    }

    @Override
    public void assertValid() {
        if (StringUtils.isBlank(permission)) {
            throw new IllegalArgumentException("PermissionEntry can not be blank!");
        }
    }

    @Override
    public String toString() {
        return "PermissionEntry{" +
               "permission='" + permission + '\'' +
               '}';
    }

    @Override
    public boolean equals(BaseEntry otherEntry) {
        PermissionEntry other = (PermissionEntry) otherEntry;
        return Objects.equals(permission, other.permission);
    }
}
