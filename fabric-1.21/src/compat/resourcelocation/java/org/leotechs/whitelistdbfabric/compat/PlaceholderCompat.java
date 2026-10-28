package org.leotechs.whitelistdbfabric.compat;

import eu.pb4.placeholders.api.PlaceholderHandler;
import eu.pb4.placeholders.api.Placeholders;
import net.minecraft.resources.ResourceLocation;

/**
 * Minecraft 1.21 - 1.21.10: identifiers are {@code ResourceLocation} in Mojang mappings.
 */
public final class PlaceholderCompat {
    private PlaceholderCompat() {}

    public static void register(String namespace, String path, PlaceholderHandler handler) {
        Placeholders.register(ResourceLocation.fromNamespaceAndPath(namespace, path), handler);
    }
}
