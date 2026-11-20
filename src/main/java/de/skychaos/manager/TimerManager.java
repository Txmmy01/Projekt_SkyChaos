package de.skychaos.manager;

import de.skychaos.SkyChaos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;

public final class TimerManager {
    private final SkyChaos plugin;
    private final BossBar bossBar;
    private BukkitTask task;
    private int remaining;
    private long elapsedSeconds;
    private boolean paused;

    public TimerManager(SkyChaos plugin) {
        this.plugin=plugin;
        bossBar=Bukkit.createBossBar("Nächstes Event in: 02:00",BarColor.BLUE,BarStyle.SOLID);
        bossBar.setVisible(false);
    }

    public void start() {
        stop();
        elapsedSeconds=0L;
        remaining=eventInterval();
        paused=false;
        for(Player player:Bukkit.getOnlinePlayers()) bossBar.addPlayer(player);
        bossBar.setVisible(true);
        updateBar();
        task=Bukkit.getScheduler().runTaskTimer(plugin,this::tick,20L,20L);
    }

    private int eventInterval() {
        return Math.max(10,Math.min(3600,plugin.getConfig().getInt("timer-seconds",120)));
    }

    private void tick() {
        if(!plugin.isActive() || plugin.isPaused() || paused) return;
        elapsedSeconds++;
        if(remaining>0) remaining--;
        if(remaining<=5 && remaining>0) {
            for(Player player:Bukkit.getOnlinePlayers()) {
                if(player.getWorld().equals(plugin.getIslandManager().getWorld())) {
                    player.showTitle(Title.title(Component.text("In "+remaining+"s passiert was..."),
                            Component.empty(),Title.Times.times(Duration.ZERO,Duration.ofSeconds(1),Duration.ofMillis(200))));
                }
            }
        }
        updateBar();
        if(remaining<=0) {
            plugin.getEventManager().triggerRandom();
            remaining=eventInterval();
            updateBar();
        }
    }

    private void updateBar() {
        int total=eventInterval();
        bossBar.setProgress(Math.max(0.0,Math.min(1.0,(double)remaining/total)));
        bossBar.setColor(remaining<=10?BarColor.RED:BarColor.BLUE);
        bossBar.setTitle(String.format("Nächstes Event in: %02d:%02d",remaining/60,remaining%60));
        for(Player player:Bukkit.getOnlinePlayers()) {
            if(player.getWorld().equals(plugin.getIslandManager().getWorld()) && !bossBar.getPlayers().contains(player))
                bossBar.addPlayer(player);
        }
    }

    public void addPlayer(Player player) {
        if(task!=null && !task.isCancelled()) bossBar.addPlayer(player);
        updateBar();
    }

    public void removePlayer(Player player) {
        bossBar.removePlayer(player);
    }

    public void pause() {
        paused=true;
        bossBar.setVisible(false);
    }

    public void resume() {
        paused=false;
        bossBar.setVisible(true);
        updateBar();
    }

    public void stop() {
        if(task!=null) {
            task.cancel();
            task=null;
        }
        bossBar.removeAll();
        bossBar.setVisible(false);
        paused=false;
    }

    public long getElapsedSeconds() { return elapsedSeconds; }
    public long getElapsedMinutes() { return elapsedSeconds/60L; }
    public int getRemaining() { return remaining; }
}
