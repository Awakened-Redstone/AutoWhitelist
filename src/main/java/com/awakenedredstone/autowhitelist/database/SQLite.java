package com.awakenedredstone.autowhitelist.database;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.util.ExtendedGameProfile;
import com.awakenedredstone.autowhitelist.whitelist.ExtendedWhitelistEntry;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.UUID;

public class SQLite {
    private static Connection connection;

    public void connect() {
        File dir = new File("./config/AutoWhitelist");
        if(!dir.exists() || !dir.isDirectory()) {
            //AutoWhitelist.LOGGER.info("Could not find database. A new one will be created.");
            return;
        }

        File file = new File("./config/AutoWhitelist/users.db");
        if (!file.exists()) return;

        try {
            String url = "jdbc:sqlite:config/AutoWhitelist/users.db";
            connection = DriverManager.getConnection(url);

        } catch (SQLException e) {
            AutoWhitelist.LOGGER.error("Failed to load old database", e);
            return;
        }
        databaseChange();
    }

    private void databaseChange() {
        if (connection == null) {
            AutoWhitelist.LOGGER.error("Connection to database not existent. Unable to query data.");
            return;
        }
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM `data`");
            ResultSet results = statement.executeQuery();
            ArrayList<ExtendedGameProfile> players = new ArrayList<>();
            while (results.next()) {
                players.add(new ExtendedGameProfile(UUID.fromString(results.getString("UUID")), results.getString("USERNAME"), results.getString("TEAM"), results.getString("ID")));
            }
            AutoWhitelist.server.getPlayerManager().getWhitelist().values().clear();
            players.forEach(v -> AutoWhitelist.server.getPlayerManager().getWhitelist().add(new ExtendedWhitelistEntry(v)));
            connection.close();
            new File("./config/AutoWhitelist/users.db").delete();
            new File("./config/AutoWhitelist").delete();
        } catch (SQLException e) {
            AutoWhitelist.LOGGER.error("Failed to get the data from the old database", e);
        }
    }
}
