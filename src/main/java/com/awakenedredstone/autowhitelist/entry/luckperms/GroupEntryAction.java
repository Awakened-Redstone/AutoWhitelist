package com.awakenedredstone.autowhitelist.entry.luckperms;

import com.awakenedredstone.autowhitelist.entry.api.ActionType;
import com.awakenedredstone.autowhitelist.entry.api.ActionFields;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import org.apache.commons.lang3.StringUtils;

public class GroupEntryAction extends LuckpermsEntryAction<GroupEntryAction.Fields> {
    public GroupEntryAction(ActionType<Fields> type, Fields fields) {
        super(type, fields, GroupEntryAction::new);
    }

    @Override
    protected Node getNode() {
        return InheritanceNode.builder(fields.group).build();
    }

    @Override
    public boolean validate(boolean early) {
        if (StringUtils.isBlank(fields.group)) {
            logger.error("Group can not be blank!");
            return false;
        }

        return true;
    }

    public record Fields(String group) implements ActionFields {
        public static final Codec<Fields> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("group").forGetter(Fields::group)
          ).apply(instance, Fields::new)
        );
    }
}
