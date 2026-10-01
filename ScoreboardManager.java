package it.maurof00.xeonkitpvp.scoreboard;

import it.maurof00.xeonkitpvp.KitPvPCore;
import it.maurof00.xeonkitpvp.data.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

public final class ScoreboardManager {
    private final KitPvPCore plugin;
    private int taskId = -1;
    public ScoreboardManager(KitPvPCore plugin) { this.plugin = plugin; }
    public void start() {
        if (!plugin.getConfig().getBoolean("scoreboard.enabled", true)) return;
        int ticks = Math.max(10, plugin.getConfig().getInt("scoreboard.refresh-ticks", 20));
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override public void run() { refreshAll(); }
        }, 1L, ticks).getTaskId();
    }
    public void stop() { if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId); }
    public void refreshAll() { for (Player player : Bukkit.getOnlinePlayers()) refresh(player); }
    public void refresh(Player player) {
        if (!plugin.getConfig().getBoolean("scoreboard.enabled", true)) return;
        if (!plugin.getConfig().getBoolean("scoreboard.all-worlds", true) && !plugin.getGameListener().isGameWorld(player.getWorld())) return;
        DataManager.PlayerStats s = plugin.getDataManager().get(player.getUniqueId(), player.getName());
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = board.registerNewObjective("xeon", "dummy");
        objective.setDisplayName(color(plugin.getConfig().getString("scoreboard.title", "&b&lKITPVP")));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        int score = 10;
        objective.getScore("§7 ").setScore(score--);
        objective.getScore("§fKills: §b" + s.kills).setScore(score--);
        objective.getScore("§fDeaths: §b" + s.deaths).setScore(score--);
        objective.getScore("§fK/D: §b" + String.format(java.util.Locale.US, "%.2f", plugin.getDataManager().kd(s))).setScore(score--);
        objective.getScore("§fCoins: §6" + s.coins).setScore(score--);
        objective.getScore("§fLevel: §e" + s.level).setScore(score--);
        objective.getScore("§fStreak: §c" + s.streak).setScore(score--);
        objective.getScore("§fBest: §d" + s.bestStreak).setScore(score--);
        objective.getScore("§8 ").setScore(score--);
        objective.getScore("§bXeonKitPvP").setScore(score--);
        player.setScoreboard(board);
    }
    private String color(String value) { return ChatColor.translateAlternateColorCodes('&', value == null ? "" : value); }
}
