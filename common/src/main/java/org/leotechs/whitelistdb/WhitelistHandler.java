package org.leotechs.whitelistdb;

import java.util.UUID;

public class WhitelistHandler {

    private boolean whitelistEnabled;
    private final DbManager db;
    private final ConfigManager config;

    public WhitelistHandler(DbManager db, ConfigManager config) {
        this.db = db;
        this.config = config;
        this.whitelistEnabled = config.isEnabled();
    }

    public boolean isWhitelistEnabled() {
        return whitelistEnabled;
    }

    public void toggleWhitelist() {
        whitelistEnabled = !whitelistEnabled;
        config.setWhitelistEnabled(whitelistEnabled);
        config.save();
    }

    /** Returns true if player is allowed to join (not whitelisted or is on the list). */
    public boolean allowPlayer(UUID uuid) {
        if (!whitelistEnabled) return true;
        return db.isPlayerWhitelisted(uuid);
    }

    /** Returns true if player is NOT banned (i.e. should be allowed through). */
    public boolean checkBanned(UUID uuid) {
        return !db.isPlayerBanned(uuid);
    }
}
