package org.leotechs.whitelistdb.forge.compat;

import com.mojang.authlib.GameProfile;

/**
 * Minecraft 1.21 - 1.21.8 (authlib 6): GameProfile is a regular class.
 */
public final class ProfileCompat {
    private ProfileCompat() {}

    public static String name(GameProfile profile) {
        return profile.getName();
    }
}
