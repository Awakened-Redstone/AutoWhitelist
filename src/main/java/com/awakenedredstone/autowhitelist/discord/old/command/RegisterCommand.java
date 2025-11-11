package com.awakenedredstone.autowhitelist.discord.old.command;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.LazyConstants;
import com.awakenedredstone.autowhitelist.entry.BaseEntryAction;
import com.awakenedredstone.autowhitelist.discord.old.DiscordBotHelper;
import com.awakenedredstone.autowhitelist.discord.old.api.ReplyCallback;
import com.awakenedredstone.autowhitelist.entry.RoleActionMap;
import com.awakenedredstone.autowhitelist.networking.GeyserProfileRepository;
import com.awakenedredstone.autowhitelist.stonecutter.Stonecutter;
import com.awakenedredstone.autowhitelist.util.Validation;
import com.awakenedredstone.autowhitelist.whitelist.override.LinkedPlayerProfile;
import com.awakenedredstone.autowhitelist.whitelist.override.LinkingWhitelist;
import com.awakenedredstone.autowhitelist.whitelist.override.LinkedWhitelistEntry;
import com.awakenedredstone.autowhitelist.whitelist.cache.WhitelistCacheEntry;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.yggdrasil.ProfileNotFoundException;
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

public class RegisterCommand extends AbstractSlashCommand {
    public RegisterCommand() {
        super("register");

        this.options.add(new OptionData(OptionType.STRING, "username", argumentText("username")).setMinLength(1).setMaxLength(36).setRequired(true));
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

    public static void execute(@NotNull Member member, @NotNull String username, boolean geyser, @NotNull ReplyCallback.InteractionReplyCallback replyCallback) {
        MessageCreateData initialReply = DiscordBotHelper.buildEmbedMessage(
          false,
          DiscordBotHelper.Feedback.buildEmbed(
            Text.translatable("discord.command.feedback.received.title"),
            Text.translatable("discord.command.feedback.received.message"),
            DiscordBotHelper.MessageType.NORMAL
          )
        );

        UUID uuid;

        try {
            uuid = UUID.fromString(username);
        } catch (IllegalArgumentException e) {
            uuid = null;
        }

        replyCallback.sendMessage(initialReply);

        String discordId = member.getId();

        Optional<Role> highestRole = DiscordBotHelper.getHighestEntryRole(member);
        if (highestRole.isEmpty()) {
            replyCallback.editMessage(
              DiscordBotHelper.buildEmbedMessage(true,
                DiscordBotHelper.Feedback.buildEmbed(
                  Text.translatable("discord.command.register.fail.not_allowed.title"),
                  Text.translatable("discord.command.register.fail.not_allowed.message"),
                  DiscordBotHelper.MessageType.NORMAL
                )
              )
            );

            return;
        }

        MinecraftServer server = AutoWhitelist.getServer();
        LinkingWhitelist whitelist = (LinkingWhitelist) server.getPlayerManager().getWhitelist();

        Optional<LinkedWhitelistEntry> whitelistedAccount = getWhitelistedAccount(discordId, whitelist);
        if (whitelistedAccount.isPresent()) {
            boolean sameAccount;
            if (uuid != null) {
                sameAccount = Stonecutter.profileId(whitelistedAccount.get().getProfile()).equals(uuid);
            } else {
                sameAccount = Stonecutter.profileName(whitelistedAccount.get().getProfile()).equalsIgnoreCase(username);
            }

            if (sameAccount) {
                replyCallback.editMessage(
                  DiscordBotHelper.buildEmbedMessage(true,
                    DiscordBotHelper.Feedback.buildEmbed(
                      Text.translatable("discord.command.register.same_username.title"),
                      Text.translatable("discord.command.register.same_username.message"),
                      DiscordBotHelper.MessageType.WARNING
                    )
                  )
                );
                return;
            }

            if (whitelistedAccount.get().getProfile().isLocked()) {
                if (AutoWhitelist.CONFIG.lockTime() == -1) {
                    replyCallback.editMessage(
                      DiscordBotHelper.buildEmbedMessage(true,
                        DiscordBotHelper.Feedback.buildEmbed(
                          Text.translatable("discord.command.register.already_registered.title"),
                          Text.translatable("discord.command.register.already_registered.message"),
                          DiscordBotHelper.MessageType.WARNING
                        )
                      )
                    );
                } else {
                    replyCallback.editMessage(
                      DiscordBotHelper.buildEmbedMessage(true,
                        DiscordBotHelper.Feedback.buildEmbed(
                          Text.translatable("discord.command.register.locked.title"),
                          Text.translatable("discord.command.register.locked.message", DiscordBotHelper.formatDiscordTimestamp(whitelistedAccount.get().getProfile().getLockedUntil())),
                          DiscordBotHelper.MessageType.WARNING
                        )
                      )
                    );
                }
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

        if (username.isEmpty()) {
            replyCallback.editMessage(
              DiscordBotHelper.buildEmbedMessage(true,
                DiscordBotHelper.Feedback.buildEmbed(
                  Text.translatable("discord.command.few_args.title"),
                  Text.translatable("discord.command.few_args.message"),
                  DiscordBotHelper.MessageType.WARNING
                )
              )
            );
            return;
        }

        // Only check username if it is a Java username
        if (uuid == null && !geyser && !Validation.isValidMinecraftUsername(username)) {
            replyCallback.editMessage(
              DiscordBotHelper.buildEmbedMessage(true,
                DiscordBotHelper.Feedback.buildEmbed(
                  Text.translatable("discord.command.register.invalid_username.title"),
                  Text.translatable("discord.command.register.invalid_username.message"),
                  DiscordBotHelper.MessageType.WARNING
                )
              )
            );
            return;
        }

        if (Stonecutter.getUserCache(server) == null) {
            AutoWhitelist.LOGGER.error("Failed to whitelist user {}, server user cache is null", username);
            replyCallback.editMessage(
              DiscordBotHelper.buildEmbedMessage(true,
                DiscordBotHelper.Feedback.buildEmbed(
                  Text.translatable("discord.command.fail.title"),
                  Text.translatable("discord.command.register.fatal", "Failed to whitelist user %s, server user cache is null".formatted(username)),
                  DiscordBotHelper.MessageType.ERROR
                )
              )
            );
            return;
        }

        /*$ WhitelistProfile >>*/net.minecraft.server.PlayerConfigEntry profile;
        if (uuid != null) {
            if (LazyConstants.isUsingGeyser()) {
                if (uuid.getMostSignificantBits() == 0 || geyser) {
                    profile = new /*$ WhitelistProfile {*/net.minecraft.server.PlayerConfigEntry/*$}*/(uuid, "Bedrock Player");
                } else {
                    profile = Stonecutter.getUserCache(server).getByUuid(uuid).orElse(null);
                }
            } else {
                profile = Stonecutter.getUserCache(server).getByUuid(uuid).orElse(null);
            }
        } else {
            if (LazyConstants.isUsingGeyser() && geyser) {
                final AtomicReference</*$ WhitelistProfile {*/net.minecraft.server.PlayerConfigEntry/*$}*/> atomicProfile = new AtomicReference<>();
                final AtomicReference<Exception> atomicException = new AtomicReference<>();
                ProfileLookupCallback profileLookupCallback = new ProfileLookupCallback() {

                    //? if <1.21.9 {
                    /*@Override
                    public void onProfileLookupSucceeded(GameProfile profile) {
                        atomicProfile.set(profile);
                    }
                    *///?} else {
                    @Override
                    public void onProfileLookupSucceeded(String name, UUID uuid) {
                        atomicProfile.set(new net.minecraft.server.PlayerConfigEntry(uuid, name));
                    }
                    //?}

                    @Override
                    public void onProfileLookupFailed(String name, Exception exception) {
                        if (!(exception instanceof ProfileNotFoundException)) {
                            atomicException.set(exception);
                        }
                        atomicProfile.set(null);
                    }
                };

                GeyserProfileRepository repository = LazyConstants.getGeyserProfileRepository();

                repository.findProfilesByNames(new String[]{username}, profileLookupCallback);

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
                          Text.translatable("discord.command.register.unknown_bedrock_profile.title"),
                          Text.translatable("discord.command.register.unknown_bedrock_profile.message"),
                          DiscordBotHelper.MessageType.WARNING
                        )
                      )
                    );
                    return;
                } else {
                    profile = atomicProfile.get();
                }
            } else {
                profile = Stonecutter.getUserCache(server).findByName(username).orElse(null);
            }
        }

