package de.skychaos.manager;

import de.skychaos.SkyChaos;
import de.skychaos.events.BadEvents;
import de.skychaos.events.ChaosEvents;
import de.skychaos.events.GoodEvents;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class EventManager {
    private final SkyChaos plugin;
    private final GoodEvents good;
    private final BadEvents bad;
    private final ChaosEvents chaos;
    private int survivedEvents;

    public EventManager(SkyChaos plugin) {
        this.plugin=plugin;
        good=new GoodEvents(plugin);
        bad=new BadEvents(plugin);
        chaos=new ChaosEvents(plugin);
    }

    public void triggerRandom() {
        Player player=Bukkit.getOnlinePlayers().stream().filter(p->p.getGameMode()==org.bukkit.GameMode.SURVIVAL).findFirst().orElse(null);
        if(player==null) return;
        long minutes=plugin.getTimerManager().getElapsedMinutes();
        boolean protection=minutes<plugin.getConfig().getInt("schutz-phase-minuten",5);
        List<String> categories=new ArrayList<>();
        addCategory(categories,"GOOD");
        addCategory(categories,"NEUTRAL");
        if(!protection) addCategory(categories,"BAD");
        if(minutes>=60) addCategory(categories,"CHAOS");
        if(categories.isEmpty()) return;
        String category=categories.get(ThreadLocalRandom.current().nextInt(categories.size()));
        runCategory(category,player);
        survivedEvents++;
        if(minutes>=plugin.getConfig().getInt("allow-double-events-ab-minute",60)
                && ThreadLocalRandom.current().nextDouble()<0.10) {
            List<String> second=new ArrayList<>(categories);
            second.remove(category);
            if(!second.isEmpty()) runCategory(second.get(ThreadLocalRandom.current().nextInt(second.size())),player);
        }
    }

    private void addCategory(List<String> list,String category) {
        String path="categories."+category;
        if(plugin.getConfigManager().getBoolean(path+".enabled",true)) {
            int weight=Math.max(0,plugin.getConfigManager().getInt(path+".weight",1));
            for(int i=0;i<weight;i++) list.add(category);
        }
    }

    private void runCategory(String category,Player player) {
        switch(category) {
            case "GOOD" -> runGood(player);
            case "NEUTRAL" -> runNeutral(player);
            case "BAD" -> runBad(player);
            case "CHAOS" -> runChaos(player);
        }
    }

    private void runGood(Player p) {
        List<Runnable> events=new ArrayList<>();
        add(events,"loot-rain",()->good.lootRain(p));
        add(events,"diamond-rain",()->good.diamondRain(p));
        add(events,"xp-boost",()->good.xpBoost(p));
        add(events,"trader-visit",()->good.traderVisit(p));
        add(events,"full-heal",()->good.fullHeal(p));
        execute(events);
    }

    private void runBad(Player p) {
        List<Runnable> events=new ArrayList<>();
        add(events,"block-disappear",()->bad.blockDisappear(p));
        add(events,"mob-attack",()->bad.mobAttack(p));
        add(events,"creeper-rain",()->bad.creeperRain(p));
        add(events,"lava-trap",()->bad.lavaTrap(p));
        add(events,"inventory-steal",()->bad.inventorySteal(p));
        add(events,"blindness",()->bad.blindness(p));
        add(events,"island-quake",()->bad.islandQuake(p));
        execute(events);
    }

    private void runChaos(Player p) {
        List<Runnable> events=new ArrayList<>();
        add(events,"fire-storm",()->chaos.fireStorm(p));
        add(events,"enderman-invasion",()->chaos.endermanInvasion(p));
        add(events,"no-build",()->chaos.noBuild(p));
        execute(events);
    }

    private void runNeutral(Player p) {
        List<Runnable> events=new ArrayList<>();
        add(events,"block-swap",()->{
            List<org.bukkit.block.Block> blocks=new ArrayList<>(plugin.getIslandManager().getIslandBlocks());
            Collections.shuffle(blocks);
            int n=Math.min(10,blocks.size());
            for(int i=0;i<n;i++) {
                var block=blocks.get(i);
                if(block.getType().isSolid()) block.setType(Material.values()[ThreadLocalRandom.current().nextInt(Material.values().length)].isBlock()
                        ? Material.values()[ThreadLocalRandom.current().nextInt(Material.values().length)] : Material.COBBLESTONE,false);
            }
            Bukkit.broadcastMessage("§e§lNEUTRAL §8» §fBlock-Swap!");
        });
        add(events,"gravity-flip",()->{
            p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST,200,1));
            Bukkit.broadcastMessage("§e§lNEUTRAL §8» §fGravity Flip!");
        });
        add(events,"mob-party",()->{
            EntityType[] types={EntityType.CHICKEN,EntityType.PIG,EntityType.COW};
            for(int i=0;i<5;i++) p.getWorld().spawnEntity(plugin.getIslandManager().randomSurfaceLocation(),types[ThreadLocalRandom.current().nextInt(types.length)]);
            Bukkit.broadcastMessage("§e§lNEUTRAL §8» §fMob-Party!");
        });
        add(events,"inventory-shuffle",()->{
            var inventory=p.getInventory();
            List<org.bukkit.inventory.ItemStack> items=new ArrayList<>();
            for(int i=0;i<9;i++) items.add(inventory.getItem(i));
            Collections.shuffle(items);
            for(int i=0;i<9;i++) inventory.setItem(i,items.get(i));
            Bukkit.broadcastMessage("§e§lNEUTRAL §8» §fInventar durcheinander!");
        });
        add(events,"giant-slime",()->{
            var slime=p.getWorld().spawn(plugin.getIslandManager().randomSurfaceLocation(),org.bukkit.entity.Slime.class);
            slime.setSize(6);
            Bukkit.broadcastMessage("§e§lNEUTRAL §8» §fRiesenschleim!");
        });
        execute(events);
    }

    private void add(List<Runnable> events,String id,Runnable action) {
        if(plugin.getConfigManager().getBoolean("events."+id+".enabled",true)
                && plugin.getConfigManager().getInt("events."+id+".weight",1)>0) events.add(action);
    }

    private void execute(List<Runnable> events) {
        if(events.isEmpty()) return;
        events.get(ThreadLocalRandom.current().nextInt(events.size())).run();
    }

    public void triggerManual() { triggerRandom(); }
    public int getSurvivedEvents() { return survivedEvents; }
    public void cleanup() { }
}
