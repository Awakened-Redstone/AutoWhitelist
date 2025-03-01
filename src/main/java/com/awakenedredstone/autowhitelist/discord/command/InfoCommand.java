package com.awakenedredstone.autowhitelist.discord.command;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.discord.DiscordBot;
import com.awakenedredstone.autowhitelist.discord.DiscordBotHelper;
import com.awakenedredstone.autowhitelist.discord.api.ReplyCallback;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedGameProfile;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelist;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelistEntry;
import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class InfoCommand extends SlashCommand {
    public InfoCommand() {
        this.name = "info";
        this.help = Text.translatable("discord.command.description.info").getString();

        this.contexts = new InteractionContextType[]{InteractionContextType.GUILD};
    }

    @Override
    @SuppressWarnings("DuplicatedCode")
    protected void execute(SlashCommandEvent event) {
        ReplyCallback.InteractionReplyCallback replyCallback = new ReplyCallback.InteractionReplyCallback() {

            @Override
            public void acknowledge() {
                lastTask = event.deferReply(AutoWhitelist.CONFIG.ephemeralReplies).submit();
            }
        };

        replyCallback.sendMessage(null);

        Member member = event.getMember();
        if (member == null) {
            AutoWhitelist.LOGGER.error("Member is null", new IllegalStateException());
            MessageEmbed embed = DiscordBotHelper.Feedback.buildEmbed(
              Text.translatable("discord.command.fatal.title"),
              Text.translatable("discord.command.fatal.generic", "Member is null"),
              DiscordBotHelper.MessageType.FATAL
            );

            replyCallback.editMessage((InteractionHook interactionHook) -> interactionHook
              .editOriginal(DiscordBotHelper.<MessageEditData>buildEmbedMessage(true, embed))
            );

            return;
        }

        String memberId = member.getId();
        MinecraftServer server = AutoWhitelist.getServer();
        ExtendedWhitelist whitelist = (ExtendedWhitelist) server.getPlayerManager().getWhitelist();

        final String eventId = event.getId();

        Optional<ExtendedWhitelistEntry> whitelistedAccount = RegisterCommand.getWhitelistedAccount(memberId, whitelist);

        if (whitelistedAccount.isPresent()) {
            ExtendedWhitelistEntry entry = whitelistedAccount.get();
            ExtendedGameProfile profile = entry.getProfile();

            EmbedBuilder embed = DiscordBotHelper.Feedback.defaultEmbed(
              Text.translatable("discord.command.info.title"),
              Text.translatable("discord.command.info.description")
            );

            //noinspection DuplicatedCode
            String[] fields = new String[]{"username", "role", "lock"};
            for (String field : fields) {
                Text title = Text.translatable("discord.command.info.field.%s.title".formatted(field));
                if (title.getString().isEmpty()) continue;

                String descriptionKey = "discord.command.info.field.%s.description".formatted(field);
                Text description = switch (field) {
                    case "username" -> Text.translatable(descriptionKey, profile.getName());
                    case "role" -> Text.translatable(descriptionKey, "<@&" + profile.getRole() + ">");
                    case "lock" -> {
                        String time = "future";
                        if (profile.getLockedUntil() == -1) {
                            time = "permanent";
                        } else if (profile.getLockedUntil() <= System.currentTimeMillis()) {
                            time = "past";
                        }
                        String timeKey = "." + time;
                        yield Text.translatable(descriptionKey + timeKey, DiscordBotHelper.formatDiscordTimestamp(profile.getLockedUntil()));
                    }
                    default -> Text.of("");
                };
                if (description.getString().isEmpty()) continue;

                embed.addField(title.getString(), description.getString(), true);
            }

            Button removeButton = Button.danger(btnId(eventId, "delete"), "Remove");

            replyCallback.editMessage((InteractionHook interactionHook) -> interactionHook
              .editOriginal(DiscordBotHelper.<MessageEditData>buildEmbedMessage(true, embed.build()))
              .setComponents(ActionRow.of(removeButton.withDisabled(profile.isLocked())))
            );

            if (!profile.isLocked()) {
                ButtonEventHandler buttonEventHandler = new ButtonEventHandler().addConsumer(btnId(eventId, "delete"), buttonEvent -> {
                    buttonEvent.editMessage(
                      new MessageEditBuilder()
                        .setContent("Are you sure you want to remove yourself from the whitelist?")
                        .setEmbeds()
                        .setComponents(
                          ActionRow.of(
                            Button.secondary(btnId(eventId, "cancel"), "Cancel"),
                            Button.danger(btnId(eventId, "confirmDelete"), "Confirm")
                          )
                        ).build()
                    ).queue();

                    ButtonEventHandler confirmEventHandler = new ButtonEventHandler().addConsumer(btnId(eventId, "confirmDelete"), confirmEvent -> {
                        AutoWhitelist.WHITELIST_CACHE.remove(whitelistedAccount.get().getProfile());
                        AutoWhitelist.removePlayer(whitelistedAccount.get().getProfile());

                        MessageEditBuilder builder = new MessageEditBuilder();
                        builder.setContent("You have been removed from the whitelist.");

                        confirmEvent.editMessage(builder.setComponents().build()).queue();
                    }).addConsumer(btnId(eventId, "cancel"), confirmEvent -> {
                        MessageEditBuilder builder = new MessageEditBuilder();
                        builder.setContent("Cancelled.");

                        confirmEvent.editMessage(builder.setComponents().build()).queue();
                    });

                    waitForButton(eventId, replyCallback, confirmEventHandler);
                });

                waitForButton(eventId, replyCallback, buttonEventHandler);
            }
        } else {
            MessageEmbed embed = DiscordBotHelper.Feedback.buildEmbed(
              Text.translatable("discord.command.info.missing.title"),
              Text.translatable("discord.command.info.missing.description")
            );

            //Check if account qualifies for registration
            Optional<Role> highestRole = DiscordBotHelper.getHighestEntryRole(member);
            Button button = Button.success(btnId(eventId, "register"), "Register").withDisabled(highestRole.isEmpty());

            replyCallback.editMessage((InteractionHook interactionHook) -> interactionHook
              .editOriginal(
                DiscordBotHelper.<MessageEditData>buildEmbedMessage(true, embed)
              ).setComponents(ActionRow.of(button))
            );
            ButtonEventHandler buttonEventHandler = new ButtonEventHandler().addConsumer(btnId(eventId, "register"), buttonEvent -> {
                Modal.Builder builder = Modal.create(
                  btnId(eventId, "register"),
                  Text.translatable("discord.modal.register.title").getString()
                ).addComponents(
                  ActionRow.of(
                    TextInput.create(
                      "username",
                      Text.translatable("discord.modal.register.input.label").getString(),
                      TextInputStyle.SHORT
                    ).setPlaceholder(
                      Text.translatable("discord.modal.register.input.placeholder").getString()
                    ).setRequired(true).build()
                  )
                );

                buttonEvent.replyModal(builder.build()).queue();

                DiscordBot.eventWaiter.waitForEvent(ModalInteractionEvent.class,
                  modalEvent -> modalEvent.getModalId().equals(btnId(eventId, "register")),
                  modalEvent -> {
                      ModalMapping usernameMapping = modalEvent.getValue("username");
                      if (usernameMapping == null) return;

                      String username = usernameMapping.getAsString();
                      ReplyCallback.InteractionReplyCallback replyCallback1 = new ReplyCallback.InteractionReplyCallback() {

                          @Override
                          public void acknowledge() {
                              lastTask = modalEvent.editComponents().submit();
                          }
                      };

                      RegisterCommand.execute(member, username, replyCallback1);
                  }
                );
            });


            waitForButton(eventId, replyCallback, buttonEventHandler);
        }
    }

    private void waitForButton(String idBase, ReplyCallback.InteractionReplyCallback replyCallback, ButtonEventHandler buttonEventHandler) {
        DiscordBot.eventWaiter.waitForEvent(ButtonInteractionEvent.class,
          buttonEvent -> buttonEvent.getComponentId().startsWith(idBase),
          buttonEventHandler::handleEvent,
          1, TimeUnit.MINUTES,
          () -> replyCallback.editMessage((Object v) -> {
              if (v instanceof InteractionHook interactionHook) {
                  return interactionHook.editOriginalComponents();
              } else if (v instanceof Message editAction) {
                  return editAction.editMessageComponents();
              } else {
                  return null;
              }
          })
        );
    }

    private String btnId(String idBase, String suffix) {
        return idBase + "-" + suffix;
    }

    private static class ButtonEventHandler {
        private final Map<String, Consumer<ButtonInteractionEvent>> consumers = new HashMap<>();

        public ButtonEventHandler addConsumer(String id, Consumer<ButtonInteractionEvent> consumer) {
            consumers.put(id, consumer);
            return this;
        }

        public void handleEvent(ButtonInteractionEvent event) {
            String id = event.getComponentId();
            if (consumers.containsKey(id)) {
                consumers.get(id).accept(event);
            }
        }
    }
}
