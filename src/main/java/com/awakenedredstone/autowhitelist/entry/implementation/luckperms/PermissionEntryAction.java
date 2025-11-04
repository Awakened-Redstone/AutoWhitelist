package com.awakenedredstone.autowhitelist.entry.implementation.luckperms;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.entry.BaseEntryAction;
import com.awakenedredstone.autowhitelist.util.Stonecutter;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.PermissionNode;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;

public class PermissionEntryAction extends LuckpermsEntryAction {
    public static final Identifier ID = AutoWhitelist.id("luckperms/permission");
    public static final MapCodec<PermissionEntryAction> CODEC = RecordCodecBuilder.mapCodec(instance ->
      instance.group(
        Codec.STRING.listOf().fieldOf("roles").forGetter(BaseEntryAction::getRoles),
        Identifier.CODEC.fieldOf("type").forGetter(BaseEntryAction::getType),
        Codec.STRING.fieldOf("permission").codec().fieldOf("execute").forGetter(permission -> permission.permission)
      ).apply(instance, PermissionEntryAction::new)
    );
    private final String permission;

    public PermissionEntryAction(List<String> roles, Identifier type, String permission) {
        super(type, roles);
        this.permission = permission;
    }

    @Override
    protected Node getNode() {
        return PermissionNode.builder(permission).build();
    }

    @Override
    public boolean isValid() {
        if (StringUtils.isBlank(permission)) {
            LOGGER.error("PermissionEntry can not be blank!");
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "PermissionEntry{" +
               "permission='" + permission + '\'' +
               '}';
    }

    @Override
    public boolean equals(BaseEntryAction otherEntry) {
        PermissionEntryAction other = (PermissionEntryAction) otherEntry;
        return Objects.equals(permission, other.permission);
    }
}
