package it.maurof00.xeonkitpvp.command;

import it.maurof00.xeonkitpvp.KitPvPCore;
import it.maurof00.xeonkitpvp.data.DataManager;
import it.maurof00.xeonkitpvp.kit.Kit;
import it.maurof00.xeonkitpvp.util.VersionUtil;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public final class KitCommand implements CommandExecutor, TabCompleter {
    private final KitPvPCore plugin;
    public KitCommand(KitPvPCore plugin) { this.plugin = plugin; }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Only players can use this command."); return true; }
        Player player = (Player) sender;
        if (args.length == 0) { plugin.getGameListener().openKitMenu(player); return true; }
        Kit kit = plugin.getKitManager().get(args[0]);
        if (kit == null) { plugin.getMessages().sendFormatted(player, "kit-not-found", "%kit%", args[0]); return true; }
        select(player, kit);
        return true;
    }
    public boolean select(Player player, Kit kit) {
        if (!plugin.getGameListener().isGameWorld(player.getWorld()) && !player.hasPermission("kitpvp.bypass")) {
            plugin.getMessages().send(player, "wrong-world");
            return false;
        }
        if (plugin.getGameListener().isCombat(player.getUniqueId()) && !player.hasPermission("kitpvp.bypass")) {
            plugin.getMessages().send(player, "kit-in-combat");
            return false;
        }
        DataManager.PlayerStats s = plugin.getDataManager().get(player.getUniqueId(), player.getName());
        boolean unlocked = plugin.getKitManager().hasKit(player.getUniqueId(), player.getName(), kit);
        if (!unlocked) {
            if (kit.getPrice() <= 0) { plugin.getMessages().sendFormatted(player, "kit-locked", "%kit%", kit.getId()); return false; }
            if (s.coins < kit.getPrice()) { plugin.getMessages().send(player, "not-enough-coins"); return false; }
            s.coins -= kit.getPrice();
            s.ownedKits.add(kit.getId());
            plugin.getMessages().sendFormatted(player, "kit-purchased", "%kit%", kit.getId(), "%price%", String.valueOf(kit.getPrice()));
        }
        long left = plugin.getKitManager().getCooldownLeft(player.getUniqueId(), kit.getId());
        if (left > 0) { plugin.getMessages().sendFormatted(player, "kit-cooldown", "%seconds%", String.valueOf(left)); return false; }
        plugin.getGameListener().giveKit(player, kit);
        s.selectedKit = kit.getId();
        plugin.getKitManager().startCooldown(player.getUniqueId(), kit.getId(), kit.getCooldown());
        plugin.getMessages().sendFormatted(player, "kit-selected", "%kit%", kit.getDisplayName());
        plugin.getScoreboardManager().refresh(player);
        VersionUtil.playSound(player, "ORB_PICKUP", "ENTITY_ITEM_PICKUP");
        return true;
    }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return Collections.emptyList();
        List<String> out = new ArrayList<String>();
        for (Kit kit : plugin.getKitManager().getKits()) if (kit.getId().startsWith(args[0].toLowerCase())) out.add(kit.getId());
        return out;
    }
}
