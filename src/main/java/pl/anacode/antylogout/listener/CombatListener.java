package pl.anacode.antylogout.listener;

import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import pl.anacode.antylogout.AnacodeAntylogout;

public class CombatListener implements Listener {

    private final AnacodeAntylogout plugin;

    public CombatListener(AnacodeAntylogout plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        Player attacker = getPlayerAttacker(event.getDamager());

        if (attacker != null && !attacker.equals(victim)) {
            plugin.getCombatManager().tagBoth(attacker, victim);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();

        if (!(projectile.getShooter() instanceof Player shooter)) return;

        if (event.getHitEntity() instanceof Player victim) {
            if (!shooter.equals(victim)) {
                plugin.getCombatManager().tagBoth(shooter, victim);
            } else {
                plugin.getCombatManager().tagPlayer(shooter);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!plugin.getConfig().getBoolean("settings.ender-pearl-triggers-combat", true)) return;

        Projectile projectile = event.getEntity();

        if (projectile instanceof EnderPearl) {
            if (projectile.getShooter() instanceof Player player) {
                plugin.getCombatManager().tagPlayer(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            if (plugin.getConfig().getBoolean("settings.ender-pearl-triggers-combat", true)) {
                plugin.getCombatManager().tagPlayer(event.getPlayer());
            }
        }
    }

    private Player getPlayerAttacker(Entity damager) {
        if (damager instanceof Player) {
            return (Player) damager;
        }

        if (damager instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player) {
                return (Player) projectile.getShooter();
            }
        }

        if (damager instanceof TNTPrimed tnt) {
            if (tnt.getSource() instanceof Player) {
                return (Player) tnt.getSource();
            }
        }

        return null;
    }
}
