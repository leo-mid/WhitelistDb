package org.leotechs.whitelistdb.paper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.leotechs.whitelistdb.ConfigManager;
import org.leotechs.whitelistdb.DbManager;
import org.leotechs.whitelistdb.PlayerCache;
import org.leotechs.whitelistdb.WhitelistHandler;

import java.io.File;
import java.nio.file.Path;
import java.util.UUID;

public class WhitelistDbPaper extends JavaPlugin implements Listener {

    private ConfigManager configManager;
    private DbManager dbManager;
    private WhitelistHandler whitelistHandler;

    @Override
    public void onEnable() {
        File configDir = getDataFolder().getParentFile();

        File rootConfigDir = new File("config");
        if (!rootConfigDir.exists()) rootConfigDir.mkdirs();

        configManager    = new ConfigManager(rootConfigDir);
        dbManager        = new DbManager(configManager);
        whitelistHandler = new WhitelistHandler(dbManager, configManager);

        PlayerCache.init(Path.of("config"));

        Bukkit.getPluginManager().registerEvents(this, this);

        getLogger().info("[WhitelistDB] Paper plugin enabled. Whitelist = " + configManager.isEnabled());

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderApi(this).register();
            getLogger().info("PlaceholderAPI detected - registered %whitelistdb_playerinfo%");
        }
    }

    @Override
    public void onDisable() {
        if (dbManager != null) {
            dbManager.close();
        }
        getLogger().info("[WhitelistDB] Paper plugin disabled.");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();

        if (!whitelistHandler.allowPlayer(uuid)) {
            event.disallow(
                    AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST,
                    Component.text(configManager.getMessage())
            );
            return;
        }

        if (!whitelistHandler.checkBanned(uuid)) {
            event.disallow(
                    AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                    Component.text(configManager.getBanReason()).color(NamedTextColor.RED)
            );
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerCache.cachePlayer(player.getName(), player.getUniqueId());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName().toLowerCase()) {
            case "whitelistdb" -> {
                if (args.length == 1 && args[0].equalsIgnoreCase("toggle")) {
                    if (!sender.hasPermission("whitelistdb.admin")) {
                        sender.sendMessage(Component.text("You don't have permission.").color(NamedTextColor.RED));
                        return true;
                    }
                    whitelistHandler.toggleWhitelist();
                    boolean enabled = whitelistHandler.isWhitelistEnabled();
                    sender.sendMessage(Component.text(
                            "Whitelist is now " + (enabled ? "ENABLED" : "DISABLED")));
                } else {
                    sender.sendMessage(Component.text("Usage: /whitelistdb toggle"));
                }
                return true;
            }

            case "wban" -> {
                if (!sender.hasPermission("whitelistdb.admin")) {
                    sender.sendMessage(Component.text("You don't have permission.").color(NamedTextColor.RED));
                    return true;
                }
                if (args.length < 1) {
                    sender.sendMessage(Component.text("Usage: /wban <player>"));
                    return true;
                }
                String targetName = String.join(" ", args);
                return handleBan(sender, targetName);
            }

            case "wunban" -> {
                if (!sender.hasPermission("whitelistdb.admin")) {
                    sender.sendMessage(Component.text("You don't have permission.").color(NamedTextColor.RED));
                    return true;
                }
                if (args.length < 1) {
                    sender.sendMessage(Component.text("Usage: /wunban <player>"));
                    return true;
                }
                String targetName = String.join(" ", args);
                return handleUnban(sender, targetName);
            }
        }
        return false;
    }

    private boolean handleBan(CommandSender sender, String name) {

        Player online = Bukkit.getPlayerExact(name);
        UUID uuid     = online != null ? online.getUniqueId() : PlayerCache.getUuid(name);

        if (uuid == null) {
            sender.sendMessage(Component.text("Player '" + name + "' not found in cache.").color(NamedTextColor.RED));
            return true;
        }

        if (dbManager.banPlayer(uuid)) {
            if (online != null) {
                online.kick(Component.text(configManager.getBanReason()).color(NamedTextColor.RED));
            }
            sender.sendMessage(Component.text("Banned player: " + name));
        } else {
            sender.sendMessage(Component.text("Failed to ban player: " + name).color(NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleUnban(CommandSender sender, String name) {
        UUID uuid = PlayerCache.getUuid(name);

        if (uuid == null) {
            sender.sendMessage(Component.text("Player '" + name + "' not found in cache.").color(NamedTextColor.RED));
            return true;
        }

        if (dbManager.unbanPlayer(uuid)) {
            sender.sendMessage(Component.text("Unbanned player: " + name));
        } else {
            sender.sendMessage(Component.text("Failed to unban player: " + name).color(NamedTextColor.RED));
        }
        return true;
    }

    public DbManager getDatabaseManager() {
        return dbManager;
    }
}
