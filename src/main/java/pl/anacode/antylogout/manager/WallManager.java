package pl.anacode.antylogout.manager;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import pl.anacode.antylogout.AnacodeAntylogout;

import java.util.*;

public class WallManager {

    private final AnacodeAntylogout plugin;
    private final Map<UUID, Set<Location>> shownWalls = new HashMap<>();

    private enum WallSide {
        NORTH, SOUTH, EAST, WEST
    }

    public WallManager(AnacodeAntylogout plugin) {
        this.plugin = plugin;
    }

    public void updateWallForPlayer(Player player) {
        if (!plugin.isWorldGuardEnabled()) return;
        if (!plugin.getCombatManager().isInCombat(player)) {
            removeWall(player);
            return;
        }

        Location loc = player.getLocation();
        NearestWallResult nearest = findNearestWall(player, loc);
        if (nearest == null) {
            removeWall(player);
            return;
        }

        int maxDistance = plugin.getConfigManager().getWallDistance();
        if (nearest.distance <= maxDistance && nearest.distance > 0) {
            showWallAlongPerimeter(player, loc, nearest);
        } else {
            removeWall(player);
        }
    }

    public void updateWall(Player player, Location location, Vector direction) {
        updateWallForPlayer(player);
    }

    public boolean hasWall(Player player) {
        Set<Location> walls = shownWalls.get(player.getUniqueId());
        return walls != null && !walls.isEmpty();
    }

    public boolean isLocationInWall(Player player, Location location) {
        Set<Location> walls = shownWalls.get(player.getUniqueId());
        if (walls == null || walls.isEmpty()) {
            return false;
        }

        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        for (Location wallLoc : walls) {
            if (wallLoc.getBlockX() == x && 
                wallLoc.getBlockY() == y && 
                wallLoc.getBlockZ() == z) {
                return true;
            }
        }

        for (Location wallLoc : walls) {
            double dist = wallLoc.distance(location);
            if (dist < 1.5) {
                return true;
            }
        }

        return false;
    }

    public Set<Location> getWallLocations(Player player) {
        Set<Location> walls = shownWalls.get(player.getUniqueId());
        return walls != null ? new HashSet<>(walls) : new HashSet<>();
    }

    public boolean isInBlockedRegion(Location location) {
        if (!plugin.isWorldGuardEnabled()) return false;

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(location.getWorld()));
        if (regionManager == null) return false;

        List<String> blocked = plugin.getConfigManager().getBlockedRegions();
        for (ProtectedRegion region : regionManager.getApplicableRegions(
                BukkitAdapter.asBlockVector(location))) {
            if (blocked.contains(region.getId())) {
                return true;
            }
        }
        return false;
    }

    public boolean isNearBlockedRegion(Player player) {
        if (!plugin.isWorldGuardEnabled()) return false;
        
        Location loc = player.getLocation();
        NearestWallResult result = findNearestWall(player, loc);
        
        if (result == null) return false;
        
        return result.distance <= plugin.getConfigManager().getWallDistance();
    }

    public void removeWall(Player player) {
        Set<Location> walls = shownWalls.remove(player.getUniqueId());
        if (walls != null && player.isOnline()) {
            for (Location loc : walls) {
                player.sendBlockChange(loc, loc.getBlock().getBlockData());
            }
        }
    }

    public void removeAllWalls() {
        for (UUID uuid : new HashSet<>(shownWalls.keySet())) {
            Player p = 
