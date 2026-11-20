package de.skychaos.manager;

import de.skychaos.SkyChaos;
import de.skychaos.world.VoidGenerator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.EndPortalFrame;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

public final class WorldSetupManager {
    private final SkyChaos plugin;
    private World overworld;
    private World nether;
    private World end;

    public WorldSetupManager(SkyChaos plugin,World overworld) {
        this.plugin=plugin;
        this.overworld=overworld;
    }

    public void setup() {
        nether=createWorld(plugin.getConfig().getString("worlds.nether","skychaos_nether"),World.Environment.NETHER);
        end=createWorld(plugin.getConfig().getString("worlds.end","skychaos_end"),World.Environment.THE_END);
        buildOverworldPortals();
        if(nether.getBlockAt(0,79,0).getType().isAir()) buildNether();
        if(end.getBlockAt(0,79,0).getType().isAir()) buildEnd();
    }

    private World createWorld(String name,World.Environment environment) {
        World world=Bukkit.getWorld(name);
        if(world!=null) return world;
        WorldCreator creator=new WorldCreator(name);
        creator.environment(environment);
        creator.generator(new VoidGenerator());
        creator.generateStructures(false);
        World created=creator.createWorld();
        if(created==null) throw new IllegalStateException("Void-Dimension konnte nicht erstellt werden: "+name);
        return created;
    }

    private void buildOverworldPortals() {
        for(int x=7;x<=30;x++) for(int z=-5;z<=5;z++) {
            overworld.getBlockAt(x,79,z).setType(Material.STONE_BRICKS,false);
            overworld.getBlockAt(x,80,z).setType(Material.AIR,false);
        }
        for(int x=6;x<=15;x++) overworld.getBlockAt(x,80,0).setType(Material.STONE_BRICKS,false);
        for(int y=80;y<=84;y++) {
            overworld.getBlockAt(18,y,0).setType(Material.OBSIDIAN,false);
            overworld.getBlockAt(21,y,0).setType(Material.OBSIDIAN,false);
        }
        for(int x=19;x<=20;x++) {
            overworld.getBlockAt(x,80,0).setType(Material.OBSIDIAN,false);
            overworld.getBlockAt(x,84,0).setType(Material.OBSIDIAN,false);
        }
        for(int x=23;x<=27;x++) for(int z=-3;z<=3;z++) {
            if(x==23 || x==27 || z==-3 || z==3) overworld.getBlockAt(x,80,z).setType(Material.STONE_BRICKS,false);
        }
        placeEndFrame(24,80,-2,BlockFace.SOUTH);
        placeEndFrame(25,80,-2,BlockFace.SOUTH);
        placeEndFrame(26,80,-2,BlockFace.SOUTH);
        placeEndFrame(24,80,2,BlockFace.NORTH);
        placeEndFrame(25,80,2,BlockFace.NORTH);
        placeEndFrame(26,80,2,BlockFace.NORTH);
        placeEndFrame(23,80,-1,BlockFace.EAST);
        placeEndFrame(23,80,0,BlockFace.EAST);
        placeEndFrame(23,80,1,BlockFace.EAST);
        placeEndFrame(27,80,-1,BlockFace.WEST);
        placeEndFrame(27,80,0,BlockFace.WEST);
        placeEndFrame(27,80,1,BlockFace.WEST);
        overworld.setSpawnLocation(0,81,0);
    }

    private void placeEndFrame(int x,int y,int z,BlockFace facing) {
        Block block=overworld.getBlockAt(x,y,z);
        BlockData data=Bukkit.createBlockData(Material.END_PORTAL_FRAME);
        if(data instanceof EndPortalFrame frame) {
            frame.setFacing(facing);
            frame.setEye(false);
            block.setBlockData(frame,false);
        } else block.setType(Material.END_PORTAL_FRAME,false);
    }

