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
import pl.anacode.antylogout.AnacodeAntylogout;

import java.util.*;

public class WallManager {

    private final AnacodeAntylogout plugin;
    private final Map<UUID, Set<Location>> shownWalls = new HashMap<>();

    public WallManager(AnacodeAntylogout plugin) {
        this.plugin = plugin;
    }

    // Kierunek ściany, od której liczymy pozycję startową
    private enum WallSide {
        NORTH, // z = minZ
        SOUTH, // z = maxZ
        EAST,  // x = maxX
        WEST   // x = minX
    }

    /**
     * Wywoływane w tasku co kilka ticków
     */
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

    /**
     * Znajduje najbliższą ścianę regionu zablokowanego (N/S/E/W)
     * Zwraca też bounding box regionu (minX, maxX, minZ, maxZ)
     */
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

            // jeśli gracz jest w środku regionu – nie pokazujemy ściany
            if (px >= minX && px <= maxX && pz >= minZ && pz <= maxZ) {
                continue;
            }

            // North (gracz przed północną ścianą -> z < minZ)
            if (pz < minZ) {
                double dist = minZ - pz;
                if (dist < bestDist) {
                    bestDist = dist;
                    best = new NearestWallResult(WallSide.NORTH, dist, minX, maxX, minZ, maxZ, world);
                }
            }

            // South (gracz za południową ścianą -> z > maxZ)
            if (pz > maxZ) {
                double dist = pz - maxZ;
                if (dist < bestDist) {
                    bestDist = dist;
                    best = new NearestWallResult(WallSide.SOUTH, dist, minX, maxX, minZ, maxZ, world);
                }
            }

            // West (gracz na zachód -> x < minX)
            if (px < minX) {
                double dist = minX - px;
                if (dist < bestDist) {
                    bestDist = dist;
                    best = new NearestWallResult(WallSide.WEST, dist, minX, maxX, minZ, maxZ, world);
                }
            }

            // East (gracz na wschód -> x > maxX)
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

    /**
     * Rysuje ścianę na obwodzie regionu jako "odcinek" o długości width,
     * centrum przy projekcji gracza na najbliższą ścianę.
     * Przy rogach ściana zawija w L‑kę na kolejną ścianę.
     */
    private void showWallAlongPerimeter(Player player, Location playerLoc, NearestWallResult r) {
        removeWall(player);

        Set<Location> wallBlocks = new HashSet<>();
        Material material = plugin.getConfigManager().getWallMaterial();

        int minX = r.minX;
        int maxX = r.maxX;
        int minZ = r.minZ;
        int maxZ = r.maxZ;

        // ilość bloków na bok
        int nX = maxX - minX + 1; // szerokość regionu
        int nZ = maxZ - minZ + 1; // długość regionu

        // długości segmentów (liczba bloków)
        int L1 = nX;       // North
        int L2 = nZ;       // East
        int L3 = nX;       // South
        int L4 = nZ;       // West
        int P  = L1 + L2 + L3 + L4; // pełny obwód w "krokach blokowych"

        int width  = plugin.getConfigManager().getWallWidth();   // np. 12
        int height = plugin.getConfigManager().getWallHeight();  // np. 12
        int cornerSize = plugin.getConfigManager().getCornerSize();
        int halfW = width / 2;
        int halfH = height / 2;

        int centerY = playerLoc.getBlockY();

        // oblicz "pozycję na obwodzie" (t0) odpowiadającą rzutowi gracza na najbliższą ścianę
        int px = playerLoc.getBlockX();
        int pz = playerLoc.getBlockZ();
        int t0; // baza na obwodzie

        switch (r.side) {
            case NORTH -> {
                int projX = clamp(px, minX, maxX);
                // North: od (minX,minZ) do (maxX,minZ)
                t0 = (projX - minX); // [0..L1-1]
            }
            case EAST -> {
                int projZ = clamp(pz, minZ, maxZ);
                // East: po North, z rośnie
                t0 = L1 + (projZ - minZ); // [L1..L1+L2-1]
            }
            case SOUTH -> {
                int projX = clamp(px, minX, maxX);
                // South: po North+East, x maleje
                t0 = L1 + L2 + (maxX - projX); // [L1+L2..L1+L2+L3-1]
            }
            case WEST -> {
                int projZ = clamp(pz, minZ, maxZ);
                // West: po North+East+South, z maleje
                t0 = L1 + L2 + L3 + (maxZ - projZ); // [..P-1]
            }
            default -> {
                t0 = 0;
            }
        }

        // dla każdego "pasa" w poziomie względem środka ściany
        for (int dx = -halfW; dx <= halfW; dx++) {
            int wIndex = dx + halfW; // 0..width-1

            // pozycja na obwodzie (może wyjść poza zakres, więc zmodujemy)
            int t = t0 + dx;

            // normalizacja do [0, P)
            int tNorm = ((t % P) + P) % P;

            // odwzorowanie tNorm -> (x,z) na obwodzie regionu
            int x, z;
            if (tNorm < L1) {
                // North
                x = minX + tNorm;
                z = minZ;
            } else if (tNorm < L1 + L2) {
                // East
                int u = tNorm - L1;
                x = maxX;
                z = minZ + u;
            } else if (tNorm < L1 + L2 + L3) {
                // South
                int u = tNorm - (L1 + L2);
                x = maxX - u;
                z = maxZ;
            } else {
                // West
                int u = tNorm - (L1 + L2 + L3);
                x = minX;
                z = maxZ - u;
            }

            // dla każdego bloku w pionie względem gracza
            for (int dy = -halfH; dy <= halfH; dy++) {
                int hIndex = dy + halfH; // 0..height-1
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

    /**
     * Zaokrąglone rogi w prostokącie width x height,
     * cornerSize = promień "ścięcia" rogów w blokach.
     */
    private boolean shouldRenderInRoundedRect(int wIndex, int hIndex,
                                              int width, int height, int cornerSize) {
        if (cornerSize <= 0) return true;

        int maxW = width  - 1;
        int maxH = height - 1;

        // lewy górny róg
        if (wIndex < cornerSize && hIndex < cornerSize) {
            int dx = cornerSize - 1 - wIndex;
            int dy = cornerSize - 1 - hIndex;
            return dx * dx + dy * dy <= cornerSize * cornerSize;
        }

        // prawy górny róg
        if (wIndex > maxW - cornerSize && hIndex < cornerSize) {
            int dx = wIndex - (maxW - cornerSize);
            int dy = cornerSize - 1 - hIndex;
            return dx * dx + dy * dy <= cornerSize * cornerSize;
        }

        // lewy dolny róg
        if (wIndex < cornerSize && hIndex > maxH - cornerSize) {
            int dx = cornerSize - 1 - wIndex;
            int dy = hIndex - (maxH - cornerSize);
            return dx * dx + dy * dy <= cornerSize * cornerSize;
        }

        // prawy dolny róg
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

    // Informacje o najbliższej ścianie i bounding boxie regionu
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
