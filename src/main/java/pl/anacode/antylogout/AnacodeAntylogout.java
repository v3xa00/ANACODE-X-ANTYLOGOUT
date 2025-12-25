package pl.anacode.antylogout;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import pl.anacode.antylogout.command.AntylogoutCommand;
import pl.anacode.antylogout.listener.CombatListener;
import pl.anacode.antylogout.listener.CommandListener;
import pl.anacode.antylogout.listener.MoveListener;
import pl.anacode.antylogout.listener.QuitListener;
import pl.anacode.antylogout.manager.CombatManager;
import pl.anacode.antylogout.manager.RegionManager;
import pl.anacode.antylogout.manager.WallManager;
import pl.anacode.antylogout.task.CombatTask;

public class AnacodeAntylogout extends JavaPlugin {

    private static AnacodeAntylogout instance;
    private CombatManager combatManager;
    private WallManager wallManager;
    private RegionManager regionManager;
    private boolean worldGuardEnabled = false;

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("");
        getLogger().info("  ___   _   _   ___   ___  ___  ___  ___ ");
        getLogger().info(" / _ \\ | \\ | | / _ \\ / __|/ _ \\|   \\| __|");
        getLogger().info("| (_) ||  \\| || (_) | (__| (_) | |) | _| ");
        getLogger().info(" \\___/ |_|\\__|\\___/ \\___|\\___/|___/|___|");
        getLogger().info("         X ANTYLOGOUT v" + getDescription().getVersion());
        getLogger().info("");

        saveDefaultConfig();

        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
            worldGuardEnabled = true;
            getLogger().info("WorldGuard wykryty! Ochrona regionow wlaczona.");
        } else {
            getLogger().warning("WorldGuard nie znaleziony! Ochrona regionow wylaczona.");
        }

        combatManager = new CombatManager(this);
        wallManager = new WallManager(this);

        if (worldGuardEnabled && getConfig().getBoolean("settings.worldguard-protection", true)) {
            regionManager = new RegionManager(this);
        }

        registerListeners();

        new CombatTask(this).runTaskTimer(this, 0L, 20L);

        getCommand("antylogout").setExecutor(new AntylogoutCommand(this));

        getLogger().info("Plugin zostal pomyslnie wlaczony!");
    }

    @Override
    public void onDisable() {
        if (wallManager != null) {
            wallManager.removeAllWalls();
        }
        if (combatManager != null) {
            combatManager.clearAll();
        }
        getLogger().info("Plugin zostal wylaczony!");
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new CombatListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CommandListener(this), this);
        Bukkit.getPluginManager().registerEvents(new QuitListener(this), this);

        if (worldGuardEnabled && getConfig().getBoolean("settings.worldguard-protection", true)) {
            Bukkit.getPluginManager().registerEvents(new MoveListener(this), this);
        }
    }

    public static AnacodeAntylogout getInstance() {
        return instance;
    }

    public CombatManager getCombatManager() {
        return combatManager;
    }

    public WallManager getWallManager() {
        return wallManager;
    }

    public RegionManager getRegionManager() {
        return regionManager;
    }

    public boolean isWorldGuardEnabled() {
        return worldGuardEnabled;
    }
}
