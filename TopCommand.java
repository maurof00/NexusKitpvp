package it.maurof00.xeonkitpvp.command;

import it.maurof00.xeonkitpvp.KitPvPCore;
import it.maurof00.xeonkitpvp.data.DataManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.*;

public final class TopCommand implements CommandExecutor, TabCompleter {
    private final KitPvPCore plugin;
    public TopCommand(KitPvPCore plugin) { this.plugin = plugin; }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String type = args.length == 0 ? "kills" : args[0].toLowerCase();
        if (!type.equals("kills") && !type.equals("coins") && !type.equals("streak")) type = "kills";
        List<DataManager.PlayerStats> stats = new ArrayList<DataManager.PlayerStats>();
        for (Map.Entry<UUID, DataManager.PlayerStats> entry : plugin.getDataManager().all()) stats.add(entry.getValue());
        final String selected = type;
        Collections.sort(stats, new Comparator<DataManager.PlayerStats>() {
            @Override public int compare(DataManager.PlayerStats a, DataManager.PlayerStats b) {
                return Integer.compare(value(b, selected), value(a, selected));
            }
        });
        sender.sendMessage(plugin.getMessages().format(plugin.getMessages().get("top-header"), "%type%", type.toUpperCase()));
        int rank = 1;
        for (DataManager.PlayerStats s : stats) {
            if (rank > 10) break;
            sender.sendMessage(plugin.getMessages().format(plugin.getMessages().get("top-line"), "%rank%", String.valueOf(rank), "%player%", s.lastName, "%value%", String.valueOf(value(s, type))));
            rank++;
        }
        return true;
    }
    private int value(DataManager.PlayerStats s, String type) { return type.equals("coins") ? s.coins : type.equals("streak") ? s.bestStreak : s.kills; }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return Collections.emptyList();
        List<String> out = new ArrayList<String>();
        for (String value : Arrays.asList("kills", "coins", "streak")) if (value.startsWith(args[0].toLowerCase())) out.add(value);
        return out;
    }
}
