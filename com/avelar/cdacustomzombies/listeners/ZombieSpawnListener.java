package com.avelar.cdacustomzombies.listeners;

import com.arzio.arziolib.api.util.CDEntityType;
import com.avelar.Main;
import com.avelar.cdacustomzombies.ZombieManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.Location;
import org.bukkit.metadata.FixedMetadataValue;

/**
 * Aplica uma definição do sistema em zumbis do Crafting Dead quando eles spawnam normalmente.
 */
public class ZombieSpawnListener implements Listener {

    private final Main plugin;

    public ZombieSpawnListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent e) {
        Entity ent = e.getEntity();
        if (!(ent instanceof LivingEntity)) return;

        ZombieManager zm = plugin.getZombieManager();

        if (!zm.isApplyOnSpawn()) return;
        if (zm.isBlacklistedWorld(ent.getWorld().getName())) return;

        // só tenta em entidades que o ArzioLib reconhece como zumbi do CD
        CDEntityType type;
        try {
            type = CDEntityType.getTypeOf(ent);
        } catch (Throwable t) {
            return;
        }
        if (type == null) return;

        String base = type.name(); // ex: CD_ZOMBIE, CD_ZOMBIE_TANKER...
        String picked = zm.pickForBaseType(base, null);
        if (picked == null) return;

        if (zm.isDebug()) {
            plugin.getLogger().info("[Spawn] base=" + base + " -> key=" + picked);
        }

        // sorteia level (Nv.1-4) para zumbis comuns (se existir regra no config)
        int lvl = zm.rollLevel();
        if (zm.getLevelRule(picked, lvl) != null) {
            zm.setEntityLevel((LivingEntity) ent, lvl);
        } else {
            zm.clearEntityLevel((LivingEntity) ent);
        }

        final LivingEntity le = (LivingEntity) ent;
        final String key = picked;
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    ZombieManager zm2 = plugin.getZombieManager();
                    if (zm2 == null) return;
                    if (le.isDead()) return;
                    zm2.applyDefinition(le, zm2.getDef(key));


                // Madrugada do Terror: chance de criar horda (spawns extras)
                try {
                    if (!le.hasMetadata("cz_horde_spawn") && zm2.isTerrorHordeEnabled()) {
                        double ch = zm2.getTerrorExtraSpawnChance();
                        if (Math.random() <= ch) {
                            int min = zm2.getTerrorExtraMin();
                            int max = zm2.getTerrorExtraMax();
                            int radius = zm2.getTerrorExtraRadius();
                            if (max < min) max = min;

                            int extra = min;
                            if (max > min) extra = min + (int) Math.floor(Math.random() * (max - min + 1));

                            Location baseLoc = le.getLocation();
                            for (int i = 0; i < extra; i++) {
                                Location l2 = baseLoc.clone().add(
                                        (Math.random() * (radius * 2 + 1)) - radius,
                                        0,
                                        (Math.random() * (radius * 2 + 1)) - radius
                                );
                                l2.setY(baseLoc.getY());

                                Entity spawned = baseLoc.getWorld().spawnEntity(l2, le.getType());
                                if (spawned instanceof LivingEntity) {
                                    spawned.setMetadata("cz_horde_spawn", new FixedMetadataValue(plugin, true));
                                }
                            }
                        }
                    }
                } catch (Throwable ignored2) {}
                                } catch (Throwable ignored) {}
            }
        }, 1L);
    }
}