package pl.anacode.antylogout.listener;

import org.bukkit.Location;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import pl.anacode.antylogout.AnacodeAntylogout;
import pl.anacode.antylogout.manager.RegionManager;
import pl.anacode.antylogout.util.MessageUtil;

public class PearlListener implements Listener {

    private final AnacodeAntylogout plugin;

    public PearlListener(AnacodeAntylogout plugin) {
        this.plugin = plugin;
    }

    /**
     * Blokuje teleportacje perla do sciany lub zablokowanego regionu
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPearlTeleport(PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) return;

        Player player = event.getPlayer();

        // Sprawdz bypass
        if (player.hasPermission("anacode.antylogout.bypass")) return;

        // Sprawdz czy gracz jest w walce
        if (!plugin.getCombatManager().isInCombat(player)) return;

        Location to = event.getTo();
        if (to == null) return;

        // Sprawdz czy teleportacja jest do sciany
        if (plugin.getWallManager().isLocationInWall(player, to)) {
            event.setCancelled(true);
            MessageUtil.sendMessage(player, "&7Nie mozesz uzyc &4perly &7w tym miejscu!");
            return;
        }

        // Sprawdz czy teleportacja jest do zablokowanego regionu
        RegionManager regionManager = plugin.getRegionManager();
        if (regionManager != null && regionManager.isLocationInBlockedRegion(to)) {
            event.setCancelled(true);
            String message = plugin.getConfig().getString("messages.region-blocked",
                "&7Nie mozesz wejsc na ten &4region &7podczas walki!");
            MessageUtil.sendMessage(player, message);
            return;
        }

        // Sprawdz czy teleportacja przechodzi przez sciane (linia miedzy from i to)
        Location from = event.getFrom();
        if (isPearlPathThroughWall(player, from, to)) {
            event.setCancelled(true);
            MessageUtil.sendMessage(player, "&7Nie mozesz uzyc &4perly &7przez sciane!");
        }
    }

    /**
     * Anuluje perle ktora trafia w sciane (fake blocks)
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPearlHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof EnderPearl pearl)) return;
        if (!(pearl.getShooter() instanceof Player player)) return;

        // Sprawdz bypass
        if (player.hasPermission("anacode.antylogout.bypass")) return;

        // Sprawdz czy gracz jest w walce
        if (!plugin.getCombatManager().isInCombat(player)) return;

        Location hitLocation = pearl.getLocation();

        // Sprawdz czy perla trafila w sciane
        if (plugin.getWallManager().isLocationInWall(player, hitLocation)) {
            // Anuluj perle - usun ja bez teleportacji
            pearl.remove();
            event.setCancelled(true);
            MessageUtil.sendMessage(player, "&7Perla trafila w &4sciane&7!");
            return;
        }

        // Sprawdz czy perla trafila w zablokowany region
        RegionManager regionManager = plugin.getRegionManager();
        if (regionManager != null && regionManager.isLocationInBlockedRegion(hitLocation)) {
            pearl.remove();
            event.setCancelled(true);
            String message = plugin.getConfig().getString("messages.region-blocked",
                "&7Nie mozesz wejsc na ten &4region &7podczas walki!");
            MessageUtil.sendMessage(player, message);
        }
    }

    /**
     * Sprawdza czy sciezka perly przechodzi przez sciane
     */
    private boolean isPearlPathThroughWall(Player player, Location from, Location to) {
        if (!plugin.getWallManager().hasWall(player)) return false;

        // Sprawdz punkty na linii miedzy from i to
        double distance = from.distance(to);
        int steps = (int) Math.ceil(distance);

        if (steps <= 0) return false;

        double stepX = (to.getX() - from.getX()) / steps;
        double stepY = (to.getY() - from.getY()) / steps;
        double stepZ = (to.getZ() - from.getZ()) / steps;

        for (int i = 0; i <= steps; i++) {
            Location checkLoc = new Location(
                from.getWorld(),
                from.getX() + (stepX * i),
                from.getY() + (stepY * i),
                from.getZ() + (stepZ * i)
            );

            if (plugin.getWallManager().isLocationInWall(player, checkLoc)) {
                return true;
            }
        }

        return false;
    }
}
