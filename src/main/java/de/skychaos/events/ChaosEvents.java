package de.skychaos.events;

import de.skychaos.SkyChaos;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

public final class ChaosEvents extends RandomEvent {
    public ChaosEvents(SkyChaos plugin) { super(plugin); }

    @Override public String id() { return "chaos"; }
    @Override public String name() { return "Chaos Events"; }
    @Override public void execute(Player player) { }

    public void fireStorm(Player player) {
        if (!enabled("fire-storm")) return;
        for (var block : plugin.getIslandManager().getIslandBlocks()) {
            if (block.getType().name().endsWith("_LOG") || block.getType().name().endsWith("_WOOD")
                    || block.getType().name().endsWith("_PLANKS")) {
                block.setType(Material.FIRE,false);
            }
        }
        Bukkit.broadcastMessage("§5§lCHAOS §8» §fFeuersturm!");
        Bukkit.getScheduler().runTaskLater(plugin,()->{
            for(var block:plugin.getIslandManager().getIslandBlocks()) if(block.getType()==Material.FIRE) block.setType(Material.AIR,false);
        },value("fire-storm","duration",10)*20L);
    }

    public void endermanInvasion(Player player) {
        if (!enabled("enderman-invasion")) return;
        for(int i=0;i<value("enderman-invasion","amount",5);i++) {
            Location location=plugin.getIslandManager().randomSurfaceLocation();
            player.getWorld().spawnEntity(location,EntityType.ENDERMAN);
        }
        Bukkit.broadcastMessage("§5§lCHAOS §8» §fEnderman-Invasion!");
    }

    public void noBuild(Player player) {
        if (!enabled("no-build")) return;
        plugin.setNoBuildUntil(System.currentTimeMillis()+value("no-build","duration",20)*1000L);
        Bukkit.broadcastMessage("§5§lCHAOS §8» §fBauen ist vorübergehend deaktiviert!");
    }
}
