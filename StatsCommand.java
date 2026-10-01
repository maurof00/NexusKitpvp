package it.maurof00.xeonkitpvp.command;

import it.maurof00.xeonkitpvp.KitPvPCore;
import it.maurof00.xeonkitpvp.data.DataManager;
import it.maurof00.xeonkitpvp.gang.GangManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public final class StatsCommand implements CommandExecutor, TabCompleter {
    private final KitPvPCore plugin;
    public StatsCommand(KitPvPCore plugin) { this.plugin = plugin; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        UUID uuid;
        String name;
        if (args.length == 0) {
            if (!(sender instanceof Player)) { sender.sendMessage("/stats <player>"); return true; }
            Player p = (Player) sender;
            uuid = p.getUniqueId(); name = p.getName();
        } else {
            Player p = Bukkit.getPlayerExact(args[0]);
            if (p != null) { uuid = p.getUniqueId(); name = p.getName(); }
            else {
                OfflinePlayer offline = Bukkit.getOfflinePlayer(args[0]);
                if (!offline.hasPlayedBefore()) { sender.sendMessage(plugin.getMessages().get("player-not-found")); return true; }
                uuid = offline.getUniqueId(); name = offline.getName() == null ? args[0] : offline.getName();
            }
        }
        DataManager.PlayerStats s = plugin.getDataManager().get(uuid, name);
        GangManager.Gang gang = plugin.getGangManager().getByPlayer(uuid);
        sender.sendMessage(plugin.getMessages().format(plugin.getMessages().get("stats-header"), "%player%", name));
        for (String line : plugin.getMessages().list("stats-lines")) {
            sender.sendMessage(plugin.getMessages().format(line,
                    "%player%", name,
                    "%kills%", String.valueOf(s.kills),
                    "%deaths%", String.valueOf(s.deaths),
                    "%kd%", String.format(Locale.US, "%.2f", plugin.getDataManager().kd(s)),
                    "%streak%", String.valueOf(s.streak),
                    "%beststreak%", String.valueOf(s.bestStreak),
                    "%level%", String.valueOf(s.level),
                    "%xp%", String.valueOf(s.xp),
                    "%nextxp%", String.valueOf(plugin.getDataManager().getNextXp(s.level)),
                    "%coins%", String.valueOf(s.coins),
                    "%gang%", gang == null ? "Nessuna" : gang.name));
        }
        return true;
    }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return Collections.emptyList();
        List<String> out = new ArrayList<String>();
        for (Player p : Bukkit.getOnlinePlayers()) if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) out.add(p.getName());
        return out;
    }
}
