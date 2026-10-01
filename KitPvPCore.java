package it.maurof00.xeonkitpvp;

import it.maurof00.xeonkitpvp.command.BountyCommand;
import it.maurof00.xeonkitpvp.command.GangCommand;
import it.maurof00.xeonkitpvp.command.KitCommand;
import it.maurof00.xeonkitpvp.command.KitPvPCommand;
import it.maurof00.xeonkitpvp.command.SpawnCommand;
import it.maurof00.xeonkitpvp.command.StatsCommand;
import it.maurof00.xeonkitpvp.command.TopCommand;
import it.maurof00.xeonkitpvp.data.DataManager;
import it.maurof00.xeonkitpvp.gang.GangManager;
import it.maurof00.xeonkitpvp.kit.KitManager;
import it.maurof00.xeonkitpvp.listener.GameListener;
import it.maurof00.xeonkitpvp.listener.MenuListener;
import it.maurof00.xeonkitpvp.scoreboard.ScoreboardManager;
import it.maurof00.xeonkitpvp.util.MessageUtil;
import it.maurof00.xeonkitpvp.util.VersionUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class KitPvPCore extends JavaPlugin {
    private DataManager dataManager;
    private KitManager kitManager;
    private GangManager gangManager;
    private ScoreboardManager scoreboardManager;
    private MessageUtil messageUtil;
    private GameListener gameListener;
    private MenuListener menuListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResourceIfMissing("lang.yml");
        saveResourceIfMissing("kits.yml");

        this.messageUtil = new MessageUtil(this);
        this.dataManager = new DataManager(this);
        this.kitManager = new KitManager(this);
        this.gangManager = new GangManager(this);
        this.scoreboardManager = new ScoreboardManager(this);

        this.gameListener = new GameListener(this);
        this.menuListener = new MenuListener(this);
        Bukkit.getPluginManager().registerEvents(gameListener, this);
        Bukkit.getPluginManager().registerEvents(menuListener, this);

        getCommand("kit").setExecutor(new KitCommand(this));
        getCommand("kit").setTabCompleter(new KitCommand(this));
        getCommand("kitpvp").setExecutor(new KitPvPCommand(this));
        getCommand("kitpvp").setTabCompleter(new KitPvPCommand(this));
        getCommand("spawn").setExecutor(new SpawnCommand(this));
        getCommand("stats").setExecutor(new StatsCommand(this));
        getCommand("stats").setTabCompleter(new StatsCommand(this));
        getCommand("top").setExecutor(new TopCommand(this));
        getCommand("top").setTabCompleter(new TopCommand(this));
        getCommand("bounty").setExecutor(new BountyCommand(this));
        getCommand("bounty").setTabCompleter(new BountyCommand(this));
        getCommand("gang").setExecutor(new GangCommand(this));
        getCommand("gang").setTabCompleter(new GangCommand(this));

        scoreboardManager.start();
        int saveTicks = Math.max(200, getConfig().getInt("core.save-interval-seconds", 30) * 20);
        Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            @Override
            public void run() {
                dataManager.save();
                gangManager.save();
            }
        }, saveTicks, saveTicks);

        getLogger().info("XeonKitPvP enabled | version=" + getDescription().getVersion()
                + " | minecraft=" + VersionUtil.getVersion()
                + " | kits=" + kitManager.size()
                + " | gangs=" + gangManager.size());
        if (getConfig().getBoolean("core.debug", false)) {
            VersionUtil.logDebug(this);
        }
    }

    @Override
    public void onDisable() {
        if (scoreboardManager != null) scoreboardManager.stop();
        if (dataManager != null) dataManager.save();
        if (gangManager != null) gangManager.save();
        getLogger().info("XeonKitPvP disabled.");
    }

    public void reloadAll() {
        reloadConfig();
        messageUtil.reload();
        kitManager.reload();
        gangManager.reload();
        if (scoreboardManager != null) { scoreboardManager.stop(); scoreboardManager.start(); }
    }

    private void saveResourceIfMissing(String resource) {
        java.io.File file = new java.io.File(getDataFolder(), resource);
        if (!file.exists()) saveResource(resource, false);
    }

    public DataManager getDataManager() { return dataManager; }
    public KitManager getKitManager() { return kitManager; }
    public GangManager getGangManager() { return gangManager; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public MessageUtil getMessages() { return messageUtil; }
    public GameListener getGameListener() { return gameListener; }
}