    private void buildNether() {
        platform(nether,0,79,0,10,Material.NETHERRACK);
        for(int x=8;x<=29;x++) {
            nether.getBlockAt(x,79,0).setType(Material.NETHER_BRICKS,false);
            nether.getBlockAt(x,80,0).setType(Material.NETHER_BRICK_SLAB,false);
            nether.getBlockAt(x,80,1).setType(Material.NETHER_BRICK_FENCE,false);
            nether.getBlockAt(x,80,-1).setType(Material.NETHER_BRICK_FENCE,false);
        }
        int x1=29,x2=44,z1=-7,z2=7;
        fill(nether,x1,79,z1,x2,79,z2,Material.NETHER_BRICKS);
        fill(nether,x1,80,z1,x2,87,z1,Material.NETHER_BRICKS);
        fill(nether,x1,80,z2,x2,87,z2,Material.NETHER_BRICKS);
        fill(nether,x1,80,z1,x1,87,z2,Material.NETHER_BRICKS);
        fill(nether,x2,80,z1,x2,87,z2,Material.NETHER_BRICKS);
        fill(nether,x1+1,80,z1+1,x2-1,86,z2-1,Material.AIR);
        fill(nether,29,80,-1,29,81,1,Material.AIR);
        fill(nether,x1+1,87,z1+1,x2-1,87,z2-1,Material.NETHER_BRICK_SLAB);
        for(int x=x1+2;x<x2;x+=4) {
            nether.getBlockAt(x,88,z1).setType(Material.NETHER_BRICK_FENCE,false);
            nether.getBlockAt(x,88,z2).setType(Material.NETHER_BRICK_FENCE,false);
        }
        for(int z=z1+2;z<z2;z+=4) {
            nether.getBlockAt(x1,88,z).setType(Material.NETHER_BRICK_FENCE,false);
            nether.getBlockAt(x2,88,z).setType(Material.NETHER_BRICK_FENCE,false);
        }
        fill(nether,34,80,-2,39,80,2,Material.NETHER_BRICKS);
        nether.getBlockAt(37,81,0).setType(Material.SPAWNER,false);
        if(nether.getBlockAt(37,81,0).getState() instanceof CreatureSpawner spawner) {
            spawner.setSpawnedType(EntityType.BLAZE);
            spawner.setDelay(300);
            spawner.setMinSpawnDelay(500);
            spawner.setMaxSpawnDelay(900);
            spawner.setSpawnCount(2);
            spawner.update(true,false);
        }
        chest(nether,42,80,0,Material.NETHER_BRICK,Material.BLAZE_ROD,Material.GOLD_INGOT,Material.NETHER_WART,Material.DIAMOND);
        chest(nether,31,80,-5,Material.NETHER_BRICK,Material.BLAZE_ROD,Material.OBSIDIAN,Material.GOLD_INGOT);
        nether.setSpawnLocation(0,81,0);
    }

    private void buildEnd() {
        platform(end,0,79,0,10,Material.END_STONE);
        for(int x=10;x<=38;x++) end.getBlockAt(x,79,0).setType(Material.OBSIDIAN,false);
        platform(end,39,79,0,16,Material.END_STONE);
        for(int x:new int[]{42,54}) for(int z:new int[]{-5,5}) {
            fill(end,x,80,z,x,87,z,Material.OBSIDIAN);
        }
        fill(end,45,80,-3,49,80,3,Material.OBSIDIAN);
        chest(end,47,80,0,Material.END_STONE,Material.ENDER_PEARL,Material.END_STONE,Material.CHORUS_FRUIT,Material.DIAMOND);
        end.setSpawnLocation(0,81,0);
    }

    private void platform(World world,int x,int y,int z,int radius,Material material) {
        fill(world,x-radius,y,z-radius,x+radius,y,z+radius,material);
        for(int dx=-radius;dx<=radius;dx++) for(int dz=-radius;dz<=radius;dz++)
            world.getBlockAt(x+dx,y+1,z+dz).setType(Material.AIR,false);
        world.setSpawnLocation(x,y+2,z);
    }

    private void fill(World world,int x1,int y1,int z1,int x2,int y2,int z2,Material material) {
        for(int x=Math.min(x1,x2);x<=Math.max(x1,x2);x++)
            for(int y=Math.min(y1,y2);y<=Math.max(y1,y2);y++)
                for(int z=Math.min(z1,z2);z<=Math.max(z1,z2);z++)
                    world.getBlockAt(x,y,z).setType(material,false);
    }

    private void chest(World world,int x,int y,int z,Material filler,Material... loot) {
        world.getBlockAt(x,y,z).setType(Material.CHEST,false);
        if(world.getBlockAt(x,y,z).getState() instanceof org.bukkit.block.Chest chest) {
            for(Material material:loot) chest.getBlockInventory().addItem(new ItemStack(material,material.getMaxStackSize()>1?4:1));
            chest.update(true,false);
        }
    }

    public boolean isChallengeWorld(World world) {
        return world!=null && (world.equals(overworld) || world.equals(nether) || world.equals(end));
    }

    public World getOverworld() { return overworld; }
    public World getNether() { return nether; }
    public World getEnd() { return end; }

    public Location getSpawn(World world) {
        if(world==null) return overworld.getSpawnLocation();
        if(world.equals(nether)) return nether.getSpawnLocation();
        if(world.equals(end)) return end.getSpawnLocation();
        return overworld.getSpawnLocation();
    }
}
