package it.maurof00.xeonkitpvp.command;

import it.maurof00.xeonkitpvp.KitPvPCore;
import it.maurof00.xeonkitpvp.data.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public final class BountyCommand implements CommandExecutor, TabCompleter {
    private final KitPvPCore plugin;
    public BountyCommand(KitPvPCore plugin) { this.plugin = plugin; }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Only players can use this command."); return true; }
        Player player = (Player)sender;
        if (!plugin.getConfig().getBoolean("bounty.enabled", true)) { player.sendMessage("Bounty disabled."); return true; }
        if (args.length == 0) { player.sendMessage("/bounty <player> [amount]"); return true; }
        Player online = Bukkit.getPlayerExact(args[0]);
        OfflinePlayer target = online != null ? online : Bukkit.getOfflinePlayer(args[0]);
        if (online == null && !target.hasPlayedBefore()) { plugin.getMessages().send(player, "player-not-found"); return true; }
        if (target.getUniqueId().equals(player.getUniqueId())) { player.sendMessage("Non puoi mettere una taglia su te stesso."); return true; }
        String targetName = online != null ? online.getName() : (target.getName() == null ? args[0] : target.getName());
        DataManager.PlayerStats targetStats = plugin.getDataManager().get(target.getUniqueId(), targetName);
        if (args.length == 1) {
            plugin.getMessages().sendFormatted(player, "bounty-info", "%player%", targetName, "%amount%", String.valueOf(targetStats.bounty));
            return true;
        }
        int amount;
        try { amount = Integer.parseInt(args[1]); } catch (NumberFormatException e) { plugin.getMessages().send(player, "bounty-invalid"); return true; }
        int min = plugin.getConfig().getInt("bounty.min", 50);
        int max = plugin.getConfig().getInt("bounty.max", 100000);
        DataManager.PlayerStats own = plugin.getDataManager().get(player.getUniqueId(), player.getName());
        if (amount < min || amount > max) { plugin.getMessages().send(player, "bounty-invalid"); return true; }
        if (own.coins < amount) { plugin.getMessages().send(player, "not-enough-coins"); return true; }
        own.coins -= amount;
        targetStats.bounty += amount;
        plugin.getMessages().sendFormatted(player, "bounty-set", "%player%", targetName, "%amount%", String.valueOf(amount));
        if (online != null) plugin.getMessages().sendFormatted(online, "bounty-received", "%player%", player.getName(), "%amount%", String.valueOf(amount));
        plugin.getScoreboardManager().refresh(player);
        return true;
    }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return Collections.emptyList();
        List<String> out = new ArrayList<String>();
        for (Player p : Bukkit.getOnlinePlayers()) if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) out.add(p.getName());
        return out;
    }
}
