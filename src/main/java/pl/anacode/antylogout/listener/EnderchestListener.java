package pl.anacode.antylogout.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import pl.anacode.antylogout.AnacodeAntylogout;
import pl.anacode.antylogout.util.MessageUtil;

public class EnderchestListener implements Listener {

    private final AnacodeAntylogout plugin;

    public EnderchestListener(AnacodeAntylogout plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        // Sprawdz czy blokada enderchesta jest wlaczona
        if (!plugin.getConfig().getBoolean("settings.block-enderchest", true)) return;

        // Sprawdz czy to enderchest
        if (event.getInventory().getType() != InventoryType.ENDER_CHEST) return;

        // Sprawdz czy gracz jest w walce
        if (!plugin.getCombatManager().isInCombat(player)) return;

        // Sprawdz bypass
        if (player.hasPermission("anacode.antylogout.bypass")) return;

        // Zablokuj
        event.setCancelled(true);
        String message = plugin.getConfig().getString("messages.enderchest-blocked",
            "&7Nie mozesz otworzyc &4enderchesta &7podczas walki!");
        MessageUtil.sendMessage(player, message);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEnderchestCommand(PlayerCommandPreprocessEvent event) {
        if (!plugin.getConfig().getBoolean("settings.block-enderchest", true)) return;

        Player player = event.getPlayer();
        String command = event.getMessage().toLowerCase().split(" ")[0].substring(1);

        // Sprawdz czy to komenda enderchesta
        if (!command.equals("ec") && !command.equals("enderchest") && !command.equals("echest")) return;

        // Sprawdz czy gracz jest w walce
        if (!plugin.getCombatManager().isInCombat(player)) return;

        // Sprawdz bypass
        if (player.hasPermission("anacode.antylogout.bypass")) return;

        // Zablokuj
        event.setCancelled(true);
        String message = plugin.getConfig().getString("messages.enderchest-blocked",
            "&7Nie mozesz otworzyc &4enderchesta &7podczas walki!");
        MessageUtil.sendMessage(player, message);
    }
}
