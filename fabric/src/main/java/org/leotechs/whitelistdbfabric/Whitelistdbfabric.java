package org.leotechs.whitelistdbfabric;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.api.PlaceholderResult;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.leotechs.whitelistdb.ConfigManager;
import org.leotechs.whitelistdb.DbManager;
import org.leotechs.whitelistdb.PlayerCache;
import org.leotechs.whitelistdb.WhitelistHandler;
import org.leotechs.whitelistdbfabric.mixin.ServerLoginNetworkHandlerAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.UUID;

public class Whitelistdbfabric implements ModInitializer {

    public static final String MODID = "whitelistdb";
    private static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    private static WhitelistHandler whitelistHandler;
    private static ConfigManager configManager;
    private static DbManager dbManager;

    @Override
    public void onInitialize() {
        File configDir = new File("config");
        if (!configDir.exists()) configDir.mkdirs();

        configManager = new ConfigManager(configDir);
        dbManager     = new DbManager(configManager);

        PlayerCache.init(Path.of("config"));

        whitelistHandler = new WhitelistHandler(dbManager, configManager);

        registerEvents();
        registerCommands();
        registerPlaceholders();

        LOGGER.info("[WhitelistDB] Loaded. Whitelist enabled = {}", configManager.isEnabled());
    }

    // -------------------------------------------------------------------------
    //  Placeholders
    // -------------------------------------------------------------------------

    private void registerPlaceholders() {
        Placeholders.registerCommon(
                Identifier.fromNamespaceAndPath(MODID, "playerinfo"),
                (ctx, arg) -> {
                    if (ctx.player() == null) return PlaceholderResult.invalid("No player context");
                    UUID uuid  = ctx.player().getUUID();
                    String val = dbManager.getPlayerPlaceholder(uuid);
                    return val != null ? PlaceholderResult.value(val) : PlaceholderResult.value("");
                }
        );
    }

    // -------------------------------------------------------------------------
    //  Events
    // -------------------------------------------------------------------------

    private void registerEvents() {
        // Cache player name → UUID on join
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                PlayerCache.cachePlayer(
                        handler.getPlayer().getGameProfile().name(),
                        handler.getPlayer().getUUID()
                )
        );

        // Check whitelist / ban on login
        ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, synchronizer) -> {
            GameProfile profile = ((ServerLoginNetworkHandlerAccessor) handler).getProfile();
            if (profile == null) return;
            UUID uuid = profile.id();

            if (!whitelistHandler.allowPlayer(uuid)) {
                handler.disconnect(Component.literal(configManager.getMessage()));
                return;
            }
            if (!whitelistHandler.checkBanned(uuid)) {
                handler.disconnect(Component.literal(configManager.getBanReason())
                        .withStyle(ChatFormatting.RED));
            }
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server ->
                LOGGER.info("[WhitelistDB] Server started. Whitelist enabled = {}",
                        whitelistHandler.isWhitelistEnabled()));
    }

    // -------------------------------------------------------------------------
    //  Commands
    // -------------------------------------------------------------------------

    private void registerCommands() {
        // /whitelistdb toggle
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(
                        Commands.literal("whitelistdb")
                                .then(Commands.literal("toggle")
                                        .requires(src -> Permissions.check(src, "whitelistdb.admin", 4))
                                        .executes(ctx -> {
                                            whitelistHandler.toggleWhitelist();
                                            boolean enabled = whitelistHandler.isWhitelistEnabled();
                                            ctx.getSource().sendSuccess(
                                                    () -> Component.literal("Whitelist is now " + (enabled ? "ENABLED" : "DISABLED")),
                                                    true
                                            );
                                            return 1;
                                        })
                                )
                )
        );

        // /wban <player>
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(
                        Commands.literal("wban")
                                .requires(src -> Permissions.check(src, "whitelistdb.admin", 4))
                                .then(Commands.argument("player", StringArgumentType.greedyString())
                                        .executes(this::banPlayer))
                )
        );

        // /wunban <player>
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(
                        Commands.literal("wunban")
                                .requires(src -> Permissions.check(src, "whitelistdb.admin", 4))
                                .then(Commands.argument("player", StringArgumentType.greedyString())
                                        .executes(this::unbanPlayer))
                )
        );
    }

    private int banPlayer(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "player");

        MinecraftServer server   = source.getServer();
        PlayerList playerList    = server.getPlayerList();
        ServerPlayer onlinePlayer = playerList.getPlayerByName(name);

        UUID uuid;
        if (onlinePlayer != null) {
            uuid = onlinePlayer.getUUID();
        } else {
            uuid = PlayerCache.getUuid(name);
        }

        if (uuid == null) {
            source.sendFailure(Component.literal("Player '" + name + "' not found in cache."));
            return 0;
        }

        if (dbManager.banPlayer(uuid)) {
            // Kick if online
            if (onlinePlayer != null) {
                onlinePlayer.connection.disconnect(
                        Component.literal(configManager.getBanReason()).withStyle(ChatFormatting.RED));
            }
            source.sendSuccess(() -> Component.literal("Banned player: " + name), true);
            return 1;
        }

        source.sendFailure(Component.literal("Failed to ban player: " + name));
        return 0;
    }

    private int unbanPlayer(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "player");

        UUID uuid = PlayerCache.getUuid(name);
        if (uuid == null) {
            source.sendFailure(Component.literal("Player '" + name + "' not found in cache."));
            return 0;
        }

        if (dbManager.unbanPlayer(uuid)) {
            source.sendSuccess(() -> Component.literal("Unbanned player: " + name), true);
            return 1;
        }

        source.sendFailure(Component.literal("Failed to unban player: " + name));
        return 0;
    }
}
