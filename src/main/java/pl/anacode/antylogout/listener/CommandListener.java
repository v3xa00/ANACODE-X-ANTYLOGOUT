package pl.anacode.antylogout.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import pl.anacode.antylogout.AnacodeAntylogout;
import pl.anacode.antylogout.util.MessageUtil;

import java.util.List;

public class CommandListener implements Listener {

    private final AnacodeAntylogout plugin;

    public CommandListener(AnacodeAntylogout plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        if (player.hasPermission("anacode.antylogout.bypass")) return;
        if (player.hasPermission("anacode.antylogout.admin")) return;

        if (!plugin.getCombatManager().isInCombat(player)) return;

        String command = event.getMessage().toLowerCase().split(" ")[0].substring(1);

        List<String> allowedCommands = plugin.getConfig().getStringList("allowed-commands");

        for (String allowed : allowedCommands) {
            if (command.equalsIgnoreCase(allowed) || command.startsWith(allowed.toLowerCase() + ":")) {
                return;
            }
        }

        event.setCancelled(true);
        String message = plugin.getConfig().getString("messages.command-blocked",
            "&7Jestes podczas walki nie mozesz teraz tego &4uzyc&7!");
        MessageUtil.sendMessage(player, message);
    }
}
