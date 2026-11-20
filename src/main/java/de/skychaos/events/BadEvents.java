package de.skychaos.events;

import de.skychaos.SkyChaos;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class BadEvents extends RandomEvent {
    public BadEvents(SkyChaos plugin) { super(plugin); }

    @Override public String id() { return "bad"; }
    @Override public String name() { return "Bad Events"; }
    @Override public void execute(Player player) { }

    public void blockDisappear(Player player) {
        if(!enabled("block-disappear")) return;
        List<Block> blocks=new ArrayList<>(plugin.getIslandManager().getIslandBlocks());
        Collections.shuffle(blocks);
        int min=Math.max(1,value("block-disappear","min",5));
        int max=Math.max(min,value("block-disappear","max",10));
        int amount=Math.min(ThreadLocalRandom.current().nextInt(min,max+1),blocks.size());
        int changed=0;
        for(Block block:blocks) {
            if(changed>=amount) break;
            if(block.getType().isSolid() && !block.getType().isInteractable()) {
                block.setType(Material.AIR,false);
                changed++;
            }
        }
        announce("Block-Verschwinden");
    }

    public void mobAttack(Player player) {
        if(!enabled("mob-attack")) return;
        EntityType[] types={EntityType.ZOMBIE,EntityType.SKELETON,EntityType.SPIDER};
        int min=Math.max(1,value("mob-attack","min",3));
        int max=Math.max(min,value("mob-attack","max",5));
        spawnHostile(player,ThreadLocalRandom.current().nextInt(min,max+1),types);
        announce("Mob-Angriff");
    }

    public void creeperRain(Player player) {
        if(!enabled("creeper-rain")) return;
        spawnHostile(player,value("creeper-rain","amount",3),new EntityType[]{EntityType.CREEPER});
        announce("Creeper-Regen");
    }

    private void spawnHostile(Player player,int amount,EntityType[] types) {
        int current=plugin.getIslandManager().countIslandEntities();
        int limit=plugin.getConfig().getInt("events.max-entities",40);
        for(int i=0;i<Math.min(Math.max(0,amount),Math.max(0,limit-current));i++) {
            Location location=safeLocation();
            if(location==null) break;
            EntityType type=types[ThreadLocalRandom.current().nextInt(types.length)];
            track(player.getWorld().spawnEntity(location,type));
        }
    }

    public void lavaTrap(Player player) {
        if(!enabled("lava-trap")) return;
        Block block=player.getLocation().getBlock().getRelative(0,-1,0);
        if(!plugin.getIslandManager().contains(block.getLocation()) || !block.getType().isSolid()) return;
        trackTemporaryBlock(block);
        block.setType(Material.LAVA,false);
        schedule(()->{
            if(block.getType()==Material.LAVA) block.setType(Material.AIR,false);
            cleanup();
        },value("lava-trap","duration",5)*20L);
        announce("Lavafalle");
    }

    public void inventorySteal(Player player) {
        if(!enabled("inventory-steal")) return;
        List<Integer> occupied=new ArrayList<>();
        for(int slot=0;slot<9;slot++) {
            ItemStack item=player.getInventory().getItem(slot);
            if(item!=null && !item.getType().isAir()) occupied.add(slot);
        }
        if(!occupied.isEmpty()) {
            int slot=occupied.get(ThreadLocalRandom.current().nextInt(occupied.size()));
            ItemStack item=player.getInventory().getItem(slot);
            Item dropped=player.getWorld().dropItem(player.getLocation(),item.clone());
            track(dropped);
            player.getInventory().setItem(slot,null);
        }
        announce("Inventardiebstahl");
    }

    public void blindness(Player player) {
        if(!enabled("blindness")) return;
        int duration=value("blindness","duration",15)*20;
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,duration,0,false,false,true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,duration,0,false,false,true));
        announce("Dunkelheit");
    }

    public void islandQuake(Player player) {
        if(!enabled("island-quake")) return;
        int fallingLimit=Math.max(0,plugin.getConfigManager().getInt("events.island-quake.max-falling-blocks",40));
        int fallingCount=0;
        for(Block block:plugin.getIslandManager().getIslandBlocks()) {
            if(fallingCount>=fallingLimit) break;
            Material type=block.getType();
            if(type==Material.SAND || type==Material.RED_SAND || type==Material.GRAVEL) {
                fallingCount++;
                FallingBlock falling=block.getWorld().spawnFallingBlock(block.getLocation().add(0.5,0.0,0.5),block.getBlockData());
                falling.setDropItem(false);
                track(falling);
                block.setType(Material.AIR,false);
                schedule(()->{
                    if(falling.isValid() && !plugin.getIslandManager().contains(falling.getLocation())) falling.remove();
                },100L);
            }
        }
        announce("Inselbeben");
    }

    public void hunger(Player player) {
        if(!enabled("hunger")) return;
        player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER,value("hunger","duration",12)*20,
                Math.max(0,value("hunger","amplifier",1)),false,false,true));
        announce("Hunger");
    }

    public void poison(Player player) {
        if(!enabled("poison")) return;
        player.addPotionEffect(new PotionEffect(PotionEffectType.POISON,value("poison","duration",8)*20,
                Math.max(0,value("poison","amplifier",0)),false,false,true));
        announce("Vergiftung");
    }

    public void cobwebTrap(Player player) {
        if(!enabled("cobweb-trap")) return;
        Block block=player.getLocation().getBlock();
        if(!plugin.getIslandManager().contains(block.getLocation()) || !block.getType().isAir()) return;
        trackTemporaryBlock(block);
        block.setType(Material.COBWEB,false);
        schedule(()->{
            if(block.getType()==Material.COBWEB) block.setType(Material.AIR,false);
            cleanup();
        },value("cobweb-trap","duration",8)*20L);
        announce("Spinnennetz-Falle");
    }

    public void lightningWarning(Player player) {
        if(!enabled("lightning-warning")) return;
        Location location=safeLocation();
        if(location==null) return;
        player.getWorld().strikeLightningEffect(location);
        announce("Blitzwarnung");
    }

    private void announce(String event) {
        Bukkit.broadcastMessage("§c§lBAD EVENT §8» §f"+event);
    }
}
