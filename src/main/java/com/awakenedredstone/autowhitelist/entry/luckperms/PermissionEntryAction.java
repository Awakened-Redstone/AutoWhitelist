package com.awakenedredstone.autowhitelist.entry.luckperms;

import com.awakenedredstone.autowhitelist.entry.api.ActionType;
import com.awakenedredstone.autowhitelist.entry.api.ActionFields;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.PermissionNode;
import org.apache.commons.lang3.StringUtils;

public class PermissionEntryAction extends LuckpermsEntryAction<PermissionEntryAction.Fields> {
    public PermissionEntryAction(ActionType<Fields> type, Fields fields) {
        super(type, fields, PermissionEntryAction::new);
    }

    @Override
    protected Node getNode() {
        return PermissionNode.builder(fields.permission).build();
    }

    @Override
    public boolean validate(boolean early) {
        if (StringUtils.isBlank(fields.permission)) {
            logger.error("Permission can not be blank!");
            return false;
        }

        return true;
    }

    public record Fields(String permission) implements ActionFields {
        public static final Codec<Fields> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("permission").forGetter(Fields::permission)
          ).apply(instance, Fields::new)
        );
    }
}
