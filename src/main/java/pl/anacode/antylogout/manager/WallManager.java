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
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null && p.isOnline()) {
                removeWall(p);
            }
        }
        shownWalls.clear();
    }

    private NearestWallResult findNearestWall(Player player, Location loc) {
        World world = player.getWorld();
        List<String> blockedRegions = plugin.getConfigManager().getBlockedRegions();

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(world));
        if (regionManager == null) return null;

        int px = loc.getBlockX();
        int pz = loc.getBlockZ();

        NearestWallResult best = null;
        double bestDist = Double.MAX_VALUE;

        for (String id : blockedRegions) {
            ProtectedRegion region = regionManager.getRegion(id);
            if (region == null) continue;

            BlockVector3 min = region.getMinimumPoint();
            BlockVector3 max = region.getMaximumPoint();

            int minX = min.getBlockX();
            int maxX = max.getBlockX();
            int minZ = min.getBlockZ();
            int maxZ = max.getBlockZ();

            if (px >= minX && px <= maxX && pz >= minZ && pz <= maxZ) {
                continue;
            }

            if (pz < minZ) {
                double dist = minZ - pz;
                if (dist < bestDist) {
                    bestDist = dist;
                    best = new NearestWallResult(WallSide.NORTH, dist, minX, maxX, minZ, maxZ, world);
                }
            }

            if (pz > maxZ) {
                double dist = pz - maxZ;
                if (dist < bestDist) {
                    bestDist = dist;
                    best = new NearestWallResult(WallSide.SOUTH, dist, minX, maxX, minZ, maxZ, world);
                }
            }

            if (px < minX) {
                double dist = minX - px;
                if (dist < bestDist) {
                    bestDist = dist;
                    best = new NearestWallResult(WallSide.WEST, dist, minX, maxX, minZ, maxZ, world);
                }
            }

            if (px > maxX) {
                double dist = px - maxX;
                if (dist < bestDist) {
                    bestDist = dist;
                    best = new NearestWallResult(WallSide.EAST, dist, minX, maxX, minZ, maxZ, world);
                }
            }
        }

        return best;
    }

    private void showWallAlongPerimeter(Player player, Location playerLoc, NearestWallResult r) {
        removeWall(player);

        Set<Location> wallBlocks = new HashSet<>();
        Material material = plugin.getConfigManager().getWallMaterial();

        int minX = r.minX;
        int maxX = r.maxX;
        int minZ = r.minZ;
        int maxZ = r.maxZ;

        int nX = maxX - minX + 1;
        int nZ = maxZ - minZ + 1;

        int L1 = nX;
        int L2 = nZ;
        int L3 = nX;
        int L4 = nZ;
        int P = L1 + L2 + L3 + L4;

        int width = plugin.getConfigManager().getWallWidth();
        int height = plugin.getConfigManager().getWallHeight();
        int cornerSize = plugin.getConfigManager().getCornerSize();
        int halfW = width / 2;
        int halfH = height / 2;

        int centerY = playerLoc.getBlockY();

        int px = playerLoc.getBlockX();
        int pz = playerLoc.getBlockZ();
        int t0;

        switch (r.side) {
            case NORTH -> {
                int projX = clamp(px, minX, maxX);
                t0 = (projX - minX);
            }
            case EAST -> {
                int projZ = clamp(pz, minZ, maxZ);
                t0 = L1 + (projZ - minZ);
            }
            case SOUTH -> {
                int projX = clamp(px, minX, maxX);
                t0 = L1 + L2 + (maxX - projX);
            }
            case WEST -> {
                int projZ = clamp(pz, minZ, maxZ);
                t0 = L1 + L2 + L3 + (maxZ - projZ);
            }
            default -> t0 = 0;
        }

        for (int dx = -halfW; dx <= halfW; dx++) {
            int wIndex = dx + halfW;

            int t = t0 + dx;
            int tNorm = ((t % P) + P) % P;

            int x, z;
            if (tNorm < L1) {
                x = minX + tNorm;
                z = minZ;
            } else if (tNorm < L1 + L2) {
                int u = tNorm - L1;
                x = maxX;
                z = minZ + u;
            } else if (tNorm < L1 + L2 + L3) {
                int u = tNorm - (L1 + L2);
                x = maxX - u;
                z = maxZ;
            } else {
                int u = tNorm - (L1 + L2 + L3);
                x = minX;
                z = maxZ - u;
            }

            for (int dy = -halfH; dy <= halfH; dy++) {
                int hIndex = dy + halfH;

                if (!shouldRenderInRoundedRect(wIndex, hIndex, width, height, cornerSize)) {
                    continue;
                }

                int y = centerY + dy;
                Location blockLoc = new Location(r.world, x, y, z);

                if (canPlaceWallBlock(blockLoc)) {
                    wallBlocks.add(blockLoc);
                    player.sendBlockChange(blockLoc, material.createBlockData());
                }
            }
        }

        shownWalls.put(player.getUniqueId(), wallBlocks);
    }

    private boolean shouldRenderInRoundedRect(int wIndex, int hIndex, 
                                              int width, int height, int cornerSize) {
        if (cornerSize <= 0) return true;

        int maxW = width - 1;
        int maxH = height - 1;

        if (wIndex < cornerSize && hIndex < cornerSize) {
            int dx = cornerSize - 1 - wIndex;
            int dy = cornerSize - 1 - hIndex;
            return dx * dx + dy * dy <= cornerSize * cornerSize;
        }

        if (wIndex > maxW - cornerSize && hIndex < cornerSize) {
            int dx = wIndex - (maxW - cornerSize);
            int dy = cornerSize - 1 - hIndex;
            return dx * dx + dy * dy <= cornerSize * cornerSize;
        }

        if (wIndex < cornerSize && hIndex > maxH - cornerSize) {
            int dx = cornerSize - 1 - wIndex;
            int dy = hIndex - (maxH - cornerSize);
            return dx * dx + dy * dy <= cornerSize * cornerSize;
        }

        if (wIndex > maxW - cornerSize && hIndex > maxH - cornerSize) {
            int dx = wIndex - (maxW - cornerSize);
            int dy = hIndex - (maxH - cornerSize);
            return dx * dx + dy * dy <= cornerSize * cornerSize;
        }

        return true;
    }

    private boolean canPlaceWallBlock(Location loc) {
        Material type = loc.getBlock().getType();
        return type.isAir()
                || type == Material.WATER
                || type == Material.LAVA
                || type == Material.CAVE_AIR
                || type == Material.VOID_AIR;
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static class NearestWallResult {
        final WallSide side;
        final double distance;
        final int minX, maxX, minZ, maxZ;
        final World world;

        NearestWallResult(WallSide side, double distance,
                          int minX, int maxX, int minZ, int maxZ,
                          World world) {
            this.side = side;
            this.distance = distance;
            this.minX = minX;
            this.maxX = maxX;
            this.minZ = minZ;
            this.maxZ = maxZ;
            this.world = world;
        }
    }
}
