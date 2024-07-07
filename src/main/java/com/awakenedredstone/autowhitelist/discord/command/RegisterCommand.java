package com.awakenedredstone.autowhitelist.discord.command;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.entry.BaseEntry;
import com.awakenedredstone.autowhitelist.discord.DiscordBotHelper;
import com.awakenedredstone.autowhitelist.discord.DiscordDataProcessor;
import com.awakenedredstone.autowhitelist.discord.api.ReplyCallback;
import com.awakenedredstone.autowhitelist.discord.api.command.CommandBase;
import com.awakenedredstone.autowhitelist.util.Stonecutter;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedGameProfile;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelist;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelistEntry;
import com.awakenedredstone.autowhitelist.whitelist.WhitelistCacheEntry;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.mojang.authlib.GameProfile;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;

import static net.dv8tion.jda.api.Permission.*;

public class RegisterCommand extends CommandBase {
    public static final RegisterCommand INSTANCE = new RegisterCommand();

    public RegisterCommand() {
        this.name = "register";
        this.description = Stonecutter.translatableText("command.description.register").getString();
    }

    public static void execute(Member member, String username, ReplyCallback replyCallback) {
        if (member == null) return;

        MessageCreateData initialReply = DiscordBotHelper.buildEmbedMessage(
          false,
            DiscordBotHelper.Feedback.buildEmbed(
              Stonecutter.translatableText("command.feedback.received.title"),
              Stonecutter.translatableText("command.feedback.received.message"),
              DiscordBotHelper.MessageType.NORMAL
            )
        );

        replyCallback.sendMessage(initialReply);

        String id = member.getId();
        List<Role> roles = DiscordBotHelper.getRolesForMember(member);

        boolean accepted = !Collections.disjoint(roles.stream().map(Role::getId).toList(), new ArrayList<>(AutoWhitelist.ENTRY_MAP_CACHE.keySet()));
        if (accepted) {
            MinecraftServer server = AutoWhitelist.getServer();
            ExtendedWhitelist whitelist = (ExtendedWhitelist) server.getPlayerManager().getWhitelist();

            Optional<ExtendedWhitelistEntry> whitelistedAccount = getWhitelistedAccount(id, whitelist);
            if (whitelistedAccount.isPresent()) {
                if (whitelistedAccount.get().getProfile().getName().equalsIgnoreCase(username)) {
                    replyCallback.editMessage(
                      DiscordBotHelper.buildEmbedMessage(true,
                        DiscordBotHelper.Feedback.buildEmbed(
                          Stonecutter.translatableText("command.register.same_username.title"),
                          Stonecutter.translatableText("command.register.same_username.message"),
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
                              Stonecutter.translatableText("command.register.already_registered.title"),
                              Stonecutter.translatableText("command.register.already_registered.message"),
                              DiscordBotHelper.MessageType.WARNING
                            )
                          )
                        );
                    } else {
                        replyCallback.editMessage(
                          DiscordBotHelper.buildEmbedMessage(true,
                            DiscordBotHelper.Feedback.buildEmbed(
                              Stonecutter.translatableText("command.register.locked.title"),
                              Stonecutter.translatableText("command.register.locked.message", DiscordBotHelper.formatDiscordTimestamp(whitelistedAccount.get().getProfile().getLockedUntil())),
                              DiscordBotHelper.MessageType.WARNING
                            )
                          )
                        );
                    }
                    return;
                }
            }

            Optional<String> highestRoleOptional = DiscordDataProcessor.getTopRole(roles);
            if (highestRoleOptional.isEmpty()) {
                AutoWhitelist.LOGGER.error("Impossible case, the user {} has no valid role, but it passed as qualified. Please report this bug.", id, new IllegalStateException());
                replyCallback.editMessage(
                  DiscordBotHelper.buildEmbedMessage(true,
                    DiscordBotHelper.Feedback.buildEmbed(
                      Stonecutter.translatableText("command.register.fatal.title"),
                      Stonecutter.translatableText("command.register.fatal", "User does not have a valid role, yet it passed as qualified. Please report this bug."),
                      DiscordBotHelper.MessageType.ERROR
                    )
                  )
                );
                return;
            }
            String highestRole = highestRoleOptional.get();
            BaseEntry entry = AutoWhitelist.ENTRY_MAP_CACHE.get(highestRole);
            try {
                entry.assertSafe();
            } catch (Throwable e) {
                replyCallback.editMessage(
                    DiscordBotHelper.buildEmbedMessage(true,
                        DiscordBotHelper.Feedback.buildEmbed(
                          Stonecutter.translatableText("command.fail.title"),
                          Stonecutter.translatableText("command.fatal.exception", e.getMessage()),
                        DiscordBotHelper.MessageType.ERROR
                        )
                    )
                );
                AutoWhitelist.LOGGER.error("Failed to whitelist user, tried to assert entry but got an exception", e);
                return;
            }

            if (username.isEmpty()) {
                replyCallback.editMessage(
                  DiscordBotHelper.buildEmbedMessage(true,
                    DiscordBotHelper.Feedback.buildEmbed(
                      Stonecutter.translatableText("command.few_args.title"),
                      Stonecutter.translatableText("command.few_args.message"),
                      DiscordBotHelper.MessageType.WARNING
                    )
                  )
                );
                return;
            }
            String[] args = username.split(" ");
            if (args.length > 1) {
                replyCallback.editMessage(
                  DiscordBotHelper.buildEmbedMessage(true,
                    DiscordBotHelper.Feedback.buildEmbed(
                      Stonecutter.translatableText("command.too_many_args.title"),
                      Stonecutter.translatableText("command.too_many_args.message"),
                      DiscordBotHelper.MessageType.WARNING
                    )
                  )
                );
                return;
            }
            String arg = args[0];

            {
                if (arg.length() > 16) {
                    replyCallback.editMessage(
                      DiscordBotHelper.buildEmbedMessage(true,
                        DiscordBotHelper.Feedback.buildEmbed(
                          Stonecutter.translatableText("command.register.invalid_username.title"),
                          Stonecutter.translatableText("command.register.invalid_username.message.too_long"),
                          DiscordBotHelper.MessageType.WARNING
                        )
                      )
                    );
                    return;
                } else if (arg.length() < 3) {
                    replyCallback.editMessage(
                        DiscordBotHelper.buildEmbedMessage(true,
                            DiscordBotHelper.Feedback.buildEmbed(
                            Stonecutter.translatableText("command.register.invalid_username.title"),
                            Stonecutter.translatableText("command.register.invalid_username.message.too_short"),
                            DiscordBotHelper.MessageType.WARNING
                            )
                        )
                    );
                    return;
                }
            }

            if (server.getUserCache() == null) {
                AutoWhitelist.LOGGER.error("Failed to whitelist user {}, server user cache is null", username);
                replyCallback.editMessage(
                  DiscordBotHelper.buildEmbedMessage(true,
                    DiscordBotHelper.Feedback.buildEmbed(
                      Stonecutter.translatableText("command.fail.title"),
                      Stonecutter.translatableText("command.register.fail.account_data", arg),
                      DiscordBotHelper.MessageType.ERROR
                    )
                  )
                );
                return;
            }

            GameProfile profile = server.getUserCache().findByName(arg).orElse(null);
            if (profile == null) {
                replyCallback.editMessage(
                  DiscordBotHelper.buildEmbedMessage(true,
                    DiscordBotHelper.Feedback.buildEmbed(
                      Stonecutter.translatableText("command.fail.title"),
                      Stonecutter.translatableText("command.register.fail.account_data", arg),
                      DiscordBotHelper.MessageType.ERROR
                    )
                  )
                );
                return;
            }
            ExtendedGameProfile extendedProfile = new ExtendedGameProfile(profile.getId(), profile.getName(), highestRole, id, AutoWhitelist.CONFIG.lockTime());
            if (AutoWhitelist.getServer().getPlayerManager().getUserBanList().contains(extendedProfile)) {
                replyCallback.editMessage(
                  DiscordBotHelper.buildEmbedMessage(true,
                    DiscordBotHelper.Feedback.buildEmbed(
                      Stonecutter.translatableText("command.register.player_banned.title"),
                      Stonecutter.translatableText("command.register.player_banned.message"),
                      DiscordBotHelper.MessageType.ERROR
                    )
                  )
                );
                return;
            }
            boolean whitelisted = whitelist.isAllowed(extendedProfile);
            if (whitelisted) {
                replyCallback.editMessage(
                  DiscordBotHelper.buildEmbedMessage(true,
                    DiscordBotHelper.Feedback.buildEmbed(
                      Stonecutter.translatableText("command.register.username_already_registered.title"),
                      Stonecutter.translatableText("command.register.username_already_registered.message"),
                      DiscordBotHelper.MessageType.ERROR
                    )
                  )
                );
            } else {
                replyCallback.editMessage(
                  DiscordBotHelper.buildEmbedMessage(true,
                    DiscordBotHelper.Feedback.buildEmbed(
                      Stonecutter.translatableText("command.register.last_steps.title"),
                      Stonecutter.translatableText("command.register.last_steps.message"),
                      DiscordBotHelper.MessageType.INFO
                    )
                  )
                );

                replyCallback.editMessage(
                  DiscordBotHelper.buildEmbedMessage(true,
                    DiscordBotHelper.Feedback.buildEmbed(
                      Stonecutter.translatableText("command.register.last_steps.title"),
                      Stonecutter.translatableText("command.register.last_steps.message"),
                      DiscordBotHelper.MessageType.INFO
                    )
                  )
                );

                if (whitelistedAccount.isPresent()) {
                    whitelist.remove(whitelistedAccount.get().getProfile());
                    if (AutoWhitelist.CONFIG.enableWhitelistCache) {
                        AutoWhitelist.WHITELIST_CACHE.remove(new WhitelistCacheEntry(whitelistedAccount.get().getProfile()));
                    }
                }
                whitelist.add(new ExtendedWhitelistEntry(extendedProfile));
                if (AutoWhitelist.CONFIG.enableWhitelistCache) {
                    AutoWhitelist.WHITELIST_CACHE.add(new WhitelistCacheEntry(extendedProfile));
                }
                entry.registerUser(profile);

                replyCallback.editMessage(
                  DiscordBotHelper.buildEmbedMessage(true,
                    DiscordBotHelper.Feedback.buildEmbed(
                      Stonecutter.translatableText("command.register.success.title"),
                      Stonecutter.translatableText("command.register.success.message"),
                      DiscordBotHelper.MessageType.SUCCESS
                    )
                  )
                );
            }
        } else {
            replyCallback.editMessage(
              DiscordBotHelper.buildEmbedMessage(true,
                DiscordBotHelper.Feedback.buildEmbed(
                  Stonecutter.translatableText("command.register.fail.not_allowed.title"),
                  Stonecutter.translatableText("command.register.fail.not_allowed.message"),
                  DiscordBotHelper.MessageType.NORMAL
                )
              )
            );
        }
    }

    @NotNull
    public static Optional<ExtendedWhitelistEntry> getWhitelistedAccount(String id, @NotNull ExtendedWhitelist whitelist) {
        return whitelist.getEntries().stream().filter(entry -> {
            try {
                ExtendedWhitelistEntry whitelistEntry = (ExtendedWhitelistEntry) entry;
                return whitelistEntry.getProfile().getDiscordId().equals(id);
            } catch (Throwable e) {
                return false;
            }
        }).map(e -> (ExtendedWhitelistEntry) e).findFirst();
    }

    @Deprecated(since = "1.0.0-beta.1")
    @ApiStatus.ScheduledForRemoval(inVersion = "1.1.0")
    public class TextCommand extends Command {
        public TextCommand() {
            this.name = RegisterCommand.this.name;
            this.help = RegisterCommand.this.description;
            this.category = new Category("Server integration");
            this.botPermissions = new Permission[]{MESSAGE_ATTACH_FILES, MESSAGE_HISTORY, MESSAGE_EMBED_LINKS, VIEW_CHANNEL, MESSAGE_SEND};
            this.arguments = "<minecraft username>";
            this.guildOnly = true;
        }

        @Override
        protected void execute(CommandEvent event) {
            RegisterCommand.execute(event.getMember(), event.getArgs(), new ReplyCallback() {
                private final Queue<MessageEditData> queue = new LinkedList<>();
                private boolean processing = false;
                private Message message;

                @Override
                public void sendMessage(MessageCreateData message) {
                    queueMessage(event.getMessage().reply(message).mentionRepliedUser(false));
                }

                @Override
                public void editMessage(MessageEditData message) {
                    if (!this.processing) {
                        queueMessage(this.message.editMessage(message));
                    } else {
                        queue.add(message);
                    }
                }

                private void queueMessage(MessageCreateAction message) {
                    this.processing = true;
                    message.queue(message1 -> {
                        this.message = message1;
                        this.processing = false;
                        if (!queue.isEmpty()) {
                            queueMessage(message1.editMessage(queue.poll()));
                        }
                    });
                }

                private void queueMessage(MessageEditAction message) {
                    this.processing = true;
                    message.queue(message1 -> {
                        this.message = message1;
                        this.processing = false;
                        if (!queue.isEmpty()) {
                            queueMessage(message1.editMessage(queue.poll()));
                        }
                    });
                }
            });
        }
    }

    public class SlashCommand extends com.jagrosh.jdautilities.command.SlashCommand {
        public SlashCommand() {
            this.name = RegisterCommand.this.name;
            this.help = RegisterCommand.this.description;
            this.guildOnly = true;

            options.add(new OptionData(OptionType.STRING, "username", "Your Minecraft username").setRequired(true));
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            //noinspection DataFlowIssue
            RegisterCommand.execute(event.getMember(), event.getOption("username").getAsString(), new ReplyCallback.InteractionReplyCallback() {

                @Override
                public void acknowledge() {
                    lastTask = event.deferReply(AutoWhitelist.CONFIG.ephemeralReplies).submit();
                }
            });
        }
    }
}
