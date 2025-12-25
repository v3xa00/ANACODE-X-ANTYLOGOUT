package pl.anacode.antylogout.manager;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.anacode.antylogout.AnacodeAntylogout;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StatsManager {

    private final AnacodeAntylogout plugin;
    private final File statsFile;
    private FileConfiguration statsConfig;
    private final Map<UUID, Integer> killsCache;
    private final Map<UUID, Integer> deathsCache;

    public StatsManager(AnacodeAntylogout plugin) {
        this.plugin = plugin;
        this.killsCache = new HashMap<>();
        this.deathsCache = new HashMap<>();
        
        // Utworz folder data jesli nie istnieje
        File dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        this.statsFile = new File(dataFolder, "stats.yml");
        loadStats();
    }

    private void loadStats() {
        if (!statsFile.exists()) {
            try {
                statsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Nie mozna utworzyc pliku stats.yml!");
                e.printStackTrace();
            }
        }
        statsConfig = YamlConfiguration.loadConfiguration(statsFile);
        
        // Zaladuj do cache
        if (statsConfig.contains("players")) {
            for (String uuidString : statsConfig.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    int kills = statsConfig.getInt("players." + uuidString + ".kills", 0);
                    int deaths = statsConfig.getInt("players." + uuidString + ".deaths", 0);
                    killsCache.put(uuid, kills);
                    deathsCache.put(uuid, deaths);
                } catch (IllegalArgumentException ignored) {}
            }
        }
        
        plugin.getLogger().info("Zaladowano statystyki dla " + killsCache.size() + " graczy.");
    }

    public void saveAllStats() {
        for (Map.Entry<UUID, Integer> entry : killsCache.entrySet()) {
            String path = "players." + entry.getKey().toString();
            statsConfig.set(path + ".kills", entry.getValue());
            statsConfig.set(path + ".deaths", deathsCache.getOrDefault(entry.getKey(), 0));
        }
        
        try {
            statsConfig.save(statsFile);
            plugin.getLogger().info("Zapisano statystyki dla " + killsCache.size() + " graczy.");
        } catch (IOException e) {
            plugin.getLogger().severe("Nie mozna zapisac pliku stats.yml!");
            e.printStackTrace();
        }
    }

    public void addKill(UUID uuid) {
        int current = killsCache.getOrDefault(uuid, 0);
        killsCache.put(uuid, current + 1);
        savePlayerStats(uuid);
    }

    public void addDeath(UUID uuid) {
        int current = deathsCache.getOrDefault(uuid, 0);
        deathsCache.put(uuid, current + 1);
        savePlayerStats(uuid);
    }

    private void savePlayerStats(UUID uuid) {
        String path = "players." + uuid.toString();
        statsConfig.set(path + ".kills", killsCache.getOrDefault(uuid, 0));
        statsConfig.set(path + ".deaths", deathsCache.getOrDefault(uuid, 0));
        
        try {
            statsConfig.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Nie mozna zapisac statystyk gracza!");
        }
    }

    public int getKills(UUID uuid) {
        return killsCache.getOrDefault(uuid, 0);
    }

    public int getDeaths(UUID uuid) {
        return deathsCache.getOrDefault(uuid, 0);
    }

    public double getKDR(UUID uuid) {
        int kills = getKills(uuid);
        int deaths = getDeaths(uuid);
        
        if (deaths == 0) {
            return kills;
        }
        
        return Math.round((double) kills / deaths * 100.0) / 100.0;
    }
}
