package com.avelar.cdacustomzombies.listeners;

import com.avelar.Main;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

/**
 * Rastreamento do último player que causou dano (pra resolver killer null quando o CD mata via bullets).
 */
public class ZombieLastHitListener implements org.bukkit.event.Listener {

    public static final String META_LAST_HIT_NAME = "cz_last_hit_name";
    public static final String META_LAST_HIT_TIME = "cz_last_hit_time";

    private final Main plugin;

    public ZombieLastHitListener(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Marca o último hit (nome + timestamp). Use isso sempre que um player acertar o zumbi.
     */
    public static void tagLastHit(Main plugin, LivingEntity mob, Player player) {
        if (mob == null || player == null) return;
        mob.setMetadata(META_LAST_HIT_NAME, new FixedMetadataValue(plugin, player.getName()));
        mob.setMetadata(META_LAST_HIT_TIME, new FixedMetadataValue(plugin, System.currentTimeMillis()));
    }

    /**
     * Retorna o nome do last-hit se estiver dentro do limite (default: 10s).
     */
    public static String getLastHitName(LivingEntity mob) {
        return getLastHitName(mob, 10_000L);
    }

    public static String getLastHitName(LivingEntity mob, long maxAgeMs) {
        try {
            if (mob == null) return null;
            if (!mob.hasMetadata(META_LAST_HIT_NAME) || !mob.hasMetadata(META_LAST_HIT_TIME)) return null;

            String name = mob.getMetadata(META_LAST_HIT_NAME).get(0).asString();
            long time = mob.getMetadata(META_LAST_HIT_TIME).get(0).asLong();

            if (name == null || name.isEmpty()) return null;
            if (System.currentTimeMillis() - time > maxAgeMs) return null;

            return name;
        } catch (Throwable t) {
            return null;
        }
    }
}
