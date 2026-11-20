package de.skychaos.manager;

import de.skychaos.SkyChaos;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public final class TimerManager {
    private final SkyChaos plugin;
    private final BossBar bossBar;
    private BukkitTask task;
    private int remaining;
    private long elapsedSeconds;
    private boolean paused;

    public TimerManager(SkyChaos plugin) {
        this.plugin = plugin;
        bossBar = Bukkit.createBossBar("Nächstes Event in: 02:00", BarColor.BLUE, BarStyle.SOLID);
    }

    public void start() {
        stop();
        elapsedSeconds = 0;
        remaining = Math.max(10, plugin.getConfig().getInt("timer-seconds",120));
        paused = false;
        for (Player player : Bukkit.getOnlinePlayers()) bossBar.addPlayer(player);
        updateBar();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    private void tick() {
        if (!plugin.isActive() || plugin.isPaused() || paused) return;
        elapsedSeconds++;
        remaining--;
        if (remaining <= 5 && remaining > 0) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.showTitle(net.kyori.adventure.title.Title.title(
                        Component.text("In " + remaining + "s passiert was..."),
                        Component.empty()));
            }
        }
        updateBar();
        if (remaining <= 0) {
            plugin.getEventManager().triggerRandom();
            remaining = Math.max(10, plugin.getConfig().getInt("timer-seconds",120));
        }
    }

    private void updateBar() {
        int total = Math.max(10, plugin.getConfig().getInt("timer-seconds",120));
        bossBar.setProgress(Math.max(0.0, Math.min(1.0, (double)remaining/total)));
        bossBar.setColor(remaining <= 10 ? BarColor.RED : BarColor.BLUE);
        bossBar.setTitle(String.format("Nächstes Event in: %02d:%02d", remaining/60, remaining%60));
        for (Player player : Bukkit.getOnlinePlayers()) if (!bossBar.getPlayers().contains(player)) bossBar.addPlayer(player);
    }

    public void pause() {
        paused = true;
    }

    public void resume() {
        paused = false;
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        bossBar.removeAll();
        paused = false;
    }

    public long getElapsedSeconds() { return elapsedSeconds; }
    public long getElapsedMinutes() { return elapsedSeconds/60; }
    public int getRemaining() { return remaining; }
}
