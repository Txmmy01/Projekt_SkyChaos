package de.skychaos.commands;

import de.skychaos.SkyChaos;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.List;

public final class ChallengeCommand implements CommandExecutor, TabCompleter {
    private final SkyChaos plugin;

    public ChallengeCommand(SkyChaos plugin) {
        this.plugin=plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length==0) {
            sender.sendMessage("§bSkyChaos §7- /challenge <build|pos1|pos2|save|start|reset|pause|resume|event|stats>");
            return true;
        }
        String sub=args[0].toLowerCase();
        if(sub.equals("build") || sub.equals("pos1") || sub.equals("pos2") || sub.equals("save")) {
            if(!sender.hasPermission("skychaos.admin")) {
                sender.sendMessage("§cDafür fehlt skychaos.admin.");
                return true;
            }
        } else if(!sender.hasPermission("skychaos.play") && !sender.hasPermission("skychaos.admin")) {
            sender.sendMessage("§cDafür fehlt skychaos.play.");
            return true;
        }
        switch(sub) {
            case "build" -> {
                if(!(sender instanceof Player player)) return playerOnly(sender);
                plugin.enterBuildMode(player);
                sender.sendMessage("§aBuild-Modus aktiviert. Setze die Ecken mit /challenge pos1 und /challenge pos2.");
            }
            case "pos1" -> {
                if(!(sender instanceof Player player)) return playerOnly(sender);
                plugin.getIslandManager().setPos1(player.getLocation());
                sender.sendMessage("§aPosition 1 gesetzt.");
            }
            case "pos2" -> {
                if(!(sender instanceof Player player)) return playerOnly(sender);
                plugin.getIslandManager().setPos2(player.getLocation());
                sender.sendMessage("§aPosition 2 gesetzt.");
            }
            case "save" -> {
                try {
                    if(plugin.getIslandManager().save()) sender.sendMessage("§aInsel gespeichert: plugins/SkyChaos/islands/starter-island.yml");
                    else sender.sendMessage("§cSetze zuerst beide Positionen in der SkyChaos-Welt.");
                } catch(IOException | IllegalArgumentException exception) {
                    sender.sendMessage("§cSpeichern fehlgeschlagen: "+exception.getMessage());
                    plugin.getLogger().warning("Insel konnte nicht gespeichert werden: "+exception.getMessage());
                }
            }
            case "start" -> {
                if(!plugin.getIslandManager().exists()) {
                    sender.sendMessage("§cEs wurde noch keine Insel gespeichert. Nutze /challenge build und /challenge save.");
                    return true;
                }
                try {
                    plugin.startChallenge(true);
                    sender.sendMessage("§aChallenge gestartet.");
                } catch(RuntimeException exception) {
                    sender.sendMessage("§cStart fehlgeschlagen: "+exception.getMessage());
                    plugin.getLogger().severe("Challenge-Start fehlgeschlagen: "+exception.getMessage());
                }
            }
            case "reset" -> {
                if(!plugin.getIslandManager().exists()) {
                    sender.sendMessage("§cEs wurde noch keine Insel gespeichert.");
                    return true;
                }
                try {
                    plugin.resetChallenge();
                    sender.sendMessage("§aChallenge zurückgesetzt.");
                } catch(RuntimeException exception) {
                    sender.sendMessage("§cReset fehlgeschlagen: "+exception.getMessage());
                    plugin.getLogger().severe("Challenge-Reset fehlgeschlagen: "+exception.getMessage());
                }
            }
            case "pause" -> {
                plugin.pauseChallenge();
                sender.sendMessage("§eChallenge pausiert.");
            }
            case "resume" -> {
                plugin.resumeChallenge();
                sender.sendMessage("§aChallenge fortgesetzt.");
            }
            case "event" -> {
                if(!plugin.isActive() || plugin.isPaused()) {
                    sender.sendMessage("§cDie Challenge muss aktiv und nicht pausiert sein.");
                    return true;
                }
                plugin.getEventManager().triggerManual();
                sender.sendMessage("§aTest-Event ausgelöst.");
            }
            case "stats" -> {
                long seconds=plugin.getTimerManager().getElapsedSeconds();
                sender.sendMessage("§b§lSkyChaos Statistik");
                sender.sendMessage("§7Status: "+(plugin.isActive()?(plugin.isPaused()?"§ePausiert":"§aAktiv"):"§cInaktiv"));
                sender.sendMessage("§7Spielzeit: §f"+(seconds/3600)+"h "+((seconds%3600)/60)+"m "+(seconds%60)+"s");
                sender.sendMessage("§7Events überlebt: §f"+plugin.getEventManager().getSurvivedEvents());
                sender.sendMessage("§7Tode: §f"+plugin.getDeaths());
            }
            default -> sender.sendMessage("§cUnbekannter Unterbefehl. Nutze /challenge.");
        }
        return true;
    }

    private boolean playerOnly(CommandSender sender) {
        sender.sendMessage("§cDieser Befehl kann nur im Spiel verwendet werden.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if(args.length==1) return List.of("build","pos1","pos2","save","start","reset","pause","resume","event","stats")
                .stream().filter(value->value.startsWith(args[0].toLowerCase())).toList();
        return List.of();
    }
}
