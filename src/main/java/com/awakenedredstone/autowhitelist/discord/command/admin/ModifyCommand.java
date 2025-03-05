package com.awakenedredstone.autowhitelist.discord.command.admin;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.LazyConstants;
import com.awakenedredstone.autowhitelist.debug.DebugFlags;
import com.awakenedredstone.autowhitelist.discord.DiscordBotHelper;
import com.awakenedredstone.autowhitelist.discord.api.ReplyCallback;
import com.awakenedredstone.autowhitelist.discord.command.RegisterCommand;
import com.awakenedredstone.autowhitelist.discord.command.SimpleSlashCommand;
import com.awakenedredstone.autowhitelist.entry.BaseEntryAction;
import com.awakenedredstone.autowhitelist.entry.RoleActionMap;
import com.awakenedredstone.autowhitelist.mixin.UserCacheAccessor;
import com.awakenedredstone.autowhitelist.networking.GeyserProfileRepository;
import com.awakenedredstone.autowhitelist.util.Validation;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedGameProfile;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelist;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelistEntry;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
/*? if <1.20.2 {*//*import com.mojang.authlib.Agent;*//*?}*/
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.yggdrasil.ProfileNotFoundException;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class ModifyCommand extends SimpleSlashCommand {
    public ModifyCommand() {
        super("modify", "admin");

        this.userPermissions = new Permission[]{Permission.MANAGE_ROLES};

        this.options.add(new OptionData(OptionType.USER, "user", argumentText("user")).setRequired(true));
        this.options.add(new OptionData(OptionType.STRING, "username", argumentText("username")).setRequired(true));
        if (LazyConstants.isUsingGeyser()) {
            this.options.add(
              new OptionData(OptionType.STRING, "account_type", argumentText("geyser"))
                .addChoices(
                  new Command.Choice(choice("geyser", "java"), "java"),
                  new Command.Choice(choice("geyser", "bedrock"), "bedrock")
                ).setRequired(false)
            );
        }
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        var replyCallback = new ReplyCallback.DefaultInteractionReplyCallback(event);

        MessageCreateData initialReply = DiscordBotHelper.buildEmbedMessage(
          false,
          DiscordBotHelper.Feedback.buildEmbed(
            Text.translatable("discord.command.feedback.received.title"),
            Text.translatable("discord.command.feedback.received.message"),
            DiscordBotHelper.MessageType.NORMAL
          )
        );

        replyCallback.sendMessage(initialReply);

        @NotNull Member member = Objects.requireNonNull(event.getOption("user", OptionMapping::getAsMember));
        @NotNull String username = Objects.requireNonNull(event.getOption("username", OptionMapping::getAsString));
        boolean geyser = event.getOption("account_type", "java", OptionMapping::getAsString).equalsIgnoreCase("bedrock");

        UUID uuid;

        try {
            uuid = UUID.fromString(username);
        } catch (IllegalArgumentException e) {
            uuid = null;
        }

        String id = member.getId();

        Optional<Role> highestRole = DiscordBotHelper.getHighestEntryRole(member);

        if (highestRole.isEmpty()) {
            replyCallback.editMessage(
              DiscordBotHelper.buildEmbedMessage(true,
                DiscordBotHelper.Feedback.buildEmbed(
                  Text.translatable("discord.command.modify.fail.not_allowed.title"),
                  Text.translatable("discord.command.modify.fail.not_allowed.message"),
                  DiscordBotHelper.MessageType.NORMAL
                )
              )
            );

            return;
        }

        MinecraftServer server = AutoWhitelist.getServer();
        ExtendedWhitelist whitelist = (ExtendedWhitelist) server.getPlayerManager().getWhitelist();

        Optional<ExtendedWhitelistEntry> whitelistedAccount = RegisterCommand.getWhitelistedAccount(id, whitelist);
        if (whitelistedAccount.isPresent()) {
            boolean sameAccount;
            if (uuid != null) {
                sameAccount = whitelistedAccount.get().getProfile().getId().equals(uuid);
            } else {
                sameAccount = whitelistedAccount.get().getProfile().getName().equalsIgnoreCase(username);
            }

            if (sameAccount) {
                replyCallback.editMessage(
                  DiscordBotHelper.buildEmbedMessage(true,
                    DiscordBotHelper.Feedback.buildEmbed(
                      Text.translatable("discord.command.modify.same_username.title"),
                      Text.translatable("discord.command.modify.same_username.message"),
                      DiscordBotHelper.MessageType.WARNING
                    )
                  )
                );
                return;
            }
        }

        BaseEntryAction entry = RoleActionMap.get(highestRole.get());
        if (!entry.isValid()) {
            replyCallback.editMessage(
              DiscordBotHelper.buildEmbedMessage(true,
                DiscordBotHelper.Feedback.buildEmbed(
                  Text.translatable("discord.command.fail.title"),
                  Text.translatable("discord.command.fatal.generic", "Failed to validate entry action"),
                  DiscordBotHelper.MessageType.FATAL
                )
              )
            );
            AutoWhitelist.LOGGER.error("Failed to whitelist user, tried to validate entry {} but failed", entry);
            return;
        }

        if (uuid == null && !geyser && !Validation.isValidMinecraftUsername(username)) {
            replyCallback.editMessage(
              DiscordBotHelper.buildEmbedMessage(true,
                DiscordBotHelper.Feedback.buildEmbed(
                  Text.translatable("discord.command.modify.invalid_username.title"),
                  Text.translatable("discord.command.modify.invalid_username.message"),
                  DiscordBotHelper.MessageType.WARNING
                )
              )
            );
            return;
        }

        if (server.getUserCache() == null) {
            AutoWhitelist.LOGGER.error("Failed to whitelist user {}, server user cache is null", username);
            replyCallback.editMessage(
              DiscordBotHelper.buildEmbedMessage(true,
                DiscordBotHelper.Feedback.buildEmbed(
                  Text.translatable("discord.command.fail.title"),
                  Text.translatable("discord.command.modify.fatal", "Failed to whitelist user %s, server user cache is null".formatted(username)),
                  DiscordBotHelper.MessageType.ERROR
                )
              )
            );
            return;
        }

        //noinspection DuplicatedCode
        if (DebugFlags.testMojangApiOnRegister && uuid != null && !geyser) {
            ProfileLookupCallback profileLookupCallback = new ProfileLookupCallback(){

                @Override
                public void onProfileLookupSucceeded(GameProfile profile) {
                    AutoWhitelist.LOGGER.info("Successfully got user profile {}" ,profile);
                }

                @Override
                public void onProfileLookupFailed(/*? if >=1.20.2 {*/String/*?} else {*//*GameProfile*//*?}*/ name, Exception exception) {
                    AutoWhitelist.LOGGER.error("Failed to get user profile for {}", name, exception);
                }
            };

            ((UserCacheAccessor) server.getUserCache()).getProfileRepository().findProfilesByNames(new String[]{username}/*? if <1.20.2 {*//*, Agent.MINECRAFT*//*?}*/, profileLookupCallback);
        }

        GameProfile profile;

        if (uuid != null) {
            if (LazyConstants.isUsingGeyser()) {
                if (uuid.getMostSignificantBits() == 0 || geyser) {

                    profile = new GameProfile(uuid, "Bedrock Player");
                } else {
                    profile = server.getUserCache().getByUuid(uuid).orElse(null);
                }
            } else {
                profile = server.getUserCache().getByUuid(uuid).orElse(null);
            }
        } else {
            if (LazyConstants.isUsingGeyser() && geyser) {
                final AtomicReference<GameProfile> atomicProfile = new AtomicReference<>();
                final AtomicReference<Exception> atomicException = new AtomicReference<>();
                ProfileLookupCallback profileLookupCallback = new ProfileLookupCallback() {

                    @Override
                    public void onProfileLookupSucceeded(GameProfile profile) {
                        atomicProfile.set(profile);
                    }

                    @Override
                    public void onProfileLookupFailed(/*? if >=1.20.2 {*/String/*?} else {*//*GameProfile*//*?}*/ name, Exception exception) {
                        if (!(exception instanceof ProfileNotFoundException)) {
                            atomicException.set(exception);
                        }
                        atomicProfile.set(null);
                    }
                };

                GeyserProfileRepository repository = LazyConstants.getGeyserProfileRepository();

                repository.findProfilesByNames(new String[]{username}/*? if <1.20.2 {*//*, Agent.MINECRAFT*//*?}*/, profileLookupCallback);

                if (atomicException.get() != null) {
                    Exception exception = atomicException.get();
                    AutoWhitelist.LOGGER.error("Failed to get Bedrock profile due to an exception", exception);
                    replyCallback.editMessage(
                      DiscordBotHelper.buildEmbedMessage(true,
                        DiscordBotHelper.Feedback.buildEmbed(
                          Text.translatable("discord.command.fail.title"),
                          Text.translatable("discord.command.fatal.exception", exception),
                          DiscordBotHelper.MessageType.FATAL
                        )
                      )
                    );
                    return;
                }

                if (atomicProfile.get() == null) {
                    replyCallback.editMessage(
                      DiscordBotHelper.buildEmbedMessage(true,
                        DiscordBotHelper.Feedback.buildEmbed(
                          Text.translatable("discord.command.modify.unknown_bedrock_profile.title"),
                          Text.translatable("discord.command.modify.unknown_bedrock_profile.message"),
                          DiscordBotHelper.MessageType.WARNING
                        )
                      )
                    );
                    return;
                } else {
                    profile = atomicProfile.get();
                }
            } else {
                profile = server.getUserCache().findByName(username).orElse(null);
            }
        }

        if (profile == null) {
            replyCallback.editMessage(
              DiscordBotHelper.buildEmbedMessage(true,
                DiscordBotHelper.Feedback.buildEmbed(
                  Text.translatable("discord.command.fail.title"),
                  Text.translatable("discord.command.modify.fail.account_data", username),
                  DiscordBotHelper.MessageType.ERROR
                )
              )
            );
            return;
        }

        if (AutoWhitelist.getServer().getPlayerManager().getUserBanList().contains(profile)) {
            replyCallback.editMessage(
              DiscordBotHelper.buildEmbedMessage(true,
                DiscordBotHelper.Feedback.buildEmbed(
                  Text.translatable("discord.command.modify.player_banned.title"),
                  Text.translatable("discord.command.modify.player_banned.message"),
                  DiscordBotHelper.MessageType.ERROR
                )
              )
            );
            return;
        }

        ExtendedGameProfile extendedProfile = new ExtendedGameProfile(profile.getId(), profile.getName(), highestRole.get().getId(), id, AutoWhitelist.CONFIG.lockTime());

        boolean whitelisted = whitelist.isAllowed(extendedProfile);
        if (whitelisted) {
            replyCallback.editMessage(
              DiscordBotHelper.buildEmbedMessage(true,
                DiscordBotHelper.Feedback.buildEmbed(
                  Text.translatable("discord.command.modify.username_already_registered.title"),
                  Text.translatable("discord.command.modify.username_already_registered.message"),
                  DiscordBotHelper.MessageType.ERROR
                )
              )
            );
        } else {
            replyCallback.editMessage(
              DiscordBotHelper.buildEmbedMessage(true,
                DiscordBotHelper.Feedback.buildEmbed(
                  Text.translatable("discord.command.modify.last_steps.title"),
                  Text.translatable("discord.command.modify.last_steps.message"),
                  DiscordBotHelper.MessageType.INFO
                )
              )
            );

            RegisterCommand.whitelistPlayer(whitelistedAccount.orElse(null), whitelist, extendedProfile, entry);

            replyCallback.editMessage(
              DiscordBotHelper.buildEmbedMessage(true,
                DiscordBotHelper.Feedback.buildEmbed(
                  Text.translatable("discord.command.modify.success.title", member.getEffectiveName()),
                  Text.translatable("discord.command.modify.success.message", member.getAsMention(), username),
                  DiscordBotHelper.MessageType.SUCCESS
                )
              )
            );
        }
    }
}
