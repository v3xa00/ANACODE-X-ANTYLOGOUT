package pl.anacode.antylogout.manager;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import pl.anacode.antylogout.AnacodeAntylogout;

import java.util.List;

public class RegionManager {

    private final AnacodeAntylogout plugin;
    private final List<String> blockedRegions;
    private final int wallDistance;

    public RegionManager(AnacodeAntylogout plugin) {
        this.plugin = plugin;
        this.blockedRegions = plugin.getConfig().getStringList("blocked-regions");
        this.wallDistance = plugin.getConfig().getInt("settings.region-wall-distance", 10);
    }

    public boolean isInBlockedRegion(Player player) {
        return isLocationInBlockedRegion(player.getLocation());
    }

    public boolean isLocationInBlockedRegion(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        com.sk89q.worldguard.protection.managers.RegionManager regions =
            container.get(BukkitAdapter.adapt(loc.getWorld()));

        if (regions == null) return false;

        BlockVector3 position = BlockVector3.at(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

        for (ProtectedRegion region : regions.getApplicableRegions(position)) {
            if (blockedRegions.stream().anyMatch(r -> r.equalsIgnoreCase(region.getId()))) {
                return true;
            }
        }

        return false;
    }

    public RegionBorderInfo getNearestBlockedRegionBorder(Player player) {
        Location playerLoc = player.getLocation();
        World world = player.getWorld();

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        com.sk89q.worldguard.protection.managers.RegionManager regions =
            container.get(BukkitAdapter.adapt(world));

        if (regions == null) return null;

        RegionBorderInfo nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (String regionName : blockedRegions) {
            ProtectedRegion region = regions.getRegion(regionName.toLowerCase());
            if (region == null) {
                region = regions.getRegion(regionName);
                if (region == null) continue;
            }

            BlockVector3 min = region.getMinimumPoint();
            BlockVector3 max = region.getMaximumPoint();

            BorderResult result = findNearestBorder(playerLoc, min, max);

            if (result != null && result.distance <= wallDistance && result.distance < nearestDistance) {
                nearestDistance = result.distance;
                nearest = new RegionBorderInfo(
                    region,
                    result.borderLocation,
                    result.direction,
                    result.distance
                );
            }
        }

        return nearest;
    }

    private BorderResult findNearestBorder(Location playerLoc, BlockVector3 min, BlockVector3 max) {
        double px = playerLoc.getX();
        double pz = playerLoc.getZ();

        if (px >= min.getX() && px <= max.getX() && pz >= min.getZ() && pz <= max.getZ()) {
            return null;
        }

        BorderResult nearest = null;
        double nearestDist = Double.MAX_VALUE;

        if (pz < min.getZ()) {
            double dist = min.getZ() - pz;
            if (dist <= wallDistance && dist < nearestDist) {
                if (px >= min.getX() - wallDistance && px <= max.getX() + wallDistance) {
                    nearestDist = dist;
                    nearest = new BorderResult(
                        new Location(playerLoc.getWorld(), px, playerLoc.getY(), min.getZ()),
                        new Vector(0, 0, 1),
                        dist
                    );
                }
            }
        }

        if (pz > max.getZ()) {
            double dist = pz - max.getZ();
            if (dist <= wallDistance && dist < nearestDist) {
                if (px >= min.getX() - wallDistance && px <= max.getX() + wallDistance) {
                    nearestDist = dist;
                    nearest = new BorderResult(
                        new Location(playerLoc.getWorld(), px, playerLoc.getY(), max.getZ()),
                        new Vector(0, 0, -1),
                        dist
                    );
                }
            }
        }

        if (px < min.getX()) {
            double dist = min.getX() - px;
            if (dist <= wallDistance && dist < nearestDist) {
                if (pz >= min.getZ() - wallDistance && pz <= max.getZ() + wallDistance) {
                    nearestDist = dist;
                    nearest = new BorderResult(
                        new Location(playerLoc.getWorld(), min.getX(), playerLoc.getY(), pz),
                        new Vector(1, 0, 0),
                        dist
                    );
                }
            }
        }

        if (px > max.getX()) {
            double dist = px - max.getX();
            if (dist <= wallDistance && dist < nearestDist) {
                if (pz >= min.getZ() - wallDistance && pz <= max.getZ() + wallDistance) {
                    nearestDist = dist;
                    nearest = new BorderResult(
                        new Location(playerLoc.getWorld(), max.getX(), playerLoc.getY(), pz),
                        new Vector(-1, 0, 0),
                        dist
                    );
                }
            }
        }

        return nearest;
    }

    private static class BorderResult {
        final Location borderLocation;
        final Vector direction;
        final double distance;

        BorderResult(Location borderLocation, Vector direction, double distance) {
            this.borderLocation = borderLocation;
            this.direction = direction;
            this.distance = distance;
        }
    }

    public static class RegionBorderInfo {
        private final ProtectedRegion region;
        private final Location borderLocation;
        private final Vector direction;
        private final double distance;

        public RegionBorderInfo(ProtectedRegion region, Location borderLocation,
                                Vector direction, double distance) {
            this.region = region;
            this.borderLocation = borderLocation;
            this.direction = direction;
            this.distance = distance;
        }

        public ProtectedRegion getRegion() { return region; }
        public Location getBorderLocation() { return borderLocation; }
        public Vector getDirection() { return direction; }
        public double getDistance() { return distance; }
    }
}
