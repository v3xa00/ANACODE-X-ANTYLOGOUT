package pl.anacode.antylogout.manager;

import org.bukkit.entity.Player;
import pl.anacode.antylogout.AnacodeAntylogout;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LastDamagerManager {

    private final AnacodeAntylogout plugin;
    private final Map<UUID, DamagerInfo> lastDamagers = new ConcurrentHashMap<>();

    public LastDamagerManager(AnacodeAntylogout plugin) {
        this.plugin = plugin;
    }

    public void setLastDamager(Player victim, Player attacker) {
        if (!plugin.getConfigManager().isLastDamagerEnabled()) return;
        if (victim.equals(attacker)) return;
        
        long expireTime = System.currentTimeMillis() + 
            (plugin.getConfigManager().getLastDamagerMemoryTime() * 1000L);
        
        lastDamagers.put(victim.getUniqueId(), new DamagerInfo(attacker.getUniqueId(), expireTime));
    }

    public UUID getLastDamager(Player victim) {
        DamagerInfo info = lastDamagers.get(victim.getUniqueId());
        
        if (info == null) return null;
        
        if (System.currentTimeMillis() > info.expireTime) {
            lastDamagers.remove(victim.getUniqueId());
            return null;
        }
        
        return info.damagerUUID;
    }

    public void clearLastDamager(Player victim) {
        lastDamagers.remove(victim.getUniqueId());
    }

    public void cleanupExpired() {
        long currentTime = System.currentTimeMillis();
        lastDamagers.entrySet().removeIf(entry -> currentTime > entry.getValue().expireTime);
    }

    private static class DamagerInfo {
        final UUID damagerUUID;
        final long expireTime;

        DamagerInfo(UUID damagerUUID, long expireTime) {
            this.damagerUUID = damagerUUID;
            this.expireTime = expireTime;
        }
    }
}
