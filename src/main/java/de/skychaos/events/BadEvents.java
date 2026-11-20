package de.skychaos.events;

import de.skychaos.SkyChaos;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Bukkit;
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
        if (!enabled("block-disappear")) return;
        List<Block> blocks=new ArrayList<>(plugin.getIslandManager().getIslandBlocks());
        Collections.shuffle(blocks);
        int amount=Math.min(blocks.size(),ThreadLocalRandom.current().nextInt(value("block-disappear","min",5),value("block-disappear","max",10)+1));
        for(int i=0;i<amount;i++) if(blocks.get(i).getType().isSolid()) blocks.get(i).setType(Material.AIR,false);
        announce("Block-Verschwinden");
    }

    public void mobAttack(Player player) {
        if (!enabled("mob-attack")) return;
        EntityType[] types={EntityType.ZOMBIE,EntityType.SKELETON,EntityType.SPIDER};
        int amount=ThreadLocalRandom.current().nextInt(value("mob-attack","min",3),value("mob-attack","max",5)+1);
        for(int i=0;i<amount;i++) {
            Location nearby=player.getLocation().clone().add(ThreadLocalRandom.current().nextInt(-3,4),0,
                    ThreadLocalRandom.current().nextInt(-3,4));
            Location location=nearby;
            if(!plugin.getIslandManager().contains(nearby)
                    || !nearby.clone().subtract(0,1,0).getBlock().getType().isSolid()) {
                location=plugin.getIslandManager().randomSurfaceLocation();
            }
            player.getWorld().spawnEntity(location,types[ThreadLocalRandom.current().nextInt(types.length)]);
        }
        announce("Mob-Angriff");
    }

    public void creeperRain(Player player) {
        if (!enabled("creeper-rain")) return;
        for(int i=0;i<value("creeper-rain","amount",3);i++) {
            Location location=plugin.getIslandManager().randomSurfaceLocation().add(0,8,0);
            player.getWorld().spawnEntity(location,EntityType.CREEPER);
        }
        announce("Creeper-Regen");
    }

    public void lavaTrap(Player player) {
        if (!enabled("lava-trap")) return;
        Block block=player.getLocation().subtract(0,1,0).getBlock();
        if(!plugin.getIslandManager().contains(block.getLocation()) || !block.getType().isSolid()) return;
        org.bukkit.block.data.BlockData previous=block.getBlockData().clone();
        block.setType(Material.LAVA,false);
        Bukkit.getScheduler().runTaskLater(plugin,()->{
            if(block.getType()==Material.LAVA) block.setBlockData(previous,false);
        },value("lava-trap","duration",5)*20L);
        announce("Lavafalle");
    }

    public void inventorySteal(Player player) {
        if (!enabled("inventory-steal")) return;
        int slot=ThreadLocalRandom.current().nextInt(0,9);
        ItemStack item=player.getInventory().getItem(slot);
        if(item!=null) {
            player.getWorld().dropItemNaturally(player.getLocation(),item.clone());
            player.getInventory().setItem(slot,null);
        }
        announce("Inventardiebstahl");
    }

    public void blindness(Player player) {
        if (!enabled("blindness")) return;
        int duration=value("blindness","duration",15)*20;
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,duration,0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,duration,0));
        announce("Dunkelheit");
    }

    public void islandQuake(Player player) {
        if (!enabled("island-quake")) return;
        for(Block block:plugin.getIslandManager().getIslandBlocks()) {
            if(block.getType()==Material.SAND || block.getType()==Material.RED_SAND || block.getType()==Material.GRAVEL) {
                block.getWorld().spawnFallingBlock(block.getLocation().add(0.5,0,0.5),block.getBlockData());
                block.setType(Material.AIR,false);
            }
        }
        announce("Inselbeben");
    }

    private void announce(String event) { Bukkit.broadcastMessage("§c§lBAD EVENT §8» §f"+event); }
}
