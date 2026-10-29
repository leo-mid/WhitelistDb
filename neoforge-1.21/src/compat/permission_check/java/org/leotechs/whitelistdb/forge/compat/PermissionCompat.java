package org.leotechs.whitelistdb.forge.compat;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * Minecraft 1.21.11: permissions moved to the new permission-check system (same as 26.x).
 */
public final class PermissionCompat {
    private PermissionCompat() {}

    public static <T extends ArgumentBuilder<CommandSourceStack, T>> T requireGamemaster(T builder) {
        return builder.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
    }
}
