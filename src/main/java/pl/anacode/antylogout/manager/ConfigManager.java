package pl.anacode.antylogout.manager;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import pl.anacode.antylogout.AnacodeAntylogout;

import java.util.List;

public class ConfigManager {

    private final AnacodeAntylogout plugin;
    private FileConfiguration config;

    public ConfigManager(AnacodeAntylogout plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public int getCombatTime() {
        return config.getInt("combat-time", 30);
    }

    public String getActionBarMessage() {
        return config.getString("messages.actionbar", "&7Jesteś podczas walki jeszcze &4%time_left%");
    }

    public String getCommandBlockedMessage() {
        return config.getString("messages.command-blocked", 
            "&7Jesteś podczas walki nie możesz teraz tego &4użyć&7!");
    }

    public String getCombatStartMessage() {
        return config.getString("messages.combat-start", "&cZostałeś oznaczony jako walczący!");
    }

    public String getCombatEndMessage() {
        return config.getString("messages.combat-end", "&aNie jesteś już w trybie walki!");
    }

    public String getRegionBlockedMessage() {
        return config.getString("messages.region-blocked", 
            "&cNie możesz wejść na ten region podczas walki!");
    }

    public String getVoidCombatMessage() {
        return config.getString("messages.void-combat", 
            "&cSpadasz w otchłań! Antylogout aktywny!");
    }

    public String getLogoutDeathMessage() {
        return config.getString("messages.logout-death",
            "&c%player% &7wylogował się podczas walki i zginął!");
    }

    public String getLogoutDeathKillerMessage() {
        return config.getString("messages.logout-death-killer",
            "&c%player% &7wylogował się podczas walki i został zabity przez &c%killer%&7!");
    }

    public List<String> getAllowedCommands() {
        return config.getStringList("allowed-commands");
    }

    public List<String> getBlockedRegions() {
        return config.getStringList("blocked-regions");
    }

    public int getWallDistance() {
        return config.getInt("wall-distance", 10);
    }

    public boolean isWallEnabled() {
        return config.getBoolean("wall.enabled", true);
    }

    public int getWallWidth() {
        return config.getInt("wall.width", 12);
    }

    public int getWallHeight() {
        return config.getInt("wall.height", 12);
    }

    public Material getWallMaterial() {
        String materialName = config.getString("wall.material", "RED_STAINED_GLASS");
        try {
            return Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            return Material.RED_STAINED_GLASS;
        }
    }

    public int getCornerSize() {
        return config.getInt("wall.corner-size", 2);
    }

    public boolean isKillOnLogout() {
        return config.getBoolean("kill-on-logout", true);
    }

    public boolean isVoidCombatEnabled() {
        return config.getBoolean("void-combat.enabled", true);
    }

    public List<String> getVoidCombatWorlds() {
        return config.getStringList("void-combat.worlds");
    }

    public int getVoidCombatBelowY() {
        return config.getInt("void-combat.below-y", -40);
    }

    public boolean isVoidCombatShowMessage() {
        return config.getBoolean("void-combat.show-message", true);
    }

    public boolean isLastDamagerEnabled() {
        return config.getBoolean("last-damager.enabled", true);
    }

    public int getLastDamagerMemoryTime() {
        return config.getInt("last-damager.memory-time", 60);
    }

    public boolean isGiveKillOnLogout() {
        return config.getBoolean("last-damager.give-kill-on-logout", true);
    }

    public boolean isBroadcastKiller() {
        return config.getBoolean("last-damager.broadcast-killer", true);
    }

    public boolean isGiveRewards() {
        return config.getBoolean("last-damager.give-rewards", false);
    }

    public List<String> getRewardCommands() {
        return config.getStringList("last-damager.reward-commands");
    }
}
