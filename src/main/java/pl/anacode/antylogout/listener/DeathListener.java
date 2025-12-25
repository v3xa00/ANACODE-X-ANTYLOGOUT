package pl.anacode.antylogout.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import pl.anacode.antylogout.AnacodeAntylogout;
import pl.anacode.antylogout.util.MessageUtil;

public class DeathListener implements Listener {

    private final AnacodeAntylogout plugin;

    public DeathListener(AnacodeAntylogout plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player killed = event.getEntity();
        Player killer = killed.getKiller();

        // Usun domyslna wiadomosc smierci
        event.deathMessage(null);

        // Usun z walki
        plugin.getCombatManager().removeFromCombatSilent(killed);

        // Dodaj smierc
        plugin.getStatsManager().addDeath(killed.getUniqueId());

        // Sprawdz czy byl zabojca
        if (killer != null && !killer.equals(killed)) {
            // Normalne zabojstwo przez gracza
            plugin.getStatsManager().addKill(killer.getUniqueId());

            // Wiadomosc o smierci
            String deathMessage = plugin.getConfig().getString("messages.death-killed",
                "&7Gracz &f%killer% &7zabil &f%killed%&7.");
            deathMessage = deathMessage.replace("%killer%", killer.getName());
            deathMessage = deathMessage.replace("%killed%", killed.getName());

            for (Player online : Bukkit.getOnlinePlayers()) {
                MessageUtil.sendRawMessage(online, deathMessage);
            }

            // ActionBar dla zabojcy
            String killActionbar = plugin.getConfig().getString("messages.kill-actionbar",
                "&7Zabiles gracza: &f%killed%&7!");
            killActionbar = killActionbar.replace("%killed%", killed.getName());
            MessageUtil.sendActionBar(killer, killActionbar);

            // Usun zabojce z walki
            plugin.getCombatManager().removeFromCombatSilent(killer);

        } else {
            // Sprawdz czy byl w walce (moze ostatni atakujacy)
            Player lastAttacker = plugin.getCombatManager().getLastAttacker(killed);

            if (lastAttacker != null && lastAttacker.isOnline() && plugin.getCombatManager().isInCombat(killed)) {
                // Smierc podczas walki - ostatni atakujacy dostaje killa
                plugin.getStatsManager().addKill(lastAttacker.getUniqueId());

                String deathMessage = plugin.getConfig().getString("messages.death-killed",
                    "&7Gracz &f%killer% &7zabil &f%killed%&7.");
                deathMessage = deathMessage.replace("%killer%", lastAttacker.getName());
                deathMessage = deathMessage.replace("%killed%", killed.getName());

                for (Player online : Bukkit.getOnlinePlayers()) {
                    MessageUtil.sendRawMessage(online, deathMessage);
                }

                String killActionbar = plugin.getConfig().getString("messages.kill-actionbar",
                    "&7Zabiles gracza: &f%killed%&7!");
                killActionbar = killActionbar.replace("%killed%", killed.getName());
                MessageUtil.sendActionBar(lastAttacker, killActionbar);

                plugin.getCombatManager().removeFromCombatSilent(lastAttacker);

            } else {
                // Samobojstwo (spadl, utonal, etc. bez walki)
                String deathMessage = plugin.getConfig().getString("messages.death-suicide",
                    "&7Gracz &f%killed% &7popelnil &4samobojstwo&7!");
                deathMessage = deathMessage.replace("%killed%", killed.getName());

                for (Player online : Bukkit.getOnlinePlayers()) {
                    MessageUtil.sendRawMessage(online, deathMessage);
                }
            }
        }
    }
}
