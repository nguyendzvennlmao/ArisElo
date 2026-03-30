package me.aris.ariselo;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class EloExpansion extends PlaceholderExpansion {
    private final ArisElo plugin;

    public EloExpansion(ArisElo plugin) { this.plugin = plugin; }

    @Override
    public @NotNull String getIdentifier() { return "ariselo"; }
    @Override
    public @NotNull String getAuthor() { return "VennLMAO"; }
    @Override
    public @NotNull String getVersion() { return "1.0"; }
    @Override
    public boolean persist() { return true; }

    @Override
    public String onPlaceholderRequest(Player p, @NotNull String identifier) {
        if (p == null) return "";
        int elo = plugin.getElo(p.getUniqueId());

        switch (identifier.toLowerCase()) {
            case "rank": return plugin.translate(plugin.getRank(elo));
            case "elo": return String.valueOf(elo);
            case "next": return String.valueOf(plugin.getNextRankElo(elo));
        }
        return null;
    }
}
