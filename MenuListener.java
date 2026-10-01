package it.maurof00.xeonkitpvp.listener;

import it.maurof00.xeonkitpvp.KitPvPCore;
import it.maurof00.xeonkitpvp.command.KitCommand;
import it.maurof00.xeonkitpvp.kit.Kit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

public final class MenuListener implements Listener {
    private static final String TITLE = ChatColor.DARK_AQUA + "Kit Selector";
    private final KitPvPCore plugin;
    public MenuListener(KitPvPCore plugin) { this.plugin = plugin; }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!TITLE.equals(event.getView().getTitle())) return;
        event.setCancelled(true);
        if (event.getClickedInventory() == null || event.getSlotType() == InventoryType.SlotType.OUTSIDE) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) return;
        Player player = (Player)event.getWhoClicked();
        String name = clicked.getItemMeta().getDisplayName();
        for (Kit kit : plugin.getKitManager().getKits()) {
            if (name.equals(kit.getDisplayName())) {
                new KitCommand(plugin).onCommand(player, null, "kit", new String[]{kit.getId()});
                if (plugin.getConfig().getBoolean("menu.close-on-select", true)) player.closeInventory();
                return;
            }
        }
    }
}
