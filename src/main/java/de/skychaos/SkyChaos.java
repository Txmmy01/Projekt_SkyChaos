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
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
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
    private final Map<UUID,Integer> lives=new HashMap<>();
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
        loadEventsConfig();
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
        ChallengeCommand command=new ChallengeCommand(this);
        if(getCommand("challenge")==null) {
            getLogger().severe("Der Command /challenge fehlt in plugin.yml.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getCommand("challenge").setExecutor(command);
        getCommand("challenge").setTabCompleter(command);
        getServer().getPluginManager().registerEvents(this,this);
        for(Player player:Bukkit.getOnlinePlayers()) prepareIdlePlayer(player);
        getLogger().info("SkyChaos 2.0.0 wurde aktiviert.");
    }

    @Override
    public void onDisable() {
        active=false;
        if(timerManager!=null) timerManager.stop();
        if(eventManager!=null) eventManager.cleanup();
        clearTemporaryEffects();
    }

    private void loadEventsConfig() {
        eventsConfig=YamlConfiguration.loadConfiguration(new File(getDataFolder(),"events.yml"));
        try(java.io.InputStreamReader reader=new java.io.InputStreamReader(getResource("events.yml"),java.nio.charset.StandardCharsets.UTF_8)) {
            eventsConfig.setDefaults(YamlConfiguration.loadConfiguration(reader));
        } catch(java.io.IOException exception) {
            getLogger().severe("events.yml-Standardwerte konnten nicht geladen werden: "+exception.getMessage());
        }
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
                if(!player.isOnline()) return;
                player.setGameMode(lives.getOrDefault(player.getUniqueId(),0)<=0?GameMode.SPECTATOR:GameMode.SURVIVAL);
                player.teleport(islandManager.getSpawnLocation());
            });
        } else {
            Bukkit.getScheduler().runTask(this,()->prepareIdlePlayer(player));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if(timerManager!=null) timerManager.removePlayer(event.getPlayer());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if(active && (paused || System.currentTimeMillis()<noBuildUntil
                || !islandManager.contains(event.getBlock().getLocation()))) event.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if(active && (paused || System.currentTimeMillis()<noBuildUntil
                || !islandManager.contains(event.getBlockPlaced().getLocation()))) event.setCancelled(true);
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        if(active && islandManager.contains(event.getBlock().getLocation())
                && !islandManager.contains(event.getToBlock().getLocation())) event.setCancelled(true);
    }

    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
        if(active && !islandManager.contains(event.getBlock().getLocation())) event.setCancelled(true);
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        if(active && !islandManager.contains(event.getBlock().getLocation())) event.setCancelled(true);
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        if(active && islandManager.contains(event.getBlock().getLocation())) {
            event.blockList().clear();
            event.setYield(0.0f);
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if(active && islandManager.contains(event.getLocation())) {
            event.blockList().clear();
            event.setYield(0.0f);
        }
    }

    @EventHandler
    public void onFallingBlockChange(EntityChangeBlockEvent event) {
        if(active && event.getEntity() instanceof org.bukkit.entity.FallingBlock
                && !islandManager.contains(event.getBlock().getLocation())) event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if(!active || paused || event.getTo()==null
                || !event.getTo().getWorld().equals(islandManager.getWorld())) return;
        if(event.getTo().getY()<islandManager.getWorld().getMinHeight()+2) {
            event.getPlayer().teleport(islandManager.getSpawnLocation());
            return;
        }
        if(event.getTo().getBlockX()!=event.getFrom().getBlockX()
                || event.getTo().getBlockZ()!=event.getFrom().getBlockZ()) {
            if(!islandManager.contains(event.getTo())) event.setTo(event.getFrom());
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if(!active || !event.getEntity().getWorld().equals(islandManager.getWorld())) return;
        Player player=event.getEntity();
        deaths++;
        int remaining=lives.getOrDefault(player.getUniqueId(),getConfig().getInt("lives",3))-1;
        lives.put(player.getUniqueId(),remaining);
        player.sendMessage(remaining<=0?"§cDu hast keine Leben mehr. Game Over!":
                "§cDu bist gestorben. §fVerbleibende Leben: §e"+remaining);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if(!active) return;
        Player player=event.getPlayer();
        event.setRespawnLocation(islandManager.getSpawnLocation());
        Bukkit.getScheduler().runTask(this,()->{
            if(!player.isOnline() || !active) return;
            if(lives.getOrDefault(player.getUniqueId(),0)<=0) player.setGameMode(GameMode.SPECTATOR);
            else player.setGameMode(GameMode.SURVIVAL);
        });
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
        if(active && !paused && System.currentTimeMillis()<xpBoostUntil)
            event.setAmount(event.getAmount()*xpBoostMultiplier);
    }

    public void prepareIdlePlayer(Player player) {
        if(active || !player.isOnline()) return;
        player.setGameMode(GameMode.SPECTATOR);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.teleport(islandManager.getWorld().getSpawnLocation());
    }

    public void startChallenge(boolean intro) {
        if(!islandManager.exists()) throw new IllegalStateException("Es wurde noch keine Starter-Insel gespeichert.");
        eventManager.cleanup();
        for(Entity entity:islandManager.getWorld().getEntities()) {
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
        clearTemporaryEffects();
        for(Player player:Bukkit.getOnlinePlayers()) {
            lives.put(player.getUniqueId(),getConfig().getInt("lives",3));
            player.getInventory().clear();
            player.setGameMode(GameMode.SURVIVAL);
            player.setAllowFlight(false);
            player.setFlying(false);
            player.setFireTicks(0);
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.setSaturation(20);
            player.teleport(islandManager.getSpawnLocation());
            giveStarterKit(player);
        }
        timerManager.start();
        if(intro) Bukkit.broadcastMessage("§b§lSkyChaos §7- Die Challenge beginnt!");
    }

    private void giveStarterKit(Player player) {
        for(String entry:getConfig().getStringList("starter-kit")) {
            String[] parts=entry.split(":",2);
            Material material=Material.matchMaterial(parts[0].trim());
            int amount=parts.length>1?parseAmount(parts[1]):1;
            if(material==null || !material.isItem() || amount<1) {
                getLogger().warning("Ungültiger Starter-Kit-Eintrag: "+entry);
                continue;
            }
            int remaining=amount;
            while(remaining>0) {
                int stack=Math.min(remaining,material.getMaxStackSize());
                player.getInventory().addItem(new org.bukkit.inventory.ItemStack(material,stack));
                remaining-=stack;
            }
        }
    }

    private int parseAmount(String value) {
        try { return Integer.parseInt(value); }
        catch(NumberFormatException exception) { return -1; }
    }

    private void clearTemporaryEffects() {
        for(Player player:Bukkit.getOnlinePlayers()) {
            for(var type:new org.bukkit.potion.PotionEffectType[]{
                    org.bukkit.potion.PotionEffectType.JUMP_BOOST,
                    org.bukkit.potion.PotionEffectType.SLOW_FALLING,
                    org.bukkit.potion.PotionEffectType.BLINDNESS,
                    org.bukkit.potion.PotionEffectType.SLOWNESS,
                    org.bukkit.potion.PotionEffectType.HUNGER,
                    org.bukkit.potion.PotionEffectType.POISON,
                    org.bukkit.potion.PotionEffectType.WITHER,
                    org.bukkit.potion.PotionEffectType.REGENERATION,
                    org.bukkit.potion.PotionEffectType.ABSORPTION}) player.removePotionEffect(type);
        }
    }

    public void reloadPluginConfigs() {
        reloadConfig();
        loadEventsConfig();
    }

    public void resetChallenge() {
        startChallenge(false);
    }

    public void pauseChallenge() {
        if(!active || paused) return;
        paused=true;
        noBuildUntil=0L;
        xpBoostUntil=0L;
        timerManager.pause();
        eventManager.cleanup();
        clearTemporaryEffects();
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
        noBuildUntil=0L;
        xpBoostUntil=0L;
        timerManager.stop();
        eventManager.cleanup();
        clearTemporaryEffects();
        for(Player player:Bukkit.getOnlinePlayers()) prepareIdlePlayer(player);
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
    public void startXpBoost(int seconds,int multiplier) {
        xpBoostUntil=System.currentTimeMillis()+Math.max(1,seconds)*1000L;
        xpBoostMultiplier=Math.max(1,multiplier);
    }
}
