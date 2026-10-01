package it.maurof00.xeonkitpvp.data;

import it.maurof00.xeonkitpvp.KitPvPCore;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class DataManager {
    public static final class PlayerStats {
        public int kills;
        public int deaths;
        public int coins;
        public int xp;
        public int level;
        public int streak;
        public int bestStreak;
        public int bounty;
        public String selectedKit = "warrior";
        public Set<String> ownedKits = new HashSet<String>();
        public String lastName = "Unknown";
    }

    private final KitPvPCore plugin;
    private final File file;
    private YamlConfiguration data;
    private final Map<UUID, PlayerStats> cache = new HashMap<UUID, PlayerStats>();

    public DataManager(KitPvPCore plugin) {
        this.plugin = plugin;
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        this.file = new File(plugin.getDataFolder(), "players.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public PlayerStats get(UUID uuid, String name) {
        PlayerStats stats = cache.get(uuid);
        if (stats == null) {
            stats = load(uuid, name);
            cache.put(uuid, stats);
        }
        if (name != null) stats.lastName = name;
        ensureStarter(stats);
        return stats;
    }

    private PlayerStats load(UUID uuid, String name) {
        PlayerStats stats = new PlayerStats();
        String base = "players." + uuid.toString();
        stats.kills = data.getInt(base + ".kills", 0);
        stats.deaths = data.getInt(base + ".deaths", 0);
        stats.coins = data.getInt(base + ".coins", plugin.getConfig().getInt("economy.join-coins", 250));
        stats.xp = data.getInt(base + ".xp", 0);
        stats.level = data.getInt(base + ".level", 1);
        stats.streak = data.getInt(base + ".streak", 0);
        stats.bestStreak = data.getInt(base + ".best-streak", 0);
        stats.bounty = data.getInt(base + ".bounty", 0);
        stats.selectedKit = data.getString(base + ".selected-kit", "warrior");
        stats.lastName = data.getString(base + ".name", name == null ? "Unknown" : name);
        stats.ownedKits.addAll(data.getStringList(base + ".owned-kits"));
        ensureStarter(stats);
        return stats;
    }

    private void ensureStarter(PlayerStats stats) {
        stats.ownedKits.add("warrior");
        if (stats.level <= 0) stats.level = 1;
        if (stats.coins < 0) stats.coins = 0;
        if (stats.xp < 0) stats.xp = 0;
    }

    public void save() {
        for (Map.Entry<UUID, PlayerStats> entry : cache.entrySet()) {
            String base = "players." + entry.getKey().toString();
            PlayerStats s = entry.getValue();
            data.set(base + ".kills", s.kills);
            data.set(base + ".deaths", s.deaths);
            data.set(base + ".coins", s.coins);
            data.set(base + ".xp", s.xp);
            data.set(base + ".level", s.level);
            data.set(base + ".streak", s.streak);
            data.set(base + ".best-streak", s.bestStreak);
            data.set(base + ".bounty", s.bounty);
            data.set(base + ".selected-kit", s.selectedKit);
            data.set(base + ".owned-kits", new ArrayList<String>(s.ownedKits));
            data.set(base + ".name", s.lastName);
        }
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save players.yml: " + e.getMessage());
        }
    }

    public Collection<Map.Entry<UUID, PlayerStats>> all() {
        loadAllIntoCache();
        return new ArrayList<Map.Entry<UUID, PlayerStats>>(cache.entrySet());
    }

    private void loadAllIntoCache() {
        if (!data.isConfigurationSection("players")) return;
        for (String key : data.getConfigurationSection("players").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                if (!cache.containsKey(uuid)) cache.put(uuid, load(uuid, data.getString("players." + key + ".name", "Unknown")));
            } catch (IllegalArgumentException ignored) { }
        }
    }

    public int getNextXp(int level) {
        return plugin.getConfig().getInt("leveling.base-xp", 100)
                + Math.max(0, level - 1) * plugin.getConfig().getInt("leveling.xp-per-level", 75);
    }

    public boolean addXp(UUID uuid, String name, int amount) {
        if (!plugin.getConfig().getBoolean("leveling.enabled", true) || amount <= 0) return false;
        PlayerStats s = get(uuid, name);
        s.xp += amount;
        boolean leveled = false;
        int max = plugin.getConfig().getInt("leveling.max-level", 100);
        while (s.level < max && s.xp >= getNextXp(s.level)) {
            s.xp -= getNextXp(s.level);
            s.level++;
            leveled = true;
        }
        return leveled;
    }

    public double kd(PlayerStats s) {
        if (s.deaths <= 0) return s.kills;
        return (double) s.kills / (double) s.deaths;
    }
}
