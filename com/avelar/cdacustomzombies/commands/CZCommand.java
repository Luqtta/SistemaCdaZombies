package com.avelar.cdacustomzombies.commands;

import com.avelar.Main;
import com.avelar.cdacustomzombies.ZombieManager;
import com.avelar.cdacustomzombies.util.CC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CZCommand implements CommandExecutor {

    private final Main plugin;
    private static final String PREFIX = "§a§l[ZOMBIE] §r";

    public CZCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("customzombies.admin")) {
            sender.sendMessage(PREFIX + "§cSem permissão.");
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            plugin.getZombieManager().reloadAll();
            sender.sendMessage(PREFIX + "§aConfig recarregada com sucesso.");
            return true;
        }

        if (args.length >= 1 && (args[0].equalsIgnoreCase("terror") || args[0].equalsIgnoreCase("madrugada"))) {
            ZombieManager zm = plugin.getZombieManager();

            if (args.length == 1 || args[1].equalsIgnoreCase("status")) {
                boolean active = zm.isTerrorActive();
                Boolean forced = zm.getTerrorForced();
                String mode = (forced == null) ? "AUTO" : (forced.booleanValue() ? "FORÇADO ON" : "FORÇADO OFF");
                sender.sendMessage(PREFIX + "§c§lMADRUGADA DO TERROR");
                sender.sendMessage("§7Status: §f" + (active ? "ATIVA" : "INATIVA") + " §8| §7Modo: §f" + mode);
                sender.sendMessage("§8- §f/" + label + " terror on §7(ativa agora)");
                sender.sendMessage("§8- §f/" + label + " terror off §7(desativa agora)");
                sender.sendMessage("§8- §f/" + label + " terror auto §7(volta por horário)");
                sender.sendMessage("§8- §f/" + label + " terror status §7(mostra estado)");
                return true;
            }

            if (args[1].equalsIgnoreCase("on")) {
                zm.forceTerrorNow(true);
                sender.sendMessage(PREFIX + "§aMadrugada forçada §fON§a.");
                return true;
            }

            if (args[1].equalsIgnoreCase("off")) {
                zm.forceTerrorNow(false);
                sender.sendMessage(PREFIX + "§cMadrugada forçada §fOFF§c.");
                return true;
            }

            if (args[1].equalsIgnoreCase("auto")) {
                zm.setTerrorForced(null);
                sender.sendMessage(PREFIX + "§eMadrugada em modo §fAUTO§e (por horário).");
                sender.sendMessage("§7Obs: pode levar até 30s pra atualizar.");
                return true;
            }

            sender.sendMessage(PREFIX + "§eUso: §f/" + label + " terror <on|off|auto|status>");
            return true;
        }

        sender.sendMessage(PREFIX + "§aCOMANDOS ADMINISTRATIVOS");
        sender.sendMessage("§8" + CC.LINE);
        sender.sendMessage("§f/" + label + " reload §7- Recarrega a config.yml");
        sender.sendMessage("§f/" + label + " terror status §7- Estado da madrugada");
        sender.sendMessage("§f/" + label + " terror on §7- Força ativar agora");
        sender.sendMessage("§f/" + label + " terror off §7- Força desativar agora");
        sender.sendMessage("§f/" + label + " terror auto §7- Volta pro horário");
        sender.sendMessage("§8" + CC.LINE);
        return true;
    }
}
