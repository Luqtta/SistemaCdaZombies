package com.avelar.cdacustomzombies.listeners;

import com.avelar.Main;
import com.avelar.cdacustomzombies.ZombieDefinition;
import com.avelar.cdacustomzombies.ZombieManager;
import com.avelar.cdacustomzombies.util.CC;
import com.arzio.arziolib.api.util.CDEntityType;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ZombieTransformListener implements Listener {

    private final Main plugin;

    public ZombieTransformListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof LivingEntity)) return;

        Player p = e.getPlayer();
        ItemStack hand;

        try {
            hand = p.getItemInHand(); // 1.6.4
        } catch (Throwable t) {
            return;
        }

        if (hand == null) return;
        if (hand.getTypeId() <= 0) return;

        ZombieManager zm = plugin.getZombieManager();

        ZombieManager.TransformRule rule = zm.getTransformRule(hand.getTypeId());
        if (rule == null) return;

        // valida DisplayNameContains (se configurado)
        if (rule.displayNameContains != null && !rule.displayNameContains.trim().isEmpty()) {
            String need = normalize(rule.displayNameContains);

            String display = "";
            try {
                ItemMeta meta = hand.getItemMeta();
                if (meta != null && meta.hasDisplayName()) display = meta.getDisplayName();
            } catch (Throwable ignored) {}

            if (!normalize(display).contains(need)) return;
        }

        // valida LoreContains (se configurado)
        if (rule.loreContains != null && !rule.loreContains.trim().isEmpty()) {
            String needLore = normalize(rule.loreContains);
            if (!hasLoreContains(hand, needLore)) return;
        }

        LivingEntity target = (LivingEntity) e.getRightClicked();

        // Só bloqueia caso seja player; se não conseguir identificar o tipo do CD, ainda permite
        if (target instanceof Player) return;
        if (!target.hasMetadata(ZombieManager.META_ZOMBIE_KEY)) {
            try {
                if (CDEntityType.getTypeOf(target) == null) {
                    // fallback: permite transformar mesmo sem identificação do CD
                }
            } catch (Throwable ignored) {
                // fallback: permite transformar mesmo sem identificação do CD
            }
        }

        ZombieDefinition toDef = zm.getDef(rule.toZombieKey);
        if (toDef == null) return;

        // ===== RAIO NO PROCESSO =====
        try {
            Location loc = target.getLocation();
            // effect só visual (não dá dano), ideal pra não matar o mob / player
            loc.getWorld().strikeLightningEffect(loc);
        } catch (Throwable ignored) {}

        // remove nível antigo (Boss/Lend não usam level)
        zm.clearEntityLevel(target);

        // aplica definição
        zm.applyDefinition(target, toDef);

        // broadcast
        zm.broadcastTransform(p, toDef.getKey());

        // consome item
        if (rule.consume) {
            int amt = hand.getAmount();
            if (amt <= 1) p.setItemInHand(null);
            else {
                hand.setAmount(amt - 1);
                p.setItemInHand(hand);
            }
        }

        e.setCancelled(true);
    }

    private boolean hasLoreContains(ItemStack hand, String needLowerStripped) {
        if (hand == null) return false;
        try {
            ItemMeta meta = hand.getItemMeta();
            if (meta == null || !meta.hasLore()) return false;
            for (String line : meta.getLore()) {
                if (normalize(line).contains(needLowerStripped)) return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private String normalize(String s) {
        if (s == null) return "";
        String colored = CC.color(s);
        String stripped = CC.strip(colored);
        if (stripped == null) return "";
        return stripped.toLowerCase();
    }
}
