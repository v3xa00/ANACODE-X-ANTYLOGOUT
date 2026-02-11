package pl.anacode.antylogout.manager;

import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import pl.anacode.antylogout.AnacodeAntylogout;
import pl.anacode.antylogout.util.ColorUtil;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CombatManager {

    private final AnacodeAntylogout plugin;
    private final Map<UUID, Long> combatPlayers = new ConcurrentHashMap<>();

    public CombatManager(AnacodeAntylogout plugin) {
        this.plugin = plugin;
    }

    public void tagPlayer(Player player) {
        if (player.hasPermission("anacode.antylogout.bypass")) {
            return;
        }

        boolean wasInCombat = isInCombat(player);
        long combatEndTime = System.currentTimeMillis() + 
            (plugin.getConfigManager().getCombatTime() * 1000L);
        
        combatPlayers.put(player.getUniqueId(), combatEndTime);

        if (!wasInCombat) {
            player.sendMessage(ColorUtil.colorize(
                plugin.getConfigManager().getCombatStartMessage()));
        }
    }

    public void tagPlayerWithTime(Player player, int seconds) {
        if (player.hasPermission("anacode.antylogout.bypass")) {
            return;
        }

        boolean wasInCombat = isInCombat(player);
        long combatEndTime = System.currentTimeMillis() + (seconds * 1000L);
        
        combatPlayers.put(player.getUniqueId(), combatEndTime);

        if (!wasInCombat) {
            player.sendMessage(ColorUtil.colorize(
                plugin.getConfigManager().getCombatStartMessage()));
        }
    }

    public void tagPlayerSilent(Player player) {
        if (player.hasPermission("anacode.antylogout.bypass")) {
            return;
        }

        long combatEndTime = System.currentTimeMillis() + 
            (plugin.getConfigManager().getCombatTime() * 1000L);
        
        combatPlayers.put(player.getUniqueId(), combatEndTime);
    }

    public void tagPlayerSilentWithTime(Player player, int seconds) {
        if (player.hasPermission("anacode.antylogout.bypass")) {
            return;
        }

        long combatEndTime = System.currentTimeMillis() + (seconds * 1000L);
        combatPlayers.put(player.getUniqueId(), combatEndTime);
    }

    public void tagBothPlayers(Player attacker, Player victim) {
        tagPlayer(attacker);
        tagPlayer(victim);
        
        plugin.getLastDamagerManager().setLastDamager(victim, attacker);
        plugin.getLastDamagerManager().setLastDamager(attacker, victim);
    }

    public boolean isInCombat(Player player) {
        if (!combatPlayers.containsKey(player.getUniqueId())) {
            return false;
        }

        long endTime = combatPlayers.get(player.getUniqueId());
        if (System.currentTimeMillis() >= endTime) {
            removeFromCombat(player);
            return false;
        }

        return true;
    }

    public int getRemainingTime(Player player) {
        if (!combatPlayers.containsKey(player.getUniqueId())) {
            return 0;
        }

        long endTime = combatPlayers.get(player.getUniqueId());
        long remaining = endTime - System.currentTimeMillis();
        
        if (remaining <= 0) {
            removeFromCombat(player);
            return 0;
        }

        return (int) Math.ceil(remaining / 1000.0);
    }

    public void removeFromCombat(Player player) {
        if (combatPlayers.containsKey(player.getUniqueId())) {
            combatPlayers.remove(player.getUniqueId());
            
            if (player.isOnline()) {
                player.sendMessage(ColorUtil.colorize(
                    plugin.getConfigManager().getCombatEndMessage()));
            }
        }
    }

    public void removeFromCombatSilent(Player player) {
        combatPlayers.remove(player.getUniqueId());
    }

    public void handleLogout(Player player) {
        if (!isInCombat(player)) return;
        if (!plugin.getConfigManager().isKillOnLogout()) return;

        UUID lastDamagerUUID = plugin.getLastDamagerManager().getLastDamager(player);
        Player killer = lastDamagerUUID != null ? Bukkit.getPlayer(lastDamagerUUID) : null;

        player.setHealth(0);
        combatPlayers.remove(player.getUniqueId());

        // Dodaj statystyki
        if (plugin.getStatsManager() != null) {
            plugin.getStatsManager().addDeath(player.getUniqueId());
            if (killer != null) {
                plugin.getStatsManager().addKill(killer.getUniqueId());
            }
        }

        if (killer != null && killer.isOnline() && plugin.getConfigManager().isBroadcastKiller()) {
            String message = plugin.getConfigManager().getLogoutDeathKillerMessage()
                .replace("%player%", player.getName())
                .replace("%killer%", killer.getName());
            Bukkit.broadcastMessage(ColorUtil.colorize(message));

            if (plugin.getConfigManager().isGiveKillOnLogout()) {
                killer.incrementStatistic(Statistic.PLAYER_KILLS);
            }

            if (plugin.getConfigManager().isGiveRewards()) {
                executeRewardCommands(killer, player);
            }
        } else {
            String message = plugin.getConfigManager().getLogoutDeathMessage()
                .replace("%player%", player.getName());
            Bukkit.broadcastMessage(ColorUtil.colorize(message));
        }

        plugin.getLastDamagerManager().clearLastDamager(player);
    }

    private void executeRewardCommands(Player killer, Player victim) {
        List<String> commands = plugin.getConfigManager().getRewardCommands();
        
        for (String command : commands) {
            String finalCommand = command
                .replace("%killer%", killer.getName())
                .replace("%victim%", victim.getName());
            
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
        }
    }

    public void killAllInCombat() {
        for (UUID uuid : combatPlayers.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.setHealth(0);
            }
        }
        combatPlayers.clear();
    }

    public Map<UUID, Long> getCombatPlayers() {
        return combatPlayers;
    }

    public void checkExpiredCombat() {
        long currentTime = System.currentTimeMillis();
        
        combatPlayers.entrySet().removeIf(entry -> {
            if (currentTime >= entry.getValue()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null && player.isOnline()) {
                    player.sendMessage(ColorUtil.colorize(
                        plugin.getConfigManager().getCombatEndMessage()));
                }
                return true;
            }
            return false;
        });

        plugin.getLastDamagerManager().cleanupExpired();
    }
}