        if (profile == null) {
            replyCallback.editMessage(
              DiscordBotHelper.buildEmbedMessage(true,
                DiscordBotHelper.Feedback.buildEmbed(
                  Text.translatable("discord.command.fail.title"),
                  Text.translatable("discord.command.register.fail.account_data", username),
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
                  Text.translatable("discord.command.register.player_banned.title"),
                  Text.translatable("discord.command.register.player_banned.message"),
                  DiscordBotHelper.MessageType.INFO
                )
              )
            );
            return;
        }

        LinkedPlayerProfile extendedProfile = new LinkedPlayerProfile(Stonecutter.profileId(profile), Stonecutter.profileName(profile), highestRole.get().getId(), discordId, AutoWhitelist.CONFIG.lockTime());

        if (AutoWhitelist.getServer().getPlayerManager().getUserBanList().contains(extendedProfile)) {
            replyCallback.editMessage(
              DiscordBotHelper.buildEmbedMessage(true,
                DiscordBotHelper.Feedback.buildEmbed(
                  Text.translatable("discord.command.register.player_banned.title"),
                  Text.translatable("discord.command.register.player_banned.message"),
                  DiscordBotHelper.MessageType.ERROR
                )
              )
            );
            return;
        }

        if (whitelist.isAllowed(extendedProfile)) {
            replyCallback.editMessage(
              DiscordBotHelper.buildEmbedMessage(true,
                DiscordBotHelper.Feedback.buildEmbed(
                  Text.translatable("discord.command.register.username_already_registered.title"),
                  Text.translatable("discord.command.register.username_already_registered.message"),
                  DiscordBotHelper.MessageType.ERROR
                )
              )
            );
            return;
        }

