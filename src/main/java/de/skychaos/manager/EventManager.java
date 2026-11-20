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
import java.util.Arrays;
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
        Player player=Bukkit.getOnlinePlayers().stream()
                .filter(p->p.getGameMode()==org.bukkit.GameMode.SURVIVAL)
                .findFirst().orElse(null);
        if(player==null || !plugin.isActive() || plugin.isPaused()) return;
        long minutes=plugin.getTimerManager().getElapsedMinutes();
        boolean protection=minutes<plugin.getConfig().getInt("schutz-phase-minuten",5);
        List<String> categories=new ArrayList<>();
        addCategory(categories,"GOOD",1);
        addCategory(categories,"NEUTRAL",1);
        if(!protection) addCategory(categories,"BAD",minutes>=plugin.getConfig().getInt("difficulty-scale-minutes",30)?2:1);
        if(minutes>=60) addCategory(categories,"CHAOS",1);
        if(categories.isEmpty()) return;
        String category=categories.get(ThreadLocalRandom.current().nextInt(categories.size()));
        runCategory(category,player);
        survivedEvents++;
        if(minutes>=plugin.getConfig().getInt("allow-double-events-ab-minute",60)
                && plugin.getConfigManager().getBoolean("events.double-event.enabled",true)
                && ThreadLocalRandom.current().nextDouble()<0.10) {
            List<String> second=categories.stream().distinct().filter(value->!value.equals(category)).toList();
            if(!second.isEmpty()) {
                runCategory(second.get(ThreadLocalRandom.current().nextInt(second.size())),player);
                survivedEvents++;
            }
        }
    }

    private void addCategory(List<String> list,String category,int multiplier) {
        String path="categories."+category;
        if(plugin.getConfigManager().getBoolean(path+".enabled",true)) {
            int weight=Math.max(0,plugin.getConfigManager().getInt(path+".weight",1))*multiplier;
            for(int i=0;i<weight;i++) list.add(category);
        }
    }

    private void runCategory(String category,Player player) {
        switch(category) {
            case "GOOD" -> runGood(player);
            case "NEUTRAL" -> runNeutral(player);
            case "BAD" -> runBad(player);
            case "CHAOS" -> runChaos(player);
            default -> { }
        }
    }

    private void runGood(Player player) {
        List<Runnable> events=new ArrayList<>();
        add(events,"loot-rain",()->good.lootRain(player));
        add(events,"diamond-rain",()->good.diamondRain(player));
        add(events,"xp-boost",()->good.xpBoost(player));
        add(events,"trader-visit",()->good.traderVisit(player));
        add(events,"full-heal",()->good.fullHeal(player));
        execute(events);
    }

    private void runBad(Player player) {
        List<Runnable> events=new ArrayList<>();
        add(events,"block-disappear",()->bad.blockDisappear(player));
        add(events,"mob-attack",()->bad.mobAttack(player));
        add(events,"creeper-rain",()->bad.creeperRain(player));
        add(events,"lava-trap",()->bad.lavaTrap(player));
        add(events,"inventory-steal",()->bad.inventorySteal(player));
        add(events,"blindness",()->bad.blindness(player));
        add(events,"island-quake",()->bad.islandQuake(player));
        execute(events);
    }

    private void runChaos(Player player) {
        List<Runnable> events=new ArrayList<>();
        add(events,"fire-storm",()->chaos.fireStorm(player));
        add(events,"enderman-invasion",()->chaos.endermanInvasion(player));
        add(events,"no-build",()->chaos.noBuild(player));
        execute(events);
    }

    private void runNeutral(Player player) {
        List<Runnable> events=new ArrayList<>();
        add(events,"block-swap",()->{
            List<org.bukkit.block.Block> blocks=new ArrayList<>(plugin.getIslandManager().getIslandBlocks());
            Collections.shuffle(blocks);
            List<Material> choices=Arrays.stream(Material.values())
                    .filter(material->material.isBlock() && !material.isAir()).toList();
            int amount=Math.min(plugin.getConfigManager().getInt("events.block-swap.amount",10),blocks.size());
            for(int i=0;i<amount;i++) {
                var block=blocks.get(i);
                if(block.getType().isSolid() && !choices.isEmpty()) {
                    block.setType(choices.get(ThreadLocalRandom.current().nextInt(choices.size())),false);
                }
            }
            Bukkit.broadcastMessage("§e§lNEUTRAL §8» §fBlock-Swap!");
        });
        add(events,"gravity-flip",()->{
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST,200,1));
            Bukkit.broadcastMessage("§e§lNEUTRAL §8» §fGravity Flip!");
        });
        add(events,"mob-party",()->{
            EntityType[] types={EntityType.CHICKEN,EntityType.PIG,EntityType.COW};
            int amount=plugin.getConfigManager().getInt("events.mob-party.amount",5);
            for(int i=0;i<amount;i++) {
                player.getWorld().spawnEntity(plugin.getIslandManager().randomSurfaceLocation(),
                        types[ThreadLocalRandom.current().nextInt(types.length)]);
            }
            Bukkit.broadcastMessage("§e§lNEUTRAL §8» §fMob-Party!");
        });
        add(events,"inventory-shuffle",()->{
            var inventory=player.getInventory();
            List<org.bukkit.inventory.ItemStack> items=new ArrayList<>();
            for(int i=0;i<9;i++) items.add(inventory.getItem(i));
            Collections.shuffle(items);
            for(int i=0;i<9;i++) inventory.setItem(i,items.get(i));
            Bukkit.broadcastMessage("§e§lNEUTRAL §8» §fInventar durcheinander!");
        });
        add(events,"giant-slime",()->{
            var slime=player.getWorld().spawn(plugin.getIslandManager().randomSurfaceLocation(),org.bukkit.entity.Slime.class);
            slime.setSize(6);
            slime.setCustomName("SkyChaos Giant Slime");
            Bukkit.broadcastMessage("§e§lNEUTRAL §8» §fRiesenschleim!");
        });
        execute(events);
    }

    private void add(List<Runnable> events,String id,Runnable action) {
        if(!plugin.getConfigManager().getBoolean("events."+id+".enabled",true)) return;
        int weight=Math.max(0,plugin.getConfigManager().getInt("events."+id+".weight",1));
        for(int i=0;i<weight;i++) events.add(action);
    }

    private void execute(List<Runnable> events) {
        if(!events.isEmpty()) events.get(ThreadLocalRandom.current().nextInt(events.size())).run();
    }

    public void triggerManual() { triggerRandom(); }
    public int getSurvivedEvents() { return survivedEvents; }
    public void cleanup() { }
}
