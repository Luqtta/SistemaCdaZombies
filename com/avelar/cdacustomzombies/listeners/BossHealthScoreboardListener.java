package com.avelar.cdacustomzombies.listeners;

import com.avelar.Main;
import com.avelar.cdacustomzombies.ZombieManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class BossHealthScoreboardListener implements Listener {

    private final Main plugin;
    private final ZombieManager zm;


    private final Map<String, Scoreboard> previousBoards = new HashMap<String, Scoreboard>();
    private final Map<String, Scoreboard> activeBoards = new HashMap<String, Scoreboard>();


    private final Map<String, Integer> targetEntityId = new HashMap<String, Integer>();
    private final Map<String, Long> lastHitTime = new HashMap<String, Long>();


    private final Map<String, String> lastLine = new HashMap<String, String>();

    private static final long TIMEOUT_MS = 10000L; 
    private static final double MAX_DISTANCE = 32.0D; 
    private static final double MAX_DISTANCE_SQ = MAX_DISTANCE * MAX_DISTANCE;

    public BossHealthScoreboardListener(Main plugin) {
        this.plugin = plugin;
        this.zm = plugin.getZombieManager();

        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                cleanup();
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

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        clearForPlayer(e.getPlayer());
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


        String title = boss ? "§5§lBOSS" : "§6§lLENDÁRIO";

        String line = "§c❤ Vida §f" + cur;

        setSidebar(p, title, line);
    }

    private void setSidebar(Player p, String title, String line) {
        String name = p.getName();

        ScoreboardManager mgr = Bukkit.getScoreboardManager();
        if (mgr == null) return;


        Scoreboard current = p.getScoreboard();
        Scoreboard active = activeBoards.get(name);

        if (active == null || current != active) {
            previousBoards.put(name, current);
            active = mgr.getNewScoreboard();
            activeBoards.put(name, active);
            try { p.setScoreboard(active); } catch (Throwable ignored) {}
        }

        Objective obj = active.getObjective("czboss");
        if (obj == null) {
            obj = active.registerNewObjective("czboss", "dummy");
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        obj.setDisplayName(title);

        String old = lastLine.get(name);
        if (old != null && !old.equals(line)) {
            try { active.resetScores(Bukkit.getOfflinePlayer(old)); } catch (Throwable ignored) {}
        }

        obj.getScore(Bukkit.getOfflinePlayer(line)).setScore(1);
        lastLine.put(name, line);
    }

    private void cleanup() {
        long now = System.currentTimeMillis();

        Iterator<Map.Entry<String, Long>> it = lastHitTime.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> e = it.next();
            String name = e.getKey();
            long last = e.getValue();

            Player p = Bukkit.getPlayerExact(name);
            if (p == null || !p.isOnline()) {
                clearForName(name, null);
                it.remove();
                continue;
            }


            if ((now - last) > TIMEOUT_MS) {
                clearForName(name, p);
                it.remove();
                continue;
            }

            Integer id = targetEntityId.get(name);
            if (id == null) continue;

            LivingEntity target = findTargetNearby(p, id.intValue());
            if (target == null || target.isDead()) {
                clearForName(name, p);
                it.remove();
                continue;
            }

            if (p.getWorld() != target.getWorld() || p.getLocation().distanceSquared(target.getLocation()) > MAX_DISTANCE_SQ) {
                clearForName(name, p);
                it.remove();
            }
        }
    }

    private LivingEntity findTargetNearby(Player p, int entityId) {
        try {
            for (Entity ent : p.getNearbyEntities(MAX_DISTANCE + 8.0D, MAX_DISTANCE + 8.0D, MAX_DISTANCE + 8.0D)) {
                if (ent != null && ent.getEntityId() == entityId && ent instanceof LivingEntity) {
                    return (LivingEntity) ent;
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private void clearByEntityId(int entityId) {
        Iterator<Map.Entry<String, Integer>> it = targetEntityId.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> e = it.next();
            Integer id = e.getValue();
            if (id != null && id.intValue() == entityId) {
                String name = e.getKey();
                Player p = Bukkit.getPlayerExact(name);
                clearForName(name, p);
                it.remove();
                lastHitTime.remove(name);
            }
        }
    }

    private void clearForPlayer(Player p) {
        if (p == null) return;
        String name = p.getName();
        clearForName(name, p);
        lastHitTime.remove(name);
    }

    private void clearForName(String name, Player p) {
        if (name == null) return;

        targetEntityId.remove(name);
        lastLine.remove(name);

        Scoreboard prev = previousBoards.remove(name);
        activeBoards.remove(name);

        if (p != null) {
            try {
                if (prev != null) {
                    p.setScoreboard(prev);
                } else {
                    ScoreboardManager mgr = Bukkit.getScoreboardManager();
                    if (mgr != null) p.setScoreboard(mgr.getNewScoreboard()); 
                }
            } catch (Throwable ignored) {}
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
