package com.avelar.cdacustomzombies.commands;

import com.avelar.Main;
import com.avelar.cdacustomzombies.ZombieManager;
import com.avelar.cdacustomzombies.util.CC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Map;

public class CZCommand implements CommandExecutor {

    private final Main plugin;
    private static final String PREFIX = "§a§l[ZOMBIE] §r";
    private static final int RBI_BOSS_ID = 9937;
    private static final int RBI_LEND_ID = 9942;

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

        if (args.length >= 1 && args[0].equalsIgnoreCase("rbi")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(PREFIX + "§cApenas jogadores podem receber a RBI.");
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage(PREFIX + "§eUso: §f/" + label + " rbi <boss|lendario>");
                return true;
            }

            String type = args[1].toLowerCase();
            boolean boss = type.equals("boss") || type.equals("b");
            boolean lend = type.equals("lendario") || type.equals("legendario") || type.equals("l");

            if (!boss && !lend) {
                sender.sendMessage(PREFIX + "§eUso: §f/" + label + " rbi <boss|lendario>");
                return true;
            }

            int itemId = boss ? RBI_BOSS_ID : RBI_LEND_ID;

            String base = "TransformItems." + itemId + ".";
            String nameCfg = plugin.getConfig().getString(base + "DisplayNameContains", "");
            String loreCfg = plugin.getConfig().getString(base + "LoreContains", "");

            String defName = boss ? "&5RBI do Boss" : "&6RBI do Lendario";
            String defLore = boss
                    ? "&7Botao direito em um zombie para transformar em &5Boss"
                    : "&7Botao direito em um zombie para transformar em &6Lendario";

            String name = (nameCfg == null || nameCfg.trim().isEmpty()) ? defName : nameCfg;
            String lore = (loreCfg == null || loreCfg.trim().isEmpty()) ? defLore : loreCfg;

            ItemStack it = new ItemStack(itemId, 1);
            try {
                ItemMeta meta = it.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(CC.color(name));
                    meta.setLore(Arrays.asList(CC.color(lore)));
                    it.setItemMeta(meta);
                }
            } catch (Throwable ignored) {}

            try { it.addUnsafeEnchantment(Enchantment.DURABILITY, 1); } catch (Throwable ignored) {}

            Player p = (Player) sender;
            Map<Integer, ItemStack> leftover = p.getInventory().addItem(it);
            if (leftover != null && !leftover.isEmpty()) {
                for (ItemStack drop : leftover.values()) {
                    p.getWorld().dropItemNaturally(p.getLocation(), drop);
                }
            }

            sender.sendMessage(PREFIX + "§aRBI gerada com sucesso.");
            return true;
        }

        sender.sendMessage(PREFIX + "§aCOMANDOS ADMINISTRATIVOS");
        sender.sendMessage("§8" + CC.LINE);
        sender.sendMessage("§f/" + label + " reload §7- Recarrega a config.yml");
        sender.sendMessage("§f/" + label + " terror status §7- Estado da madrugada");
        sender.sendMessage("§f/" + label + " terror on §7- Força ativar agora");
        sender.sendMessage("§f/" + label + " terror off §7- Força desativar agora");
        sender.sendMessage("§f/" + label + " terror auto §7- Volta pro horário");
        sender.sendMessage("§f/" + label + " rbi <boss|lendario> §7- Gera RBI especial");
        sender.sendMessage("§8" + CC.LINE);
        return true;
    }
}
