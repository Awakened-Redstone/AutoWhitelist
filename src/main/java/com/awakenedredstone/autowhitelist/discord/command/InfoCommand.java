package com.awakenedredstone.autowhitelist.discord.command;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.discord.Bot;
import com.awakenedredstone.autowhitelist.discord.BotHelper;
import com.awakenedredstone.autowhitelist.discord.api.ReplyCallback;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedGameProfile;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelist;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelistEntry;
import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
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
/*? if <1.19 {*/
import net.minecraft.text.TranslatableText;
/*?}*/
import net.minecraft.text.Text;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

public class InfoCommand extends SlashCommand {
    public InfoCommand() {
        this.name = "info";
        this.help = /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("command.description.info").getString();

        this.guildOnly = true;
    }

    @Override
    @SuppressWarnings("DuplicatedCode")
    protected void execute(SlashCommandEvent event) {
        boolean ephemeral = AutoWhitelist.CONFIG.ephemeralReplies;
        ReplyCallback.InteractionReplyCallback replyCallback = new ReplyCallback.InteractionReplyCallback() {

            @Override
            public void acknowledge() {
                lastTask = event.deferReply(ephemeral).submit();
            }
        };

        replyCallback.sendMessage(null);

        Member member = event.getMember();
        if (member == null) return;

        String memberId = member.getId();
        MinecraftServer server = AutoWhitelist.getServer();
        ExtendedWhitelist whitelist = (ExtendedWhitelist) server.getPlayerManager().getWhitelist();

        final String eventId = UUID.randomUUID().toString().replaceAll("-", "");

        Function<String, String> getId = (String id) -> id.substring(id.indexOf("-") + 1);


        Optional<ExtendedWhitelistEntry> whitelistedAccount = whitelist.getEntries().stream().filter(entry -> {
            try {
                ExtendedWhitelistEntry whitelistEntry = (ExtendedWhitelistEntry) entry;
                return whitelistEntry.getProfile().getDiscordId().equals(memberId);
            } catch (Throwable e) {
                return false;
            }
        }).map(e -> (ExtendedWhitelistEntry) e).findFirst();

        if (whitelistedAccount.isPresent()) {
            ExtendedWhitelistEntry entry = whitelistedAccount.get();
            ExtendedGameProfile profile = entry.getProfile();

            EmbedBuilder embed = BotHelper.Feedback.defaultEmbed(
              /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("command.info.title"),
              /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("command.info.description")
            );

            String[] fields = new String[]{"username", "role", "lock"};
            for (String field : fields) {
                Text title = /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("command.info.field.%s.title".formatted(field));
                if (title.getString().isEmpty()) continue;

                String descriptionKey = "command.info.field.%s.description".formatted(field);
                Text description = switch (field) {
                    case "username" -> /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/(descriptionKey, profile.getName());
                    case "role" -> /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/(descriptionKey, "<@&" + profile.getRole() + ">");
                    case "lock" -> {
                        String time = "future";
                        if (profile.getLockedUntil() == -1) {
                            time = "permanent";
                        } else if (profile.getLockedUntil() <= System.currentTimeMillis()) {
                            time = "past";
                        }
                        String timeKey = "." + time;
                        yield /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/(descriptionKey + timeKey, BotHelper.formatDiscordTimestamp(profile.getLockedUntil()));
                    }
                    default -> Text.of("");
                };
                if (description.getString().isEmpty()) continue;

                embed.addField(title.getString(), description.getString(), true);
            }

            Button removeButton = Button.danger(btnId(eventId, "delete"), "Remove");

            replyCallback.editMessage((InteractionHook interactionHook) -> interactionHook
              .editOriginal(BotHelper.<MessageEditData>buildEmbedMessage(true, embed.build()))
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
                        whitelist.remove(whitelistedAccount.get());

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
            EmbedBuilder embed = BotHelper.Feedback.defaultEmbed(
              /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("command.info.missing.title"),
              /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("command.info.missing.description")
            );

            //Check if account qualifies for registration
            List<Role> roles = BotHelper.getRolesForMember(member);
            boolean accepted = !Collections.disjoint(roles.stream().map(Role::getId).toList(), new ArrayList<>(AutoWhitelist.ENTRY_MAP_CACHE.keySet()));

            Button button = Button.success(btnId(eventId, "register"), "Register").withDisabled(!accepted);

            replyCallback.editMessage((InteractionHook interactionHook) -> interactionHook
              .editOriginal(
                BotHelper.<MessageEditData>buildEmbedMessage(true, embed.build())
              ).setComponents(ActionRow.of(button))
            );
            ButtonEventHandler buttonEventHandler = new ButtonEventHandler().addConsumer(btnId(eventId, "register"), buttonEvent -> {
                Modal.Builder builder = Modal.create(
                  btnId(eventId, "register"),
                  /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("discord.modal.register.title").getString()
                ).addComponents(
                  ActionRow.of(
                    TextInput.create(
                      "username",
                      /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("discord.modal.register.input.label").getString(),
                      TextInputStyle.SHORT
                    ).setPlaceholder(
                      /*? if >=1.19 {*//*Text.translatable*//*?} else {*/new TranslatableText/*?}*/("discord.modal.register.input.placeholder").getString()
                    ).setRequired(true).build()
                  )
                );

                buttonEvent.replyModal(builder.build()).queue();

                Bot.eventWaiter.waitForEvent(ModalInteractionEvent.class,
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
        Bot.eventWaiter.waitForEvent(ButtonInteractionEvent.class,
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
