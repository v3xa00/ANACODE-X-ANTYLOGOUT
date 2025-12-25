package pl.anacode.antylogout.task;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import pl.anacode.antylogout.AnacodeAntylogout;

import java.util.UUID;

public class CombatTask extends BukkitRunnable {

    private final AnacodeAntylogout plugin;

    public CombatTask(AnacodeAntylogout plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        plugin.getCombatManager().updatePlayers();

        String actionbarTemplate = plugin.getConfig().getString("messages.actionbar",
            "&7Jestes podczas walki jeszcze &4%time_left%s");

        for (UUID uuid : plugin.getCombatManager().getCombatPlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) continue;

            int timeLeft = plugin.getCombatManager().getRemainingTime(player);
            if (timeLeft <= 0) continue;

            String message = actionbarTemplate.replace("%time_left%", String.valueOf(timeLeft));
            Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(message);

            player.sendActionBar(component);
        }
    }
}
