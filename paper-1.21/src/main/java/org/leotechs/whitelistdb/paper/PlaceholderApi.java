package org.leotechs.whitelistdb.paper;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlaceholderApi extends PlaceholderExpansion{

    private final WhitelistDbPaper plugin;

    public PlaceholderApi(WhitelistDbPaper plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "whitelistdb";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Leo Midili";
    }

    @Override
    public @NotNull String getVersion() {
        return "v1.6.0";
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (identifier.equals("playerinfo")) {
            return plugin.getDatabaseManager().getPlayerPlaceholder(player.getUniqueId());
        }
        return null;
    }
}
