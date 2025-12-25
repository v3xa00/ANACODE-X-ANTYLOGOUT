package pl.anacode.antylogout.manager;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import pl.anacode.antylogout.AnacodeAntylogout;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WallManager {

    private final AnacodeAntylogout plugin;
    private final Map<UUID, WallData> playerWalls;
    private final int wallWidth;
    private final int wallHeight;

    public WallManager(AnacodeAntylogout plugin) {
        this.plugin = plugin;
        this.playerWalls = new ConcurrentHashMap<>();
        this.wallWidth = plugin.getConfig().getInt("settings.wall-width", 12);
        this.wallHeight = plugin.getConfig().getInt("settings.wall-height", 10);
    }

    private static class WallData {
        Set<Location> blocks = new HashSet<>();
        int wallFixedCoord;
        int lastCenterCoord;
        boolean wallAlongZ;
        int baseY;
    }

    public void updateWall(Player player, Location borderLocation, Vector direction) {
        if (player == null || !player.isOnline()) return;

        boolean wallAlongZ = Math.abs(direction.getX()) > Math.abs(direction.getZ());

        int wallFixedCoord;
        int playerVarCoord;

        if (wallAlongZ) {
            wallFixedCoord = borderLocation.getBlockX();
            playerVarCoord = player.getLocation().getBlockZ();
        } else {
            wallFixedCoord = borderLocation.getBlockZ();
            playerVarCoord = player.getLocation().getBlockX();
        }

        int baseY = player.getLocation().getBlockY() - 1;

        WallData wallData = playerWalls.get(player.getUniqueId());

        if (wallData == null ||
            wallData.wallAlongZ != wallAlongZ ||
            wallData.wallFixedCoord != wallFixedCoord) {

            createNewWall(player, wallFixedCoord, playerVarCoord, baseY, wallAlongZ);
            return;
        }

        int movement = playerVarCoord - wallData.lastCenterCoord;

        if (movement == 0) {
            if (baseY != wallData.baseY) {
                createNewWall(player, wallFixedCoord, playerVarCoord, baseY, wallAlongZ);
            }
            return;
        }

        shiftWall(player, wallData, movement, playerVarCoord, baseY);
    }

    private void createNewWall(Player player, int wallFixedCoord, int centerCoord,
                                int baseY, boolean wallAlongZ) {
        removeWall(player);

        WallData wallData = new WallData();
        wallData.wallFixedCoord = wallFixedCoord;
        wallData.lastCenterCoord = centerCoord;
        wallData.wallAlongZ = wallAlongZ;
        wallData.baseY = baseY;

        int halfWidth = wallWidth / 2;

        for (int h = 0; h < wallHeight; h++) {
            for (int w = -halfWidth; w <= halfWidth; w++) {

                if (isCornerToSkip(w, h, halfWidth, wallHeight)) {
                    continue;
                }

                Location blockLoc;
                if (wallAlongZ) {
                    blockLoc = new Location(
                        player.getWorld(),
                        wallFixedCoord,
                        baseY + h,
                        centerCoord + w
                    );
                } else {
                    blockLoc = new Location(
                        player.getWorld(),
                        centerCoord + w,
                        baseY + h,
                        wallFixedCoord
                    );
                }

                wallData.blocks.add(blockLoc);
                player.sendBlockChange(blockLoc, Material.RED_STAINED_GLASS.createBlockData());
            }
        }

        playerWalls.put(player.getUniqueId(), wallData);
    }

    private void shiftWall(Player player, WallData wallData, int movement,
                           int newCenterCoord, int newBaseY) {

        int halfWidth = wallWidth / 2;
        int oldCenter = wallData.lastCenterCoord;

        boolean yChanged = (newBaseY != wallData.baseY);

        if (yChanged || Math.abs(movement) > halfWidth) {
            createNewWall(player, wallData.wallFixedCoord, newCenterCoord,
                         newBaseY, wallData.wallAlongZ);
            return;
        }

        Set<Location> newBlocks = new HashSet<>();
        Set<Location> toRemove = new HashSet<>();
        Set<Location> toAdd = new HashSet<>();

        for (int h = 0; h < wallHeight; h++) {
            for (int w = -halfWidth; w <= halfWidth; w++) {

                if (isCornerToSkip(w, h, halfWidth, wallHeight)) {
                    continue;
                }

                Location newLoc;

                if (wallData.wallAlongZ) {
                    newLoc = new Location(
                        player.getWorld(),
                        wallData.wallFixedCoord,
                        wallData.baseY + h,
                        newCenterCoord + w
                    );
                } else {
                    newLoc = new Location(
                        player.getWorld(),
                        newCenterCoord + w,
                        wallData.baseY + h,
                        wallData.wallFixedCoord
                    );
                }

                newBlocks.add(newLoc);
            }
        }

        for (Location oldLoc : wallData.blocks) {
            boolean found = false;
            for (Location newLoc : newBlocks) {
                if (isSameBlock(oldLoc, newLoc)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                toRemove.add(oldLoc);
            }
        }

        for (Location newLoc : newBlocks) {
            boolean found = false;
            for (Location oldLoc : wallData.blocks) {
                if (isSameBlock(oldLoc, newLoc)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                toAdd.add(newLoc);
            }
        }

        for (Location loc : toRemove) {
            player.sendBlockChange(loc, loc.getBlock().getBlockData());
        }

        for (Location loc : toAdd) {
            player.sendBlockChange(loc, Material.RED_STAINED_GLASS.createBlockData());
        }

        wallData.blocks = newBlocks;
        wallData.lastCenterCoord = newCenterCoord;
    }

    private boolean isSameBlock(Location loc1, Location loc2) {
        return loc1.getBlockX() == loc2.getBlockX() &&
               loc1.getBlockY() == loc2.getBlockY() &&
               loc1.getBlockZ() == loc2.getBlockZ() &&
               Objects.equals(loc1.getWorld(), loc2.getWorld());
    }

    private boolean isCornerToSkip(int w, int h, int halfWidth, int height) {
        int absW = Math.abs(w);

        if (h >= height - 2) {
            if (h == height - 1 && absW >= halfWidth - 1) return true;
            if (h == height - 2 && absW == halfWidth) return true;
        }

        if (h <= 1) {
            if (h == 0 && absW >= halfWidth - 1) return true;
            if (h == 1 && absW == halfWidth) return true;
        }

        return false;
    }

    public void removeWall(Player player) {
        if (player == null) return;

        WallData wallData = playerWalls.remove(player.getUniqueId());

        if (wallData != null && player.isOnline()) {
            for (Location loc : wallData.blocks) {
                player.sendBlockChange(loc, loc.getBlock().getBlockData());
            }
        }
    }

    public void removeAllWalls() {
        for (UUID uuid : new HashSet<>(playerWalls.keySet())) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null) {
                removeWall(player);
            }
        }
        playerWalls.clear();
    }

    public boolean hasWall(Player player) {
        return playerWalls.containsKey(player.getUniqueId());
    }

    public boolean isLocationInWall(Player player, Location location) {
        WallData data = playerWalls.get(player.getUniqueId());
        if (data == null) return false;

        for (Location wallBlock : data.blocks) {
            if (isSameBlock(wallBlock, location)) {
                return true;
            }
        }
        return false;
    }
}
