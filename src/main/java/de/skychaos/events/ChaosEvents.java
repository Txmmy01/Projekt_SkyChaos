package de.skychaos.events;

import de.skychaos.SkyChaos;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
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

    public void fireStorm(Player player) {
        if(!enabled("fire-storm")) return;
        List<org.bukkit.block.Block> flames=new ArrayList<>();
        for(var block:plugin.getIslandManager().getIslandBlocks()) {
            String type=block.getType().name();
            if(type.endsWith("_LOG") || type.endsWith("_WOOD") || type.endsWith("_PLANKS")) {
                var above=block.getRelative(0,1,0);
                if(above.getType().isAir() && plugin.getIslandManager().contains(above.getLocation())) {
                    trackTemporaryBlock(above);
                    above.setType(Material.FIRE,false);
                    flames.add(above);
                }
            }
        }
        World world=player.getWorld();
        Boolean oldRule=world.getGameRuleValue(GameRule.DO_FIRE_TICK);
        world.setGameRule(GameRule.DO_FIRE_TICK,false);
        Bukkit.broadcastMessage("§5§lCHAOS §8» §fFeuersturm!");
        schedule(()->{
            for(var flame:flames) if(flame.getType()==Material.FIRE) flame.setType(Material.AIR,false);
            if(oldRule!=null) world.setGameRule(GameRule.DO_FIRE_TICK,oldRule);
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
        player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER,value("wither","duration",8)*20,0,false,false,true));
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
