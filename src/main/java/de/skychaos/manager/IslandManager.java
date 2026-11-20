package de.skychaos.manager;

import de.skychaos.SkyChaos;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class IslandManager {
    private final SkyChaos plugin;
    private final World world;
    private final File file;
    private final Set<UUID> trackedEntities=new HashSet<>();
    private Location pos1;
    private Location pos2;
    private int minX;
    private int minY;
    private int minZ;
    private int maxX;
    private int maxY;
    private int maxZ;
    private boolean boundsLoaded;

    public IslandManager(SkyChaos plugin,World world) {
        this.plugin=plugin;
        this.world=world;
        File folder=new File(plugin.getDataFolder(),"islands");
        if(!folder.exists() && !folder.mkdirs()) throw new IllegalStateException("Insel-Verzeichnis konnte nicht erstellt werden.");
        file=new File(folder,"starter-island.yml");
        loadBounds();
    }

    public void setPos1(Location location) {
        pos1=location==null?null:location.clone();
    }

    public void setPos2(Location location) {
        pos2=location==null?null:location.clone();
    }

    public void createDefaultStarterIsland() throws IOException {
        if(file.isFile()) return;
        for(int x=-5;x<=5;x++) for(int z=-5;z<=5;z++) {
            world.getBlockAt(x,79,z).setType(Material.DIRT,false);
            world.getBlockAt(x,80,z).setType(Material.GRASS_BLOCK,false);
        }
        for(int y=81;y<=84;y++) world.getBlockAt(-3,y,-3).setType(Material.OAK_LOG,false);
        for(int x=-5;x<=-1;x++) for(int y=83;y<=86;y++) for(int z=-5;z<=-1;z++) {
            if(Math.abs(x+3)+Math.abs(z+3)+Math.abs(y-84)<=5
                    && world.getBlockAt(x,y,z).getType().isAir())
                world.getBlockAt(x,y,z).setType(Material.OAK_LEAVES,false);
        }
        for(int z=1;z<=3;z++) {
            if(z!=2) world.getBlockAt(1,81,z).setType(Material.STONE_BRICKS,false);
            world.getBlockAt(3,81,z).setType(Material.STONE_BRICKS,false);
        }
        world.getBlockAt(2,80,1).setType(Material.STONE_BRICKS,false);
        world.getBlockAt(2,80,3).setType(Material.STONE_BRICKS,false);
        world.getBlockAt(2,81,0).setType(Material.STONE_BRICKS,false);
        world.getBlockAt(2,81,4).setType(Material.STONE_BRICKS,false);
        world.getBlockAt(2,81,1).setType(Material.WATER,false);
        world.getBlockAt(2,81,3).setType(Material.LAVA,false);
        world.getBlockAt(2,81,2).setType(Material.AIR,false);
        world.getBlockAt(-2,81,0).setType(Material.CHEST,false);
        if(world.getBlockAt(-2,81,0).getState() instanceof Container chest) {
            chest.getInventory().addItem(
                    new ItemStack(Material.ICE,2),
                    new ItemStack(Material.LAVA_BUCKET,1),
                    new ItemStack(Material.WATER_BUCKET,1),
                    new ItemStack(Material.OAK_SAPLING,2),
                    new ItemStack(Material.WHEAT_SEEDS,4),
                    new ItemStack(Material.BREAD,3),
                    new ItemStack(Material.BONE_MEAL,4)
            );
            chest.update(true,false);
        }
        world.setSpawnLocation(0,81,0);
        setPos1(new Location(world,-5,79,-5));
        setPos2(new Location(world,5,86,5));
        if(!save()) throw new IOException("Die automatische Starterinsel konnte nicht gespeichert werden.");
        plugin.getLogger().info("Die Standard-Skyblock-Starterinsel wurde automatisch erstellt.");
    }

    public boolean save() throws IOException {
        if(pos1==null || pos2==null || pos1.getWorld()==null || pos2.getWorld()==null
                || !world.equals(pos1.getWorld()) || !world.equals(pos2.getWorld())) return false;
        int x1=pos1.getBlockX(),x2=pos2.getBlockX();
        int y1=pos1.getBlockY(),y2=pos2.getBlockY();
        int z1=pos1.getBlockZ(),z2=pos2.getBlockZ();
        int ox=Math.min(x1,x2),oy=Math.max(world.getMinHeight(),Math.min(y1,y2)),oz=Math.min(z1,z2);
        int ex=Math.max(x1,x2),ey=Math.min(world.getMaxHeight()-1,Math.max(y1,y2)),ez=Math.max(z1,z2);
        long sx=(long)ex-ox+1,sy=(long)ey-oy+1,sz=(long)ez-oz+1;
        long maxBlocks=Math.max(1,plugin.getConfig().getInt("island.max-blocks",50000));
        if(sx<1 || sy<1 || sz<1 || sx>maxBlocks || sy>maxBlocks || sz>maxBlocks || ex==Integer.MAX_VALUE || ez==Integer.MAX_VALUE)
            throw new IllegalArgumentException("Der Bereich ist leer oder überschreitet island.max-blocks.");
        long count=Math.multiplyExact(Math.multiplyExact(sx,sy),sz);
        if(count>maxBlocks) throw new IllegalArgumentException("Der Bereich überschreitet island.max-blocks.");
        int radius=plugin.getConfig().getInt("island.max-radius",128);
        if(sx>radius*2L+1 || sz>radius*2L+1) throw new IllegalArgumentException("Die Insel überschreitet island.max-radius.");

        YamlConfiguration yaml=new YamlConfiguration();
        yaml.set("format-version",2);
        yaml.set("world",world.getName());
        yaml.set("origin.x",ox);
        yaml.set("origin.y",oy);
        yaml.set("origin.z",oz);
        yaml.set("size.x",(int)sx);
        yaml.set("size.y",(int)sy);
        yaml.set("size.z",(int)sz);
        List<Map<String,Object>> blocks=new ArrayList<>();
        for(int x=ox;x<=ex;x++) for(int y=oy;y<=ey;y++) for(int z=oz;z<=ez;z++) {
            Block block=world.getBlockAt(x,y,z);
            if(block.getType().isAir()) continue;
            Map<String,Object> entry=new LinkedHashMap<>();
            entry.put("x",x-ox);
            entry.put("y",y-oy);
            entry.put("z",z-oz);
            entry.put("data",block.getBlockData().getAsString());
            BlockState state=block.getState();
            if(state instanceof Container container) {
                List<Map<String,Object>> contents=new ArrayList<>();
                ItemStack[] items=container.getInventory().getContents();
                for(int slot=0;slot<items.length;slot++) {
                    if(items[slot]!=null) {
                        Map<String,Object> serialized=new LinkedHashMap<>(items[slot].serialize());
                        serialized.put("slot",slot);
                        contents.add(serialized);
                    }
                }
                entry.put("inventory",contents);
            }
            blocks.add(entry);
        }
        yaml.set("blocks",blocks);
        File temporary=new File(file.getParentFile(),file.getName()+".tmp");
        yaml.save(temporary);
        try {
            Files.move(temporary.toPath(),file.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
        } catch(IOException exception) {
            Files.move(temporary.toPath(),file.toPath(),StandardCopyOption.REPLACE_EXISTING);
        }
        minX=ox; minY=oy; minZ=oz; maxX=ex; maxY=ey; maxZ=ez; boundsLoaded=true;
        return true;
    }

    public void restore() {
        if(!file.isFile()) throw new IllegalStateException("starter-island.yml fehlt. Baue zuerst eine Insel und nutze /challenge save.");
        YamlConfiguration yaml=YamlConfiguration.loadConfiguration(file);
        int ox=yaml.getInt("origin.x"),oy=yaml.getInt("origin.y"),oz=yaml.getInt("origin.z");
        int sx=yaml.getInt("size.x"),sy=yaml.getInt("size.y"),sz=yaml.getInt("size.z");
        long maxBlocks=Math.max(1,plugin.getConfig().getInt("island.max-blocks",50000));
        if(sx<1 || sy<1 || sz<1 || sx>maxBlocks || sy>maxBlocks || sz>maxBlocks
                || oy<world.getMinHeight() || (long)oy+sy>world.getMaxHeight()
                || (long)ox+sx-1>=Integer.MAX_VALUE || (long)oz+sz-1>=Integer.MAX_VALUE) {
            throw new IllegalStateException("Die gespeicherte Insel ist ungültig oder überschreitet die Weltgrenzen.");
        }
        long count=Math.multiplyExact(Math.multiplyExact((long)sx,sy),sz);
        if(count>maxBlocks) {
            throw new IllegalStateException("Die gespeicherte Insel ist ungültig oder überschreitet die Weltgrenzen.");
        }
        if(!world.getName().equals(yaml.getString("world",world.getName())))
            throw new IllegalStateException("Die gespeicherte Insel gehört zu einer anderen Welt.");

        int cleanupHeight=Math.max(0,plugin.getConfig().getInt("island.cleanup-height",32));
        int clearTo=Math.min(world.getMaxHeight()-1,oy+sy+cleanupHeight);
        for(int x=0;x<sx;x++) for(int y=oy;y<=clearTo;y++) for(int z=0;z<sz;z++)
            world.getBlockAt(ox+x,y,oz+z).setType(Material.AIR,false);

        for(Map<?,?> entry:yaml.getMapList("blocks")) {
            try {
                int x=ox+number(entry.get("x"));
                int y=oy+number(entry.get("y"));
                int z=oz+number(entry.get("z"));
                if(x<ox || x>=ox+sx || y<oy || y>=oy+sy || z<oz || z>=oz+sz) continue;
                Block block=world.getBlockAt(x,y,z);
                block.setBlockData(Bukkit.createBlockData(String.valueOf(entry.get("data"))),false);
                BlockState state=block.getState();
                if(state instanceof Container container && entry.get("inventory") instanceof List<?> contents) {
                    container.getInventory().clear();
                    for(Object raw:contents) {
                        if(!(raw instanceof Map<?,?> itemMap)) continue;
                        Map<String,Object> itemData=new HashMap<>();
                        itemMap.forEach((key,value)->itemData.put(String.valueOf(key),value));
                        Object slotValue=itemData.remove("slot");
                        if(!(slotValue instanceof Number slotNumber)) continue;
                        int slot=slotNumber.intValue();
                        if(slot>=0 && slot<container.getInventory().getSize()) {
                            container.getInventory().setItem(slot,ItemStack.deserialize(itemData));
                        }
                    }
                    container.update(true,false);
                }
            } catch(RuntimeException exception) {
                plugin.getLogger().warning("Ein Inselblock konnte nicht geladen werden: "+exception.getMessage());
            }
        }
        minX=ox; minY=oy; minZ=oz; maxX=ox+sx-1; maxY=oy+sy-1; maxZ=oz+sz-1; boundsLoaded=true;
    }

    private int number(Object value) {
        if(value instanceof Number number) return number.intValue();
        throw new IllegalArgumentException("Ungültige Blockkoordinate in starter-island.yml.");
    }

    public boolean contains(Location location) {
        if(location==null || location.getWorld()==null || !world.equals(location.getWorld()) || !file.isFile()) return false;
        if(!boundsLoaded) loadBounds();
        if(!boundsLoaded) return false;
        int headroom=Math.max(0,plugin.getConfig().getInt("island.cleanup-height",32));
        return location.getBlockX()>=minX && location.getBlockX()<=maxX
                && location.getBlockY()>=minY && location.getBlockY()<=Math.min(world.getMaxHeight()-1,maxY+headroom)
                && location.getBlockZ()>=minZ && location.getBlockZ()<=maxZ;
    }

    private void loadBounds() {
        boundsLoaded=false;
        if(!file.isFile()) return;
        YamlConfiguration yaml=YamlConfiguration.loadConfiguration(file);
        int sx=yaml.getInt("size.x"),sy=yaml.getInt("size.y"),sz=yaml.getInt("size.z");
        int ox=yaml.getInt("origin.x"),oy=yaml.getInt("origin.y"),oz=yaml.getInt("origin.z");
        long maxBlocks=Math.max(1,plugin.getConfig().getInt("island.max-blocks",50000));
        if(sx<1 || sy<1 || sz<1 || sx>maxBlocks || sy>maxBlocks || sz>maxBlocks
                || (long)sx*sy*sz>maxBlocks || oy<world.getMinHeight()
                || (long)oy+sy>world.getMaxHeight() || (long)ox+sx-1>=Integer.MAX_VALUE
                || (long)oz+sz-1>=Integer.MAX_VALUE || !world.getName().equals(yaml.getString("world",world.getName()))) return;
        minX=ox; minY=oy; minZ=oz;
        maxX=ox+sx-1; maxY=oy+sy-1; maxZ=oz+sz-1;
        boundsLoaded=true;
    }

    public List<Block> getIslandBlocks() {
        if(!boundsLoaded) loadBounds();
        List<Block> result=new ArrayList<>();
        if(!boundsLoaded) return result;
        for(int x=minX;x<=maxX;x++) for(int y=minY;y<=maxY;y++) for(int z=minZ;z<=maxZ;z++) {
            Block block=world.getBlockAt(x,y,z);
            if(!block.getType().isAir()) result.add(block);
        }
        return result;
    }

    public List<Location> randomSurfaceLocations(int amount) {
        if(amount<=0) return List.of();
        List<Block> surfaces=new ArrayList<>();
        for(Block block:getIslandBlocks()) {
            if(block.getType().isSolid() && block.getRelative(0,1,0).getType().isAir()
                    && block.getRelative(0,2,0).getType().isAir() && block.getY()+2<world.getMaxHeight()) surfaces.add(block);
        }
        if(surfaces.isEmpty()) return List.of();
        Collections.shuffle(surfaces);
        List<Location> locations=new ArrayList<>(Math.min(amount,surfaces.size()));
        for(int i=0;i<Math.min(amount,surfaces.size());i++)
            locations.add(surfaces.get(i).getLocation().add(0.5,1.0,0.5));
        return locations;
    }

    public Location randomSurfaceLocation() {
        List<Location> locations=randomSurfaceLocations(1);
        return locations.isEmpty()?null:locations.getFirst();
    }

    public boolean hasSurface() {
        return randomSurfaceLocation()!=null;
    }

    public Location getSpawnLocation() {
        Location configured=new Location(world,plugin.getConfig().getDouble("island-spawn.x",0.5),
                plugin.getConfig().getDouble("island-spawn.y",80.0),
                plugin.getConfig().getDouble("island-spawn.z",0.5),
                (float)plugin.getConfig().getDouble("island-spawn.yaw",0.0),
                (float)plugin.getConfig().getDouble("island-spawn.pitch",0.0));
        if(contains(configured) && configured.getBlock().isPassable()
                && configured.clone().add(0,1,0).getBlock().isPassable()
                && configured.clone().subtract(0,1,0).getBlock().getType().isSolid()) return configured;
        Location surface=randomSurfaceLocation();
        if(surface!=null) return surface;
        throw new IllegalStateException("Kein sicherer Insel-Spawnpunkt gefunden. Prüfe island-spawn in config.yml.");
    }

    public int countIslandEntities() {
        int count=0;
        for(Entity entity:world.getEntities()) {
            if(!(entity instanceof Player) && contains(entity.getLocation())) count++;
        }
        return count;
    }

    public int countIslandItems() {
        int count=0;
        for(Entity entity:world.getEntities()) {
            if(entity instanceof Item && contains(entity.getLocation())) count++;
        }
        return count;
    }

    public void trackEventEntity(Entity entity) {
        if(entity!=null) {
            trackedEntities.removeIf(uuid->Bukkit.getEntity(uuid)==null);
            trackedEntities.add(entity.getUniqueId());
        }
    }

    public void cleanupTrackedEntities() {
        for(UUID uuid:trackedEntities) {
            Entity entity=Bukkit.getEntity(uuid);
            if(entity!=null && entity.isValid()) entity.remove();
        }
        trackedEntities.clear();
    }

    public World getWorld() { return world; }
    public boolean exists() { return file.isFile(); }
    public File getFile() { return file; }
}
