package de.skychaos.manager;

import de.skychaos.SkyChaos;

import java.util.Locale;
import java.util.Set;

public final class DifficultyManager {
    private static final Set<String> LEVELS=Set.of("easy","medium","hard");
    private final SkyChaos plugin;

    public DifficultyManager(SkyChaos plugin) {
        this.plugin=plugin;
        String current=plugin.getConfig().getString("difficulty.current","medium");
        if(current==null || !LEVELS.contains(current.toLowerCase(Locale.ROOT))) {
            plugin.getConfig().set("difficulty.current","medium");
            plugin.saveConfig();
        }
    }

    public boolean set(String level) {
        if(level==null || !LEVELS.contains(level.toLowerCase(Locale.ROOT))) return false;
        plugin.getConfig().set("difficulty.current",level.toLowerCase(Locale.ROOT));
        plugin.saveConfig();
        return true;
    }

    public String get() {
        return plugin.getConfig().getString("difficulty.current","medium").toUpperCase(Locale.ROOT);
    }

    public int getInt(String key,int fallback) {
        return plugin.getConfig().getInt("difficulty.profiles."+get().toLowerCase(Locale.ROOT)+"."+key,fallback);
    }

    public double getDouble(String key,double fallback) {
        return plugin.getConfig().getDouble("difficulty.profiles."+get().toLowerCase(Locale.ROOT)+"."+key,fallback);
    }

    public int chestCount() {
        return Math.max(0,Math.min(40,getInt("chest-count",8)));
    }

    public double lootMultiplier() {
        return Math.max(0.1,Math.min(5.0,getDouble("loot-multiplier",1.0)));
    }

    public double badEventMultiplier() {
        return Math.max(0.1,Math.min(5.0,getDouble("bad-event-multiplier",1.0)));
    }

    public int eventInterval() {
        return Math.max(30,Math.min(3600,getInt("event-interval",plugin.getConfig().getInt("timer-seconds",120))));
    }
}
