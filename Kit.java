package it.maurof00.xeonkitpvp.kit;

import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.List;

public final class Kit {
    private final String id;
    private String displayName;
    private ItemStack menuItem;
    private int price;
    private String permission;
    private int cooldown;
    private final List<ItemStack> items = new ArrayList<ItemStack>();
    private final List<PotionEffect> effects = new ArrayList<PotionEffect>();
    private ItemStack helmet, chestplate, leggings, boots;
    private String abilityType = "NONE";
    private ItemStack abilityItem;
    private String abilityName;
    private int abilityCooldown;

    public Kit(String id) { this.id = id; }
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public ItemStack getMenuItem() { return menuItem; }
    public void setMenuItem(ItemStack menuItem) { this.menuItem = menuItem; }
    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }
    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }
    public int getCooldown() { return cooldown; }
    public void setCooldown(int cooldown) { this.cooldown = cooldown; }
    public List<ItemStack> getItems() { return items; }
    public List<PotionEffect> getEffects() { return effects; }
    public ItemStack getHelmet() { return helmet; }
    public void setHelmet(ItemStack helmet) { this.helmet = helmet; }
    public ItemStack getChestplate() { return chestplate; }
    public void setChestplate(ItemStack chestplate) { this.chestplate = chestplate; }
    public ItemStack getLeggings() { return leggings; }
    public void setLeggings(ItemStack leggings) { this.leggings = leggings; }
    public ItemStack getBoots() { return boots; }
    public void setBoots(ItemStack boots) { this.boots = boots; }
    public String getAbilityType() { return abilityType; }
    public void setAbilityType(String abilityType) { this.abilityType = abilityType; }
    public ItemStack getAbilityItem() { return abilityItem; }
    public void setAbilityItem(ItemStack abilityItem) { this.abilityItem = abilityItem; }
    public String getAbilityName() { return abilityName; }
    public void setAbilityName(String abilityName) { this.abilityName = abilityName; }
    public int getAbilityCooldown() { return abilityCooldown; }
    public void setAbilityCooldown(int abilityCooldown) { this.abilityCooldown = abilityCooldown; }
}
