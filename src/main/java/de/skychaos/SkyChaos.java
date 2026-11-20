package de.skychaos;

import de.skychaos.commands.ChallengeCommand;
import de.skychaos.manager.EventManager;
import de.skychaos.manager.IslandManager;
import de.skychaos.manager.TimerManager;
import de.skychaos.world.VoidGenerator;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SkyChaos extends JavaPlugin implements Listener {
    private IslandManager islandManager;
    private TimerManager timerManager;
    private EventManager eventManager;
    private YamlConfiguration eventsConfig;
    private final Map<UUID,Integer> lives = new HashMap<>();
    private boolean active;
    private boolean paused;
    private boolean buildMode;
    private int deaths;
    private long noBuildUntil;
    private long xpBoostUntil;
    private int xpBoostMultiplier=2;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResourceIfMissing("events.yml");
        eventsConfig=YamlConfiguration.loadConfiguration(new File(getDataFolder(),"events.yml"));
        String worldName=getConfig().getString("island-world","skychaos");
        World world=Bukkit.getWorld(worldName);
        if(world==null) {
            WorldCreator creator=new WorldCreator(worldName);
            creator.generator(new VoidGenerator());
            creator.generateStructures(false);
            world=creator.createWorld();
        }
        if(world==null) {
            getLogger().severe("Die Void-Welt konnte nicht geladen werden.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        world.setSpawnLocation(0,80,0);
        islandManager=new IslandManager(this,world);
        timerManager=new TimerManager(this);
        eventManager=new EventManager(this);
        ChallengeCommand challengeCommand=new ChallengeCommand(this);
        getCommand("challenge").setExecutor(challengeCommand);
        getCommand("challenge").setTabCompleter(challengeCommand);
        getServer().getPluginManager().registerEvents(this,this);
        for(Player player:Bukkit.getOnlinePlayers()) prepareIdlePlayer(player);
        getLogger().info("SkyChaos wurde aktiviert.");
    }

    @Override
    public void onDisable() {
        if(timerManager!=null) timerManager.stop();
        if(eventManager!=null) eventManager.cleanup();
    }

    private void saveResourceIfMissing(String name) {
        File file=new File(getDataFolder(),name);
        if(!file.exists()) saveResource(name,false);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player=event.getPlayer();
        if(active) {
            timerManager.addPlayer(player);
            lives.putIfAbsent(player.getUniqueId(),getConfig().getInt("lives",3));
            Bukkit.getScheduler().runTask(this,()->{
                if(lives.getOrDefault(player.getUniqueId(),0)<=0) player.setGameMode(GameMode.SPECTATOR);
                else player.setGameMode(GameMode.SURVIVAL);
                player.teleport(islandManager.getSpawnLocation());
            });
        } else {
            Bukkit.getScheduler().runTask(this,()->prepareIdlePlayer(player));
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if(!active) return;
        if(paused || System.currentTimeMillis()<noBuildUntil || !islandManager.contains(event.getBlock().getLocation())) event.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if(!active) return;
        if(paused || System.currentTimeMillis()<noBuildUntil || !islandManager.contains(event.getBlock().getLocation())) event.setCancelled(true);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if(!active) return;
        Player player=event.getEntity();
        deaths++;
        int remaining=lives.getOrDefault(player.getUniqueId(),getConfig().getInt("lives",3))-1;
        lives.put(player.getUniqueId(),remaining);
        if(remaining<=0) player.sendMessage("§cDu hast keine Leben mehr. Game Over!");
        else player.sendMessage("§cDu bist gestorben. §fVerbleibende Leben: §e"+remaining);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if(!active) return;
        Player player=event.getPlayer();
        event.setRespawnLocation(islandManager.getSpawnLocation());
        if(lives.getOrDefault(player.getUniqueId(),0)<=0) {
            Bukkit.getScheduler().runTask(this,()->player.setGameMode(GameMode.SPECTATOR));
        } else {
            Bukkit.getScheduler().runTask(this,()->{
                player.setGameMode(GameMode.SURVIVAL);
                player.teleport(islandManager.getSpawnLocation());
            });
        }
    }

    @EventHandler
    public void onGiantSlimeDeath(EntityDeathEvent event) {
        if(event.getEntity() instanceof org.bukkit.entity.Slime slime
                && "SkyChaos Giant Slime".equals(slime.getCustomName())) {
            event.getDrops().add(new org.bukkit.inventory.ItemStack(Material.DIAMOND,2));
            event.getDrops().add(new org.bukkit.inventory.ItemStack(Material.EMERALD,4));
            event.setDroppedExp(event.getDroppedExp()+25);
        }
    }

    @EventHandler
    public void onExpChange(PlayerExpChangeEvent event) {
        if(active && !paused && System.currentTimeMillis()<xpBoostUntil) event.setAmount(event.getAmount()*xpBoostMultiplier);
    }

    public void prepareIdlePlayer(Player player) {
        if(active) return;
        player.setGameMode(GameMode.SPECTATOR);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.teleport(islandManager.getWorld().getSpawnLocation());
    }

    public void startChallenge(boolean intro) {
        eventManager.cleanup();
        for(org.bukkit.entity.Entity entity:islandManager.getWorld().getEntities()) {
            if(!(entity instanceof Player) && islandManager.contains(entity.getLocation())) entity.remove();
        }
        islandManager.restore();
        active=true;
        paused=false;
        buildMode=false;
        noBuildUntil=0L;
        xpBoostUntil=0L;
        deaths=0;
        eventManager.resetStats();
        lives.clear();
        for(Player player:Bukkit.getOnlinePlayers()) {
            lives.put(player.getUniqueId(),getConfig().getInt("lives",3));
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
        if(intro) Bukkit.broadcastMessage("§b§lSkyChaos §7- Die Challenge beginnt!");
    }

    private void giveStarterKit(Player player) {
        for(String entry:getConfig().getStringList("starter-kit")) {
            String[] parts=entry.split(":");
            Material material=Material.matchMaterial(parts[0]);
            int amount=parts.length>1?parseAmount(parts[1]):1;
            if(material!=null && material.isItem()) player.getInventory().addItem(new org.bukkit.inventory.ItemStack(material,Math.max(1,amount)));
        }
    }

    private int parseAmount(String value) {
        try { return Integer.parseInt(value); }
        catch(NumberFormatException exception) { return 1; }
    }

    public void resetChallenge() { startChallenge(false); }

    public void pauseChallenge() {
        if(!active || paused) return;
        paused=true;
        timerManager.pause();
        eventManager.cleanup();
    }

    public void resumeChallenge() {
        if(!active || !paused) return;
        paused=false;
        timerManager.resume();
    }

    public void stopChallenge() {
        active=false;
        paused=false;
        buildMode=false;
        timerManager.stop();
        eventManager.cleanup();
    }

    public void enterBuildMode(Player player) {
        stopChallenge();
        buildMode=true;
        player.setGameMode(GameMode.CREATIVE);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.teleport(islandManager.getWorld().getSpawnLocation());
    }

    public boolean isActive() { return active; }
    public boolean isPaused() { return paused; }
    public boolean isBuildMode() { return buildMode; }
    public void setBuildMode(boolean value) { buildMode=value; }
    public IslandManager getIslandManager() { return islandManager; }
    public TimerManager getTimerManager() { return timerManager; }
    public EventManager getEventManager() { return eventManager; }
    public YamlConfiguration getConfigManager() { return eventsConfig; }
    public int getDeaths() { return deaths; }
    public void setNoBuildUntil(long timestamp) { noBuildUntil=timestamp; }
    public void startXpBoost(int seconds,int multiplier) { xpBoostUntil=System.currentTimeMillis()+seconds*1000L; xpBoostMultiplier=Math.max(1,multiplier); }
}
