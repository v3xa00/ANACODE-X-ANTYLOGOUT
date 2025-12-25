package pl.anacode.antylogout.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import pl.anacode.antylogout.AnacodeAntylogout;
import pl.anacode.antylogout.util.MessageUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StatsCommand implements CommandExecutor, TabCompleter {

    private final AnacodeAntylogout plugin;

    public StatsCommand(AnacodeAntylogout plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        UUID targetUUID;
        String targetName;

        if (args.length == 0) {
            // Sprawdz wlasne statystyki
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Uzycie: /stats <gracz>");
                return true;
            }
            targetUUID = player.getUniqueId();
            targetName = player.getName();
        } else {
            // Sprawdz statystyki innego gracza
            Player target = Bukkit.getPlayer(args[0]);
            if (target != null) {
                targetUUID = target.getUniqueId();
                targetName = target.getName();
            } else {
                // Gracz offline - sprobuj znalezc po nazwie
                @SuppressWarnings("deprecation")
                org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[0]);
                if (offlinePlayer.hasPlayedBefore()) {
                    targetUUID = offlinePlayer.getUniqueId();
                    targetName = offlinePlayer.getName() != null ? offlinePlayer.getName() : args[0];
                } else {
                    sendMessage(sender, "&cNie znaleziono gracza: " + args[0]);
                    return true;
                }
            }
        }

        // Pobierz statystyki
        int kills = plugin.getStatsManager().getKills(targetUUID);
        int deaths = plugin.getStatsManager().getDeaths(targetUUID);
        double kdr = plugin.getStatsManager().getKDR(targetUUID);

        // Wyswietl
        sendMessage(sender, plugin.getConfig().getString("messages.stats-header", 
            "&8&m─────────&8[&4 Statystyki &8]&m─────────"));
        
        String playerLine = plugin.getConfig().getString("messages.stats-player", "&7Gracz: &f%player%");
        sendMessage(sender, playerLine.replace("%player%", targetName));
        
        String killsLine = plugin.getConfig().getString("messages.stats-kills", "&7Zabojstwa: &c%kills%");
        sendMessage(sender, killsLine.replace("%kills%", String.valueOf(kills)));
        
        String deathsLine = plugin.getConfig().getString("messages.stats-deaths", "&7Smierci: &c%deaths%");
        sendMessage(sender, deathsLine.replace("%deaths%", String.valueOf(deaths)));
        
        String kdrLine = plugin.getConfig().getString("messages.stats-kdr", "&7K/D: &e%kdr%");
        sendMessage(sender, kdrLine.replace("%kdr%", String.valueOf(kdr)));
        
        sendMessage(sender, plugin.getConfig().getString("messages.stats-footer", 
            "&8&m──────────────────────────────"));

        return true;
    }

    private void sendMessage(CommandSender sender, String message) {
        if (sender instanceof Player player) {
            MessageUtil.sendRawMessage(player, message);
        } else {
            sender.sendMessage(MessageUtil.colorize(message));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        }

        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
            .toList();
    }
}
