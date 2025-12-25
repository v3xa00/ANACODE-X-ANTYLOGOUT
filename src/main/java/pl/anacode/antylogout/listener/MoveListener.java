package pl.anacode.antylogout.listener;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import pl.anacode.antylogout.AnacodeAntylogout;
import pl.anacode.antylogout.manager.RegionManager;
import pl.anacode.antylogout.util.MessageUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MoveListener implements Listener {

    private final AnacodeAntylogout plugin;
    private final Map<UUID, Long> messageCooldown = new HashMap<>();
    private static final long COOLDOWN_TIME = 2000;

    public MoveListener(AnacodeAntylogout plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (player.hasPermission("anacode.antylogout.bypass")) return;

        if (!plugin.getCombatManager().isInCombat(player)) {
            if (plugin.getWallManager().hasWall(player)) {
                plugin.getWallManager().removeWall(player);
            }
            return;
        }

        RegionManager regionManager = plugin.getRegionManager();
        if (regionManager == null) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        if (regionManager.isLocationInBlockedRegion(to)) {
            event.setCancelled(true);

            long now = System.currentTimeMillis();
            Long lastMessage = messageCooldown.get(player.getUniqueId());

            if (lastMessage == null || now - lastMessage > COOLDOWN_TIME) {
                String message = plugin.getConfig().getString("messages.region-blocked",
                    "&7Nie mozesz wejsc na ten &4region &7podczas walki!");
                MessageUtil.sendMessage(player, message);
                messageCooldown.put(player.getUniqueId(), now);
            }
            return;
        }

        if (isMovingThroughWall(player, from, to)) {
            event.setCancelled(true);
            return;
        }

        boolean changedBlock = from.getBlockX() != to.getBlockX() ||
                               from.getBlockY() != to.getBlockY() ||
                               from.getBlockZ() != to.getBlockZ();

        if (!changedBlock) return;

        RegionManager.RegionBorderInfo borderInfo = regionManager.getNearestBlockedRegionBorder(player);

        if (borderInfo != null) {
            plugin.getWallManager().updateWall(
                player,
                borderInfo.getBorderLocation(),
                borderInfo.getDirection()
            );
        } else {
            if (plugin.getWallManager().hasWall(player)) {
                plugin.getWallManager().removeWall(player);
            }
        }
    }

    private boolean isMovingThroughWall(Player player, Location from, Location to) {
        if (!plugin.getWallManager().hasWall(player)) return false;

        double playerWidth = 0.3;

        Location[] checkPoints = {
            to.clone(),
            to.clone().add(playerWidth, 0, playerWidth),
            to.clone().add(-playerWidth, 0, playerWidth),
            to.clone().add(playerWidth, 0, -playerWidth),
            to.clone().add(-playerWidth, 0, -playerWidth),
            to.clone().add(0, 1, 0),
        };

        for (Location check : checkPoints) {
            if (plugin.getWallManager().isLocationInWall(player, check)) {
                return true;
            }
        }

        return false;
    }
}
