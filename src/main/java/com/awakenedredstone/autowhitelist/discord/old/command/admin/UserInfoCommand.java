package com.awakenedredstone.autowhitelist.discord.old.command.admin;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.discord.old.DiscordBotHelper;
import com.awakenedredstone.autowhitelist.discord.old.api.ReplyCallback;
import com.awakenedredstone.autowhitelist.discord.old.command.RegisterCommand;
import com.awakenedredstone.autowhitelist.discord.old.command.AbstractSlashCommand;
import com.awakenedredstone.autowhitelist.util.Stonecutter;
import com.awakenedredstone.autowhitelist.util.Texts;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedPlayerProfile;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelist;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelistEntry;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Optional;

public class UserInfoCommand extends AbstractSlashCommand {
    public UserInfoCommand() {
        super("userinfo", "admin");

        this.userPermissions = new Permission[]{Permission.MANAGE_ROLES};

        this.options.add(new OptionData(OptionType.USER, "user", argumentText("user")).setRequired(false));
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        var replyCallback = new ReplyCallback.DefaultInteractionReplyCallback(event);

        replyCallback.sendMessage(null);

        ExtendedWhitelist whitelist = (ExtendedWhitelist) AutoWhitelist.getServer().getPlayerManager().getWhitelist();
        Member member = event.getOption("user", event.getMember(), OptionMapping::getAsMember);

        //noinspection DuplicatedCode
        if (member == null) {
            AutoWhitelist.LOGGER.error("Member is null", new IllegalStateException());
            MessageEmbed embed = DiscordBotHelper.Feedback.buildEmbed(
              Text.translatable("discord.command.fatal.title"),
              Text.translatable("discord.command.fatal.generic", "Member is null"),
              DiscordBotHelper.MessageType.FATAL
            );

            replyCallback.editMessage(DiscordBotHelper.buildEmbedMessage(true, embed));

            return;
        }

        Optional<ExtendedWhitelistEntry> whitelistedAccount = RegisterCommand.getWhitelistedAccount(member.getId(), whitelist);

        List<Role> roles = DiscordBotHelper.getRolesForMember(member);
        List<Role> validRoles = DiscordBotHelper.getValidRolesForMember(member);
        boolean accepted = DiscordBotHelper.getHighestEntryRole(roles).isPresent();

        EmbedBuilder embed = DiscordBotHelper.Feedback.defaultEmbed(
          Text.translatable("discord.command.userinfo.title"),
          Text.translatable("discord.command.userinfo.description", member.getAsMention())
        );

        embed.addField(Texts.translated("discord.command.userinfo.roles"), String.join(" ", roles.stream().map(IMentionable::getAsMention).toList()), false);
        embed.addField(
          Texts.translated("discord.command.userinfo.valid_roles"),
          validRoles.isEmpty() ? "None" : String.join(" ", validRoles.stream().map(IMentionable::getAsMention).toList()),
          false
        );
        embed.addField(Texts.translated("discord.command.userinfo.whitelisted"), String.valueOf(whitelistedAccount.isPresent()), true);
        embed.addField(Texts.translated("discord.command.userinfo.qualifies"), String.valueOf(accepted), true);

        if (whitelistedAccount.isPresent()) {
            ExtendedWhitelistEntry entry = whitelistedAccount.get();
            ExtendedPlayerProfile profile = entry.getProfile();

            //noinspection DuplicatedCode
            String[] fields = new String[]{"username", "role", "lock"};
            for (String field : fields) {
                String title = Texts.translated("discord.command.info.field.%s.title".formatted(field));
                if (title.isEmpty()) continue;

                String descriptionKey = "discord.command.info.field.%s.description".formatted(field);
                String description = switch (field) {
                    case "username" -> Texts.translated(descriptionKey, Stonecutter.profileName(profile));
                    case "role" -> Texts.translated(descriptionKey, "<@&" + profile.getRole() + ">");
                    case "lock" -> {
                        String time = "future";
                        if (profile.getLockedUntil() == -1) {
                            time = "permanent";
                        } else if (profile.getLockedUntil() <= System.currentTimeMillis()) {
                            time = "past";
                        }
                        String timeKey = "." + time;
                        yield Texts.translated(descriptionKey + timeKey, DiscordBotHelper.formatDiscordTimestamp(profile.getLockedUntil()));
                    }
                    default -> "";
                };
                if (description.isEmpty()) continue;

                embed.addField(title, description, true);
            }
        }

        replyCallback.submitEdit((InteractionHook interactionHook) -> interactionHook
          .editOriginal(DiscordBotHelper.<MessageEditData>buildEmbedMessage(true, embed.build()))
        );
    }
}
