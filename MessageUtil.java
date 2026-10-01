package it.maurof00.xeonkitpvp.util;

import it.maurof00.xeonkitpvp.KitPvPCore;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MessageUtil {
    private final KitPvPCore plugin;
    private YamlConfiguration lang;
    private String prefix;

    public MessageUtil(KitPvPCore plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "lang.yml");
        lang = YamlConfiguration.loadConfiguration(file);
        prefix = color(lang.getString("messages.prefix", ""));
    }

    public String get(String path) {
        return prefix + color(lang.getString("messages." + path, path));
    }

    public String raw(String path) {
        return color(lang.getString("messages." + path, path));
    }

    public List<String> list(String path) {
        List<String> values = lang.getStringList("messages." + path);
        if (values == null) return Collections.emptyList();
        List<String> out = new ArrayList<String>();
        for (String value : values) out.add(prefix + color(value));
        return out;
    }

    public void send(Player player, String path) {
        player.sendMessage(get(path));
    }

    public String format(String message, String... replacements) {
        String result = message;
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            result = result.replace(replacements[i], replacements[i + 1]);
        }
        return result;
    }

    public void sendFormatted(Player player, String path, String... replacements) {
        player.sendMessage(format(get(path), replacements));
    }

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
}
