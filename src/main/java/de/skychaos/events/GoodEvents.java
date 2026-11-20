package de.skychaos.events;

import de.skychaos.SkyChaos;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class GoodEvents extends RandomEvent {
    public GoodEvents(SkyChaos plugin) { super(plugin); }

    @Override public String id() { return "good"; }
    @Override public String name() { return "Good Events"; }
    @Override public void execute(Player player) { }

    public void lootRain(Player player) {
        if (!enabled()) return;
        List<Material> pool = List.of(Material.BREAD, Material.OAK_LOG, Material.COBBLESTONE, Material.IRON_INGOT, Material.ARROW, Material.BONE, Material.COAL);
        for (int i=0;i<value("amount",10);i++) {
            Location location=plugin.getIslandManager().randomSurfaceLocation().add(0,8,0);
            Material material=pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
            player.getWorld().dropItem(location,new ItemStack(material));
        }
        announce("Lootregen");
    }

    public void diamondRain(Player player) {
        if (!enabled()) return;
        int amount=ThreadLocalRandom.current().nextInt(value("min",3),value("max",5)+1);
        for (int i=0;i<amount;i++) player.getWorld().dropItem(plugin.getIslandManager().randomSurfaceLocation().add(0,5,0),new ItemStack(Material.DIAMOND));
        announce("Diamantenregen");
    }

    public void xpBoost(Player player) {
        if (!enabled()) return;
        player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, value("duration",60)*20, 0, false, true));
        announce("XP-Boost");
    }

    public void traderVisit(Player player) {
        if (!enabled()) return;
        Location location=plugin.getIslandManager().randomSurfaceLocation();
        Villager villager=(Villager)player.getWorld().spawnEntity(location, EntityType.VILLAGER);
        villager.setCustomName("§bSkyChaos-Händler");
        villager.setCustomNameVisible(true);
        Bukkit.getScheduler().runTaskLater(plugin,villager::remove,value("duration",60)*20L);
        announce("Händlerbesuch");
    }

    public void fullHeal(Player player) {
        if (!enabled()) return;
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20);
        announce("Volle Heilung");
    }

    private void announce(String event) { Bukkit.broadcastMessage("§a§lGOOD EVENT §8» §f"+event); }
}
