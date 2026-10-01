package it.maurof00.xeonkitpvp.kit;

import it.maurof00.xeonkitpvp.KitPvPCore;
import it.maurof00.xeonkitpvp.util.VersionUtil;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.util.*;

public final class KitManager {
    private final KitPvPCore plugin;
    private final File file;
    private YamlConfiguration config;
    private final Map<String, Kit> kits = new LinkedHashMap<String, Kit>();
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<UUID, Map<String, Long>>();
    private final Map<UUID, Long> abilityCooldowns = new HashMap<UUID, Long>();

    public KitManager(KitPvPCore plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "kits.yml");
        reload();
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(file);
        kits.clear();
        ConfigurationSection section = config.getConfigurationSection("kits");
        if (section == null) return;
        for (String id : section.getKeys(false)) {
            ConfigurationSection k = section.getConfigurationSection(id);
            if (k == null) continue;
            Kit kit = new Kit(id.toLowerCase());
            kit.setDisplayName(color(k.getString("name", id)));
            kit.setPrice(Math.max(0, k.getInt("price", 0)));
            kit.setPermission(k.getString("permission", "").trim());
            kit.setCooldown(Math.max(0, k.getInt("cooldown", 0)));
            kit.setMenuItem(item(k.getString("material", "IRON_SWORD"), "&f" + id, null));
            for (String itemLine : k.getStringList("items")) kit.getItems().add(parseStack(itemLine));
            ConfigurationSection armor = k.getConfigurationSection("armor");
            if (armor != null) {
                kit.setHelmet(parseStack(armor.getString("helmet", "AIR")));
                kit.setChestplate(parseStack(armor.getString("chestplate", "AIR")));
                kit.setLeggings(parseStack(armor.getString("leggings", "AIR")));
                kit.setBoots(parseStack(armor.getString("boots", "AIR")));
            }
            for (String effectLine : k.getStringList("effects")) {
                PotionEffect effect = parseEffect(effectLine);
                if (effect != null) kit.getEffects().add(effect);
            }
            ConfigurationSection ability = k.getConfigurationSection("ability");
            if (ability != null) {
                kit.setAbilityType(ability.getString("type", "NONE").toUpperCase());
                kit.setAbilityCooldown(Math.max(0, ability.getInt("cooldown", 0)));
                kit.setAbilityName(color(ability.getString("name", "&bAbility")));
                kit.setAbilityItem(item(ability.getString("material", "AIR"), ability.getString("name", "&bAbility"), null));
            }
            kits.put(kit.getId(), kit);
        }
    }

    public int size() { return kits.size(); }
    public Collection<Kit> getKits() { return kits.values(); }
    public Kit get(String id) { return id == null ? null : kits.get(id.toLowerCase()); }

    public boolean hasKit(UUID uuid, String name, Kit kit) {
        if (kit == null) return false;
        if (plugin.getDataManager().get(uuid, name).ownedKits.contains(kit.getId())) return true;
        String permission = kit.getPermission();
        return !permission.isEmpty() && plugin.getServer().getPlayer(uuid) != null && plugin.getServer().getPlayer(uuid).hasPermission(permission);
    }

    public long getCooldownLeft(UUID uuid, String kitId) {
        Map<String, Long> map = cooldowns.get(uuid);
        if (map == null) return 0;
        Long until = map.get(kitId);
        if (until == null) return 0;
        long remaining = until - System.currentTimeMillis();
        return Math.max(0, (remaining + 999L) / 1000L);
    }

    public void startCooldown(UUID uuid, String kitId, int seconds) {
        if (seconds <= 0) return;
        Map<String, Long> map = cooldowns.get(uuid);
        if (map == null) { map = new HashMap<String, Long>(); cooldowns.put(uuid, map); }
        map.put(kitId, System.currentTimeMillis() + seconds * 1000L);
    }

    public long getAbilityCooldownLeft(UUID uuid) {
        Long until = abilityCooldowns.get(uuid);
        if (until == null) return 0;
        long remaining = until - System.currentTimeMillis();
        return Math.max(0, (remaining + 999L) / 1000L);
    }

    public void startAbilityCooldown(UUID uuid, int seconds) {
        if (seconds > 0) abilityCooldowns.put(uuid, System.currentTimeMillis() + seconds * 1000L);
    }

    private ItemStack parseStack(String line) {
        if (line == null || line.trim().isEmpty()) return new ItemStack(VersionUtil.material("AIR"));
        String[] parts = line.trim().split(":");
        String materialName = parts[0];
        short data = 0;
        int amount = 1;
        try {
            if (parts.length > 1) data = Short.parseShort(parts[1]);
            if (parts.length > 2) amount = Math.max(1, Integer.parseInt(parts[2]));
        } catch (NumberFormatException ignored) {}
        ItemStack stack = new ItemStack(VersionUtil.material(materialName), amount);
        VersionUtil.applyDurability(stack, data);
        if (parts.length > 3) {
            ItemMeta meta = stack.getItemMeta();
            List<String> lore = new ArrayList<String>();
            for (int i = 3; i < parts.length; i++) lore.add(color(parts[i].replace('_', ' ')));
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private PotionEffect parseEffect(String line) {
        String[] parts = line.split(":");
        if (parts.length < 3) return null;
        PotionEffectType type = PotionEffectType.getByName(parts[0].toUpperCase());
        if (type == null) return null;
        try {
            int amplifier = Integer.parseInt(parts[1]);
            int duration = Integer.parseInt(parts[2]);
            return new PotionEffect(type, duration, amplifier);
        } catch (NumberFormatException ex) { return null; }
    }

    private ItemStack item(String materialName, String name, List<String> lore) {
        ItemStack stack = new ItemStack(VersionUtil.material(materialName));
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            if (lore != null) meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public static String color(String value) { return ChatColor.translateAlternateColorCodes('&', value == null ? "" : value); }
    public void clearCooldowns(UUID uuid) { cooldowns.remove(uuid); abilityCooldowns.remove(uuid); }
}
