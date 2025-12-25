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
import java.util.Arrays;
import java.util.List;

public class AntylogoutCommand implements CommandExecutor, TabCompleter {

    private final AnacodeAntylogout plugin;

    public AntylogoutCommand(AnacodeAntylogout plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("anacode.antylogout.admin")) {
            if (sender instanceof Player player) {
                MessageUtil.sendMessage(player, "&cNie masz uprawnien!");
            }
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reloadConfig();
                sendMessage(sender, "&aKonfiguracja zostala przeladowana!");
            }
            case "status" -> {
                if (args.length < 2) {
                    sendMessage(sender, "&7Gracze w walce:");
                    for (var uuid : plugin.getCombatManager().getCombatPlayers()) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null) {
                            int time = plugin.getCombatManager().getRemainingTime(player);
                            sendMessage(sender, "&8- &c" + player.getName() + " &7(&4" + time + "s&7)");
                        }
                    }
                } else {
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        sendMessage(sender, "&cGracz nie jest online!");
                        return true;
                    }

                    if (plugin.getCombatManager().isInCombat(target)) {
                        int time = plugin.getCombatManager().getRemainingTime(target);
                        sendMessage(sender, "&7Gracz &c" + target.getName() + " &7jest w walce (&4" + time + "s&7)");
                    } else {
                        sendMessage(sender, "&7Gracz &a" + target.getName() + " &7nie jest w walce.");
                    }
                }
            }
            case "remove" -> {
                if (args.length < 2) {
                    sendMessage(sender, "&cUzycie: /antylogout remove <gracz>");
                    return true;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sendMessage(sender, "&cGracz nie jest online!");
                    return true;
                }

                if (plugin.getCombatManager().isInCombat(target)) {
                    plugin.getCombatManager().removeFromCombat(target);
                    sendMessage(sender, "&7Usunieto &c" + target.getName() + " &7z walki!");
                } else {
                    sendMessage(sender, "&7Gracz nie jest w walce.");
                }
            }
            case "add" -> {
                if (args.length < 2) {
                    sendMessage(sender, "&cUzycie: /antylogout add <gracz>");
                    return true;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sendMessage(sender, "&cGracz nie jest online!");
                    return true;
                }

                plugin.getCombatManager().tagPlayer(target);
                sendMessage(sender, "&7Dodano &c" + target.getName() + " &7do walki!");
            }
            default -> sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sendMessage(sender, "&8&m                                        ");
        sendMessage(sender, "&4&lANACODE X ANTYLOGOUT &7- Pomoc");
        sendMessage(sender, "&8&m                                        ");
        sendMessage(sender, "&c/antylogout reload &8- &7Przeladuj config");
        sendMessage(sender, "&c/antylogout status [gracz] &8- &7Sprawdz status");
        sendMessage(sender, "&c/antylogout add <gracz> &8- &7Dodaj do walki");
        sendMessage(sender, "&c/antylogout remove <gracz> &8- &7Usun z walki");
        sendMessage(sender, "&8&m                                        ");
    }

    private void sendMessage(CommandSender sender, String message) {
        if (sender instanceof Player player) {
            MessageUtil.sendMessage(player, message);
        } else {
            sender.sendMessage(MessageUtil.colorize(message));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("reload", "status", "add", "remove"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("status") ||
                args[0].equalsIgnoreCase("add") ||
                args[0].equalsIgnoreCase("remove")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    completions.add(player.getName());
                }
            }
        }

        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
            .toList();
    }
}
