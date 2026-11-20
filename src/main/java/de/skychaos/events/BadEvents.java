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
import org.bukkit.scheduler.Bukkit;
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
        if (!enabled()) return;
        List<Block> blocks=new ArrayList<>(plugin.getIslandManager().getIslandBlocks());
        Collections.shuffle(blocks);
        int amount=Math.min(blocks.size(),ThreadLocalRandom.current().nextInt(value("min",5),value("max",10)+1));
        for(int i=0;i<amount;i++) if(blocks.get(i).getType().isSolid()) blocks.get(i).setType(Material.AIR,false);
        announce("Block-Verschwinden");
    }

    public void mobAttack(Player player) {
        if (!enabled()) return;
        EntityType[] types={EntityType.ZOMBIE,EntityType.SKELETON,EntityType.SPIDER};
        int amount=ThreadLocalRandom.current().nextInt(value("min",3),value("max",5)+1);
        for(int i=0;i<amount;i++) {
            Location location=plugin.getIslandManager().randomSurfaceLocation();
            player.getWorld().spawnEntity(location,types[ThreadLocalRandom.current().nextInt(types.length)]);
        }
        announce("Mob-Angriff");
    }

    public void creeperRain(Player player) {
        if (!enabled()) return;
        for(int i=0;i<value("amount",3);i++) {
            Location location=plugin.getIslandManager().randomSurfaceLocation().add(0,8,0);
            player.getWorld().spawnEntity(location,EntityType.CREEPER);
        }
        announce("Creeper-Regen");
    }

    public void lavaTrap(Player player) {
        if (!enabled()) return;
        Block block=player.getLocation().subtract(0,1,0).getBlock();
        if (!plugin.getIslandManager().contains(block.getLocation()) || !block.getType().isAir()) return;
        Material previous=block.getType();
        block.setType(Material.LAVA,false);
        Bukkit.getScheduler().runTaskLater(plugin,()->block.setType(previous,false),value("duration",5)*20L);
        announce("Lavafalle");
    }

    public void inventorySteal(Player player) {
        if (!enabled()) return;
        int slot=ThreadLocalRandom.current().nextInt(0,9);
        ItemStack item=player.getInventory().getItem(slot);
        if(item!=null) {
            player.getWorld().dropItemNaturally(player.getLocation(),item.clone());
            player.getInventory().setItem(slot,null);
        }
        announce("Inventardiebstahl");
    }

    public void blindness(Player player) {
        if (!enabled()) return;
        int duration=value("duration",15)*20;
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,duration,0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,duration,0));
        announce("Dunkelheit");
    }

    public void islandQuake(Player player) {
        if (!enabled()) return;
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
