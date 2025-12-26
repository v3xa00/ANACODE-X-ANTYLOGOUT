package pl.anacode.antylogout.listener;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;
import pl.anacode.antylogout.AnacodeAntylogout;
import pl.anacode.antylogout.manager.RegionManager;
import pl.anacode.antylogout.util.MessageUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MoveListener implements Listener {

    private final AnacodeAntylogout plugin;
    private final Map<UUID, Long> messageCooldown = new HashMap<>();
    private final Map<UUID, Long> pushCooldown = new HashMap<>();
    private static final long COOLDOWN_TIME = 2000;
    private static final long PUSH_COOLDOWN_TIME = 100;

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

        // Sprawdz czy gracz JEST w zablokowanym regionie - wypchnij go
        if (regionManager.isLocationInBlockedRegion(player.getLocation())) {
            pushPlayerOutOfRegion(player);
            return;
        }

        // Sprawdz czy gracz PROBUJE wejsc na zablokowany region
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

        // Sprawdz czy gracz probuje przejsc przez sciane
        if (isMovingThroughWall(player, from, to)) {
            event.setCancelled(true);
            return;
        }

        // Aktualizacja sciany
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

    /**
     * Wypycha gracza poza zablokowany region
     */
    private void pushPlayerOutOfRegion(Player player) {
        // Cooldown zeby nie spamowac velocity
        long now = System.currentTimeMillis();
        Long lastPush = pushCooldown.get(player.getUniqueId());
        if (lastPush != null && now - lastPush < PUSH_COOLDOWN_TIME) {
            return;
        }
        pushCooldown.put(player.getUniqueId(), now);

        RegionManager regionManager = plugin.getRegionManager();
        if (regionManager == null) return;

        // Znajdz kierunek do najblizszej granicy regionu
        Vector pushDirection = regionManager.getDirectionOutOfRegion(player);
        
        if (pushDirection != null) {
            // Wypchnij gracza
            pushDirection.setY(0.1); // Lekko w gore zeby nie blokował sie w ziemi
            pushDirection.multiply(0.8); // Sila wypychania
            
            player.setVelocity(pushDirection);

            // Wiadomosc z cooldownem
            Long lastMessage = messageCooldown.get(player.getUniqueId());
            if (lastMessage == null || now - lastMessage > COOLDOWN_TIME) {
                String message = plugin.getConfig().getString("messages.region-blocked",
                    "&7Nie mozesz wejsc na ten &4region &7podczas walki!");
                MessageUtil.sendMessage(player, message);
                messageCooldown.put(player.getUniqueId(), now);
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
