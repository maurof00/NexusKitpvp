package it.maurof00.xeonkitpvp.listener;

import it.maurof00.xeonkitpvp.KitPvPCore;
import it.maurof00.xeonkitpvp.command.StatsCommand;
import it.maurof00.xeonkitpvp.data.DataManager;
import it.maurof00.xeonkitpvp.kit.Kit;
import it.maurof00.xeonkitpvp.util.MessageUtil;
import it.maurof00.xeonkitpvp.util.VersionUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GameListener implements Listener {
    private static final String KIT_MENU_TITLE = ChatColor.DARK_AQUA + "Kit Selector";
    private final KitPvPCore plugin;
    private final Map<UUID, Long> combatUntil = new ConcurrentHashMap<UUID, Long>();
    private final Map<UUID, Long> protectedUntil = new HashMap<UUID, Long>();

    public GameListener(final KitPvPCore plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskTimer(plugin, new BukkitRunnable() {
            @Override public void run() {
                long now = System.currentTimeMillis();
                for (UUID uuid : new ArrayList<UUID>(combatUntil.keySet())) {
                    Long until = combatUntil.get(uuid);
                    if (until != null && until <= now) {
                        combatUntil.remove(uuid);
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null) plugin.getMessages().send(player, "combat-ended");
                    }
                }
            }
        }, 20L, 20L);
    }

    public boolean isGameWorld(World world) {
        if (world == null) return false;
        List<String> worlds = plugin.getConfig().getStringList("game.enabled-worlds");
        if (worlds.isEmpty()) return true;
        for (String name : worlds) if (world.getName().equalsIgnoreCase(name)) return true;
        return false;
    }

    @EventHandler public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        DataManager.PlayerStats stats = plugin.getDataManager().get(player.getUniqueId(), player.getName());
        stats.ownedKits.add("warrior");
        plugin.getScoreboardManager().refresh(player);
        protectedUntil.put(player.getUniqueId(), System.currentTimeMillis() + 5000L);
        if (isGameWorld(player.getWorld())) {
            final Kit selected = plugin.getKitManager().get(stats.selectedKit);
            if (selected != null) new BukkitRunnable() {
                @Override public void run() {
                    if (player.isOnline()) giveKit(player, selected);
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (isCombat(uuid)) {
            Player player = event.getPlayer();
            broadcast(plugin.getMessages().format(plugin.getMessages().raw("quit-combat"), "%player%", player.getName()));
            DataManager.PlayerStats stats = plugin.getDataManager().get(uuid, player.getName());
            stats.deaths++;
            stats.streak = 0;
        }
        combatUntil.remove(uuid);
        protectedUntil.remove(uuid);
        plugin.getKitManager().clearCooldowns(uuid);
        plugin.getDataManager().save();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player victim = (Player) event.getEntity();
        Player attacker = getAttacker(event.getDamager());
        if (attacker == null) return;
        if (!isGameWorld(victim.getWorld()) || !plugin.getConfig().getBoolean("game.allow-pvp", true)) { event.setCancelled(true); return; }
        if (!isGameWorld(attacker.getWorld())) { event.setCancelled(true); return; }
        if (isSpawnProtected(victim) && !attacker.hasPermission("kitpvp.bypass")) { event.setCancelled(true); return; }
        if (isSpawnProtected(attacker) && !attacker.hasPermission("kitpvp.bypass")) { event.setCancelled(true); return; }
        if (!plugin.getConfig().getBoolean("gang.friendly-fire", false) && plugin.getGangManager().sameGang(victim.getUniqueId(), attacker.getUniqueId())) { event.setCancelled(true); return; }
        tagCombat(victim, attacker);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamageOther(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (!isGameWorld(player.getWorld())) return;
        if (isSpawnProtected(player) && event.getCause() != EntityDamageEvent.DamageCause.VOID && !player.hasPermission("kitpvp.bypass")) {
            event.setCancelled(true);
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID && plugin.getConfig().getBoolean("game.prevent-void-death", true)) {
            event.setCancelled(true);
            teleportSpawn(player, true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        if (!isGameWorld(victim.getWorld())) return;
        event.setKeepInventory(true);
        if (plugin.getConfig().getBoolean("game.no-item-drops", true)) event.getDrops().clear();
        event.setDroppedExp(0);
        event.setDeathMessage(null);
        DataManager.PlayerStats victimStats = plugin.getDataManager().get(victim.getUniqueId(), victim.getName());
        victimStats.deaths++;
        victimStats.streak = 0;
        boolean victimLevelUp = plugin.getDataManager().addXp(victim.getUniqueId(), victim.getName(), plugin.getConfig().getInt("economy.xp-per-death", 5));
        if (victimLevelUp) victim.sendMessage(ChatColor.GREEN + "Level up! Ora sei livello " + victimStats.level + ".");

        Player killer = victim.getKiller();
        if (killer != null && !killer.equals(victim)) handleKill(killer, victim);
        else plugin.getMessages().send(victim, "death");

        combatUntil.remove(victim.getUniqueId());
        protectedUntil.put(victim.getUniqueId(), System.currentTimeMillis() + 3000L);
    }

    @EventHandler public void onRespawn(final PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        if (!isGameWorld(player.getWorld())) return;
        Location spawn = getSpawn();
        if (spawn != null) event.setRespawnLocation(spawn);
        new BukkitRunnable() {
            @Override public void run() {
                if (!player.isOnline()) return;
                DataManager.PlayerStats stats = plugin.getDataManager().get(player.getUniqueId(), player.getName());
                Kit kit = plugin.getKitManager().get(stats.selectedKit);
                if (kit != null) giveKit(player, kit);
                protectedUntil.put(player.getUniqueId(), System.currentTimeMillis() + 3000L);
                plugin.getScoreboardManager().refresh(player);
            }
        }.runTaskLater(plugin, 1L);
    }

    private void handleKill(Player killer, Player victim) {
        DataManager.PlayerStats killerStats = plugin.getDataManager().get(killer.getUniqueId(), killer.getName());
        DataManager.PlayerStats victimStats = plugin.getDataManager().get(victim.getUniqueId(), victim.getName());
        killerStats.kills++;
        killerStats.streak++;
        killerStats.bestStreak = Math.max(killerStats.bestStreak, killerStats.streak);
        killerStats.coins += Math.max(0, plugin.getConfig().getInt("economy.kill-coins", 25));
        boolean levelUp = plugin.getDataManager().addXp(killer.getUniqueId(), killer.getName(), plugin.getConfig().getInt("economy.xp-per-kill", 25));
        if (levelUp) killer.sendMessage(ChatColor.GREEN + "Level up! Ora sei livello " + killerStats.level + ".");

        int bounty = victimStats.bounty;
        if (bounty > 0) {
            int reward = (int)Math.floor(bounty * (plugin.getConfig().getInt("bounty.killer-reward-percent", 100) / 100.0D));
            killerStats.coins += reward;
            victimStats.bounty = 0;
            plugin.getMessages().sendFormatted(killer, "killed-bounty", "%victim%", victim.getName(), "%amount%", String.valueOf(reward));
        }
        plugin.getMessages().sendFormatted(killer, "killed", "%victim%", victim.getName(), "%killer%", killer.getName());
        if (plugin.getConfig().getBoolean("killstreak.enabled", true)) {
            if (killerStats.streak >= 3 && plugin.getConfig().getBoolean("killstreak.announcements", true))
                broadcast(plugin.getMessages().format(plugin.getMessages().raw("streak"), "%player%", killer.getName(), "%streak%", String.valueOf(killerStats.streak)));
            killerStats.coins += Math.max(0, plugin.getConfig().getInt("economy.streak-coins", 10));
            giveStreakReward(killer, killerStats.streak);
        }
        plugin.getScoreboardManager().refresh(killer);
    }

    private void giveStreakReward(Player player, int streak) {
        String path = "killstreak.rewards." + streak;
        if (!plugin.getConfig().isConfigurationSection(path)) return;
        DataManager.PlayerStats stats = plugin.getDataManager().get(player.getUniqueId(), player.getName());
        stats.coins += Math.max(0, plugin.getConfig().getInt(path + ".coins", 0));
        for (String command : plugin.getConfig().getStringList(path + ".commands"))
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isGameWorld(event.getPlayer().getWorld()) && plugin.getConfig().getBoolean("game.block-build", false) && !event.getPlayer().hasPermission("kitpvp.bypass")) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (isGameWorld(event.getPlayer().getWorld()) && plugin.getConfig().getBoolean("game.block-break", false) && !event.getPlayer().hasPermission("kitpvp.bypass")) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFood(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player && isGameWorld(((Player) event.getEntity()).getWorld()) && plugin.getConfig().getBoolean("game.no-hunger", true)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!isGameWorld(player.getWorld())) return;
        ItemStack item = event.getItem();
        if (item == null) return;
        if (isNamedItem(item, plugin.getConfig().getString("menu.items.kits.name", "&bSelettore Kit"))) {
            openKitMenu(player);
            event.setCancelled(true);
            return;
        }
        if (isNamedItem(item, plugin.getConfig().getString("menu.items.stats.name", "&eProfilo"))) {
            new StatsCommand(plugin).onCommand(player, null, "stats", new String[0]);
            event.setCancelled(true);
            return;
        }
        DataManager.PlayerStats stats = plugin.getDataManager().get(player.getUniqueId(), player.getName());
        Kit kit = plugin.getKitManager().get(stats.selectedKit);
        if (kit != null && kit.getAbilityItem() != null && sameType(item, kit.getAbilityItem()) && event.getAction().name().startsWith("RIGHT_CLICK")) {
            event.setCancelled(true);
            useAbility(player, kit);
            return;
        }
        if (event.getAction().name().startsWith("RIGHT_CLICK") && sameMaterial(item, VersionUtil.material("MUSHROOM_SOUP"))) {
            int heal = plugin.getConfig().getInt("game.soup-heal-half-hearts", 6);
            player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + heal));
            item.setType(VersionUtil.material("BOWL"));
            VersionUtil.updateInventory(player);
        }
    }

    private void useAbility(Player player, Kit kit) {
        long left = plugin.getKitManager().getAbilityCooldownLeft(player.getUniqueId());
        if (left > 0) { plugin.getMessages().sendFormatted(player, "kit-cooldown", "%seconds%", String.valueOf(left)); return; }
        String type = kit.getAbilityType();
        boolean used = false;
        if ("HEAL".equals(type)) {
            player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 6.0D)); used = true;
        } else if ("DASH".equals(type)) {
            Vector velocity = player.getLocation().getDirection().normalize().multiply(1.5D);
            velocity.setY(0.45D); player.setVelocity(velocity); used = true;
        } else if ("FIREBALL".equals(type)) {
            player.launchProjectile(SmallFireball.class); used = true;
        } else if ("LIGHTNING".equals(type)) {
            Block target = player.getTargetBlock(null, 30);
            player.getWorld().strikeLightning(target.getLocation()); used = true;
        } else if ("BLINK".equals(type)) {
            Block target = player.getTargetBlock(null, 12);
            Location to = target.getLocation().add(0.5, 1.0, 0.5);
            player.teleport(to); used = true;
        } else if ("GRAPPLE".equals(type)) {
            Location target = player.getTargetBlock(null, 25).getLocation().add(0.5, 0.5, 0.5);
            Vector delta = target.toVector().subtract(player.getLocation().toVector());
            double distance = delta.length();
            if (distance > 2.0D && distance < 30.0D) {
                player.setVelocity(delta.normalize().multiply(Math.min(2.0D, 0.6D + distance / 12.0D)));
                used = true;
            }
        }
        if (used) {
            plugin.getKitManager().startAbilityCooldown(player.getUniqueId(), kit.getAbilityCooldown());
            VersionUtil.playSound(player, "CLICK", "UI_BUTTON_CLICK");
        }
    }

    public void openKitMenu(Player player) {
        int configuredSize = plugin.getConfig().getInt("menu.size", 54);
        int size = Math.max(9, Math.min(54, (configuredSize / 9) * 9));
        if (size == 0) size = 9;
        org.bukkit.inventory.Inventory inventory = Bukkit.createInventory(null, size, KIT_MENU_TITLE);
        ItemStack filler = parseMaterialItem(plugin.getConfig().getString("menu.filler", "STAINED_GLASS_PANE:15"), ChatColor.DARK_GRAY + " ");
        for (int i = 0; i < inventory.getSize(); i++) inventory.setItem(i, filler);
        int slot = 10;
        for (Kit kit : plugin.getKitManager().getKits()) {
            if (slot >= inventory.getSize()) break;
            boolean unlocked = plugin.getKitManager().hasKit(player.getUniqueId(), player.getName(), kit);
            if (!unlocked && !plugin.getConfig().getBoolean("menu.show-locked-kits", true)) continue;
            ItemStack item = kit.getMenuItem().clone();
            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;
            List<String> lore = new ArrayList<String>();
            lore.add(ChatColor.GRAY + "Prezzo: " + ChatColor.GOLD + kit.getPrice() + " coin");
            lore.add(unlocked ? ChatColor.GREEN + "Sbloccato" : ChatColor.RED + "Clicca per acquistare");
            lore.add(ChatColor.DARK_GRAY + "Cooldown: " + kit.getCooldown() + "s");
            if (!"NONE".equals(kit.getAbilityType())) lore.add(ChatColor.AQUA + "Ability: " + kit.getAbilityName());
            meta.setDisplayName(kit.getDisplayName());
            meta.setLore(lore);
            item.setItemMeta(meta);
            inventory.setItem(slot, item);
            slot++;
            if (slot % 9 == 8) slot += 2;
        }
        player.openInventory(inventory);
    }

    private ItemStack parseMaterialItem(String spec, String name) {
        String[] parts = spec.split(":");
        short data = 0;
        if (parts.length > 1) try { data = Short.parseShort(parts[1]); } catch (NumberFormatException ignored) {}
        ItemStack item = new ItemStack(VersionUtil.material(parts[0]), 1);
        VersionUtil.applyDurability(item, data);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName(name); item.setItemMeta(meta); }
        return item;
    }

    private boolean isNamedItem(ItemStack item, String configured) {
        return item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                && ChatColor.stripColor(item.getItemMeta().getDisplayName()).equals(ChatColor.stripColor(MessageUtil.color(configured)));
    }
    private boolean sameType(ItemStack a, ItemStack b) { return a.getType().equals(b.getType()); }
    private boolean sameMaterial(ItemStack a, Material b) { return a.getType().equals(b); }

    public void giveKit(Player player, Kit kit) {
        PlayerInventory inv = player.getInventory();
        inv.clear();
        inv.setArmorContents(new ItemStack[4]);
        for (PotionEffect effect : new ArrayList<PotionEffect>(player.getActivePotionEffects())) player.removePotionEffect(effect.getType());
        for (ItemStack item : kit.getItems()) if (item != null && item.getType() != Material.AIR) inv.addItem(item.clone());
        inv.setHelmet(nonAir(kit.getHelmet()));
        inv.setChestplate(nonAir(kit.getChestplate()));
        inv.setLeggings(nonAir(kit.getLeggings()));
        inv.setBoots(nonAir(kit.getBoots()));
        for (PotionEffect effect : kit.getEffects()) player.addPotionEffect(effect, true);
        if (kit.getAbilityItem() != null && kit.getAbilityItem().getType() != Material.AIR) inv.addItem(kit.getAbilityItem().clone());
        VersionUtil.updateInventory(player);
    }

    private ItemStack nonAir(ItemStack item) { return item == null || item.getType() == Material.AIR ? null : item.clone(); }

    public void teleportSpawn(Player player) {
        teleportSpawn(player, false);
    }

    private void teleportSpawn(Player player, boolean bypassCombat) {
        if (!bypassCombat && isCombat(player.getUniqueId()) && !player.hasPermission("kitpvp.bypass")) {
            plugin.getMessages().send(player, "spawn-in-combat");
            return;
        }
        Location spawn = getSpawn();
        if (spawn == null) { plugin.getMessages().send(player, "no-spawn"); return; }
        player.teleport(spawn);
        combatUntil.remove(player.getUniqueId());
        protectedUntil.put(player.getUniqueId(), System.currentTimeMillis() + 3000L);
        plugin.getScoreboardManager().refresh(player);
        plugin.getMessages().send(player, "spawned");
    }

    private Location getSpawn() {
        String worldName = plugin.getConfig().getString("spawn.world", "kitpvp");
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world,
                plugin.getConfig().getDouble("spawn.x", 0.5D),
                plugin.getConfig().getDouble("spawn.y", 100.0D),
                plugin.getConfig().getDouble("spawn.z", 0.5D),
                (float)plugin.getConfig().getDouble("spawn.yaw", 0.0D),
                (float)plugin.getConfig().getDouble("spawn.pitch", 0.0D));
    }

    private boolean isSpawnProtected(Player player) {
        Long until = protectedUntil.get(player.getUniqueId());
        if (until != null && until > System.currentTimeMillis()) return true;
        if (until != null) protectedUntil.remove(player.getUniqueId());
        Location spawn = getSpawn();
        if (spawn == null || !VersionUtil.sameWorld(player.getLocation(), spawn)) return false;
        return player.getLocation().distance(spawn) <= plugin.getConfig().getDouble("game.spawn-protection-radius", 10.0D);
    }

    private Player getAttacker(Entity damager) {
        if (damager instanceof Player) return (Player)damager;
        if (damager instanceof Projectile && ((Projectile)damager).getShooter() instanceof Player) return (Player)((Projectile)damager).getShooter();
        if (damager instanceof Tameable && ((Tameable)damager).getOwner() instanceof Player) return (Player)((Tameable)damager).getOwner();
        return null;
    }

    private void tagCombat(Player a, Player b) {
        int seconds = plugin.getConfig().getInt("game.combat-tag-seconds", 15);
        long until = System.currentTimeMillis() + seconds * 1000L;
        combatUntil.put(a.getUniqueId(), until);
        combatUntil.put(b.getUniqueId(), until);
        plugin.getMessages().sendFormatted(a, "combat-tagged", "%seconds%", String.valueOf(seconds));
        plugin.getMessages().sendFormatted(b, "combat-tagged", "%seconds%", String.valueOf(seconds));
    }

    public boolean isCombat(UUID uuid) {
        Long until = combatUntil.get(uuid);
        if (until == null) return false;
        if (until <= System.currentTimeMillis()) { combatUntil.remove(uuid); return false; }
        return true;
    }

    private void broadcast(String message) { Bukkit.broadcastMessage(MessageUtil.color(message)); }
}
