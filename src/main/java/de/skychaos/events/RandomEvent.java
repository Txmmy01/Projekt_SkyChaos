package de.skychaos.events;

import de.skychaos.SkyChaos;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public abstract class RandomEvent {
    protected final SkyChaos plugin;
    private final List<BukkitTask> tasks=new ArrayList<>();
    private final Set<UUID> spawnedEntities=new HashSet<>();
    private final Map<Block,BlockData> temporaryBlocks=new HashMap<>();

    protected RandomEvent(SkyChaos plugin) {
        this.plugin=plugin;
    }

    public abstract String id();
    public abstract String name();
    public abstract void execute(Player player);

    protected boolean enabled(String eventId) {
        return plugin.getConfigManager().getBoolean("events."+eventId+".enabled",true);
    }

    protected int value(String eventId,String key,int fallback) {
        return plugin.getConfigManager().getInt("events."+eventId+"."+key,fallback);
    }

    protected Location safeLocation() {
        return plugin.getIslandManager().randomSurfaceLocation();
    }

    protected void track(Entity entity) {
        if(entity!=null) spawnedEntities.add(entity.getUniqueId());
    }

    protected void trackTemporaryBlock(Block block) {
        temporaryBlocks.putIfAbsent(block,block.getBlockData().clone());
    }

    protected void schedule(Runnable action,long delayTicks) {
        BukkitTask[] holder=new BukkitTask[1];
        holder[0]=Bukkit.getScheduler().runTaskLater(plugin,()->{
            try {
                action.run();
            } finally {
                tasks.remove(holder[0]);
            }
        },Math.max(1L,delayTicks));
        tasks.add(holder[0]);
    }

    public void cleanup() {
        for(BukkitTask task:tasks) if(!task.isCancelled()) task.cancel();
        tasks.clear();
        for(UUID uuid:spawnedEntities) {
            Entity entity=Bukkit.getEntity(uuid);
            if(entity!=null && entity.isValid()) entity.remove();
        }
        spawnedEntities.clear();
        for(Map.Entry<Block,BlockData> entry:temporaryBlocks.entrySet()) {
            Block block=entry.getKey();
            if(block.getType().isAir() || block.getBlockData().getMaterial()==org.bukkit.Material.COBWEB
                    || block.getType()==org.bukkit.Material.LAVA || block.getType()==org.bukkit.Material.FIRE) {
                block.setBlockData(entry.getValue(),false);
            }
        }
        temporaryBlocks.clear();
    }
}
