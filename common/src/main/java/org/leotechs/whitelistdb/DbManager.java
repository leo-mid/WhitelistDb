package org.leotechs.whitelistdb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.UUID;
import java.util.regex.Pattern;

public class DbManager implements AutoCloseable {

    // Only allow simple identifiers: letters, digits, underscores, dots – no spaces or SQL metacharacters.
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[\\w.]{1,64}$");

    private Connection conn;
    private static ConfigManager configManager;
    public static final Logger LOGGER = LoggerFactory.getLogger("whitelistdb");
    private final String table;
    private final String placeholderColumn;

    public DbManager(ConfigManager configManager) {
        DbManager.configManager = configManager;
        ConfigManager.Config cfg = configManager.get();

        String t  = cfg.getTable();
        String pc = cfg.getPlaceholderColumn();
        if (!SAFE_IDENTIFIER.matcher(t).matches()) {
            throw new IllegalArgumentException("Unsafe table name in config: " + t);
        }
        if (!SAFE_IDENTIFIER.matcher(pc).matches()) {
            throw new IllegalArgumentException("Unsafe placeholder_column name in config: " + pc);
        }
        this.table             = t;
        this.placeholderColumn = pc;

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

    private synchronized Connection getConnection() {
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
                LOGGER.info("Reconnected to database.");
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
        // table is validated at construction time – safe to embed
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
        // placeholderColumn is validated at construction time – safe to embed
        try (PreparedStatement st = c.prepareStatement(
                "SELECT " + placeholderColumn + " FROM " + table + " WHERE uuid = CAST(? AS uuid)")) {
            st.setString(1, uuid.toString());
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) return rs.getString(placeholderColumn);
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to get placeholder for {}", uuid, e);
        }
        return null;
    }

    @Override
    public synchronized void close() {
        if (conn != null) {
            try {
                conn.close();
                LOGGER.info("[WhitelistDB] Database connection closed.");
            } catch (SQLException e) {
                LOGGER.error("Error closing database connection", e);
            } finally {
                conn = null;
            }
        }
    }
}