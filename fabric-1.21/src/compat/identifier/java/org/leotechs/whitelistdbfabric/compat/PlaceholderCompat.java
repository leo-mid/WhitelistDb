package org.leotechs.whitelistdbfabric.compat;

import eu.pb4.placeholders.api.PlaceholderHandler;
import eu.pb4.placeholders.api.Placeholders;
import net.minecraft.resources.Identifier;

/**
 * Minecraft 1.21.11: {@code ResourceLocation} was renamed to {@code Identifier} in Mojang mappings.
 */
public final class PlaceholderCompat {
    private PlaceholderCompat() {}

    public static void register(String namespace, String path, PlaceholderHandler handler) {
        Placeholders.register(Identifier.fromNamespaceAndPath(namespace, path), handler);
    }
}
