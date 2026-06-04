package org.leotechs.whitelistdb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.UUID;

public class DbManager {

    private Connection conn;
    private static ConfigManager configManager;
    public static final Logger LOGGER = LoggerFactory.getLogger("whitelistdb");
    private String table;
    private String placeholder_column;

    public DbManager(ConfigManager configManager) {
        DbManager.configManager = configManager;
        ConfigManager.Config cfg = configManager.get();
        this.table = cfg.getTable();
        this.placeholder_column = cfg.getPlaceholderColumn();
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            LOGGER.error("PostgreSQL JDBC driver not found in classpath", e);
            return;
        }
        try {
            conn = DriverManager.getConnection(cfg.jdbcUrl(), cfg.getUsername(), cfg.getPassword());
            LOGGER.info("Connected to database successfully.");
        } catch (SQLException e) {
            LOGGER.error("Failed to connect to database", e);
        }
    }

    private Connection getConnection() {
        ConfigManager.Config cfg = configManager.get();
        try {
            if (conn == null || conn.isClosed()) {
                try {
                    Class.forName("org.postgresql.Driver");
                } catch (ClassNotFoundException e) {
                    LOGGER.error("PostgreSQL JDBC driver not found", e);
                    return null;
                }
                conn = DriverManager.getConnection(cfg.jdbcUrl(), cfg.getUsername(), cfg.getPassword());
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to reconnect to database", e);
            conn = null;
        }
        return conn;
    }

    public boolean isPlayerWhitelisted(UUID uuid) {
        Connection c = getConnection();
        if (c == null) return false;
        try (PreparedStatement st = c.prepareStatement(
                "SELECT 1 FROM " + table + " WHERE uuid = CAST(? AS uuid) LIMIT 1")) {
            st.setString(1, uuid.toString());
            try (ResultSet rs = st.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to check whitelist for {}", uuid, e);
        }
        return false;
    }

    public boolean isPlayerBanned(UUID uuid) {
        Connection c = getConnection();
        if (c == null) return false;
        try (PreparedStatement st = c.prepareStatement(
                "SELECT banned FROM " + table + " WHERE uuid = CAST(? AS uuid) LIMIT 1")) {
            st.setString(1, uuid.toString());
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) return rs.getBoolean("banned");
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to check ban status for {}", uuid, e);
        }
        return false;
    }

    public boolean banPlayer(UUID uuid) {
        Connection c = getConnection();
        if (c == null) return false;
        try (PreparedStatement st = c.prepareStatement(
                "UPDATE " + table + " SET banned = true WHERE uuid = CAST(? AS uuid)")) {
            st.setString(1, uuid.toString());
            st.executeUpdate();
            return true;
        } catch (SQLException e) {
            LOGGER.error("Failed to ban player {}", uuid, e);
        }
        return false;
    }

    public boolean unbanPlayer(UUID uuid) {
        Connection c = getConnection();
        if (c == null) return false;
        try (PreparedStatement st = c.prepareStatement(
                "UPDATE " + table + " SET banned = false WHERE uuid = CAST(? AS uuid)")) {
            st.setString(1, uuid.toString());
            st.executeUpdate();
            return true;
        } catch (SQLException e) {
            LOGGER.error("Failed to unban player {}", uuid, e);
        }
        return false;
    }

    public String getPlayerPlaceholder(UUID uuid) {
        Connection c = getConnection();
        if (c == null) return null;
        try (PreparedStatement st = c.prepareStatement(
                "SELECT " + placeholder_column + " FROM " + table + " WHERE uuid = CAST(? AS uuid)")) {
            st.setString(1, uuid.toString());
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) return rs.getString(placeholder_column);
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to get placeholder for {}", uuid, e);
        }
        return null;
    }
}