        replyCallback.editMessage(
          DiscordBotHelper.buildEmbedMessage(true,
            DiscordBotHelper.Feedback.buildEmbed(
              Text.translatable("discord.command.register.last_steps.title"),
              Text.translatable("discord.command.register.last_steps.message"),
              DiscordBotHelper.MessageType.INFO
            )
          )
        );

        whitelistPlayer(whitelistedAccount.orElse(null), whitelist, extendedProfile, entry);

        replyCallback.editMessage(
          DiscordBotHelper.buildEmbedMessage(true,
            DiscordBotHelper.Feedback.buildEmbed(
              Text.translatable("discord.command.register.success.title"),
              Text.translatable("discord.command.register.success.message"),
              DiscordBotHelper.MessageType.SUCCESS
            )
          )
        );
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        @NotNull Member member = Objects.requireNonNull(event.getMember());
        @NotNull String username = Objects.requireNonNull(event.getOption("username", OptionMapping::getAsString));
        boolean geyser = event.getOption("account_type", "java", OptionMapping::getAsString).equalsIgnoreCase("bedrock");

        execute(member, username, geyser, new ReplyCallback.DefaultInteractionReplyCallback(event));
    }

    public static void whitelistPlayer(LinkedWhitelistEntry whitelistedAccount, LinkingWhitelist whitelist, LinkedPlayerProfile extendedProfile, BaseEntryAction entry) {
        if (whitelistedAccount != null) {
            whitelist.remove(whitelistedAccount.getProfile());
            if (AutoWhitelist.CONFIG.enableWhitelistCache) {
                AutoWhitelist.getWhitelistCache().remove(new WhitelistCacheEntry(whitelistedAccount.getProfile()));
            }
        }

        whitelist.add(new LinkedWhitelistEntry(extendedProfile));
        if (AutoWhitelist.CONFIG.enableWhitelistCache) {
            WhitelistCacheEntry cacheEntry;
            if ((cacheEntry = AutoWhitelist.getWhitelistCache().getFromId(extendedProfile.getDiscordId())) != null) {
                AutoWhitelist.getWhitelistCache().remove(cacheEntry);
            }
            AutoWhitelist.getWhitelistCache().add(new WhitelistCacheEntry(extendedProfile));
        }
        AutoWhitelist.LOGGER.debug("Whitelisting {} for entry of type {} (Role: {})", Stonecutter.profileName(extendedProfile), entry.getType(), extendedProfile.getRole());
        entry.registerUser(extendedProfile);
    }

    @NotNull
    public static Optional<LinkedWhitelistEntry> getWhitelistedAccount(String id, @NotNull LinkingWhitelist whitelist) {
        return whitelist.getEntries().stream().filter(entry -> {
            try {
                LinkedWhitelistEntry whitelistEntry = (LinkedWhitelistEntry) entry;
                return whitelistEntry.getProfile().getDiscordId().equals(id);
            } catch (Throwable e) {
                return false;
            }
        }).map(e -> (LinkedWhitelistEntry) e).findFirst();
    }
}
