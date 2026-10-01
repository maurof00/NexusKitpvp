package it.maurof00.xeonkitpvp.util;

import it.maurof00.xeonkitpvp.KitPvPCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VersionUtil {
    private static final Pattern MC = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");

    private VersionUtil() {}

    public static String getVersion() {
        return Bukkit.getVersion();
    }

    public static int[] getNumbers() {
        Matcher matcher = MC.matcher(getVersion());
        if (!matcher.find()) return new int[] {1, 8, 8};
        int patch = matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3));
        return new int[] {Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), patch};
    }

    public static boolean isAtLeast(int major, int minor) {
        int[] v = getNumbers();
        if (v[0] != major) return v[0] > major;
        return v[1] >= minor;
    }

    public static Material material(String requested) {
        if (requested == null || requested.trim().isEmpty()) return Material.AIR;
        String name = requested.trim().toUpperCase();
        Material material = Material.matchMaterial(name);
        if (material != null) return material;

        if (name.equals("PLAYER_HEAD")) material = Material.matchMaterial("SKULL_ITEM");
        if (name.equals("OAK_PLANKS")) material = Material.matchMaterial("WOOD");
        if (name.equals("OAK_LOG")) material = Material.matchMaterial("LOG");
        if (name.equals("FIRE_CHARGE")) material = Material.matchMaterial("FIREBALL");
        if (name.equals("STAINED_GLASS_PANE")) material = Material.matchMaterial("GRAY_STAINED_GLASS_PANE");
        if (name.equals("GRAY_STAINED_GLASS_PANE")) material = Material.matchMaterial("STAINED_GLASS_PANE");
        if (name.equals("NETHERITE_SWORD")) material = Material.matchMaterial("DIAMOND_SWORD");
        if (name.equals("NETHERITE_CHESTPLATE")) material = Material.matchMaterial("DIAMOND_CHESTPLATE");
        if (name.equals("NETHERITE_LEGGINGS")) material = Material.matchMaterial("DIAMOND_LEGGINGS");
        if (name.equals("NETHERITE_BOOTS")) material = Material.matchMaterial("DIAMOND_BOOTS");
        if (name.equals("NETHERITE_HELMET")) material = Material.matchMaterial("DIAMOND_HELMET");
        if (material == null) return Material.AIR;
        return material;
    }

    public static void playSound(Player player, String legacy, String modern) {
        String sound = isAtLeast(9, 0) ? modern : legacy;
        try {
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (Throwable ignored) {
            // Sound names changed between versions. Gameplay is more important than a click noise.
        }
    }


    public static void applyDurability(org.bukkit.inventory.ItemStack item, short durability) {
        if (item == null || durability == 0) return;
        try {
            java.lang.reflect.Method method = item.getClass().getMethod("setDurability", short.class);
            method.invoke(item, durability);
        } catch (Throwable ignored) {
            // Durability/data values disappeared from the modern item model.
        }
    }

    public static void updateInventory(Player player) {
        try {
            java.lang.reflect.Method method = player.getClass().getMethod("updateInventory");
            method.invoke(player);
        } catch (Throwable ignored) {
            // Modern servers refresh the inventory automatically.
        }
    }

    public static void logDebug(KitPvPCore plugin) {
        int[] v = getNumbers();
        plugin.getLogger().info("Debug: Bukkit=" + getVersion()
                + " parsed=" + v[0] + "." + v[1] + "." + v[2]
                + " | Java=" + System.getProperty("java.version")
                + " | Worlds=" + Bukkit.getWorlds().size());
    }

    public static boolean sameWorld(Location a, Location b) {
        return a != null && b != null && a.getWorld() != null && b.getWorld() != null
                && a.getWorld().getName().equalsIgnoreCase(b.getWorld().getName());
    }
}
