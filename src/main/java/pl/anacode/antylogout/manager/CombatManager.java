package pl.anacode.antylogout.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pl.anacode.antylogout.AnacodeAntylogout;
import pl.anacode.antylogout.util.MessageUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CombatManager {

    private final AnacodeAntylogout plugin;
    private final Map<UUID, Long> combatPlayers;
    private final int combatTime;

    public CombatManager(AnacodeAntylogout plugin) {
        this.plugin = plugin;
        this.combatPlayers = new ConcurrentHashMap<>();
        this.combatTime = plugin.getConfig().getInt("settings.combat-time", 30);
    }

    public void tagPlayer(Player player) {
        if (player == null) return;
        if (player.hasPermission("anacode.antylogout.bypass")) return;
        if (isWorldDisabled(player.getWorld().getName())) return;

        boolean wasInCombat = isInCombat(player);
        combatPlayers.put(player.getUniqueId(), System.currentTimeMillis() + (combatTime * 1000L));

        if (!wasInCombat) {
            String message = plugin.getConfig().getString("messages.combat-enter", "&7Wszedles w &4tryb walki&7!");
            MessageUtil.sendMessage(player, message);
        }
    }

    public void tagBoth(Player attacker, Player victim) {
        tagPlayer(attacker);
        tagPlayer(victim);
    }

    public boolean isInCombat(Player player) {
        if (player == null) return false;

        Long endTime = combatPlayers.get(player.getUniqueId());
        if (endTime == null) return false;

        if (System.currentTimeMillis() > endTime) {
            removeFromCombat(player);
            return false;
        }

        return true;
    }

    public int getRemainingTime(Player player) {
        Long endTime = combatPlayers.get(player.getUniqueId());
        if (endTime == null) return 0;

        long remaining = endTime - System.currentTimeMillis();
        return Math.max(0, (int) (remaining / 1000));
    }

    public void removeFromCombat(Player player) {
        if (player == null) return;

        if (combatPlayers.remove(player.getUniqueId()) != null) {
            String message = plugin.getConfig().getString("messages.combat-leave", "&7Wyszedles z &atrybu walki&7!");
            MessageUtil.sendMessage(player, message);
            plugin.getWallManager().removeWall(player);
        }
    }

    public Set<UUID> getCombatPlayers() {
        return new HashSet<>(combatPlayers.keySet());
    }

    public void clearAll() {
        combatPlayers.clear();
    }

    public boolean isWorldDisabled(String worldName) {
        List<String> disabledWorlds = plugin.getConfig().getStringList("disabled-worlds");
        return disabledWorlds.contains(worldName);
    }

    public void updatePlayers() {
        Iterator<Map.Entry<UUID, Long>> iterator = combatPlayers.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();

            if (System.currentTimeMillis() > entry.getValue()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                iterator.remove();

                if (player != null && player.isOnline()) {
                    String message = plugin.getConfig().getString("messages.combat-leave", "&7Wyszedles z &atrybu walki&7!");
                    MessageUtil.sendMessage(player, message);
                    plugin.getWallManager().removeWall(player);
                }
            }
        }
    }
}
