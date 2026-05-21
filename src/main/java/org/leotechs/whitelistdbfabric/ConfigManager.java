package org.leotechs.whitelistdbfabric;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;

public class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /// Makes the config class with its default settings

    public static class Config {
        private String host = "localhost";
        private int port = 5432;
        private String database = "minecraft";
        private String username = "postgres";
        private String password = "password";
        private String table = "server_whitelists";
        private String placeholder_column = "placeholder_column";
        private boolean ssl = false;
        private String message = "You are not whitelisted!";
        private boolean enabled = true;
        private String banReason = "You have been banned!";


        public String jdbcUrl() {
            return "jdbc:postgresql://" + host + ":" + port + "/" + database +
                    (ssl ? "?sslmode=require" : "");
        }

        /// Returns the database username
        /// @return this.username
        public String getUsername(){
            return this.username;
        }

        /// Returns the database password
        /// @return this.password
        public String getPassword(){
            return this.password;
        }

        /// Returns the database table
        /// @return this.table
        public String getTable(){
            return this.table;
        }

        /// Returns the database placeholder column
        /// @return this.placeholder_column
        public String getPlaceholderColumn(){
            return this.placeholder_column;
        }
    }

    private final File configFile;
    private Config config;

    /// Creates the config manager object
    /// @param configDir - The directory to save the config file

    public ConfigManager(File configDir) {
        this.configFile = new File(configDir, "whitelistdb-config.json");
        load();
    }

    /// Returns the config object
    /// @return this.config

    public Config get() {
        return config;
    }

    /// Loads the config

    public void load() {
        try {
            if (!configFile.exists()) {
                config = new Config();
                save(); // create default config
                return;
            }

            try (FileReader reader = new FileReader(configFile, StandardCharsets.UTF_8)) {
                config = GSON.fromJson(reader, Config.class);
            }

        } catch (Exception e) {
            e.printStackTrace();
            config = new Config();
        }
    }

    // Saves the config
    public void save() {
        try (FileWriter writer = new FileWriter(configFile, StandardCharsets.UTF_8)) {
            GSON.toJson(config, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /// Gets the whitelist kick message
    /// @returns config.message
    public String getMessage() {
        return config.message;
    }

    /// Checks to see if the whitelist is enabled or not
    /// @returns config.enabled
    public boolean isEnabled() {
        return config.enabled;
    }

    /// Changes the whitelist status
    /// @param enabled - The new status of the whitelist
    public void setWhitelistEnabled(boolean enabled) {
        config.enabled = enabled;
    }

    /// Gets the banned message reason
    /// @returns config.banReason
    public String getBanReason() {
        return config.banReason;
    }
}