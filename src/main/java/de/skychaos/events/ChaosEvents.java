package de.skychaos.events;

import de.skychaos.SkyChaos;
import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public final class ChaosEvents extends RandomEvent {
    public ChaosEvents(SkyChaos plugin) { super(plugin); }

    @Override public String id() { return "chaos"; }
    @Override public String name() { return "Chaos Events"; }
    @Override public void execute(Player player) { }

    private World fireWorld;
    private Integer previousFireSpreadRadius;

    @Override
    public void cleanup() {
        if(fireWorld!=null && previousFireTick!=null) fireWorld.setGameRule(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER,previousFireSpreadRadius);
        fireWorld=null;
        previousFireSpreadRadius=null;
        super.cleanup();
    }

    public void fireStorm(Player player) {
        if(!enabled("fire-storm")) return;
        List<org.bukkit.block.Block> flames=new ArrayList<>();
        int fireLimit=Math.max(0,plugin.getConfigManager().getInt("events.fire-storm.max-fire-blocks",50));
        int fireCount=0;
        for(var block:plugin.getIslandManager().getIslandBlocks()) {
            if(fireCount>=fireLimit) break;
            String type=block.getType().name();
            if(type.endsWith("_LOG") || type.endsWith("_WOOD") || type.endsWith("_PLANKS")) {
                var above=block.getRelative(0,1,0);
                if(above.getType().isAir() && plugin.getIslandManager().contains(above.getLocation())) {
                    trackTemporaryBlock(above);
                    above.setType(Material.FIRE,false);
                    flames.add(above);
                    fireCount++;
                }
            }
        }
        World world=player.getWorld();
        fireWorld=world;
        previousFireSpreadRadius=world.getGameRuleValue(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER);
        world.setGameRule(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER,0);
        Bukkit.broadcastMessage("§5§lCHAOS §8» §fFeuersturm!");
        schedule(()->{
            for(var flame:flames) if(flame.getType()==Material.FIRE) flame.setType(Material.AIR,false);
            cleanup();
        },value("fire-storm","duration",10)*20L);
    }

    public void endermanInvasion(Player player) {
        if(!enabled("enderman-invasion")) return;
        spawnMobs(player,value("enderman-invasion","amount",5),EntityType.ENDERMAN);
        Bukkit.broadcastMessage("§5§lCHAOS §8» §fEnderman-Invasion!");
    }

    public void noBuild(Player player) {
        if(!enabled("no-build")) return;
        plugin.setNoBuildUntil(System.currentTimeMillis()+value("no-build","duration",20)*1000L);
        Bukkit.broadcastMessage("§5§lCHAOS §8» §fBauen ist vorübergehend deaktiviert!");
    }

    public void wither(Player player) {
        if(!enabled("wither")) return;
        plugin.applyChallengeEffect(player,new PotionEffect(PotionEffectType.WITHER,value("wither","duration",8)*20,0,false,false,true));
        Bukkit.broadcastMessage("§5§lCHAOS §8» §fWither-Fluch!");
    }

    public void mobSwarm(Player player) {
        if(!enabled("mob-swarm")) return;
        EntityType[] types={EntityType.ZOMBIE,EntityType.SKELETON,EntityType.SPIDER};
        int amount=Math.max(0,value("mob-swarm","amount",8));
        int current=plugin.getIslandManager().countIslandEntities();
        int available=Math.max(0,plugin.getConfig().getInt("events.max-entities",40)-current);
        for(int i=0;i<Math.min(amount,available);i++) {
            Location location=safeLocation();
            if(location==null) break;
            track(player.getWorld().spawnEntity(location,types[i%types.length]));
        }
        Bukkit.broadcastMessage("§5§lCHAOS §8» §fMob-Schwarm!");
    }

    private void spawnMobs(Player player,int amount,EntityType type) {
        int current=plugin.getIslandManager().countIslandEntities();
        int available=Math.max(0,plugin.getConfig().getInt("events.max-entities",40)-current);
        for(int i=0;i<Math.min(Math.max(0,amount),available);i++) {
            Location location=safeLocation();
            if(location==null) break;
            track(player.getWorld().spawnEntity(location,type));
        }
    }
}
