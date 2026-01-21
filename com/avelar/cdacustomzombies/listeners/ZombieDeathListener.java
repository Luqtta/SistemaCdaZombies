package com.avelar.cdacustomzombies.listeners;

import com.avelar.Main;
import com.avelar.cdacustomzombies.ZombieManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

/**
 * Trata morte de zumbis do sistema:
 * - limpa drops/XP
 * - chama ZombieManager.processZombieDeath (ele resolve killer + retry + recompensa)
 */
public class ZombieDeathListener implements Listener {

    private final Main plugin;

    public ZombieDeathListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        if (!(e.getEntity() instanceof LivingEntity)) return;

        LivingEntity mob = e.getEntity();

        ZombieManager zm = plugin.getZombieManager();
        if (zm.getAppliedKey(mob) == null) return;

        // sempre limpa drop/exp (o reward é controlado no manager)
        e.getDrops().clear();
        e.setDroppedExp(0);

        Player killer = e.getEntity().getKiller(); // pode ser null no CD
        zm.processZombieDeath(mob, killer);
    }

    @EventHandler(ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent e) {
        ZombieManager zm = plugin.getZombieManager();
        if (zm == null) return;

        // Se for explosão do zumbi explosivo, não quebra blocos
        if (zm.consumeExplosiveExplosion(e.getLocation())) {
            e.blockList().clear();
            e.setYield(0.0f);
        }
    }
}
