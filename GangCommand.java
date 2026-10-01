package it.maurof00.xeonkitpvp.command;

import it.maurof00.xeonkitpvp.KitPvPCore;
import it.maurof00.xeonkitpvp.gang.GangManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public final class GangCommand implements CommandExecutor, TabCompleter {
    private final KitPvPCore plugin;
    public GangCommand(KitPvPCore plugin) { this.plugin = plugin; }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Only players can use this command."); return true; }
        Player player = (Player)sender;
        if (!plugin.getConfig().getBoolean("gang.enabled", true)) { player.sendMessage("Gang disabled."); return true; }
        if (args.length == 0) { player.sendMessage("/gang <create|invite|join|leave|info|disband>"); return true; }
        String sub = args[0].toLowerCase();
        GangManager.Gang gang = plugin.getGangManager().getByPlayer(player.getUniqueId());
        if (sub.equals("create")) {
            if (gang != null) { plugin.getMessages().send(player, "gang-already"); return true; }
            if (args.length < 2) { player.sendMessage("/gang create <name>"); return true; }
            if (!plugin.getGangManager().create(args[1], player.getUniqueId())) { plugin.getMessages().send(player, "gang-exists"); return true; }
            plugin.getMessages().sendFormatted(player, "gang-created", "%gang%", args[1]); return true;
        }
        if (sub.equals("invite")) {
            if (gang == null || !gang.leader.equals(player.getUniqueId())) { plugin.getMessages().send(player, "gang-not-in"); return true; }
            if (args.length < 2) { player.sendMessage("/gang invite <player>"); return true; }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) { plugin.getMessages().send(player, "player-not-found"); return true; }
            if (plugin.getGangManager().getByPlayer(target.getUniqueId()) != null) { target.sendMessage("Il giocatore è già in una gang."); return true; }
            plugin.getGangManager().invite(target.getUniqueId(), gang.name);
            plugin.getMessages().sendFormatted(player, "gang-invited", "%player%", target.getName(), "%gang%", gang.name);
            plugin.getMessages().sendFormatted(target, "gang-invite-received", "%player%", player.getName(), "%gang%", gang.name);
            return true;
        }
        if (sub.equals("join")) {
            if (gang != null) { plugin.getMessages().send(player, "gang-already"); return true; }
            if (args.length < 2) { player.sendMessage("/gang join <name>"); return true; }
            String invite = plugin.getGangManager().inviteFor(player.getUniqueId());
            if (invite == null || !invite.equalsIgnoreCase(args[1])) { plugin.getMessages().send(player, "gang-no-invite"); return true; }
            if (!plugin.getGangManager().addMember(args[1], player.getUniqueId())) { plugin.getMessages().send(player, "gang-not-found"); return true; }
            plugin.getGangManager().clearInvite(player.getUniqueId());
            plugin.getMessages().sendFormatted(player, "gang-joined", "%gang%", args[1]); return true;
        }
        if (sub.equals("leave")) {
            if (gang == null) { plugin.getMessages().send(player, "gang-not-in"); return true; }
            if (gang.leader.equals(player.getUniqueId())) { plugin.getMessages().send(player, "gang-leader-cannot-leave"); return true; }
            plugin.getGangManager().removeMember(player.getUniqueId()); plugin.getMessages().send(player, "gang-left"); return true;
        }
        if (sub.equals("disband")) {
            if (gang == null || !gang.leader.equals(player.getUniqueId())) { plugin.getMessages().send(player, "gang-not-leader"); return true; }
            plugin.getGangManager().disband(player.getUniqueId()); plugin.getMessages().send(player, "gang-disbanded"); return true;
        }
        if (sub.equals("info")) {
            GangManager.Gang target = args.length > 1 ? plugin.getGangManager().getByName(args[1]) : gang;
            if (target == null) { plugin.getMessages().send(player, "gang-not-found"); return true; }
            Player leader = Bukkit.getPlayer(target.leader);
            player.sendMessage(plugin.getMessages().format(plugin.getMessages().get("gang-info-header"), "%gang%", target.name));
            player.sendMessage(plugin.getMessages().format(plugin.getMessages().get("gang-info-leader"), "%leader%", leader == null ? target.leader.toString() : leader.getName()));
            player.sendMessage(plugin.getMessages().format(plugin.getMessages().get("gang-info-members"), "%members%", String.valueOf(target.members.size())));
            return true;
        }
        player.sendMessage("/gang <create|invite|join|leave|info|disband>");
        return true;
    }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return filter(Arrays.asList("create","invite","join","leave","info","disband"), args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("invite")) {
            List<String> out = new ArrayList<String>(); for (Player p : Bukkit.getOnlinePlayers()) if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) out.add(p.getName()); return out;
        }
        return Collections.emptyList();
    }
    private List<String> filter(List<String> values, String prefix) { List<String> out = new ArrayList<String>(); for (String value : values) if (value.startsWith(prefix.toLowerCase())) out.add(value); return out; }
}
