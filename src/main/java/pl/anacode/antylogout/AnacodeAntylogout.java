package pl.anacode.antylogout;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import pl.anacode.antylogout.command.AntylogoutCommand;
import pl.anacode.antylogout.listener.*;
import pl.anacode.antylogout.manager.*;
import pl.anacode.antylogout.task.*;

public class AnacodeAntylogout extends JavaPlugin {

    private static AnacodeAntylogout instance;
    private ConfigManager configManager;
    private CombatManager combatManager;
    private WallManager wallManager;
    private LastDamagerManager lastDamagerManager;
    private RegionManager regionManager;
    private StatsManager statsManager;
    private VoidCheckTask voidCheckTask;
    private boolean worldGuardEnabled = false;

    @Override
    public void onEnable() {
        instance = this;
        
        saveDefaultConfig();
        
        // Inicjalizacja managerów (kolejność ważna!)
        configManager = new ConfigManager(this);
        lastDamagerManager = new LastDamagerManager(this);
        combatManager = new CombatManager(this);
        wallManager = new WallManager(this);
        statsManager = new StatsManager(this);
        
        // Sprawdź WorldGuard
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
            worldGuardEnabled = true;
            regionManager = new RegionManager(this);
            getLogger().info("WorldGuard znaleziony! Integracja włączona.");
        } else {
            getLogger().warning("WorldGuard nie znaleziony! Funkcja regionów wyłączona.");
        }
        
        // Rejestracja listenerów
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new CommandListener(this), this);
        
        if (worldGuardEnabled) {
            getServer().getPluginManager().registerEvents(new MoveListener(this), this);
            getServer().getPluginManager().registerEvents(new PearlListener(this), this);
        }
        
        // Rejestracja komendy
        getCommand("antylogout").setExecutor(new AntylogoutCommand(this));
        
        // Start tasków
        new ActionBarTask(this).runTaskTimer(this, 0L, 20L);
        
        if (worldGuardEnabled && configManager.isWallEnabled()) {
            new WallUpdateTask(this).runTaskTimer(this, 0L, 5L);
        }
        
        // Void Check Task
        if (configManager.isVoidCombatEnabled()) {
            voidCheckTask = new VoidCheckTask(this);
            voidCheckTask.runTaskTimer(this, 0L, 10L);
        }
        
        getLogger().info("========================================");
        getLogger().info("  ANACODE X ANTYLOGOUT v" + getDescription().getVersion());
        getLogger().info("  Plugin został pomyślnie włączony!");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        if (voidCheckTask != null) {
            voidCheckTask.cancel();
        }
        
        if (combatManager != null) {
            combatManager.killAllInCombat();
        }
        
        if (wallManager != null) {
            wallManager.removeAllWalls();
        }
        
        if (statsManager != null) {
            statsManager.saveAllStats();
        }
        
        getLogger().info("ANACODE X ANTYLOGOUT został wyłączony!");
    }

    public static AnacodeAntylogout getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public CombatManager getCombatManager() {
        return combatManager;
    }

    public WallManager getWallManager() {
        return wallManager;
    }

    public LastDamagerManager getLastDamagerManager() {
        return lastDamagerManager;
    }

    public RegionManager getRegionManager() {
        return regionManager;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public VoidCheckTask getVoidCheckTask() {
        return voidCheckTask;
    }

    public boolean isWorldGuardEnabled() {
        return worldGuardEnabled;
    }
}
