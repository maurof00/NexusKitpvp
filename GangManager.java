package it.maurof00.xeonkitpvp.gang;

import it.maurof00.xeonkitpvp.KitPvPCore;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class GangManager {
    public static final class Gang {
        public String name;
        public UUID leader;
        public final Set<UUID> members = new HashSet<UUID>();
        public Gang(String name, UUID leader) {
            this.name = name;
            this.leader = leader;
            this.members.add(leader);
        }
    }

    private final KitPvPCore plugin;
    private final File file;
    private YamlConfiguration config;
    private final Map<String, Gang> gangs = new LinkedHashMap<String, Gang>();
    private final Map<UUID, String> memberToGang = new HashMap<UUID, String>();
    private final Map<UUID, String> invites = new HashMap<UUID, String>();

    public GangManager(KitPvPCore plugin) {
        this.plugin = plugin;
        file = new File(plugin.getDataFolder(), "gangs.yml");
        reload();
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(file);
        gangs.clear();
        memberToGang.clear();
        if (!config.isConfigurationSection("gangs")) return;
        for (String key : config.getConfigurationSection("gangs").getKeys(false)) {
            String base = "gangs." + key;
            try {
                UUID leader = UUID.fromString(config.getString(base + ".leader"));
                Gang gang = new Gang(config.getString(base + ".name", key), leader);
                for (String member : config.getStringList(base + ".members")) {
                    try { gang.members.add(UUID.fromString(member)); } catch (IllegalArgumentException ignored) { }
                }
                gangs.put(key.toLowerCase(), gang);
                for (UUID uuid : gang.members) memberToGang.put(uuid, key.toLowerCase());
            } catch (Exception ignored) { }
        }
    }

    public int size() { return gangs.size(); }
    public Gang getByName(String name) { return name == null ? null : gangs.get(name.toLowerCase()); }
    public Gang getByPlayer(UUID uuid) {
        String name = memberToGang.get(uuid);
        return name == null ? null : gangs.get(name);
    }
    public boolean create(String name, UUID leader) {
        if (name == null || name.length() < 2 || name.length() > 16 || !name.matches("[A-Za-z0-9_]+")) return false;
        if (getByName(name) != null || getByPlayer(leader) != null) return false;
        Gang gang = new Gang(name, leader);
        gangs.put(name.toLowerCase(), gang);
        memberToGang.put(leader, name.toLowerCase());
        return true;
    }
    public boolean addMember(String name, UUID uuid) {
        Gang gang = getByName(name);
        if (gang == null || getByPlayer(uuid) != null) return false;
        if (gang.members.size() >= plugin.getConfig().getInt("gang.max-members", 15)) return false;
        gang.members.add(uuid);
        memberToGang.put(uuid, name.toLowerCase());
        return true;
    }
    public boolean removeMember(UUID uuid) {
        Gang gang = getByPlayer(uuid);
        if (gang == null) return false;
        gang.members.remove(uuid);
        memberToGang.remove(uuid);
        if (gang.members.isEmpty()) gangs.remove(gang.name.toLowerCase());
        return true;
    }
    public boolean disband(UUID uuid) {
        Gang gang = getByPlayer(uuid);
        if (gang == null || !gang.leader.equals(uuid)) return false;
        gangs.remove(gang.name.toLowerCase());
        for (UUID member : gang.members) memberToGang.remove(member);
        return true;
    }
    public void invite(UUID target, String gangName) { invites.put(target, gangName.toLowerCase()); }
    public String inviteFor(UUID target) { return invites.get(target); }
    public void clearInvite(UUID target) { invites.remove(target); }

    public void save() {
        config = new YamlConfiguration();
        for (Map.Entry<String, Gang> entry : gangs.entrySet()) {
            Gang gang = entry.getValue();
            String base = "gangs." + entry.getKey();
            config.set(base + ".name", gang.name);
            config.set(base + ".leader", gang.leader.toString());
            List<String> members = new ArrayList<String>();
            for (UUID uuid : gang.members) members.add(uuid.toString());
            config.set(base + ".members", members);
        }
        try { config.save(file); }
        catch (IOException e) { plugin.getLogger().warning("Could not save gangs.yml: " + e.getMessage()); }
    }

    public boolean sameGang(UUID a, UUID b) {
        Gang ga = getByPlayer(a);
        Gang gb = getByPlayer(b);
        return ga != null && gb != null && ga.name.equalsIgnoreCase(gb.name);
    }
}
