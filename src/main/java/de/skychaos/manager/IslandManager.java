package de.skychaos.manager;

import de.skychaos.SkyChaos;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class IslandManager {
    private final SkyChaos plugin;
    private final World world;
    private final File file;
    private Location pos1;
    private Location pos2;
    private int minX;
    private int minY;
    private int minZ;
    private int maxX;
    private int maxY;
    private int maxZ;

    public IslandManager(SkyChaos plugin, World world) {
        this.plugin = plugin;
        this.world = world;
        File folder = new File(plugin.getDataFolder(), "islands");
        if (!folder.exists()) folder.mkdirs();
        file = new File(folder, "starter-island.yml");
    }

    public void setPos1(Location location) {
        pos1 = location.clone();
    }

    public void setPos2(Location location) {
        pos2 = location.clone();
    }

    public boolean save() throws IOException {
        if (pos1 == null || pos2 == null || !pos1.getWorld().equals(world) || !pos2.getWorld().equals(world)) return false;
        minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        minY = Math.max(world.getMinHeight(), Math.min(pos1.getBlockY(), pos2.getBlockY()));
        minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        maxY = Math.min(world.getMaxHeight() - 1, Math.max(pos1.getBlockY(), pos2.getBlockY()));
        maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        long count = (long)(maxX-minX+1)*(maxY-minY+1)*(maxZ-minZ+1);
        if (count > plugin.getConfig().getInt("island.max-blocks", 50000)) throw new IllegalArgumentException("Der Bereich enthält zu viele Blöcke.");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("origin.x", minX);
        yaml.set("origin.y", minY);
        yaml.set("origin.z", minZ);
        yaml.set("size.x", maxX-minX+1);
        yaml.set("size.y", maxY-minY+1);
        yaml.set("size.z", maxZ-minZ+1);
        yaml.set("blocks", new ArrayList<>());
        List<Map<String, Object>> blocks = new ArrayList<>();
        for (int x=minX; x<=maxX; x++) {
            for (int y=minY; y<=maxY; y++) {
                for (int z=minZ; z<=maxZ; z++) {
                    Block block = world.getBlockAt(x,y,z);
                    if (block.getType().isAir()) continue;
                    Map<String, Object> entry = new java.util.LinkedHashMap<>();
                    entry.put("x", x-minX);
                    entry.put("y", y-minY);
                    entry.put("z", z-minZ);
                    entry.put("data", block.getBlockData().getAsString());
                    BlockState state = block.getState();
                    if (state instanceof Container container) {
                        List<Map<String, Object>> contents = new ArrayList<>();
                        ItemStack[] items = container.getInventory().getContents();
                        for (int slot=0; slot<items.length; slot++) {
                            if (items[slot] != null) {
                                Map<String, Object> item = new java.util.LinkedHashMap<>(items[slot].serialize());
                                item.put("slot", slot);
                                contents.add(item);
                            }
                        }
                        entry.put("inventory", contents);
                    }
                    blocks.add(entry);
                }
            }
        }
        yaml.set("blocks", blocks);
        yaml.save(file);
        return true;
    }

    public void restore() {
        if (!file.exists()) throw new IllegalStateException("starter-island.yml fehlt. Baue zuerst eine Insel und nutze /challenge save.");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        int ox = yaml.getInt("origin.x");
        int oy = yaml.getInt("origin.y");
        int oz = yaml.getInt("origin.z");
        int sx = yaml.getInt("size.x");
        int sy = yaml.getInt("size.y");
        int sz = yaml.getInt("size.z");
        if (sx < 1 || sy < 1 || sz < 1) throw new IllegalStateException("Die gespeicherte Insel ist ungültig.");
        for (int x=0; x<sx; x++) for (int y=0; y<sy; y++) for (int z=0; z<sz; z++) world.getBlockAt(ox+x,oy+y,oz+z).setType(Material.AIR, false);
        for (Map<?, ?> entry : yaml.getMapList("blocks")) {
            int x = ox + ((Number)entry.get("x")).intValue();
            int y = oy + ((Number)entry.get("y")).intValue();
            int z = oz + ((Number)entry.get("z")).intValue();
            Block block = world.getBlockAt(x,y,z);
            block.setBlockData(org.bukkit.Bukkit.createBlockData(String.valueOf(entry.get("data"))), false);
            BlockState state = block.getState();
            if (state instanceof Container container && entry.get("inventory") instanceof List<?> items) {
                container.getInventory().clear();
                for (Object raw : items) {
                    if (raw instanceof Map<?, ?> itemMap) {
                        Map<String, Object> itemData = new java.util.HashMap<>();
                        itemMap.forEach((key,value) -> itemData.put(String.valueOf(key), value));
                        int slot = ((Number)itemData.remove("slot")).intValue();
                        if (slot >= 0 && slot < container.getInventory().getSize()) {
                            container.getInventory().setItem(slot, ItemStack.deserialize(itemData));
                        }
                    }
                }
                container.update(true, false);
            }
        }
        minX=ox; minY=oy; minZ=oz; maxX=ox+sx-1; maxY=oy+sy-1; maxZ=oz+sz-1;
    }

    public boolean contains(Location location) {
        if (location == null || location.getWorld() == null || !location.getWorld().equals(world) || !file.exists()) return false;
        if (maxX < minX) loadBounds();
        return location.getBlockX() >= minX && location.getBlockX() <= maxX
                && location.getBlockY() >= minY && location.getBlockY() <= maxY + 16
                && location.getBlockZ() >= minZ && location.getBlockZ() <= maxZ;
    }

    private void loadBounds() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        minX=yaml.getInt("origin.x"); minY=yaml.getInt("origin.y"); minZ=yaml.getInt("origin.z");
        maxX=minX+yaml.getInt("size.x")-1; maxY=minY+yaml.getInt("size.y")-1; maxZ=minZ+yaml.getInt("size.z")-1;
    }

    public List<Block> getIslandBlocks() {
        if (maxX < minX) loadBounds();
        List<Block> result = new ArrayList<>();
        for (int x=minX; x<=maxX; x++) for (int y=minY; y<=maxY; y++) for (int z=minZ; z<=maxZ; z++) {
            Block block = world.getBlockAt(x,y,z);
            if (!block.getType().isAir()) result.add(block);
        }
        return result;
    }

    public Location randomSurfaceLocation() {
        if (maxX < minX) loadBounds();
        for (int attempt=0; attempt<40; attempt++) {
            int x=minX+(int)(Math.random()*(maxX-minX+1));
            int z=minZ+(int)(Math.random()*(maxZ-minZ+1));
            for (int y=maxY+1; y>=minY; y--) {
                Block block=world.getBlockAt(x,y,z);
                if (!block.getType().isAir() && block.getType().isSolid()) return block.getLocation().add(0.5,1,0.5);
            }
        }
        return getSpawnLocation();
    }

    public Location getSpawnLocation() {
        return new Location(world, plugin.getConfig().getDouble("island-spawn.x",0.5),
                plugin.getConfig().getDouble("island-spawn.y",80.0),
                plugin.getConfig().getDouble("island-spawn.z",0.5),
                (float)plugin.getConfig().getDouble("island-spawn.yaw",0.0),
                (float)plugin.getConfig().getDouble("island-spawn.pitch",0.0));
    }

    public World getWorld() { return world; }
    public boolean exists() { return file.exists(); }
    public File getFile() { return file; }
}
