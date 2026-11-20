package de.skychaos.manager;

import de.skychaos.SkyChaos;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class LootIslandManager {
    private final SkyChaos plugin;
    private final World world;
    private final File file;
    private final List<Location> islands=new ArrayList<>();

    public LootIslandManager(SkyChaos plugin,World world) {
        this.plugin=plugin;
        this.world=world;
        file=new File(plugin.getDataFolder(),"loot-islands.yml");
        load();
    }

    public void regenerate() {
        clear();
        int amount=plugin.getDifficultyManager().chestCount();
        int minRadius=Math.max(20,plugin.getConfig().getInt("loot-islands.min-distance-from-spawn",40));
        int maxRadius=Math.max(minRadius+10,plugin.getConfig().getInt("loot-islands.max-distance-from-spawn",120));
        int minBetween=Math.max(10,plugin.getConfig().getInt("loot-islands.min-distance-between",24));
        int attempts=0;
        while(islands.size()<amount && attempts<amount*100) {
            attempts++;
            double angle=ThreadLocalRandom.current().nextDouble(Math.PI*2.0);
            double radius=ThreadLocalRandom.current().nextDouble(minRadius,maxRadius);
            int x=(int)Math.round(Math.cos(angle)*radius);
            int z=(int)Math.round(Math.sin(angle)*radius);
            if(Math.abs(x)<minRadius && Math.abs(z)<minRadius) continue;
            boolean close=false;
            for(Location old:islands) {
                if(old.distanceSquared(new Location(world,x,80,z))<minBetween*(double)minBetween) {
                    close=true;
                    break;
                }
            }
            if(close) continue;
            buildIsland(x,80,z);
        }
        save();
        plugin.getLogger().info("Loot-Inseln erstellt: "+islands.size()+"/"+amount);
    }

    private void buildIsland(int x,int y,int z) {
        for(int dx=-1;dx<=1;dx++) for(int dz=-1;dz<=1;dz++) {
            Block block=world.getBlockAt(x+dx,y,z+dz);
            block.setType(Material.GRASS_BLOCK,false);
        }
        Block chestBlock=world.getBlockAt(x,y+1,z);
        chestBlock.setType(Material.CHEST,false);
        if(chestBlock.getState() instanceof Chest chest) {
            fillChest(chest);
            chest.update(true,false);
        }
        islands.add(new Location(world,x,y,z));
    }

    private void fillChest(Chest chest) {
        var inventory=chest.getBlockInventory();
        inventory.clear();
        Material[] pool={
                Material.COBBLESTONE,Material.OAK_LOG,Material.DIRT,Material.OAK_SAPLING,
                Material.WHEAT_SEEDS,Material.BREAD,Material.COAL,Material.IRON_INGOT,
                Material.GOLD_INGOT,Material.STRING,Material.BONE,Material.ARROW,
                Material.OBSIDIAN,Material.ENDER_PEARL,Material.BLAZE_ROD,
                Material.DIAMOND,Material.EMERALD,Material.WATER_BUCKET,Material.ICE
        };
        int rolls=ThreadLocalRandom.current().nextInt(4,7);
        int bonus=(int)Math.floor((rolls-1)*(plugin.getDifficultyManager().lootMultiplier()-1.0));
        rolls=Math.max(2,Math.min(12,rolls+bonus));
        for(int i=0;i<rolls;i++) {
            Material material=pool[ThreadLocalRandom.current().nextInt(pool.length)];
            int base=material.getMaxStackSize()>1?ThreadLocalRandom.current().nextInt(2,9):1;
            int amount=Math.max(1,Math.min(material.getMaxStackSize(),
                    (int)Math.round(base*plugin.getDifficultyManager().lootMultiplier())));
            inventory.addItem(new ItemStack(material,amount));
        }
    }

    private void load() {
        if(!file.isFile()) return;
        YamlConfiguration yaml=YamlConfiguration.loadConfiguration(file);
        for(String key:yaml.getKeys(false)) {
            int x=yaml.getInt(key+".x"),y=yaml.getInt(key+".y"),z=yaml.getInt(key+".z");
            islands.add(new Location(world,x,y,z));
        }
    }

    private void save() {
        YamlConfiguration yaml=new YamlConfiguration();
        for(int i=0;i<islands.size();i++) {
            Location location=islands.get(i);
            yaml.set("island-"+i+".x",location.getBlockX());
            yaml.set("island-"+i+".y",location.getBlockY());
            yaml.set("island-"+i+".z",location.getBlockZ());
        }
        try {
            yaml.save(file);
        } catch(IOException exception) {
            plugin.getLogger().severe("Loot-Inseln konnten nicht gespeichert werden: "+exception.getMessage());
        }
    }

    public void clear() {
        for(Location location:islands) {
            int x=location.getBlockX(),y=location.getBlockY(),z=location.getBlockZ();
            for(int dx=-1;dx<=1;dx++) for(int dz=-1;dz<=1;dz++)
                world.getBlockAt(x+dx,y,z+dz).setType(Material.AIR,false);
            world.getBlockAt(x,y+1,z).setType(Material.AIR,false);
        }
        islands.clear();
        if(file.exists()) file.delete();
    }

    public int size() {
        return islands.size();
    }
}
