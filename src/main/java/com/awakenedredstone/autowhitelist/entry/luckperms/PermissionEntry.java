package com.awakenedredstone.autowhitelist.entry.luckperms;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.entry.BaseEntry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.PermissionNode;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class PermissionEntry extends LuckpermsEntry {
    public static final Identifier ID = AutoWhitelist.id("luckperms/permission");
    public static final MapCodec<PermissionEntry> CODEC = RecordCodecBuilder.mapCodec(instance ->
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
    public void assertSafe() {
        if (StringUtils.isBlank(permission)) {
            throw new IllegalArgumentException("PermissionEntry can not be blank!");
        }
    }

    @Override
    public void purgeInvalid() {/**/}
}
