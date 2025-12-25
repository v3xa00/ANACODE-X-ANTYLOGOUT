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

        player.setHealth(0);

        String broadcastMessage = plugin.getConfig().getString("messages.logout-broadcast",
            "&4%player% &7wylogowal sie podczas walki i &czginal&7!");
        broadcastMessage = broadcastMessage.replace("%player%", player.getName());

        for (Player online : Bukkit.getOnlinePlayers()) {
            MessageUtil.sendMessage(online, broadcastMessage);
        }
    }
}
