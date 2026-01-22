package com.avelar.cdacustomzombies.listeners;

import com.arzio.arziolib.api.event.packet.CDBulletHitEvent;
import com.avelar.Main;
import com.avelar.cdacustomzombies.ZombieDefinition;
import com.avelar.cdacustomzombies.ZombieManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Captura hits do Crafting Dead (ArzioLib) e também hits Bukkit (melee/projétil).
 * - Tag last-hit pra resolver killer null
 * - Headshot: se a definição permitir, força kill (damage alto com damager = player)
 */
public class ZombieDamageListener implements Listener {

    private final Main plugin;

    public ZombieDamageListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBulletHit(CDBulletHitEvent e) {
        if (e.getHitType() != CDBulletHitEvent.HitType.ENTITY) return;

        Entity hit = e.getEntityHit();
        if (!(hit instanceof LivingEntity)) return;

        LivingEntity mob = (LivingEntity) hit;

        ZombieManager zm = plugin.getZombieManager();
        String key = zm.getAppliedKey(mob);
        if (key == null) return; // só nossos custom

        Player shooter = e.getPlayer();
        if (shooter != null) {
            ZombieLastHitListener.tagLastHit(plugin, mob, shooter);
        }

        if (!e.isHeadshot()) return;

        ZombieDefinition def = zm.getDef(key);
        if (def == null || !def.isHeadshotAllowed()) return;

        // força kill e tenta garantir killer
        try {
            mob.damage(99999.0, shooter);
        } catch (Throwable t) {
            // fallback
            mob.setHealth(0);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBukkitDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof LivingEntity)) return;

        LivingEntity mob = (LivingEntity) e.getEntity();

        ZombieManager zm = plugin.getZombieManager();
        String key = zm.getAppliedKey(mob);
        if (key == null) return;

        Player damager = resolvePlayerDamager(e.getDamager());
        if (damager != null) {
            ZombieLastHitListener.tagLastHit(plugin, mob, damager);
        }
    }

    private Player resolvePlayerDamager(Entity damager) {
        if (damager instanceof Player) return (Player) damager;
        if (damager instanceof Projectile) {
            // Bukkit 1.6.x: getShooter() costuma retornar LivingEntity (não ProjectileSource)
            try {
                Object shooter = ((Projectile) damager).getShooter();
                if (shooter instanceof Player) return (Player) shooter;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }
}
