package pl.anacode.antylogout.hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.anacode.antylogout.AnacodeAntylogout;

public class PlaceholderHook extends PlaceholderExpansion {

    private final AnacodeAntylogout plugin;

    public PlaceholderHook(AnacodeAntylogout plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "anacode";
    }

    @Override
    public @NotNull String getAuthor() {
        return "ANACODE";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        switch (params.toLowerCase()) {
            case "kills":
                return String.valueOf(plugin.getStatsManager().getKills(player.getUniqueId()));
            
            case "deaths":
                return String.valueOf(plugin.getStatsManager().getDeaths(player.getUniqueId()));
            
            case "kdr":
                return String.valueOf(plugin.getStatsManager().getKDR(player.getUniqueId()));
            
            case "incombat":
                if (player.isOnline()) {
                    return plugin.getCombatManager().isInCombat(player.getPlayer()) ? "Tak" : "Nie";
                }
                return "Nie";
            
            case "combattime":
                if (player.isOnline()) {
                    return String.valueOf(plugin.getCombatManager().getRemainingTime(player.getPlayer()));
                }
                return "0";
            
            default:
                return null;
        }
    }
}
