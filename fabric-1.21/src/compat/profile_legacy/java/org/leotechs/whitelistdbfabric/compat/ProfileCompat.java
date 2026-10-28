package org.leotechs.whitelistdbfabric.compat;

import com.mojang.authlib.GameProfile;

import java.util.UUID;

/**
 * Minecraft 1.21 - 1.21.8 (authlib 6): GameProfile is a regular class.
 */
public final class ProfileCompat {
    private ProfileCompat() {}

    public static UUID id(GameProfile profile) {
        return profile.getId();
    }

    public static String name(GameProfile profile) {
        return profile.getName();
    }
}
