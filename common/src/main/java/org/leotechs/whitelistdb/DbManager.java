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
    private final String banColumn;
    private final boolean banReasonsEnabled;
    private final String banReasonColumn;

    /** Whether a player is banned and, if ban reasons are enabled, why. */
    public record BanStatus(boolean banned, String reason) {
        public static final BanStatus NOT_BANNED = new BanStatus(false, null);
    }

    public DbManager(ConfigManager configManager) {
        DbManager.configManager = configManager;
        ConfigManager.Config cfg = configManager.get();

        // Table and column names can't be passed as SQL parameters, so they're validated here and
        // then embedded in the queries below.
        this.table             = requireSafeIdentifier("table", cfg.getTable());
        this.placeholderColumn = requireSafeIdentifier("placeholder_column", cfg.getPlaceholderColumn());
        this.banColumn         = requireSafeIdentifier("ban_column", cfg.getBanColumn());
        this.banReasonsEnabled = cfg.isBanReasonsEnabled();
        this.banReasonColumn   = banReasonsEnabled
                ? requireSafeIdentifier("ban_reason_column", cfg.getBanReasonColumn())
                : null;

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

    private static String requireSafeIdentifier(String setting, String value) {
        if (value == null || !SAFE_IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException("Unsafe " + setting + " name in config: " + value);
        }
        return value;
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

    /**
     * Marks the player as banned. When ban reasons are enabled the reason column is set too
     * (cleared if {@code reason} is null); when they're disabled the reason is ignored.
     */
    public boolean banPlayer(UUID uuid, String reason) {
        Connection c = getConnection();
        if (c == null) return false;
        if(configManager.isBanReasonsEnabled()){
            try (PreparedStatement st = c.prepareStatement(
                    "UPDATE " + table + " SET banned = true, " + configManager.getBanReasonColumn() + " = ? WHERE uuid = CAST(? AS uuid)")) {
                st.setString(1, reason);
                st.setString(2, uuid.toString());
                st.executeUpdate();
                return true;
            } catch (SQLException e) {
                LOGGER.error("Failed to ban player {}", uuid, e);
            }
        } else{
            try (PreparedStatement st = c.prepareStatement(
                    "UPDATE " + table + " SET banned = true WHERE uuid = CAST(? AS uuid)")) {
                st.setString(1, uuid.toString());
                st.executeUpdate();
                return true;
            } catch (SQLException e) {
                LOGGER.error("Failed to ban player {}", uuid, e);
            }
        }
        return false;
    }

    /** Unbans the player and, when ban reasons are enabled, clears the stored reason. */
    public boolean unbanPlayer(UUID uuid) {
        Connection c = getConnection();
        if (c == null) return false;
        String sql = banReasonsEnabled
                ? "UPDATE " + table + " SET " + banColumn + " = false, " + banReasonColumn + " = NULL WHERE uuid = CAST(? AS uuid)"
                : "UPDATE " + table + " SET " + banColumn + " = false WHERE uuid = CAST(? AS uuid)";
        try (PreparedStatement st = c.prepareStatement(sql)) {
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