package com.awakenedredstone.autowhitelist.database;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.util.MemberPlayer;
import com.mojang.authlib.GameProfile;

import java.io.File;
import java.sql.*;
import java.util.*;

public class SQLite {
    public static Connection connection;

    public void connect() {
        File dir = new File("./config/AutoWhitelist");
        if(!dir.exists() || !dir.isDirectory()) {
            AutoWhitelist.logger.info("Could not find database. A new one will be created.");
            dir.mkdir();
        }

        try {
            String url = "jdbc:sqlite:config/AutoWhitelist/users.db";
            connection = DriverManager.getConnection(url);

        } catch (SQLException e) {
            AutoWhitelist.logger.error("Failed to load database", e);
            return;
        }
        try {
            Statement statement = connection.createStatement();
            statement.execute("CREATE TABLE IF NOT EXISTS \"data\" (" +
                    "\"ID\" TEXT NOT NULL UNIQUE, " +
                    "\"UUID\" TEXT NOT NULL UNIQUE, " +
                    "\"USERNAME\" TEXT NOT NULL UNIQUE, " +
                    "\"TEAM\" TEXT NOT NULL)");
        } catch (SQLException e) {
            AutoWhitelist.logger.error("Failed to load database!", e);
        }
    }

    public void updateData(String id, String username, String uuid, String team) {
        if (connection == null) {
            AutoWhitelist.logger.error("Connection to database not existent. Unable to query data.");
            return;
        }
        try {
            PreparedStatement statement = connection.prepareStatement("UPDATE `data` SET \"USERNAME\"=?, \"TEAM\"=? WHERE \"ID\"=? AND \"UUID\"=?");
            statement.setString(1, username);
            statement.setString(2, team);
            statement.setString(3, id);
            statement.setString(4, uuid);
            statement.executeUpdate();
        } catch (SQLException e) {
            AutoWhitelist.logger.error("Failed to update the database!", e);
        }
    }

    public void updateData(String id, String username, String uuid) {
        if (connection == null) {
            AutoWhitelist.logger.error("Connection to database not existent. Unable to query data.");
            return;
        }
        try {
            PreparedStatement statement = connection.prepareStatement("UPDATE `data` SET \"USERNAME\"=? WHERE \"ID\"=? AND \"UUID\"=?");
            statement.setString(1, username);
            statement.setString(2, id);
            statement.setString(3, uuid);
            statement.executeUpdate();
        } catch (SQLException e) {
            AutoWhitelist.logger.error("Failed to update the database!", e);
        }
    }

    public List<String> getIds() {
        if (connection == null) {
            AutoWhitelist.logger.error("Connection to database not existent. Unable to query data.");
            return Collections.emptyList();
        }
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM `data`");
            ResultSet results = statement.executeQuery();
            ArrayList<String> ids = new ArrayList<>();
            while (results.next()) {
                ids.add(results.getString("ID"));
            }
            return ids;
        } catch (SQLException e) {
            AutoWhitelist.logger.error("Failed to get the data from the database!", e);
            return Collections.emptyList();
        }
    }

    public List<MemberPlayer> getMembers() {
        if (connection == null) {
            AutoWhitelist.logger.error("Connection to database not existent. Unable to query data.");
            return Collections.emptyList();
        }
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM `data`");
            ResultSet results = statement.executeQuery();
            ArrayList<MemberPlayer> players = new ArrayList<>();
            while (results.next()) {
                players.add(new MemberPlayer(new GameProfile(UUID.fromString(results.getString("UUID")), results.getString("USERNAME")), results.getString("TEAM"), results.getString("ID")));
            }
            return players;
        } catch (SQLException e) {
            AutoWhitelist.logger.error("Failed to get the data from the database!", e);
            return Collections.emptyList();
        }
    }

    public List<GameProfile> getPlayers() {
        if (connection == null) {
            AutoWhitelist.logger.error("Connection to database not existent. Unable to query data.");
            return Collections.emptyList();
        }
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM `data`");
            ResultSet results = statement.executeQuery();
            ArrayList<GameProfile> players = new ArrayList<>();
            while (results.next()) {
                players.add(new GameProfile(UUID.fromString(results.getString("UUID")), results.getString("USERNAME")));
            }
            return players;
        } catch (SQLException e) {
            AutoWhitelist.logger.error("Failed to get the data from the database!", e);
            return Collections.emptyList();
        }
    }

    public List<String> getUsernames() {
        if (connection == null) {
            AutoWhitelist.logger.error("Connection to database not existent. Unable to query data.");
            return Collections.emptyList();
        }
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM `data`");
            ResultSet results = statement.executeQuery();
            ArrayList<String> usernames = new ArrayList<>();
            while (results.next()) {
                usernames.add(results.getString("USERNAME"));
            }
            return usernames;
        } catch (SQLException e) {
            AutoWhitelist.logger.error("Failed to get the data from the database!", e);
            return Collections.emptyList();
        }
    }

    public void addMember(String id, String username, String uuid, String team) {
        if (connection == null) {
            AutoWhitelist.logger.error("Connection to database not existent. Unable to query data.");
            return;
        }
        try {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO `data` VALUES (?,?,?,?)");
            statement.setString(1, id);
            statement.setString(2, uuid);
            statement.setString(3, username);
            statement.setString(4, team);
            statement.executeUpdate();
        } catch (SQLException e) {
            AutoWhitelist.logger.error("Failed to update the database!", e);
        }
    }

    public boolean removeMemberById(String id) {
        if (connection == null) {
            AutoWhitelist.logger.error("Connection to database not existent. Unable to query data.");
            return false;
        }
        try {
            PreparedStatement statement = connection.prepareStatement("DELETE FROM `data` WHERE \"ID\"=?");
            statement.setString(1, id);
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            AutoWhitelist.logger.error("Failed to update the database!", e);
            return false;
        }
    }

    public boolean removeMemberByNick(String nickname) {
        if (connection == null) {
            AutoWhitelist.logger.error("Connection to database not existent. Unable to query data.");
            return false;
        }
        try {
            PreparedStatement statement = connection.prepareStatement("DELETE FROM `data` WHERE \"USERNAME\"=?");
            statement.setString(1, nickname);
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            AutoWhitelist.logger.error("Failed to update the database!", e);
            return false;
        }
    }

    public boolean removeMemberByUuid(String uuid) {
        if (connection == null) {
            AutoWhitelist.logger.error("Connection to database not existent. Unable to query data.");
            return false;
        }
        try {
            PreparedStatement statement = connection.prepareStatement("DELETE FROM `data` WHERE \"UUID\"=?");
            statement.setString(1, uuid);
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            AutoWhitelist.logger.error("Failed to update the database!", e);
            return false;
        }
    }
}
