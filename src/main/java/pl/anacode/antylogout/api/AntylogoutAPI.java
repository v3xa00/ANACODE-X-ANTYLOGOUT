package pl.anacode.antylogout.api;

import org.bukkit.entity.Player;
import pl.anacode.antylogout.AnacodeAntylogout;

/**
 * API dla pluginu ANACODE X ANTYLOGOUT
 * 
 * Przykład użycia w innym pluginie:
 * 
 * AntylogoutAPI api = AntylogoutAPI.getInstance();
 * 
 * // Sprawdz czy gracz jest w walce
 * if (api.isPlayerInCombat(player)) {
 *     // gracz jest w walce
 * }
 * 
 * // Dodaj gracza do walki
 * api.tagPlayer(player);
 * 
 * // Dodaj dwoch graczy do walki (atak)
 * api.tagPlayers(attacker, victim);
 * 
 * // Usun gracza z walki
 * api.untagPlayer(player);
 * 
 * // Pobierz pozostaly czas walki
 * int seconds = api.getCombatTimeLeft(player);
 * 
 * // Pobierz statystyki
 * int kills = api.getKills(player);
 * int deaths = api.getDeaths(player);
 * double kdr = api.getKDR(player);
 */
public class AntylogoutAPI {

    private static AntylogoutAPI instance;
    private final AnacodeAntylogout plugin;

    private AntylogoutAPI(AnacodeAntylogout plugin) {
        this.plugin = plugin;
    }

    /**
     * Pobiera instancje API
     * @return instancja AntylogoutAPI lub null jesli plugin nie jest wlaczony
     */
    public static AntylogoutAPI getInstance() {
        if (instance == null) {
            AnacodeAntylogout plugin = AnacodeAntylogout.getInstance();
            if (plugin != null) {
                instance = new AntylogoutAPI(plugin);
            }
        }
        return instance;
    }

    /**
     * Inicjalizuje API (wywolywane przez glowny plugin)
     */
    public static void init(AnacodeAntylogout plugin) {
        instance = new AntylogoutAPI(plugin);
    }

    /**
     * Resetuje API (wywolywane przy wylaczaniu pluginu)
     */
    public static void reset() {
        instance = null;
    }

    // ==================== COMBAT METHODS ====================

    /**
     * Sprawdza czy gracz jest w walce
     * @param player gracz do sprawdzenia
     * @return true jesli gracz jest w walce
     */
    public boolean isPlayerInCombat(Player player) {
        if (player == null) return false;
        return plugin.getCombatManager().isInCombat(player);
    }

    /**
     * Dodaje gracza do walki (odnawia czas jesli juz jest w walce)
     * @param player gracz do dodania
     */
    public void tagPlayer(Player player) {
        if (player == null) return;
        plugin.getCombatManager().tagPlayer(player);
    }

    /**
     * Dodaje dwoch graczy do walki (np. atakujacy i ofiara)
     * @param attacker atakujacy
     * @param victim ofiara
     */
    public void tagPlayers(Player attacker, Player victim) {
        if (attacker == null || victim == null) return;
        plugin.getCombatManager().tagBoth(attacker, victim);
    }

    /**
     * Usuwa gracza z walki
     * @param player gracz do usuniecia
     */
    public void untagPlayer(Player player) {
        if (player == null) return;
        plugin.getCombatManager().removeFromCombat(player);
    }

    /**
     * Usuwa gracza z walki bez wysylania wiadomosci
     * @param player gracz do usuniecia
     */
    public void untagPlayerSilent(Player player) {
        if (player == null) return;
        plugin.getCombatManager().removeFromCombatSilent(player);
    }

    /**
     * Pobiera pozostaly czas walki gracza w sekundach
     * @param player gracz
     * @return pozostaly czas w sekundach, 0 jesli nie jest w walce
     */
    public int getCombatTimeLeft(Player player) {
        if (player == null) return 0;
        return plugin.getCombatManager().getRemainingTime(player);
    }

    /**
     * Pobiera ostatniego atakujacego gracza
     * @param player gracz (ofiara)
     * @return ostatni atakujacy lub null
     */
    public Player getLastAttacker(Player player) {
        if (player == null) return null;
        return plugin.getCombatManager().getLastAttacker(player);
    }

    // ==================== STATS METHODS ====================

    /**
     * Pobiera liczbe zabojstw gracza
     * @param player gracz
     * @return liczba zabojstw
     */
    public int getKills(Player player) {
        if (player == null) return 0;
        return plugin.getStatsManager().getKills(player.getUniqueId());
    }

    /**
     * Pobiera liczbe smierci gracza
     * @param player gracz
     * @return liczba smierci
     */
    public int getDeaths(Player player) {
        if (player == null) return 0;
        return plugin.getStatsManager().getDeaths(player.getUniqueId());
    }

    /**
     * Pobiera K/D ratio gracza
     * @param player gracz
     * @return K/D ratio
     */
    public double getKDR(Player player) {
        if (player == null) return 0.0;
        return plugin.getStatsManager().getKDR(player.getUniqueId());
    }

    /**
     * Dodaje zabojstwo graczowi
     * @param player gracz
     */
    public void addKill(Player player) {
        if (player == null) return;
        plugin.getStatsManager().addKill(player.getUniqueId());
    }

    /**
     * Dodaje smierc graczowi
     * @param player gracz
     */
    public void addDeath(Player player) {
        if (player == null) return;
        plugin.getStatsManager().addDeath(player.getUniqueId());
    }

    // ==================== REGION METHODS ====================

    /**
     * Sprawdza czy gracz jest w zablokowanym regionie
     * @param player gracz
     * @return true jesli jest w zablokowanym regionie
     */
    public boolean isInBlockedRegion(Player player) {
        if (player == null) return false;
        if (plugin.getRegionManager() == null) return false;
        return plugin.getRegionManager().isInBlockedRegion(player);
    }

    /**
     * Sprawdza czy gracz ma aktywna sciane
     * @param player gracz
     * @return true jesli ma sciane
     */
    public boolean hasWall(Player player) {
        if (player == null) return false;
        return plugin.getWallManager().hasWall(player);
    }

    // ==================== PLUGIN INFO ====================

    /**
     * Pobiera wersje pluginu
     * @return wersja pluginu
     */
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    /**
     * Sprawdza czy WorldGuard jest wlaczony
     * @return true jesli WorldGuard jest dostepny
     */
    public boolean isWorldGuardEnabled() {
        return plugin.isWorldGuardEnabled();
    }

    /**
     * Sprawdza czy PlaceholderAPI jest wlaczony
     * @return true jesli PlaceholderAPI jest dostepny
     */
    public boolean isPlaceholderAPIEnabled() {
        return plugin.isPlaceholderAPIEnabled();
    }
}
