package org.leotechs.whitelistdb.forge;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.leotechs.whitelistdb.ConfigManager;
import org.leotechs.whitelistdb.DbManager;
import org.leotechs.whitelistdb.PlayerCache;
import org.leotechs.whitelistdb.WhitelistHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.UUID;

@Mod("whitelistdb")
public class WhitelistDbForge {

    public static final String MODID = "whitelistdb";
    private static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    private static ConfigManager configManager;
    private static DbManager dbManager;
    private static WhitelistHandler whitelistHandler;

    public WhitelistDbForge(IEventBus modEventBus) {
        File configDir = new File("config");
        if (!configDir.exists()) configDir.mkdirs();

        configManager    = new ConfigManager(configDir);
        dbManager        = new DbManager(configManager);
        whitelistHandler = new WhitelistHandler(dbManager, configManager);

        PlayerCache.init(Path.of("config"));

        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);

        LOGGER.info("[WhitelistDB] NeoForge module initialized.");
    }

    private void onServerStarted(ServerStartedEvent event) {
        LOGGER.info("[WhitelistDB] Server started. Whitelist enabled = {}",
                whitelistHandler.isWhitelistEnabled());
    }

    
    private void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        UUID uuid = player.getUUID();
        PlayerCache.cachePlayer(player.getGameProfile().name(), uuid);

        if (!whitelistHandler.allowPlayer(uuid)) {
            player.connection.disconnect(Component.literal(configManager.getMessage()));
            return;
        }

        if (!whitelistHandler.checkBanned(uuid)) {
            player.connection.disconnect(
                    Component.literal(configManager.getBanReason()).withStyle(ChatFormatting.RED));
        }
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("whitelistdb")
                        .then(Commands.literal("toggle")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .executes(ctx -> {
                                    whitelistHandler.toggleWhitelist();
                                    boolean en = whitelistHandler.isWhitelistEnabled();
                                    ctx.getSource().sendSuccess(
                                            () -> Component.literal("Whitelist is now " + (en ? "ENABLED" : "DISABLED")),
                                            true
                                    );
                                    return 1;
                                })
                        )
        );

        dispatcher.register(
                Commands.literal("wban")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("player", StringArgumentType.greedyString())
                                .executes(this::banPlayer))
        );

        dispatcher.register(
                Commands.literal("wunban")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("player", StringArgumentType.greedyString())
                                .executes(this::unbanPlayer))
        );
    }

    private int banPlayer(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "player");

        MinecraftServer server = source.getServer();
        PlayerList playerList  = server.getPlayerList();
        ServerPlayer online    = playerList.getPlayerByName(name);

        UUID uuid = online != null ? online.getUUID() : PlayerCache.getUuid(name);

        if (uuid == null) {
            source.sendFailure(Component.literal("Player '" + name + "' not found in cache."));
            return 0;
        }

        if (dbManager.banPlayer(uuid)) {
            if (online != null) {
                online.connection.disconnect(
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
