package org.leotechs.whitelistdb;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;

public class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Logger LOGGER = LoggerFactory.getLogger("whitelistdb");

    public static class Config {
        private String host = "localhost";
        private int port = 5432;
        private String database = "minecraft";
        private String username = "postgres";
        private String password = "password";
        private String table = "server_whitelists";
        private String placeholderColumn = "tag";
        private String banColumn = "banned";
        private boolean banReasonsEnabled = false;
        private String banReasonColumn = "ban_reason";
        private boolean ssl = false;
        private String message = "You are not whitelisted!";
        private boolean enabled = true;
        private String banReason = "You have been banned!";
        private String banMessageWithReason = "You have been banned!\nReason: %reason%";

        public String jdbcUrl() {
            return "jdbc:postgresql://" + host + ":" + port + "/" + database
                    + (ssl ? "?sslmode=require" : "");
        }

        public String getUsername()          { return username; }
        public String getPassword()          { return password; }
        public String getTable()             { return table; }
        public String getPlaceholderColumn() { return placeholderColumn; }
        public String getMessage()           { return message; }
        public boolean isEnabled()           { return enabled; }
        public String getBanReason()         { return banReason; }
        public String getBanColumn()         { return banColumn; }
        public boolean isBanReasonsEnabled() { return banReasonsEnabled; }
        public String getBanReasonColumn()   { return banReasonColumn; }
        public String getBanMessageWithReason() { return banMessageWithReason; }
    }

    private final File configFile;
    private Config config;

    public ConfigManager(File configDir) {
        this.configFile = new File(configDir, "whitelistdb-config.json");
        load();
    }

    public Config get() { return config; }

    public void load() {
        try {
            if (!configFile.exists()) {
                config = new Config();
                save();
                return;
            }
            try (FileReader reader = new FileReader(configFile, StandardCharsets.UTF_8)) {
                config = GSON.fromJson(reader, Config.class);
            }
            if (config == null) config = new Config();
            // Write the file back so settings added in newer versions show up in older config files
            save();
        } catch (Exception e) {
            LOGGER.error("Failed to load config, using defaults", e);
            config = new Config();
        }
    }

    public void save() {
        try (FileWriter writer = new FileWriter(configFile, StandardCharsets.UTF_8)) {
            GSON.toJson(config, writer);
        } catch (Exception e) {
            LOGGER.error("Failed to save config", e);
        }
    }

    public String getMessage()    { return config.getMessage(); }
    public boolean isEnabled()    { return config.isEnabled(); }
    public String getBanReason()  { return config.getBanReason(); }
    public boolean isBanReasonsEnabled() { return config.isBanReasonsEnabled(); }
    public String getBanMessageWithReason() { return config.getBanMessageWithReason(); }
    public String getBanReasonColumn() { return config.getBanReasonColumn(); }

    public void setWhitelistEnabled(boolean enabled) {
        config.enabled = enabled;
    }
}