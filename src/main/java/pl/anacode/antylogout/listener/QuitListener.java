package pl.anacode.antylogout.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.anacode.antylogout.AnacodeAntylogout;
import pl.anacode.antylogout.util.MessageUtil;

public class QuitListener implements Listener {

    private final AnacodeAntylogout plugin;

    public QuitListener(AnacodeAntylogout plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        handleLogout(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onKick(PlayerKickEvent event) {
        handleLogout(event.getPlayer());
    }

    private void handleLogout(Player player) {
        plugin.getWallManager().removeWall(player);

        if (!plugin.getCombatManager().isInCombat(player)) return;

        if (!plugin.getConfig().getBoolean("settings.kill-on-logout", true)) return;

        // Pobierz ostatniego atakujacego
        Player killer = plugin.getCombatManager().getLastAttacker(player);

        // Dodaj smierc graczowi
        plugin.getStatsManager().addDeath(player.getUniqueId());

        // Broadcast wiadomosc o combat logu
        String deathMessage = plugin.getConfig().getString("messages.death-combatlog",
            "&7Gracz &f%killed% &cwylogowal sie podczas &4walki&7!");
        deathMessage = deathMessage.replace("%killed%", player.getName());

        for (Player online : Bukkit.getOnlinePlayers()) {
            MessageUtil.sendRawMessage(online, deathMessage);
        }

        // Jesli byl atakujacy - daj mu zabojstwo
        if (killer != null && killer.isOnline()) {
            plugin.getStatsManager().addKill(killer.getUniqueId());
            
            // ActionBar dla zabojcy
            String killActionbar = plugin.getConfig().getString("messages.kill-actionbar",
                "&7Zabiles gracza: &f%killed%&7!");
            killActionbar = killActionbar.replace("%killed%", player.getName());
            MessageUtil.sendActionBar(killer, killActionbar);
        }

        // Zabij gracza
        player.setHealth(0);

        // Usun z walki
        plugin.getCombatManager().removeFromCombatSilent(player);
    }
}
