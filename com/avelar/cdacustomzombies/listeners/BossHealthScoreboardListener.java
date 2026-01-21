package com.avelar.cdacustomzombies.listeners;

import com.avelar.Main;
import com.avelar.cdacustomzombies.ZombieManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Mostra HP do Boss/Lendário no scoreboard lateral (por jogador).
 */
public class BossHealthScoreboardListener implements Listener {

    private final Main plugin;
    private final ZombieManager zm;

    private final Map<String, Scoreboard> previousBoards = new HashMap<String, Scoreboard>();
    private final Map<String, Integer> targetEntityId = new HashMap<String, Integer>();
    private final Map<String, Long> lastHitTime = new HashMap<String, Long>();
    private final Map<String, String> lastLine = new HashMap<String, String>();

    private static final long TIMEOUT_MS = 10000L;

    public BossHealthScoreboardListener(Main plugin) {
        this.plugin = plugin;
        this.zm = plugin.getZombieManager();

        // limpa scoreboards após timeout
        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                cleanupExpired();
            }
        }, 20L, 20L);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof LivingEntity)) return;

        LivingEntity mob = (LivingEntity) e.getEntity();
        String key = zm.getAppliedKey(mob);
        if (key == null) return;

        boolean boss = "BOSS".equalsIgnoreCase(key);
        boolean lend = "CD_ZOMBIE_LENDARIO".equalsIgnoreCase(key);
        if (!boss && !lend) return;

        Player damager = resolvePlayerDamager(e.getDamager());
        if (damager == null) return;

        showBossHealth(damager, mob, boss);
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        if (!(e.getEntity() instanceof LivingEntity)) return;

        LivingEntity mob = e.getEntity();
        String key = zm.getAppliedKey(mob);
        if (key == null) return;

        boolean boss = "BOSS".equalsIgnoreCase(key);
        boolean lend = "CD_ZOMBIE_LENDARIO".equalsIgnoreCase(key);
        if (!boss && !lend) return;

        clearByEntityId(mob.getEntityId());
    }

    private void showBossHealth(Player p, LivingEntity mob, boolean boss) {
        if (p == null || mob == null) return;

        String name = p.getName();
        targetEntityId.put(name, mob.getEntityId());
        lastHitTime.put(name, System.currentTimeMillis());

        Damageable dmg = (Damageable) mob;
        int cur = (int) Math.ceil(dmg.getHealth());
        int max = (int) Math.ceil(dmg.getMaxHealth());
        if (cur < 0) cur = 0;
        if (max < 1) max = 1;

        String title = boss ? "§5§lBOSS §fHP" : "§6§lLENDÁRIO §fHP";
        String line = "§a" + cur + "§7/§f" + max; // <= 16 chars

        setSidebar(p, title, line);
    }

    private void setSidebar(Player p, String title, String line) {
        String name = p.getName();

        ScoreboardManager mgr = Bukkit.getScoreboardManager();
        if (mgr == null) return;

        Scoreboard board = p.getScoreboard();
        if (!previousBoards.containsKey(name)) {
            previousBoards.put(name, board);
            board = mgr.getNewScoreboard();
            p.setScoreboard(board);
        }

        Objective obj = board.getObjective("czboss");
        if (obj == null) {
            obj = board.registerNewObjective("czboss", "dummy");
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        obj.setDisplayName(title);

        // remove linha anterior
        String old = lastLine.get(name);
        if (old != null) {
            try { board.resetScores(Bukkit.getOfflinePlayer(old)); } catch (Throwable ignored) {}
        }

        obj.getScore(Bukkit.getOfflinePlayer(line)).setScore(1);
        lastLine.put(name, line);
    }

    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Long>> it = lastHitTime.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> e = it.next();
            String name = e.getKey();
            long last = e.getValue();

            Player p = Bukkit.getPlayerExact(name);
            if (p == null || !p.isOnline() || (now - last) > TIMEOUT_MS) {
                clearForPlayer(name, p);
                it.remove();
            }
        }
    }

    private void clearByEntityId(int entityId) {
        for (Map.Entry<String, Integer> e : targetEntityId.entrySet()) {
            if (e.getValue() != null && e.getValue() == entityId) {
                Player p = Bukkit.getPlayerExact(e.getKey());
                clearForPlayer(e.getKey(), p);
            }
        }
    }

    private void clearForPlayer(String name, Player p) {
        if (name == null) return;
        targetEntityId.remove(name);
        lastLine.remove(name);

        Scoreboard prev = previousBoards.remove(name);
        if (p != null && prev != null) {
            try { p.setScoreboard(prev); } catch (Throwable ignored) {}
        }
    }

    private Player resolvePlayerDamager(Entity damager) {
        if (damager instanceof Player) return (Player) damager;
        if (damager instanceof Projectile) {
            try {
                Object shooter = ((Projectile) damager).getShooter();
                if (shooter instanceof Player) return (Player) shooter;
            } catch (Throwable ignored) {}
        }
        return null;
    }
}
