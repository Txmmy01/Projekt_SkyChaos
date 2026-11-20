package de.skychaos.events;

import de.skychaos.SkyChaos;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
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
        if(!enabled("loot-rain")) return;
        List<Material> pool=List.of(Material.BREAD,Material.OAK_LOG,Material.COBBLESTONE,Material.IRON_INGOT,Material.ARROW,Material.BONE,Material.COAL);
        int amount=Math.min(value("loot-rain","amount",10),plugin.getConfig().getInt("events.max-dropped-items",80));
        for(int i=0;i<amount;i++) {
            Location surface=safeLocation();
            if(surface==null) break;
            Item item=player.getWorld().dropItem(surface.add(0,6,0),new ItemStack(pool.get(ThreadLocalRandom.current().nextInt(pool.size()))));
            track(item);
        }
        announce("Lootregen");
    }

    public void diamondRain(Player player) {
        if(!enabled("diamond-rain")) return;
        int min=Math.max(1,value("diamond-rain","min",3));
        int max=Math.max(min,value("diamond-rain","max",5));
        int amount=Math.min(ThreadLocalRandom.current().nextInt(min,max+1),plugin.getConfig().getInt("events.max-dropped-items",80));
        for(int i=0;i<amount;i++) {
            Location surface=safeLocation();
            if(surface==null) break;
            Item item=player.getWorld().dropItem(surface.add(0,5,0),new ItemStack(Material.DIAMOND));
            track(item);
        }
        announce("Diamantenregen");
    }

    public void xpBoost(Player player) {
        if(!enabled("xp-boost")) return;
        plugin.startXpBoost(value("xp-boost","duration",60),value("xp-boost","multiplier",2));
        announce("XP-Boost");
    }

    public void traderVisit(Player player) {
        if(!enabled("trader-visit")) return;
        if(plugin.getIslandManager().countIslandEntities()>=plugin.getConfig().getInt("events.max-entities",40)) return;
        Location location=safeLocation();
        if(location==null) return;
        Villager villager=player.getWorld().spawn(location,Villager.class);
        villager.setCustomName("§bSkyChaos-Händler");
        villager.setCustomNameVisible(true);
        villager.setAI(false);
        villager.setInvulnerable(true);
        MerchantRecipe diamondTrade=new MerchantRecipe(new ItemStack(Material.DIAMOND),64);
        diamondTrade.addIngredient(new ItemStack(Material.IRON_INGOT,8));
        MerchantRecipe emeraldTrade=new MerchantRecipe(new ItemStack(Material.EMERALD,2),64);
        emeraldTrade.addIngredient(new ItemStack(Material.COBBLESTONE,16));
        villager.setRecipes(List.of(diamondTrade,emeraldTrade));
        track(villager);
        schedule(villager::remove,value("trader-visit","duration",60)*20L);
        announce("Händlerbesuch");
    }

    public void fullHeal(Player player) {
        if(!enabled("full-heal")) return;
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20);
        announce("Volle Heilung");
    }

    public void regeneration(Player player) {
        if(!enabled("regeneration")) return;
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,value("regeneration","duration",12)*20,
                Math.max(0,value("regeneration","amplifier",1)),false,false,true));
        announce("Regeneration");
    }

    public void absorption(Player player) {
        if(!enabled("absorption")) return;
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION,value("absorption","duration",45)*20,
                Math.max(0,value("absorption","amplifier",1)),false,false,true));
        announce("Absorption");
    }

    public void saturation(Player player) {
        if(!enabled("saturation")) return;
        player.setFoodLevel(20);
        player.setSaturation(20);
        announce("Sättigung");
    }

    private void announce(String event) {
        Bukkit.broadcastMessage("§a§lGOOD EVENT §8» §f"+event);
    }
}
