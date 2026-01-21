package com.avelar.cdacustomzombies.listeners;

import com.avelar.Main;
import com.avelar.cdacustomzombies.ZombieManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

public class ZombieCombatListener implements Listener {

    private final Main plugin;
    private final Random random = new Random();

    public ZombieCombatListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onZombieHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof LivingEntity)) return;
        if (!(e.getEntity() instanceof Player)) return;

        LivingEntity damager = (LivingEntity) e.getDamager();
        Player victim = (Player) e.getEntity();

        ZombieManager zm = plugin.getZombieManager();
        String key = zm.getAppliedKey(damager);
        if (key == null) return;

        boolean boss = "BOSS".equalsIgnoreCase(key);
        boolean lend = "CD_ZOMBIE_LENDARIO".equalsIgnoreCase(key);

        int level = zm.getEntityLevel(damager);

        // multiplica dano (Boss/Lend usa config Combat.*.AttackMultiplier)
        // comuns usam Levels.<KEY>.<N>.DamageMultiplier (se existir)
        double mult;
        if (boss) mult = zm.getBossAttackMultiplier();
        else if (lend) mult = zm.getLegendaryAttackMultiplier();
        else mult = zm.getDamageMultiplierFor(key, level);

        if (mult > 0) e.setDamage(e.getDamage() * mult);

        // flamejante: dano extra + fogo
        if (ZombieManager.KEY_FLAMEJANTE.equalsIgnoreCase(key) && zm.isFlameEnabled()) {
            double extra = zm.getFlameExtraDamage(key, level);
            if (extra > 0) e.setDamage(e.getDamage() + extra);

            int fireTicks = zm.getFlameFireTicks(key, level);
            if (fireTicks > 0) {
                boolean apply = zm.isFlameApplyFireAlways();
                if (!apply && zm.getFlameApplyFireChance() > 0) {
                    apply = random.nextDouble() <= zm.getFlameApplyFireChance();
                }
                if (apply) {
                    try { victim.setFireTicks(fireTicks); } catch (Throwable ignored) {}
                }
            }
        }

        if (!boss && !lend) return;

        // knock up
        double knockChance = boss ? zm.getBossKnockUpChance() : zm.getLegendaryKnockUpChance();
        double knockPower  = boss ? zm.getBossKnockUpPower()  : zm.getLegendaryKnockUpPower();
        if (knockChance > 0 && random.nextDouble() < knockChance) {
            zm.knockUp(victim, knockPower);
            try {
                String prefix = zm.getPrefix();
                String mobName = boss ? "§5Boss" : "§6Lendário";
                victim.sendMessage(prefix + "§fO zumbi " + mobName + " §farremessou você no ar!");
            } catch (Throwable ignored) {}
        }

        // blind (config: 1 = Blind I)
        double blindChance = boss ? zm.getBossBlindChance() : zm.getLegendaryBlindChance();
        int blindDur = boss ? zm.getBossBlindDurationTicks() : zm.getLegendaryBlindDurationTicks();
        int blindAmp = boss ? zm.getBossBlindAmplifier() : zm.getLegendaryBlindAmplifier();

        if (blindChance > 0 && blindDur > 0 && random.nextDouble() < blindChance) {
            try {
                int amp = Math.max(0, blindAmp - 1);
                victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blindDur, amp));
                String prefix = zm.getPrefix();
                String mobName = boss ? "§5Boss" : "§6Lendário";
                victim.sendMessage(prefix + "§fO zumbi " + mobName + " §ffoi muito forte e te atordoou!");
            } catch (Throwable ignored) {}
        }
    }
}
