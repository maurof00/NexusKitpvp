package it.maurof00.xeonkitpvp.command;

import it.maurof00.xeonkitpvp.KitPvPCore;
import it.maurof00.xeonkitpvp.data.DataManager;
import it.maurof00.xeonkitpvp.util.VersionUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public final class KitPvPCommand implements CommandExecutor, TabCompleter {
    private final KitPvPCore plugin;
    public KitPvPCommand(KitPvPCore plugin) { this.plugin = plugin; }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) { sendHelp(sender); return true; }
        String sub = args[0].toLowerCase();
        if (sub.equals("spawn")) { if (sender instanceof Player) plugin.getGameListener().teleportSpawn((Player)sender); return true; }
        if (sub.equals("stats")) { new StatsCommand(plugin).onCommand(sender, command, label, args.length > 1 ? new String[]{args[1]} : new String[0]); return true; }
        if (sub.equals("top")) { showTop(sender, args.length > 1 ? args[1].toLowerCase() : "kills"); return true; }
        if (sub.equals("bounty")) { if (sender instanceof Player) new BountyCommand(plugin).onCommand(sender, command, label, Arrays.copyOfRange(args, 1, args.length)); return true; }
        if (sub.equals("kit")) { if (sender instanceof Player) new KitCommand(plugin).onCommand(sender, command, label, Arrays.copyOfRange(args, 1, args.length)); return true; }
        if (sub.equals("reload")) {
            if (!admin(sender)) return true;
            plugin.reloadAll();
            sender.sendMessage(plugin.getMessages().get("reload"));
            return true;
        }
        if (sub.equals("debug")) {
            if (!admin(sender)) return true;
            sender.sendMessage(plugin.getMessages().get("debug-header"));
            sender.sendMessage(ChatColor.GRAY + "Version: " + ChatColor.WHITE + plugin.getDescription().getVersion());
            sender.sendMessage(ChatColor.GRAY + "Bukkit: " + ChatColor.WHITE + VersionUtil.getVersion());
            sender.sendMessage(ChatColor.GRAY + "Kits: " + ChatColor.WHITE + plugin.getKitManager().size());
            sender.sendMessage(ChatColor.GRAY + "Gangs: " + ChatColor.WHITE + plugin.getGangManager().size());
            sender.sendMessage(ChatColor.GRAY + "Worlds: " + ChatColor.WHITE + String.join(", ", plugin.getConfig().getStringList("game.enabled-worlds")));
            sender.sendMessage(ChatColor.GRAY + "PVP: " + ChatColor.WHITE + plugin.getConfig().getBoolean("game.allow-pvp", true));
            sender.sendMessage(ChatColor.GRAY + "Scoreboard: " + ChatColor.WHITE + plugin.getConfig().getBoolean("scoreboard.enabled", true));
            sender.sendMessage(ChatColor.GRAY + "Java: " + ChatColor.WHITE + System.getProperty("java.version"));
            return true;
        }
        if (sub.equals("setspawn")) {
            if (!admin(sender) || !(sender instanceof Player)) return true;
            Player p = (Player)sender; World world = p.getWorld();
            plugin.getConfig().set("spawn.world", world.getName());
            plugin.getConfig().set("spawn.x", p.getLocation().getX());
            plugin.getConfig().set("spawn.y", p.getLocation().getY());
            plugin.getConfig().set("spawn.z", p.getLocation().getZ());
            plugin.getConfig().set("spawn.yaw", p.getLocation().getYaw());
            plugin.getConfig().set("spawn.pitch", p.getLocation().getPitch());
            plugin.saveConfig(); plugin.getMessages().send(p, "spawn-set");
            return true;
        }
        if (sub.equals("arena")) return arena(sender, args);
        sendHelp(sender); return true;
    }
    private boolean arena(CommandSender sender, String[] args) {
        if (!admin(sender)) return true;
        if (args.length < 2) { sender.sendMessage("/kitpvp arena <add|remove|list> [world]"); return true; }
        List<String> worlds = new ArrayList<String>(plugin.getConfig().getStringList("game.enabled-worlds"));
        String action = args[1].toLowerCase();
        if (action.equals("list")) { sender.sendMessage(plugin.getMessages().format(plugin.getMessages().get("arena-list"), "%worlds%", String.join(", ", worlds))); return true; }
        if (args.length < 3) { sender.sendMessage("/kitpvp arena " + action + " <world>"); return true; }
        String world = args[2];
        if (action.equals("add")) {
            if (!worlds.contains(world)) worlds.add(world);
            plugin.getConfig().set("game.enabled-worlds", worlds); plugin.saveConfig();
            sender.sendMessage(plugin.getMessages().format(plugin.getMessages().get("arena-added"), "%world%", world)); return true;
        }
        if (action.equals("remove")) {
            if (!worlds.remove(world)) { sender.sendMessage(plugin.getMessages().format(plugin.getMessages().get("arena-not-found"), "%world%", world)); return true; }
            plugin.getConfig().set("game.enabled-worlds", worlds); plugin.saveConfig();
            sender.sendMessage(plugin.getMessages().format(plugin.getMessages().get("arena-removed"), "%world%", world)); return true;
        }
        return true;
    }
    private void showTop(CommandSender sender, String type) {
        final String normalized = type.equals("coins") || type.equals("streak") ? type : "kills";
        List<DataManager.PlayerStats> stats = new ArrayList<DataManager.PlayerStats>();
        for (Map.Entry<UUID, DataManager.PlayerStats> entry : plugin.getDataManager().all()) stats.add(entry.getValue());
        Collections.sort(stats, new Comparator<DataManager.PlayerStats>() {
            @Override public int compare(DataManager.PlayerStats a, DataManager.PlayerStats b) { return Integer.compare(value(b, normalized), value(a, normalized)); }
        });
        sender.sendMessage(plugin.getMessages().format(plugin.getMessages().get("top-header"), "%type%", normalized.toUpperCase()));
        int rank = 1;
        for (DataManager.PlayerStats s : stats) {
            if (rank > 10) break;
            sender.sendMessage(plugin.getMessages().format(plugin.getMessages().get("top-line"), "%rank%", String.valueOf(rank), "%player%", s.lastName, "%value%", String.valueOf(value(s, normalized))));
            rank++;
        }
    }
    private int value(DataManager.PlayerStats s, String type) { return type.equals("coins") ? s.coins : type.equals("streak") ? s.bestStreak : s.kills; }
    private void sendHelp(CommandSender sender) { for (String line : plugin.getMessages().list("help")) sender.sendMessage(line); if (sender.hasPermission("kitpvp.admin")) sender.sendMessage(ChatColor.RED + "/kitpvp setspawn | arena | reload | debug"); }
    private boolean admin(CommandSender sender) { if (!sender.hasPermission("kitpvp.admin")) { sender.sendMessage(plugin.getMessages().get("no-permission")); return false; } return true; }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return filter(Arrays.asList("help","spawn","stats","top","bounty","kit","setspawn","arena","reload","debug"), args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("top")) return filter(Arrays.asList("kills","coins","streak"), args[1]);
        if (args.length == 2 && args[0].equalsIgnoreCase("arena")) return filter(Arrays.asList("add","remove","list"), args[1]);
        return Collections.emptyList();
    }
    private List<String> filter(List<String> values, String prefix) { List<String> out = new ArrayList<String>(); for (String value : values) if (value.startsWith(prefix.toLowerCase())) out.add(value); return out; }
}
