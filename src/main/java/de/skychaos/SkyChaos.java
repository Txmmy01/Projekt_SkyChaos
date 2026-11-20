package de.skychaos;

import de.skychaos.commands.ChallengeCommand;
import de.skychaos.manager.EventManager;
import de.skychaos.manager.IslandManager;
import de.skychaos.manager.TimerManager;
import de.skychaos.world.VoidGenerator;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class SkyChaos extends JavaPlugin {
    private IslandManager islandManager;
    private TimerManager timerManager;
    private EventManager eventManager;
    private boolean active;
    private boolean paused;
    private boolean buildMode;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResourceIfMissing("events.yml");
        String worldName = getConfig().getString("island-world", "skychaos");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            WorldCreator creator = new WorldCreator(worldName);
            creator.generator(new VoidGenerator());
            creator.generateStructures(false);
            world = creator.createWorld();
        }
        if (world == null) {
            getLogger().severe("Die Void-Welt konnte nicht geladen werden.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        world.setSpawnLocation(0, 80, 0);
        islandManager = new IslandManager(this, world);
        timerManager = new TimerManager(this);
        eventManager = new EventManager(this);
        getCommand("challenge").setExecutor(new ChallengeCommand(this));
        getCommand("challenge").setTabCompleter(new ChallengeCommand(this));
        for (Player player : Bukkit.getOnlinePlayers()) {
            prepareIdlePlayer(player);
        }
        getLogger().info("SkyChaos wurde aktiviert.");
    }

    @Override
    public void onDisable() {
        if (timerManager != null) timerManager.stop();
        if (eventManager != null) eventManager.cleanup();
    }

    private void saveResourceIfMissing(String name) {
        File file = new File(getDataFolder(), name);
        if (!file.exists()) saveResource(name, false);
    }

    public void prepareIdlePlayer(Player player) {
        if (active) return;
        player.setGameMode(GameMode.SPECTATOR);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.teleport(islandManager.getWorld().getSpawnLocation());
    }

    public void startChallenge(boolean intro) {
        islandManager.restore();
        active = true;
        paused = false;
        buildMode = false;
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.getInventory().clear();
            player.setGameMode(GameMode.SURVIVAL);
            player.setAllowFlight(false);
            player.setFlying(false);
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.setFireTicks(0);
            player.teleport(islandManager.getSpawnLocation());
            giveStarterKit(player);
        }
        timerManager.start();
        if (intro) Bukkit.broadcastMessage("§b§lSkyChaos §7- Die Challenge beginnt!");
    }

    private void giveStarterKit(Player player) {
        for (String entry : getConfig().getStringList("starter-kit")) {
            String[] parts = entry.split(":");
            org.bukkit.Material material = org.bukkit.Material.matchMaterial(parts[0]);
            int amount = parts.length > 1 ? parseAmount(parts[1]) : 1;
            if (material != null && material.isItem()) {
                player.getInventory().addItem(new org.bukkit.inventory.ItemStack(material, Math.max(1, amount)));
            }
        }
    }

    private int parseAmount(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return 1;
        }
    }

    public void resetChallenge() {
        startChallenge(false);
    }

    public void pauseChallenge() {
        if (!active || paused) return;
        paused = true;
        timerManager.pause();
        eventManager.cleanup();
    }

    public void resumeChallenge() {
        if (!active || !paused) return;
        paused = false;
        timerManager.resume();
    }

    public void stopChallenge() {
        active = false;
        paused = false;
        buildMode = false;
        timerManager.stop();
        eventManager.cleanup();
    }

    public void enterBuildMode(Player player) {
        stopChallenge();
        buildMode = true;
        player.setGameMode(GameMode.CREATIVE);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.teleport(islandManager.getWorld().getSpawnLocation());
    }

    public boolean isActive() { return active; }
    public boolean isPaused() { return paused; }
    public boolean isBuildMode() { return buildMode; }
    public void setBuildMode(boolean value) { buildMode = value; }
    public IslandManager getIslandManager() { return islandManager; }
    public TimerManager getTimerManager() { return timerManager; }
    public EventManager getEventManager() { return eventManager; }
}
