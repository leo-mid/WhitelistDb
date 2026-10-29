package org.leotechs.whitelistdb.forge.compat;

import com.mojang.authlib.GameProfile;

/**
 * Minecraft 1.21.9 - 1.21.11 (authlib 7): GameProfile is a record.
 */
public final class ProfileCompat {
    private ProfileCompat() {}

    public static String name(GameProfile profile) {
        return profile.name();
    }
}
