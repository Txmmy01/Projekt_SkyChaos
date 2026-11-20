package de.skychaos;

import de.skychaos.commands.ChallengeCommand;
import de.skychaos.manager.EventManager;
import de.skychaos.manager.IslandManager;
import de.skychaos.manager.DifficultyManager;
import de.skychaos.manager.LootIslandManager;
import de.skychaos.manager.WorldSetupManager;
import de.skychaos.manager.TimerManager;
import de.skychaos.world.VoidGenerator;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRules;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SkyChaos extends JavaPlugin implements Listener {
    private IslandManager islandManager;
    private TimerManager timerManager;
    private EventManager eventManager;
    private DifficultyManager difficultyManager;
    private LootIslandManager lootIslandManager;
    private WorldSetupManager worldSetupManager;
    private YamlConfiguration eventsConfig;
    private final Map<UUID,Integer> lives=new HashMap<>();
    private final Map<UUID,Map<PotionEffectType,EffectSnapshot>> effectSnapshots=new HashMap<>();
    private boolean active;
    private boolean paused;
    private boolean buildMode;
    private int deaths;
    private long noBuildUntil;
    private long xpBoostUntil;
    private int xpBoostMultiplier=2;

    private record EffectSnapshot(PotionEffect previous,long capturedAt,int appliedAmplifier,long expiresAt) { }
    private Boolean previousMobGriefing;
    private Boolean previousMobSpawning;
    private NamespacedKey giantSlimeKey;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        giantSlimeKey=new NamespacedKey(this,"giant_slime");
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
        try {
            islandManager.createDefaultStarterIsland();
        } catch(java.io.IOException exception) {
            getLogger().severe("Die Standard-Starterinsel konnte nicht erstellt werden: "+exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        difficultyManager=new DifficultyManager(this);
        worldSetupManager=new WorldSetupManager(this,world);
        try {
            worldSetupManager.setup();
        } catch(RuntimeException exception) {
            getLogger().severe("Die SkyChaos-Dimensionen konnten nicht eingerichtet werden: "+exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        lootIslandManager=new LootIslandManager(this,world);
        if(lootIslandManager.size()==0 && difficultyManager.chestCount()>0) lootIslandManager.regenerate();
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
        getLogger().info("SkyChaos 2.1.0 wurde aktiviert.");
    }

    @Override
    public void onDisable() {
        active=false;
        if(timerManager!=null) timerManager.stop();
        if(eventManager!=null) eventManager.cleanup();
        clearTemporaryEffects();
        restoreWorldRules();
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
            lives.putIfAbsent(player.getUniqueId(),Math.max(1,getConfig().getInt("lives",3)));
            Bukkit.getScheduler().runTask(this,()->{
                if(!player.isOnline()) return;
                boolean spectator=lives.getOrDefault(player.getUniqueId(),0)<=0;
                player.setGameMode(spectator?GameMode.SPECTATOR:GameMode.SURVIVAL);
                player.setAllowFlight(spectator);
                if(!spectator) player.setFlying(false);
                player.teleport(resolveChallengeSpawn(player));
                timerManager.addPlayer(player);
            });
        } else {
            Bukkit.getScheduler().runTask(this,()->prepareIdlePlayer(player));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if(timerManager!=null) timerManager.removePlayer(event.getPlayer());
        clearTemporaryEffects(event.getPlayer());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if(active && (paused || System.currentTimeMillis()<noBuildUntil)) event.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if(active && (paused || System.currentTimeMillis()<noBuildUntil)) event.setCancelled(true);
    }

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if(active && (paused || System.currentTimeMillis()<noBuildUntil)) event.setCancelled(true);
    }

    @EventHandler
    public void onBucketFill(PlayerBucketFillEvent event) {
        if(active && (paused || System.currentTimeMillis()<noBuildUntil)) event.setCancelled(true);
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        if(active && islandManager.contains(event.getBlock().getLocation())
                && !islandManager.contains(event.getToBlock().getLocation())) event.setCancelled(true);
    }

    @EventHandler
    public void onBlockSpread(BlockSpreadEvent event) {
        if(active && islandManager.contains(event.getSource().getLocation())
                && !islandManager.contains(event.getBlock().getLocation())) event.setCancelled(true);
    }

    private boolean isNetherPortalInterior(org.bukkit.Location location) {
        return location.getWorld().equals(islandManager.getWorld())
                && location.getBlockX()>=19 && location.getBlockX()<=20
                && location.getBlockY()>=81 && location.getBlockY()<=83
                && location.getBlockZ()==0;
    }

    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
        if(active && event.getBlock().getWorld().equals(islandManager.getWorld())
                && !islandManager.contains(event.getBlock().getLocation())
                && !isNetherPortalInterior(event.getBlock().getLocation())) event.setCancelled(true);
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        if(active && event.getBlock().getWorld().equals(islandManager.getWorld())
                && !islandManager.contains(event.getBlock().getLocation())) event.setCancelled(true);
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
        if(active && islandManager.contains(event.getEntity().getLocation())) {
            event.blockList().clear();
            event.setYield(0.0f);
        }
    }

    @EventHandler
    public void onEntityTeleport(EntityTeleportEvent event) {
        if(active && event.getEntity() instanceof org.bukkit.entity.Enderman
                && islandManager.contains(event.getFrom()) && event.getTo()!=null
                && !islandManager.contains(event.getTo())) event.setCancelled(true);
    }

    @EventHandler
    public void onFallingBlockChange(EntityChangeBlockEvent event) {
        if(active && event.getBlock().getWorld().equals(islandManager.getWorld())
                && event.getEntity() instanceof org.bukkit.entity.FallingBlock
                && !islandManager.contains(event.getBlock().getLocation())) event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        if(!worldSetupManager.isChallengeWorld(event.getFrom().getWorld())) return;
        if(!active) {
            event.setCancelled(true);
            return;
        }
        World from=event.getFrom().getWorld();
        World destination=null;
        if(from.equals(worldSetupManager.getOverworld())) {
            if(event.getCause()==PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) destination=worldSetupManager.getNether();
            if(event.getCause()==PlayerTeleportEvent.TeleportCause.END_PORTAL) destination=worldSetupManager.getEnd();
        } else if(from.equals(worldSetupManager.getNether()) || from.equals(worldSetupManager.getEnd())) {
            destination=worldSetupManager.getOverworld();
        }
        if(destination!=null) {
            event.setTo(worldSetupManager.getSpawn(destination));
            event.setCanCreatePortal(false);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if(!active || paused || event.getTo()==null || event.getTo().getWorld()==null
                || !worldSetupManager.isChallengeWorld(event.getTo().getWorld())) return;
        if(event.getTo().getY()<event.getTo().getWorld().getMinHeight()+2) {
            event.getPlayer().teleport(worldSetupManager.getSpawn(event.getTo().getWorld()));
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if(!active || !worldSetupManager.isChallengeWorld(event.getEntity().getWorld())) return;
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
        boolean safeSpawn=true;
        org.bukkit.Location respawnLocation;
        try {
            respawnLocation=worldSetupManager.getSpawn(player.getWorld());
        } catch(IllegalStateException exception) {
            safeSpawn=false;
            respawnLocation=worldSetupManager.getOverworld().getSpawnLocation();
        }
        event.setRespawnLocation(respawnLocation);
        boolean hasSafeSpawn=safeSpawn;
        Bukkit.getScheduler().runTask(this,()->{
            if(!player.isOnline() || !active) return;
            if(lives.getOrDefault(player.getUniqueId(),0)<=0 || !hasSafeSpawn) {
                player.setGameMode(GameMode.SPECTATOR);
                player.setAllowFlight(true);
            } else {
                player.setGameMode(GameMode.SURVIVAL);
                player.setAllowFlight(false);
                player.setFlying(false);
            }
        });
    }

    @EventHandler
    public void onGiantSlimeDeath(EntityDeathEvent event) {
        if(event.getEntity() instanceof org.bukkit.entity.Slime slime
                && slime.getPersistentDataContainer().has(giantSlimeKey,PersistentDataType.BYTE)) {
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

    private org.bukkit.Location resolveChallengeSpawn(Player player) {
        try {
            return worldSetupManager.getSpawn(player.getWorld());
        } catch(IllegalStateException exception) {
            player.setGameMode(GameMode.SPECTATOR);
            player.setAllowFlight(true);
            player.sendMessage("§eDie Insel hat keinen sicheren Spawn mehr. Du wurdest in den Spectator-Modus versetzt.");
            return islandManager.getWorld().getSpawnLocation();
        }
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
        worldSetupManager.setup();
        for(Entity entity:islandManager.getWorld().getEntities()) {
            if(!(entity instanceof Player) && islandManager.contains(entity.getLocation())) entity.remove();
        }
        islandManager.restore();
        worldSetupManager.setup();
        lootIslandManager.regenerate();
        org.bukkit.Location challengeSpawn=islandManager.getSpawnLocation();
        active=true;
        paused=false;
        applyWorldRules();
        buildMode=false;
        noBuildUntil=0L;
        xpBoostUntil=0L;
        deaths=0;
        eventManager.resetStats();
        lives.clear();
        clearTemporaryEffects();
        for(Player player:Bukkit.getOnlinePlayers()) {
            lives.put(player.getUniqueId(),Math.max(1,getConfig().getInt("lives",3)));
            player.getInventory().clear();
            player.setGameMode(GameMode.SURVIVAL);
            player.setAllowFlight(false);
            player.setFlying(false);
            player.setFireTicks(0);
            var maxHealth=player.getAttribute(Attribute.MAX_HEALTH);
            if(maxHealth!=null) player.setHealth(maxHealth.getValue());
            player.setFoodLevel(20);
            player.setSaturation(20);
            player.teleport(challengeSpawn);
            giveStarterKit(player);
        }
        timerManager.start();
        if(intro) broadcastLegacy("§b§lSkyChaos §7- Die Challenge beginnt!");
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

    private void applyWorldRules() {
        World world=islandManager.getWorld();
        if(previousMobGriefing==null) previousMobGriefing=world.getGameRuleValue(GameRules.MOB_GRIEFING);
        if(previousMobSpawning==null) previousMobSpawning=world.getGameRuleValue(GameRules.SPAWN_MOBS);
        world.setGameRule(GameRules.MOB_GRIEFING,false);
        world.setGameRule(GameRules.SPAWN_MOBS,false);
    }

    private void restoreWorldRules() {
        if(islandManager==null) return;
        World world=islandManager.getWorld();
        if(previousMobGriefing!=null) world.setGameRule(GameRules.MOB_GRIEFING,previousMobGriefing);
        if(previousMobSpawning!=null) world.setGameRule(GameRules.SPAWN_MOBS,previousMobSpawning);
        previousMobGriefing=null;
        previousMobSpawning=null;
    }

    public void applyChallengeEffect(Player player,PotionEffect effect) {
        long now=System.currentTimeMillis();
        Map<PotionEffectType,EffectSnapshot> snapshots=effectSnapshots.computeIfAbsent(player.getUniqueId(),key->new HashMap<>());
        EffectSnapshot old=snapshots.get(effect.getType());
        if(!snapshots.containsKey(effect.getType())) {
            old=new EffectSnapshot(player.getPotionEffect(effect.getType()),now,effect.getAmplifier(),now+effect.getDuration()*50L);
        } else {
            old=new EffectSnapshot(old.previous(),old.capturedAt(),effect.getAmplifier(),now+effect.getDuration()*50L);
        }
        snapshots.put(effect.getType(),old);
        player.addPotionEffect(effect);
    }

    private void clearTemporaryEffects() {
        for(Player player:Bukkit.getOnlinePlayers()) clearTemporaryEffects(player);
        effectSnapshots.clear();
    }

    private void clearTemporaryEffects(Player player) {
        Map<PotionEffectType,EffectSnapshot> snapshots=effectSnapshots.remove(player.getUniqueId());
        if(snapshots==null) return;
        long now=System.currentTimeMillis();
        for(Map.Entry<PotionEffectType,EffectSnapshot> entry:snapshots.entrySet()) {
            PotionEffectType type=entry.getKey();
            EffectSnapshot snapshot=entry.getValue();
            PotionEffect current=player.getPotionEffect(type);
            boolean challengeEffect=current!=null && current.getAmplifier()==snapshot.appliedAmplifier()
                    && now<=snapshot.expiresAt()+1000L;
            if(challengeEffect) player.removePotionEffect(type);
            PotionEffect previous=snapshot.previous();
            if(previous!=null && (challengeEffect || current==null)) {
                int elapsed=(int)Math.max(0L,(now-snapshot.capturedAt())/50L);
                int remaining=previous.getDuration()-elapsed;
                if(remaining>0) {
                    player.addPotionEffect(new PotionEffect(type,remaining,previous.getAmplifier(),
                            previous.isAmbient(),previous.hasParticles(),previous.hasIcon()));
                }
            }
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
        restoreWorldRules();
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

    public static void broadcastLegacy(String message) {
        Bukkit.broadcast(LegacyComponentSerializer.legacySection().deserialize(message));
    }

    public void markGiantSlime(org.bukkit.entity.Slime slime) {
        slime.getPersistentDataContainer().set(giantSlimeKey,PersistentDataType.BYTE,(byte)1);
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
public DifficultyManager getDifficultyManager() { return difficultyManager; }
    public LootIslandManager getLootIslandManager() { return lootIslandManager; }
    public WorldSetupManager getWorldSetupManager() { return worldSetupManager; }
    public int getEventIntervalSeconds() { return difficultyManager==null?getConfig().getInt("timer-seconds",120):difficultyManager.eventInterval(); }
    public void setNoBuildUntil(long timestamp) { noBuildUntil=timestamp; }
    public void startXpBoost(int seconds,int multiplier) {
        xpBoostUntil=System.currentTimeMillis()+Math.max(1,seconds)*1000L;
        xpBoostMultiplier=Math.max(1,multiplier);
    }
}
