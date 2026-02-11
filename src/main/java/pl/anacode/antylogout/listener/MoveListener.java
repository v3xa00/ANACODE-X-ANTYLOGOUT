package pl.anacode.antylogout.listener;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;
import pl.anacode.antylogout.AnacodeAntylogout;
import pl.anacode.antylogout.util.ColorUtil;

public class MoveListener implements Listener {

    private final AnacodeAntylogout plugin;

    public MoveListener(AnacodeAntylogout plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        if (!plugin.getCombatManager().isInCombat(player)) {
            return;
        }
        
        if (player.hasPermission("anacode.antylogout.bypass")) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        
        if (to == null) return;
        
        if (from.getBlockX() == to.getBlockX() && 
            from.getBlockY() == to.getBlockY() && 
            from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        if (plugin.getWallManager().hasWall(player)) {
            if (plugin.getWallManager().isLocationInWall(player, to)) {
                event.setCancelled(true);
                
                Location safeLocation = from.clone();
                safeLocation.setYaw(to.getYaw());
                safeLocation.setPitch(to.getPitch());
                player.teleport(safeLocation);
                return;
            }
        }

        if (plugin.getWallManager().isInBlockedRegion(to) && 
                !plugin.getWallManager().isInBlockedRegion(from)) {
            
            event.setCancelled(true);
            
            Location safeLocation = from.clone();
            safeLocation.setYaw(to.getYaw());
            safeLocation.setPitch(to.getPitch());
            player.teleport(safeLocation);
            
            player.sendMessage(ColorUtil.colorize(
                plugin.getConfigManager().getRegionBlockedMessage()));
            return;
        }

        Vector direction = to.toVector().subtract(from.toVector());
        plugin.getWallManager().updateWall(player, to, direction);
    }
}
