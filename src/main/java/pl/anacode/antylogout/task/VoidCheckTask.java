package pl.anacode.antylogout.task;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import pl.anacode.antylogout.AnacodeAntylogout;
import pl.anacode.antylogout.util.ColorUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class VoidCheckTask extends BukkitRunnable {

    private final AnacodeAntylogout plugin;
    private final Set<UUID> notifiedPlayers = new HashSet<>();

    public VoidCheckTask(AnacodeAntylogout plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (!plugin.getConfigManager().isVoidCombatEnabled()) {
            return;
        }

        List<String> voidWorlds = plugin.getConfigManager().getVoidCombatWorlds();
        int belowY = plugin.getConfigManager().getVoidCombatBelowY();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("anacode.antylogout.bypass")) {
                notifiedPlayers.remove(player.getUniqueId());
                continue;
            }

            Location loc = player.getLocation();
            String worldName = loc.getWorld().getName();

            if (voidWorlds.contains(worldName) && loc.getY() < belowY) {
                plugin.getCombatManager().tagPlayerSilent(player);

                if (plugin.getConfigManager().isVoidCombatShowMessage() 
                        && !notifiedPlayers.contains(player.getUniqueId())) {
                    player.sendMessage(ColorUtil.colorize(
                        plugin.getConfigManager().getVoidCombatMessage()));
                    notifiedPlayers.add(player.getUniqueId());
                }
            } else {
                notifiedPlayers.remove(player.getUniqueId());
            }
        }
    }

    public void removePlayer(UUID uuid) {
        notifiedPlayers.remove(uuid);
    }
}
