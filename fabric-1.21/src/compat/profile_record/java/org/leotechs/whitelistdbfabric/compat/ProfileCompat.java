package org.leotechs.whitelistdbfabric.compat;

import com.mojang.authlib.GameProfile;

import java.util.UUID;

/**
 * Minecraft 1.21.9 - 1.21.11 (authlib 7): GameProfile is a record.
 */
public final class ProfileCompat {
    private ProfileCompat() {}

    public static UUID id(GameProfile profile) {
        return profile.id();
    }

    public static String name(GameProfile profile) {
        return profile.name();
    }
}
