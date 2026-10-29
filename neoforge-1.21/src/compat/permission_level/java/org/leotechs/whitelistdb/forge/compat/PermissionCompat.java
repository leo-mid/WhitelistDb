package org.leotechs.whitelistdb.forge.compat;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * Minecraft 1.21 - 1.21.10: permission levels are plain ints checked with {@code source.hasPermission(int)}.
 */
public final class PermissionCompat {
    private PermissionCompat() {}

    public static <T extends ArgumentBuilder<CommandSourceStack, T>> T requireGamemaster(T builder) {
        return builder.requires(src -> src.hasPermission(Commands.LEVEL_GAMEMASTERS));
    }
}
