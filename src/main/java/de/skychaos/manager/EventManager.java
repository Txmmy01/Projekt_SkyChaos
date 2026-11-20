package de.skychaos.manager;

import de.skychaos.SkyChaos;
import de.skychaos.events.BadEvents;
import de.skychaos.events.ChaosEvents;
import de.skychaos.events.GoodEvents;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
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
        if(!plugin.isActive() || plugin.isPaused()) return;
        List<Player> participants=Bukkit.getOnlinePlayers().stream()
                .filter(player->player.getGameMode()==GameMode.SURVIVAL)
                .filter(player->plugin.getIslandManager().contains(player.getLocation()))
                .toList();
        if(participants.isEmpty() || !plugin.getIslandManager().hasSurface()) return;
        Player target=participants.get(ThreadLocalRandom.current().nextInt(participants.size()));
        long minutes=plugin.getTimerManager().getElapsedMinutes();
        boolean protectedPhase=minutes<plugin.getConfig().getInt("schutz-phase-minuten",5);
        List<String> categories=availableCategories(minutes,protectedPhase);
        if(categories.isEmpty()) return;
        String selected=choose(categories);
        if(runCategory(selected,target)) survivedEvents++;
        int doubleAfter=plugin.getConfig().getInt("allow-double-events-ab-minute",60);
        double chance=Math.max(0.0,Math.min(1.0,plugin.getConfig().getDouble("double-event-chance",0.10)));
        if(minutes>=doubleAfter && plugin.getConfigManager().getBoolean("events.double-event.enabled",true)
                && ThreadLocalRandom.current().nextDouble()<chance) {
            List<String> second=availableCategories(minutes,protectedPhase);
            second.remove(selected);
            if(!second.isEmpty() && runCategory(choose(second),target)) survivedEvents++;
        }
    }

    private List<String> availableCategories(long minutes,boolean protectedPhase) {
        List<String> result=new ArrayList<>();
        addCategory(result,"GOOD",1);
        addCategory(result,"NEUTRAL",1);
        if(!protectedPhase) {
            int multiplier=minutes>=plugin.getConfig().getInt("difficulty-scale-minutes",30)?2:1;
            addCategory(result,"BAD",multiplier);
        }
        if(minutes>=plugin.getConfig().getInt("allow-double-events-ab-minute",60)) addCategory(result,"CHAOS",1);
        return result;
    }

    private void addCategory(List<String> result,String category,int multiplier) {
        String path="categories."+category;
        if(!plugin.getConfigManager().getBoolean(path+".enabled",true)) return;
        int weight=Math.max(0,plugin.getConfigManager().getInt(path+".weight",0))*multiplier;
        for(int i=0;i<weight;i++) result.add(category);
    }

    private String choose(List<String> choices) {
        return choices.get(ThreadLocalRandom.current().nextInt(choices.size()));
    }

    private boolean runCategory(String category,Player player) {
        List<Runnable> choices=new ArrayList<>();
        switch(category) {
            case "GOOD" -> {
                add(choices,"loot-rain",()->good.lootRain(player));
                add(choices,"diamond-rain",()->good.diamondRain(player));
                add(choices,"xp-boost",()->good.xpBoost(player));
                add(choices,"trader-visit",()->good.traderVisit(player));
                add(choices,"full-heal",()->good.fullHeal(player));
                add(choices,"regeneration",()->good.regeneration(player));
                add(choices,"absorption",()->good.absorption(player));
                add(choices,"saturation",()->good.saturation(player));
            }
            case "NEUTRAL" -> {
                add(choices,"block-swap",()->blockSwap());
                add(choices,"gravity-flip",()->{
                    player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST,
                            plugin.getConfigManager().getInt("events.gravity-flip.duration",10)*20,1,false,false,true));
                    announce("NEUTRAL","Gravity Flip");
                });
                add(choices,"mob-party",()->mobParty(player));
                add(choices,"inventory-shuffle",()->inventoryShuffle(player));
                add(choices,"giant-slime",()->giantSlime(player));
                add(choices,"low-gravity",()->{
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING,
                            plugin.getConfigManager().getInt("events.low-gravity.duration",15)*20,0,false,false,true));
                    announce("NEUTRAL","Low Gravity");
                });
            }
            case "BAD" -> {
                add(choices,"block-disappear",()->bad.blockDisappear(player));
                add(choices,"mob-attack",()->bad.mobAttack(player));
                add(choices,"creeper-rain",()->bad.creeperRain(player));
                add(choices,"lava-trap",()->bad.lavaTrap(player));
                add(choices,"inventory-steal",()->bad.inventorySteal(player));
                add(choices,"blindness",()->bad.blindness(player));
                add(choices,"island-quake",()->bad.islandQuake(player));
                add(choices,"hunger",()->bad.hunger(player));
                add(choices,"poison",()->bad.poison(player));
                add(choices,"cobweb-trap",()->bad.cobwebTrap(player));
                add(choices,"lightning-warning",()->bad.lightningWarning(player));
            }
            case "CHAOS" -> {
                add(choices,"fire-storm",()->chaos.fireStorm(player));
                add(choices,"enderman-invasion",()->chaos.endermanInvasion(player));
                add(choices,"no-build",()->chaos.noBuild(player));
                add(choices,"wither",()->chaos.wither(player));
                add(choices,"mob-swarm",()->chaos.mobSwarm(player));
            }
            default -> { return false; }
        }
        if(choices.isEmpty()) return false;
        choices.get(ThreadLocalRandom.current().nextInt(choices.size())).run();
        return true;
    }

    private void add(List<Runnable> choices,String id,Runnable action) {
        if(!plugin.getConfigManager().getBoolean("events."+id+".enabled",true)) return;
        int weight=Math.max(0,plugin.getConfigManager().getInt("events."+id+".weight",0));
        for(int i=0;i<Math.min(weight,100);i++) choices.add(action);
    }

    private void blockSwap() {
        List<org.bukkit.block.Block> blocks=new ArrayList<>(plugin.getIslandManager().getIslandBlocks());
        Collections.shuffle(blocks);
        List<Material> safeMaterials=List.of(Material.COBBLESTONE,Material.STONE,Material.DIRT,Material.GRAVEL,
                Material.OAK_PLANKS,Material.SAND,Material.ANDESITE,Material.DIORITE,Material.GRANITE);
        int amount=Math.min(plugin.getConfigManager().getInt("events.block-swap.amount",10),blocks.size());
        int changed=0;
        for(var block:blocks) {
            if(changed>=amount) break;
            if(block.getType().isSolid() && !block.getType().isInteractable()) {
                block.setType(safeMaterials.get(ThreadLocalRandom.current().nextInt(safeMaterials.size())),false);
                changed++;
            }
        }
        announce("NEUTRAL","Block-Swap");
    }

    private void mobParty(Player player) {
        EntityType[] types={EntityType.CHICKEN,EntityType.PIG,EntityType.COW};
        int count=plugin.getIslandManager().countIslandEntities();
        int available=Math.max(0,plugin.getConfig().getInt("events.max-entities",40)-count);
        int amount=Math.min(plugin.getConfigManager().getInt("events.mob-party.amount",5),available);
        for(int i=0;i<amount;i++) {
            var location=plugin.getIslandManager().randomSurfaceLocation();
            if(location==null) break;
            plugin.getIslandManager().trackEventEntity(player.getWorld().spawnEntity(location,types[ThreadLocalRandom.current().nextInt(types.length)]));
        }
        announce("NEUTRAL","Mob-Party");
    }

    private void inventoryShuffle(Player player) {
        var inventory=player.getInventory();
        List<org.bukkit.inventory.ItemStack> items=new ArrayList<>();
        for(int i=0;i<9;i++) items.add(inventory.getItem(i));
        Collections.shuffle(items);
        for(int i=0;i<9;i++) inventory.setItem(i,items.get(i));
        announce("NEUTRAL","Inventar durcheinander");
    }

    private void giantSlime(Player player) {
        if(plugin.getIslandManager().countIslandEntities()>=plugin.getConfig().getInt("events.max-entities",40)) return;
        var location=plugin.getIslandManager().randomSurfaceLocation();
        if(location==null) return;
        var slime=player.getWorld().spawn(location,org.bukkit.entity.Slime.class);
        slime.setSize(Math.max(2,Math.min(8,plugin.getConfigManager().getInt("events.giant-slime.size",6))));
        slime.setCustomName("SkyChaos Giant Slime");
        slime.setCustomNameVisible(true);
        plugin.getIslandManager().trackEventEntity(slime);
        announce("NEUTRAL","Riesenschleim");
    }

    private void announce(String category,String event) {
        Bukkit.broadcastMessage("§e§l"+category+" EVENT §8» §f"+event);
    }

    public void triggerManual() {
        triggerRandom();
    }

    public int getSurvivedEvents() { return survivedEvents; }
    public void resetStats() { survivedEvents=0; }

    public void cleanup() {
        good.cleanup();
        bad.cleanup();
        chaos.cleanup();
        plugin.getIslandManager().cleanupTrackedEntities();
    }
}
