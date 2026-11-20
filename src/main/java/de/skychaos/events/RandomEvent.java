package de.skychaos.events;

import de.skychaos.SkyChaos;
import org.bukkit.entity.Player;

public abstract class RandomEvent {
    protected final SkyChaos plugin;

    protected RandomEvent(SkyChaos plugin) {
        this.plugin = plugin;
    }

    public abstract String id();
    public abstract String name();
    public abstract void execute(Player player);

    protected boolean enabled() {
        return plugin.getConfigManager().getBoolean("events." + id() + ".enabled", true);
    }

    protected int value(String key, int fallback) {
        return plugin.getConfigManager().getInt("events." + id() + "." + key, fallback);
    }
}
