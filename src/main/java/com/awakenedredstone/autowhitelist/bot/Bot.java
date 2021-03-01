package com.awakenedredstone.autowhitelist.bot;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.database.SQLite;
import com.awakenedredstone.autowhitelist.util.InvalidResultException;
import com.awakenedredstone.autowhitelist.util.MemberPlayer;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.WhitelistEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.login.LoginException;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Bot extends ListenerAdapter {

    private static Bot instance;

    private static ScheduledFuture<?> scheduledUpdate;

    private static JDA jda = null;
    private static String token = AutoWhitelist.config.getConfigs().get("token").getAsString();
    private static String appId = AutoWhitelist.config.getConfigs().get("application-id").getAsString();
    private static String serverId = AutoWhitelist.config.getConfigs().get("discord-server-id").getAsString();
    private static String prefix = AutoWhitelist.config.getConfigs().get("prefix").getAsString();
    private static long updateDelay = AutoWhitelist.config.getConfigs().get("whitelist-auto-update-delay-seconds").getAsLong();

    public Bot() {
        init();
    }

    private void sendFeedbackMessage(MessageChannel channel, String title, String message) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor(jda.getSelfUser().getName(), "https://discord.com", jda.getSelfUser().getAvatarUrl());
        embedBuilder.setTitle(title);
        embedBuilder.setDescription(message);
        embedBuilder.setFooter("Minecraft PhoenixSC Edition");
        MessageAction messageAction = channel.sendMessage(embedBuilder.build());
        messageAction.queue();
    }

    private void sendTempFeedbackMessage(MessageChannel channel, String title, String message, int seconds) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor(jda.getSelfUser().getName(), "https://discord.com", jda.getSelfUser().getAvatarUrl());
        embedBuilder.setTitle(title);
        embedBuilder.setDescription(message);
        embedBuilder.setFooter(String.format("This message will be deleted %s seconds after being sent.", seconds));
        MessageAction messageAction = channel.sendMessage(embedBuilder.build());
        messageAction.queue(m -> m.delete().queueAfter(seconds, TimeUnit.SECONDS));
    }

    public static void stopBot() {
        if (jda != null) {
            scheduledUpdate.cancel(true);
            jda.shutdown();
        }
    }

    public static Bot getInstance() {
        return instance;
    }

    public void reloadBot(ServerCommandSource source) {
        token = AutoWhitelist.config.getConfigs().get("token").getAsString();
        appId = AutoWhitelist.config.getConfigs().get("application-id").getAsString();
        serverId = AutoWhitelist.config.getConfigs().get("discord-server-id").getAsString();
        prefix = AutoWhitelist.config.getConfigs().get("prefix").getAsString();
        updateDelay = AutoWhitelist.config.getConfigs().get("whitelist-auto-update-delay-seconds").getAsLong();
        source.sendFeedback(new LiteralText("Restarting the bot."), true);
        scheduledUpdate.cancel(true);
        jda.shutdown();
        init();
        source.sendFeedback(new LiteralText("Discord bot starting."), true);
    }

    private void init() {
        try {
            jda = JDABuilder.createDefault(token).enableIntents(GatewayIntent.GUILD_MEMBERS).setMemberCachePolicy(MemberCachePolicy.ALL).build();
            jda.addEventListener(this);
            jda.getPresence().setActivity(Activity.playing("on the Member Server"));
            instance = this;
        } catch (LoginException e) {
            AutoWhitelist.logger.error("Failed to start bot, got an Exception", e);
        }
    }

    @Override
    public void onReady(@NotNull ReadyEvent e) {
        AutoWhitelist.logger.info("Bot started. Parsing registered users.");
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
        scheduledUpdate = scheduler.scheduleWithFixedDelay(this::updateWhitelist, 0, updateDelay, TimeUnit.SECONDS);
    }

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent e) {
        User user = e.getUser();

        List<MemberPlayer> players = new ArrayList<>();
        new SQLite().getMembers().stream().filter(member -> user.getId().equals(member.getUserId())).findFirst().ifPresent(players::add);
        if (players.size() > 1) {
            AutoWhitelist.logger.error("Found more than one registered user with same discord id: " + user.getId());
            return;
        } else if (players.size() == 0) return;
        MemberPlayer player = players.get(0);

        if (!AutoWhitelist.server.getPlayerManager().isOperator(player.getProfile())) {
            new SQLite().removeMemberById(player.getUserId());
            AutoWhitelist.removePlayer(player.getProfile());
        }

        AutoWhitelist.updateWhitelist();
    }

    private void updateWhitelist() {
        List<String> ids = new SQLite().getIds();
        List<MemberPlayer> memberList = new SQLite().getMembers();
        Guild guild = jda.getGuildById(serverId);
        if (guild == null) {
            AutoWhitelist.logger.error("Failed to get discord server, got null");
            return;
        }
        guild.loadMembers().onSuccess(members -> {
            List<Member> users = members.stream().filter(member -> ids.contains(member.getId())).collect(Collectors.toList());

            for (Member user : users) {

                List<MemberPlayer> players = memberList.stream().filter(player -> user.getId().equals(player.getUserId())).collect(Collectors.toList());
                MemberPlayer player = players.get(0);

                List<String> roles = getMemberRoles();
                List<Role> userRoles = user.getRoles().stream().filter((role) -> roles.contains(role.getId())).collect(Collectors.toList());
                if (userRoles.size() >= 1) {
                    int higher = 0;
                    Role best = null;
                    for (Role role : userRoles) {
                        if (role.getPosition() > higher) {
                            higher = role.getPosition();
                            best = role;
                        }
                    }
                    if (best == null) {
                        AutoWhitelist.logger.error("Failed to get best tier role!");
                        return;
                    }
                    for (Map.Entry<String, JsonElement> entry : AutoWhitelist.config.getConfigs().get("whitelist").getAsJsonObject().entrySet()) {
                        JsonArray jsonArray = entry.getValue().getAsJsonArray();
                        for (JsonElement value : jsonArray) {
                            if (value.getAsString().equals(best.getId())) {
                                if (ids.contains(user.getId()) && !player.getTeam().equals(entry.getKey())) {
                                    try {
                                        new SQLite().updateData(user.getId(), getUsername(player.getProfile().getId().toString()), player.getProfile().getId().toString(), entry.getKey());
                                    } catch (IOException e) {
                                        AutoWhitelist.logger.error("Failed to get username!", e);
                                        return;
                                    }
                                }
                            }
                        }
                    }
                } else if (!AutoWhitelist.server.getPlayerManager().isOperator(player.getProfile())) {
                    new SQLite().removeMemberById(player.getUserId());
                    AutoWhitelist.removePlayer(player.getProfile());
                }
            }
            AutoWhitelist.updateWhitelist();
        });
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent e) {
        if (e.isWebhookMessage() || e.getAuthor().isBot() || e.getChannelType() != ChannelType.TEXT) return;
        if (!e.getMessage().getContentRaw().startsWith(prefix + "register")) return;
        Member author = e.getMember();
        if (author == null) { sendFeedbackMessage(e.getChannel(), "Something went wrong.", "Sorry I could not get your discord user. With so you haven't been added to the server."); return; }
        Message _message = e.getMessage();
        String message = _message.getContentRaw();
        message = message.replaceFirst(prefix + "register ", "");
        String[] values = message.split(" ");

        {
            if (new SQLite().getIds().contains(author.getId())) {
                sendFeedbackMessage(e.getChannel(), "You only can register one account", "You can't have more than one Minecraft account registered.");
                return;
            }
        }

        {
            if (values.length != 1) {
                sendFeedbackMessage(e.getChannel(), "Invalid command usage.", String.format("Please not that the command is `%sregister <minecraft nickname> <uuid>`\nExample: `%sregister Notch`", prefix, prefix));
                return;
            } else if (values[0].length() > 16 || values[0].length() < 3) {
                sendFeedbackMessage(e.getChannel(), "Invalid nickname.", "The nickname you inserted is either too big or too small.");
                return;
            }
        }

        sendTempFeedbackMessage(e.getChannel(), "Command feedback.", "Your request has been received and is being processed, if you don't get another feedback message in the next minute than please contact a moderator.", 10);

        List<String> roles = getMemberRoles();
        List<Role> userRoles = author.getRoles().stream().filter((role) -> roles.contains(role.getId())).collect(Collectors.toList());
        if (userRoles.size() >= 1) {
            int higher = 0;
            Role best = null;
            for (Role role : userRoles) {
                if (role.getPosition() > higher) {
                    higher = role.getPosition();
                    best = role;
                }
            }
            if (best == null) {
                sendFeedbackMessage(e.getChannel(), "Something really bad happened.", "I was unable to get your best member role, due to that issue I couldn't add you to the server, please inform a moderator or PhoenixSC.");
                return;
            }
            for (Map.Entry<String, JsonElement> entry : AutoWhitelist.config.getConfigs().get("whitelist").getAsJsonObject().entrySet()) {
                JsonArray jsonArray = entry.getValue().getAsJsonArray();
                for (JsonElement value : jsonArray) {
                    if (value.getAsString().equals(best.getId())) {
                        if (!new SQLite().getIds().contains(author.getId())) {
                            try {
                                String uuid = getUUID(values[0]);
                                List<GameProfile> players = new SQLite().getPlayers();
                                if (players.stream().map(GameProfile::getName).anyMatch(username -> username.equals(values[0])) && players.stream().map(GameProfile::getId).anyMatch(uuid_ -> uuid_.toString().equals(uuid))) {
                                    sendFeedbackMessage(e.getChannel(), "This account is already registered.", "The username you inserted is already registered in the database.");
                                    return;
                                }

                                new SQLite().addMember(author.getId(), values[0], uuid, entry.getKey());
                                AutoWhitelist.server.getPlayerManager().getWhitelist().add(new WhitelistEntry(new GameProfile(UUID.fromString(uuid), values[0])));
                                Scoreboard scoreboard = AutoWhitelist.server.getScoreboard();
                                Team team = scoreboard.getTeam(entry.getKey());
                                if (team == null) {
                                    sendFeedbackMessage(e.getChannel(), "Something really bad happened.", "I could not get the team equivalent to your member level, due to this issue I couldn't add you to the server, please inform a moderator or PhoenixSC.");
                                    return;
                                }
                                scoreboard.addPlayerToTeam(values[0], team);
                                sendFeedbackMessage(e.getChannel(), "Welcome to the group!", "Your Minecraft account has been added to the database and soon you will be able to join the server.");
                            } catch (IOException exception) {
                                sendFeedbackMessage(e.getChannel(), "Something really bad happened.", "I was unable to get your UUID, due to this issue I couldn't add you to the server, please inform a moderator or PhoenixSC.");
                                AutoWhitelist.logger.error("Failed to get UUID.", exception);
                                return;
                            } catch (InvalidResultException exception) {
                                sendFeedbackMessage(e.getChannel(), "Something went wrong.", "Seams that the username you inserted is not of an original Minecraft Java Edition account or the Mojang API is down. Please check if your username is correct, if it is than try again later.");
                                return;
                            }
                        }
                    }
                }
            }
        } else {
            sendFeedbackMessage(e.getChannel(), "Sorry, but I couldn't accept your request.", "It seams that you don't have the required subscription/member level or don't have your Twitch/Youtube account linked to your Discord account.");
        }
    }

    private List<String> getMemberRoles() {
        List<String> roles = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : AutoWhitelist.config.getConfigs().get("whitelist").getAsJsonObject().entrySet()) {
            entry.getValue().getAsJsonArray().forEach((element) -> roles.add(element.getAsString()));
        }
        return roles;
    }

    private String getUUID(String username) throws IOException, InvalidResultException {
        URL url = new URL(String.format("https://api.mojang.com/users/profiles/minecraft/%s", username));

        try (InputStream is = url.openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String jsonText = readAll(rd);
            JsonParser parser = new JsonParser();
            JsonElement json = parser.parse(jsonText);
            if (json.isJsonNull() || json.getAsJsonObject().get("id") == null)
                throw new InvalidResultException("Invalid username:" + username);
            String _uuid = json.getAsJsonObject().get("id").getAsString();
            if (_uuid.length() != 32) throw new InvalidResultException("Invalid UUID string:" + _uuid);
            String[] split = new String[]{_uuid.substring(0, 8), _uuid.substring(8, 12), _uuid.substring(12, 16), _uuid.substring(16, 20), _uuid.substring(20, 32)};
            return split[0] + "-" + split[1] + "-" + split[2] + "-" + split[3] + "-" + split[4];
        }
    }

    private String getUsername(String uuid) throws IOException {
        URL url = new URL(String.format("https://sessionserver.mojang.com/session/minecraft/profile/%s", uuid));

        try (InputStream is = url.openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String jsonText = readAll(rd);
            JsonParser parser = new JsonParser();
            JsonElement json = parser.parse(jsonText);
            return json.getAsJsonObject().get("name").getAsString();
        }
    }

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }
}
