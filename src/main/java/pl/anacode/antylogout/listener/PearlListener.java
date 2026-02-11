package pl.anacode.antylogout.listener;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import pl.anacode.antylogout.AnacodeAntylogout;
import pl.anacode.antylogout.util.ColorUtil;

public class PearlListener implements Listener {

    private final AnacodeAntylogout plugin;

    public PearlListener(AnacodeAntylogout plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            return;
        }

        Player player = event.getPlayer();

        if (!plugin.getCombatManager().isInCombat(player)) {
            return;
        }

        if (player.hasPermission("anacode.antylogout.bypass")) {
            return;
        }

        Location to = event.getTo();
        if (to == null) return;

        if (plugin.getWallManager().hasWall(player)) {
            if (plugin.getWallManager().isLocationInWall(player, to)) {
                event.setCancelled(true);
                player.sendMessage(ColorUtil.colorize(
                    "&cNie możesz teleportować się przez barierę!"));
                return;
            }
        }

        if (plugin.getWallManager().isInBlockedRegion(to)) {
            event.setCancelled(true);
            player.sendMessage(ColorUtil.colorize(
                plugin.getConfigManager().getRegionBlockedMessage()));
        }
    }
}
