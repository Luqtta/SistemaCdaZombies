package com.avelar.cdacustomzombies;

import com.avelar.Main;
import com.avelar.cdacustomzombies.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class ZombieManager {

    public static final String KEY_EXPLOSIVO = "CD_ZOMBIE_EXPLOSIVO";
    public static final String KEY_FLAMEJANTE = "CD_ZOMBIE_FLAMEJANTE";
    public static final String META_ZOMBIE_KEY = "cz_key";
    public static final String META_HANDLED_DEATH = "cz_handled_death";
    public static final String META_LEVEL = "cz_level";

    private final Main plugin;

    private boolean debug;
    private boolean notifyKillRewardMessage;
    private boolean applyOnSpawn;

    private List<String> blacklistWorlds = new ArrayList<String>();

    private final Map<String, ZombieDefinition> defs = new HashMap<String, ZombieDefinition>();
    private final Map<String, WeightedPicker> spawnPickers = new HashMap<String, WeightedPicker>();
    private final Map<String, Map<String, Integer>> spawnWeightsRaw = new HashMap<String, Map<String, Integer>>();
    private final Map<Integer, TransformRule> transformRules = new HashMap<Integer, TransformRule>();
    private final Map<String, Reward> rewards = new HashMap<String, Reward>();

    private final List<ExplosionMark> explosiveMarks = new ArrayList<ExplosionMark>();

    // Levels (Nv.1-4)
    private final Map<Integer, Double> levelChances = new LinkedHashMap<Integer, Double>();
    private final Map<String, Map<Integer, LevelRule>> levelRules = new HashMap<String, Map<Integer, LevelRule>>();
    private final Random levelRng = new Random();

    // Broadcast (só Death + Transform)
    private String prefix;
    private String bossDeathMsg, legDeathMsg, transformMsg;
    private Sound bossDeathSound, legDeathSound, transformSound;

    // Combat
    private double bossAttackMultiplier, bossKnockUpChance, bossKnockUpPower, bossBlindChance;
    private int bossBlindDurationTicks, bossBlindAmplifier;
    private int bossSpeedAmplifier, bossStrengthAmplifier;

    private double legendaryAttackMultiplier, legendaryKnockUpChance, legendaryKnockUpPower, legendaryBlindChance;

    // Flame zombie
    private boolean flameEnabled;
    private boolean flameApplyFireAlways;
    private double flameApplyFireChance;
    private String flameVisualMode;
    private int flameVisualIntervalTicks;
    private int flameTaskId = -1;
    private final Random flameRandom = new Random();

    // Madrugada do Terror (evento noturno)
    private boolean terrorEnabled;
    private int terrorStartHour;
    private int terrorEndHour;
    private String terrorPrefix;
    private String terrorStartMessage;
    private String terrorEndMessage;
    private int terrorReminderMinutes;
    private String terrorReminderMessage;
    private long terrorLastReminderMillis;
    private Boolean terrorForcedActive;
    private double terrorMoneyMultiplier;
    private double terrorRareChanceMultiplier;
    private double terrorBossWeightMultiplier;
    private double terrorLegendaryWeightMultiplier;

    // Hordas na madrugada
    private boolean terrorHordeEnabled;
    private double terrorExtraSpawnChance;
    private int terrorExtraMin;
    private int terrorExtraMax;
    private int terrorExtraRadius;

    private boolean terrorActive;

    private int legendaryBlindDurationTicks, legendaryBlindAmplifier;
    private int legendarySpeedAmplifier, legendaryStrengthAmplifier;

    public ZombieManager(Main plugin) {
        this.plugin = plugin;
    }

    public void reloadAll() {
        debug = plugin.getConfig().getBoolean("Debug", false);
        blacklistWorlds = plugin.getConfig().getStringList("BlacklistWorlds");

        notifyKillRewardMessage = plugin.getConfig().getBoolean("Notifications.KillRewardMessage", true);
        applyOnSpawn = plugin.getConfig().getBoolean("ApplyOnSpawn", true);

        loadBroadcast();
        loadDefs();
        loadSpawnWeights();
        loadTransformItems();
        loadRewards();
        loadCombat();
        loadLevels();
        loadFlameConfig();
        loadTerror();

        startFlameTask();

        dbg("Reload completo.");
    }

    // -----------------------
    // basics
    // -----------------------
    public boolean isDebug() { return debug; }
    public boolean isApplyOnSpawn() { return applyOnSpawn; }
    public boolean isNotifyKillRewardMessage() { return notifyKillRewardMessage; }

    public boolean isBlacklistedWorld(String world) {
        if (world == null) return false;
        return blacklistWorlds.contains(world);
    }

    private void dbg(String msg) {
        if (!debug) return;
        Bukkit.getConsoleSender().sendMessage("[CDACustomZombies] " + msg);
    }

    // -----------------------
    // broadcast (apenas death/transform)
    // -----------------------
    private void loadBroadcast() {
        prefix = CC.color(plugin.getConfig().getString("Broadcast.Prefix", "&a[ZOMBIE]&r "));

        bossDeathMsg = plugin.getConfig().getString("Broadcast.BossDeath", "&a{player} matou o &5Boss&a!");
        legDeathMsg  = plugin.getConfig().getString("Broadcast.LegendaryDeath", "&a{player} matou o &6Lendario&a");
        transformMsg = plugin.getConfig().getString("Broadcast.Transform", "&f{player} transformou um zumbi em {to}&f");

        bossDeathSound = readSound("Broadcast.Sounds.BossDeath", Sound.WITHER_DEATH);
        legDeathSound  = readSound("Broadcast.Sounds.LegendaryDeath", Sound.LEVEL_UP);
        transformSound = readSound("Broadcast.Sounds.Transform", Sound.AMBIENCE_THUNDER);
    }

    private Sound readSound(String path, Sound def) {
        String s = plugin.getConfig().getString(path, "");
        if (s == null || s.trim().isEmpty()) return def;
        try { return Sound.valueOf(s.trim().toUpperCase()); }
        catch (Throwable ignored) { return def; }
    }

    private void broadcastAll(String rawMsg, Sound sound) {
        if (rawMsg == null || rawMsg.trim().isEmpty()) return;

        Bukkit.broadcastMessage(prefix + CC.color(rawMsg));

        try {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.playSound(p.getLocation(), sound, 1.0F, 1.0F);
            }
        } catch (Throwable ignored) {}
    }

    public void broadcastBossDeath(Player killer) {
        if (killer == null) return;
        String out = bossDeathMsg.replace("{player}", killer.getName());
        broadcastAll(out, bossDeathSound);
    }

    public void broadcastLegendaryDeath(Player killer) {
        if (killer == null) return;
        String out = legDeathMsg.replace("{player}", killer.getName());
        broadcastAll(out, legDeathSound);
    }

    public String getPrefix() {
        return (prefix == null || prefix.trim().isEmpty()) ? "§a[ZOMBIE] " : prefix;
    }

    // assinatura usada pelo TransformListener atual
    public void broadcastTransform(Player player, String toKey) {
        if (player == null) return;
        String out = transformMsg
                .replace("{player}", player.getName())
                .replace("{to}", prettyKeyColored(toKey));
        broadcastAll(out, transformSound);
    }

    public String prettyKeyColored(String key) {
        if (key == null) return CC.color("&eZombie");
        key = key.toUpperCase();
        if (key.equals("BOSS")) return CC.color("&5Boss");
        if (key.equals("CD_ZOMBIE_LENDARIO")) return CC.color("&6Lendario");
        return CC.color("&e" + key);
    }

    // -----------------------
    // defs / weights / transforms / rewards / combat
    // -----------------------
    private void loadDefs() {
        defs.clear();
        ConfigurationSection zs = plugin.getConfig().getConfigurationSection("Zombies");
        if (zs == null) return;

        for (String key : zs.getKeys(false)) {
            String base = "Zombies." + key + ".";
            String name = CC.color(plugin.getConfig().getString(base + "Nome", key));
            double hp = plugin.getConfig().getDouble(base + "VidaBase", 20.0);
            boolean head = plugin.getConfig().getBoolean(base + "HeadShot", false);

            List<String> effects = plugin.getConfig().getStringList(base + "Efeitos");
            double money = plugin.getConfig().getDouble(base + "DinheiroBase", 0.0);
            List<String> itens = plugin.getConfig().getStringList(base + "Itens");

            defs.put(key.toUpperCase(), new ZombieDefinition(
                    key.toUpperCase(),
                    name,
                    hp,
                    head,
                    effects,
                    money,
                    itens
            ));
        }

        dbg("Defs carregadas: " + defs.size());
    }

    private void loadSpawnWeights() {
    spawnPickers.clear();
    spawnWeightsRaw.clear();
    ConfigurationSection sw = plugin.getConfig().getConfigurationSection("SpawnWeights");
    if (sw == null) return;

    for (String baseType : sw.getKeys(false)) {
        ConfigurationSection sec = sw.getConfigurationSection(baseType);
        if (sec == null) continue;

        Map<String, Integer> raw = new HashMap<String, Integer>();
        WeightedPicker picker = new WeightedPicker();
        for (String toKey : sec.getKeys(false)) {
            int w = sec.getInt(toKey, 0);
            if (w <= 0) continue;
            raw.put(toKey.toUpperCase(), w);
            picker.add(toKey.toUpperCase(), w);
        }
        spawnWeightsRaw.put(baseType.toUpperCase(), raw);
        spawnPickers.put(baseType.toUpperCase(), picker);
    }

    dbg("SpawnWeights carregado: " + spawnPickers.size());
}

    private void loadTransformItems() {
        transformRules.clear();
        ConfigurationSection ti = plugin.getConfig().getConfigurationSection("TransformItems");
        if (ti == null) return;

        for (String idStr : ti.getKeys(false)) {
            try {
                int id = Integer.parseInt(idStr);
                String base = "TransformItems." + idStr + ".";
                String to = plugin.getConfig().getString(base + "ToZombie", "");
                boolean consume = plugin.getConfig().getBoolean(base + "Consume", true);
                String contains = plugin.getConfig().getString(base + "DisplayNameContains", "");
                String loreContains = plugin.getConfig().getString(base + "LoreContains", "");
                transformRules.put(id, new TransformRule(id, to.toUpperCase(), consume, contains, loreContains));
            } catch (Throwable ignored) {}
        }

        dbg("TransformItems carregado: " + transformRules.size());
    }

    private void loadRewards() {
        rewards.clear();
        ConfigurationSection rs = plugin.getConfig().getConfigurationSection("Rewards");
        if (rs == null) return;

        for (String key : rs.getKeys(false)) {
            String base = "Rewards." + key + ".";
            double chance = plugin.getConfig().getDouble(base + "Chance", 1.0);
            double money = plugin.getConfig().getDouble(base + "Money", 0.0);
            List<String> items = plugin.getConfig().getStringList(base + "Items");
            rewards.put(key.toUpperCase(), new Reward(chance, money, items));
        }

        dbg("Rewards carregado: " + rewards.size());
    }

    private void loadCombat() {
        bossAttackMultiplier = plugin.getConfig().getDouble("Combat.Boss.AttackMultiplier", 1.0);
        bossKnockUpChance = plugin.getConfig().getDouble("Combat.Boss.KnockUpChance", 0.0);
        bossKnockUpPower = plugin.getConfig().getDouble("Combat.Boss.KnockUpPower", 0.0);
        bossBlindChance = plugin.getConfig().getDouble("Combat.Boss.BlindChance", 0.0);
        bossBlindDurationTicks = plugin.getConfig().getInt("Combat.Boss.BlindDurationTicks", 0);
        bossBlindAmplifier = plugin.getConfig().getInt("Combat.Boss.BlindAmplifier", 1);
        bossSpeedAmplifier = plugin.getConfig().getInt("Combat.Boss.SpeedAmplifier", 0);
        bossStrengthAmplifier = plugin.getConfig().getInt("Combat.Boss.StrengthAmplifier", 0);

        legendaryAttackMultiplier = plugin.getConfig().getDouble("Combat.Legendary.AttackMultiplier", 1.0);
        legendaryKnockUpChance = plugin.getConfig().getDouble("Combat.Legendary.KnockUpChance", 0.0);
        legendaryKnockUpPower = plugin.getConfig().getDouble("Combat.Legendary.KnockUpPower", 0.0);
        legendaryBlindChance = plugin.getConfig().getDouble("Combat.Legendary.BlindChance", 0.0);
        legendaryBlindDurationTicks = plugin.getConfig().getInt("Combat.Legendary.BlindDurationTicks", 0);
        legendaryBlindAmplifier = plugin.getConfig().getInt("Combat.Legendary.BlindAmplifier", 1);
        legendarySpeedAmplifier = plugin.getConfig().getInt("Combat.Legendary.SpeedAmplifier", 0);
        legendaryStrengthAmplifier = plugin.getConfig().getInt("Combat.Legendary.StrengthAmplifier", 0);
    }

    private void loadFlameConfig() {
        flameEnabled = plugin.getConfig().getBoolean("FlameZombie.Enabled", true);
        flameApplyFireAlways = plugin.getConfig().getBoolean("FlameZombie.Hit.ApplyFireAlways", true);
        flameApplyFireChance = plugin.getConfig().getDouble("FlameZombie.Hit.ApplyFireChance", 0.35);
        flameVisualMode = plugin.getConfig().getString("FlameZombie.Visual.Mode", "EFFECT");
        flameVisualIntervalTicks = plugin.getConfig().getInt("FlameZombie.Visual.IntervalTicks", 12);

        if (flameApplyFireChance < 0) flameApplyFireChance = 0;
        if (flameApplyFireChance > 1) flameApplyFireChance = 1;
        if (flameVisualIntervalTicks < 5) flameVisualIntervalTicks = 5;
    }

    private void startFlameTask() {
        if (flameTaskId != -1) {
            try { Bukkit.getScheduler().cancelTask(flameTaskId); } catch (Throwable ignored) {}
            flameTaskId = -1;
        }

        if (!flameEnabled) return;

        flameTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    runFlameVisualTick();
                } catch (Throwable ignored) {}
            }
        }, 20L, flameVisualIntervalTicks);
    }

    private void runFlameVisualTick() {
        String mode = (flameVisualMode == null ? "EFFECT" : flameVisualMode.trim());

        for (org.bukkit.World w : Bukkit.getWorlds()) {
            try {
                for (org.bukkit.entity.LivingEntity le : w.getLivingEntities()) {
                    if (le == null || le.isDead()) continue;
                    String key = getAppliedKey(le);
                    if (!KEY_FLAMEJANTE.equalsIgnoreCase(key)) continue;
                    applyFlameVisual(le, mode);
                }
            } catch (Throwable ignored) {}
        }
    }

    private void applyFlameVisual(org.bukkit.entity.LivingEntity le, String mode) {
        if (le == null) return;

        if ("FIRETICKS".equalsIgnoreCase(mode)) {
            try {
                le.setFireTicks(40); 
            } catch (Throwable ignored) {}
            return;
        }

        if ("PACKET".equalsIgnoreCase(mode)) {
            if (trySendFlameParticlePacket(le.getLocation())) return;
        }

        // EFFECT (fallback)
        try {
            org.bukkit.Location base = le.getLocation();
            for (int i = 0; i < 3; i++) {
                double ox = (flameRandom.nextDouble() - 0.5) * 0.8;
                double oy = flameRandom.nextDouble() * 1.2;
                double oz = (flameRandom.nextDouble() - 0.5) * 0.8;
                org.bukkit.Location l2 = base.clone().add(ox, oy, oz);
                base.getWorld().playEffect(l2, org.bukkit.Effect.MOBSPAWNER_FLAMES, 0);
            }
        } catch (Throwable ignored) {}
    }

    private boolean trySendFlameParticlePacket(org.bukkit.Location loc) {
        try {
            Class<?> packetClass = Class.forName("net.minecraft.server.v1_6_R3.Packet63WorldParticles");
            Object packet = packetClass.getConstructor().newInstance();

            setField(packet, "a", "flame");
            setField(packet, "b", (float) loc.getX());
            setField(packet, "c", (float) loc.getY());
            setField(packet, "d", (float) loc.getZ());
            setField(packet, "e", 0.4f);
            setField(packet, "f", 0.6f);
            setField(packet, "g", 0.4f);
            setField(packet, "h", 0.05f);
            setField(packet, "i", 10);

            for (org.bukkit.entity.Player p : loc.getWorld().getPlayers()) {
                try {
                    Object craftPlayer = p;
                    java.lang.reflect.Method getHandle = craftPlayer.getClass().getMethod("getHandle");
                    Object handle = getHandle.invoke(craftPlayer);
                    Object conn = handle.getClass().getField("playerConnection").get(handle);
                    java.lang.reflect.Method sendPacket = conn.getClass().getMethod("sendPacket", Class.forName("net.minecraft.server.v1_6_R3.Packet"));
                    sendPacket.invoke(conn, packet);
                } catch (Throwable ignored) {}
            }
            return true;
        } catch (Throwable ignored) {}

        return false;
    }

    private void setField(Object obj, String field, Object value) {
        try {
            java.lang.reflect.Field f = obj.getClass().getDeclaredField(field);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (Throwable ignored) {}
    }


	
	private void loadTerror() {
	    terrorEnabled = plugin.getConfig().getBoolean("Madrugada.Enabled", true);
	    terrorStartHour = plugin.getConfig().getInt("Madrugada.StartHour", 0);
	    terrorEndHour = plugin.getConfig().getInt("Madrugada.EndHour", 6);
	    terrorPrefix = plugin.getConfig().getString("Madrugada.Prefix", "&c[MADRUGADA]&r ");
	    terrorStartMessage = plugin.getConfig().getString("Madrugada.StartBroadcast", "&c[MADRUGADA]&f A Madrugada do Terror começou!");
	    terrorEndMessage = plugin.getConfig().getString("Madrugada.EndBroadcast", "&c[MADRUGADA]&f A Madrugada do Terror terminou!");
	
	    terrorReminderMinutes = plugin.getConfig().getInt("Madrugada.ReminderMinutes", 5);
	    terrorReminderMessage = plugin.getConfig().getString("Madrugada.ReminderBroadcast", "&fA &cMadrugada do Terror&f está ativa! &7(00:00 - 06:00)");
	
	
	    terrorForcedActive = null;
	    terrorLastReminderMillis = 0L;
	
	    terrorMoneyMultiplier = plugin.getConfig().getDouble("Madrugada.MoneyMultiplier", 1.15);
	    terrorRareChanceMultiplier = plugin.getConfig().getDouble("Madrugada.RareDropChanceMultiplier", 1.25);
	    terrorBossWeightMultiplier = plugin.getConfig().getDouble("Madrugada.BossWeightMultiplier", 1.5);
	    terrorLegendaryWeightMultiplier = plugin.getConfig().getDouble("Madrugada.LegendaryWeightMultiplier", 3.5);
	
	    terrorHordeEnabled = plugin.getConfig().getBoolean("Madrugada.Horde.Enabled", true);
	    terrorExtraSpawnChance = plugin.getConfig().getDouble("Madrugada.Horde.ExtraSpawnChance", 0.35);
	    terrorExtraMin = plugin.getConfig().getInt("Madrugada.Horde.MinExtra", 2);
	    terrorExtraMax = plugin.getConfig().getInt("Madrugada.Horde.MaxExtra", 5);
	    terrorExtraRadius = plugin.getConfig().getInt("Madrugada.Horde.Radius", 8);
	
	    // sanity
	    if (terrorMoneyMultiplier <= 0) terrorMoneyMultiplier = 1.0;
	    if (terrorRareChanceMultiplier <= 0) terrorRareChanceMultiplier = 1.0;
	    if (terrorBossWeightMultiplier <= 0) terrorBossWeightMultiplier = 1.0;
	    if (terrorLegendaryWeightMultiplier <= 0) terrorLegendaryWeightMultiplier = 1.0;
	
	    if (terrorReminderMinutes < 1) terrorReminderMinutes = 30;
	    if (terrorReminderMessage == null) terrorReminderMessage = "";
	
	    if (terrorExtraMin < 0) terrorExtraMin = 0;
	    if (terrorExtraMax < terrorExtraMin) terrorExtraMax = terrorExtraMin;
	    if (terrorExtraRadius < 1) terrorExtraRadius = 1;
	}
	
	public boolean isTerrorActive() {
	    return terrorEnabled && terrorActive;
	}
	
	public double getTerrorMoneyMultiplier() {
	    return isTerrorActive() ? terrorMoneyMultiplier : 1.0;
	}
	
	public double getTerrorRareChanceMultiplier() {
	    return isTerrorActive() ? terrorRareChanceMultiplier : 1.0;
	}
	
	public double getTerrorBossWeightMultiplier() {
	    return isTerrorActive() ? terrorBossWeightMultiplier : 1.0;
	}
	
	public double getTerrorLegendaryWeightMultiplier() {
	    return isTerrorActive() ? terrorLegendaryWeightMultiplier : 1.0;
	}
	
	public boolean isTerrorHordeEnabled() {
	    return isTerrorActive() && terrorHordeEnabled;
	}
	
	public double getTerrorExtraSpawnChance() { return terrorExtraSpawnChance; }
	public int getTerrorExtraMin() { return terrorExtraMin; }
	public int getTerrorExtraMax() { return terrorExtraMax; }
	public int getTerrorExtraRadius() { return terrorExtraRadius; }

	public void tickTerrorClock() {
	    if (!terrorEnabled) {
	        terrorForcedActive = null;
	        terrorActive = false;
	        terrorLastReminderMillis = 0L;
	        return;
	    }
	
	    java.util.Calendar cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("America/Sao_Paulo"));
	    int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
	
	    boolean inWindow;
	    if (terrorStartHour == terrorEndHour) {
	        inWindow = false;
	    } else if (terrorStartHour < terrorEndHour) {
	        inWindow = (hour >= terrorStartHour && hour < terrorEndHour);
	    } else {
	        inWindow = (hour >= terrorStartHour || hour < terrorEndHour);
	    }
	
	    // se foi forçado manualmente, ignora relógio
	    boolean desired = (terrorForcedActive != null) ? terrorForcedActive.booleanValue() : inWindow;
	
        if (desired != terrorActive) {
            terrorActive = desired;
            terrorLastReminderMillis = System.currentTimeMillis();

            String msg = terrorActive ? terrorStartMessage : terrorEndMessage;
            if (msg != null && msg.length() > 0) {
                String cleaned = stripMadrugadaPrefix(msg);
                broadcastMultiline(CC.color(cleaned));
                playTerrorSoundAll();
            }
        }
	
	    // lembrete a cada X minutos enquanto ativo
	    if (terrorActive && terrorReminderMinutes > 0 && terrorReminderMessage != null && terrorReminderMessage.trim().length() > 0) {
	        long now = System.currentTimeMillis();
	        long interval = terrorReminderMinutes * 60L * 1000L;
	        if (terrorLastReminderMillis == 0L) terrorLastReminderMillis = now;
	
            if (now - terrorLastReminderMillis >= interval) {
                terrorLastReminderMillis = now;
                // reminder sem prefix (removemos o prefixo para ficar apenas a mensagem)
                broadcastMultiline(CC.color(stripMadrugadaPrefix(terrorReminderMessage)));
                playTerrorSoundAll();
            }
	    }
	}
	
	// força a madrugada (para testes). null = modo automático por horário
	public void setTerrorForced(Boolean forcedActive) {
	    this.terrorForcedActive = forcedActive;
	}
	
	public Boolean getTerrorForced() {
	    return terrorForcedActive;
	}
	
	// ===== Compat helpers for commands (/cz terror ...) =====
	// Mantém nomes curtos para evitar quebrar comandos/patches.
	public void forceTerror(boolean active) {
	    forceTerrorNow(active);
	}
	
	public void clearForceTerror() {
	    // null = modo automático por horário
	    setTerrorForced(null);
	    // Recalcula pelo relógio e anuncia se necessário
	    tickTerrorClock();
	}
	
	public boolean isTerrorForced() {
	    return terrorForcedActive != null;
	}
	
	// força ligar/desligar e já anuncia agora
	public void forceTerrorNow(boolean active) {
	    if (!terrorEnabled) {
	        terrorEnabled = true;
	    }
	    this.terrorForcedActive = active;
	
        if (active != terrorActive) {
            terrorActive = active;
            terrorLastReminderMillis = System.currentTimeMillis();

            String msg = terrorActive ? terrorStartMessage : terrorEndMessage;
            if (msg != null && msg.length() > 0) {
                // broadcast multiline sem prefix e também toca o som (comportamento igual ao relógio)
                broadcastMultiline(CC.color(stripMadrugadaPrefix(msg)));
                playTerrorSoundAll();
            }
        }
	}
	
	private void loadLevels() {
	    levelChances.clear();
	    levelRules.clear();
	
	    // chances (opcional). Se não existir, default 1..4: 0.55/0.28/0.13/0.04
	    org.bukkit.configuration.ConfigurationSection lc = plugin.getConfig().getConfigurationSection("LevelChances");
	    if (lc != null) {
	        for (String k : lc.getKeys(false)) {
	            try {
	                int lvl = Integer.parseInt(k);
	                double ch = plugin.getConfig().getDouble("LevelChances." + k, 0.0);
	                if (lvl > 0 && ch > 0) levelChances.put(lvl, ch);
	            } catch (Throwable ignored) {}
	        }
	    }
	    if (levelChances.isEmpty()) {
	        levelChances.put(1, 0.55);
	        levelChances.put(2, 0.28);
	        levelChances.put(3, 0.13);
	        levelChances.put(4, 0.04);
	    }
	
	    // regras
	    org.bukkit.configuration.ConfigurationSection ls = plugin.getConfig().getConfigurationSection("Levels");
	    if (ls != null) {
	        for (String key : ls.getKeys(false)) {
	            org.bukkit.configuration.ConfigurationSection secKey = ls.getConfigurationSection(key);
	            if (secKey == null) continue;
	
	            Map<Integer, LevelRule> map = new HashMap<Integer, LevelRule>();
	            for (String lvlStr : secKey.getKeys(false)) {
	                try {
	                    int lvl = Integer.parseInt(lvlStr);
	                    String base = "Levels." + key + "." + lvlStr + ".";
	                    double hpMul = plugin.getConfig().getDouble(base + "HealthMultiplier", 1.0);
	                    double dmgMul = plugin.getConfig().getDouble(base + "DamageMultiplier", 1.0);
	                    int spBonus = plugin.getConfig().getInt(base + "SpeedBonusAmplifier", 0);
                    float explosionPower = (float) plugin.getConfig().getDouble(base + "ExplosionPower", -1.0);
                    int fireTicks = plugin.getConfig().getInt(base + "FireTicks", -1);
                    double extraDamage = plugin.getConfig().getDouble(base + "ExtraDamage", -1.0);
                    map.put(lvl, new LevelRule(hpMul, dmgMul, spBonus, explosionPower, fireTicks, extraDamage));
	                } catch (Throwable ignored) {}
	            }
	            if (!map.isEmpty()) levelRules.put(key.toUpperCase(), map);
	        }
	    }
	
	    dbg("Levels carregado: chances=" + levelChances.size() + " rules=" + levelRules.size());
	}
	
	public int rollLevel() {
	    // roleta por chance (soma não precisa ser 1.0)
	    double total = 0.0;
	    for (double v : levelChances.values()) total += v;
	    if (total <= 0) return 1;
	
	    double r = levelRng.nextDouble() * total;
	    double acc = 0.0;
	    for (Map.Entry<Integer, Double> e : levelChances.entrySet()) {
	        acc += e.getValue();
	        if (r <= acc) return e.getKey();
	    }
	    return 1;
	}
	
	public int getEntityLevel(LivingEntity ent) {
	    if (ent == null) return 1;
	    if (!ent.hasMetadata(META_LEVEL)) return 1;
	    try {
	        return ent.getMetadata(META_LEVEL).get(0).asInt();
	    } catch (Throwable ignored) { return 1; }
	}
	
	public void setEntityLevel(LivingEntity ent, int level) {
	    if (ent == null) return;
	    if (level <= 0) level = 1;
	    ent.setMetadata(META_LEVEL, new FixedMetadataValue(plugin, level));
	}
	
	public void clearEntityLevel(LivingEntity ent) {
	    if (ent == null) return;
	    try { ent.removeMetadata(META_LEVEL, plugin); } catch (Throwable ignored) {}
	}
	
	public LevelRule getLevelRule(String key, int level) {
	    if (key == null) return null;
	    Map<Integer, LevelRule> map = levelRules.get(key.toUpperCase());
	    if (map == null) return null;
	    return map.get(level);
	}
	
	public double getDamageMultiplierFor(String key, int level) {
	    LevelRule r = getLevelRule(key, level);
	    if (r == null) return 1.0;
	    if (r.damageMultiplier <= 0) return 1.0;
	    return r.damageMultiplier;
	}

    public boolean isFlameEnabled() { return flameEnabled; }
    public boolean isFlameApplyFireAlways() { return flameApplyFireAlways; }
    public double getFlameApplyFireChance() { return flameApplyFireChance; }
    public int getFlameVisualIntervalTicks() { return flameVisualIntervalTicks; }
    public String getFlameVisualMode() { return flameVisualMode; }

    /**
     * Retorna o power da explosão para o zumbi explosivo, baseado no config Levels.<key>.<level>.ExplosionPower.
     * Se não existir, retorna um default seguro (2.2f + (level-1)*0.6f).
     */
    public float getExplosionPower(String key, int level) {
        if (key == null || level < 1) return 2.2f;
        LevelRule lr = getLevelRule(key, level);
        if (lr != null && lr.explosionPower > 0) return lr.explosionPower;
        return 2.2f + (level - 1) * 0.6f;
    }

    public int getFlameFireTicks(String key, int level) {
        if (key == null || level < 1) return 60;
        LevelRule lr = getLevelRule(key, level);
        if (lr != null && lr.fireTicks > 0) return lr.fireTicks;
        int[] fallback = new int[] {60, 80, 100, 120};
        int idx = Math.min(Math.max(level, 1), 4) - 1;
        return fallback[idx];
    }

    public double getFlameExtraDamage(String key, int level) {
        if (key == null || level < 1) return 0.5;
        LevelRule lr = getLevelRule(key, level);
        if (lr != null && lr.extraDamage >= 0) return lr.extraDamage;
        double[] fallback = new double[] {0.5, 1.0, 1.5, 2.0};
        int idx = Math.min(Math.max(level, 1), 4) - 1;
        return fallback[idx];
    }

    /**
     * Helper para pegar o level do entity (cz_level), retorna 1..4, fallback 1.
     */
    public int getLevelFromEntity(LivingEntity e) {
        int lvl = getEntityLevel(e);
        if (lvl < 1 || lvl > 4) return 1;
        return lvl;
    }

    // getters pro CombatListener
    public double getBossAttackMultiplier() { return bossAttackMultiplier; }
    public double getBossKnockUpChance() { return bossKnockUpChance; }
    public double getBossKnockUpPower() { return bossKnockUpPower; }
    public double getBossBlindChance() { return bossBlindChance; }
    public int getBossBlindDurationTicks() { return bossBlindDurationTicks; }
    public int getBossBlindAmplifier() { return bossBlindAmplifier; }

    public double getLegendaryAttackMultiplier() { return legendaryAttackMultiplier; }
    public double getLegendaryKnockUpChance() { return legendaryKnockUpChance; }
    public double getLegendaryKnockUpPower() { return legendaryKnockUpPower; }
    public double getLegendaryBlindChance() { return legendaryBlindChance; }
    public int getLegendaryBlindDurationTicks() { return legendaryBlindDurationTicks; }
    public int getLegendaryBlindAmplifier() { return legendaryBlindAmplifier; }

    // -----------------------
    // lookups
    // -----------------------
    public ZombieDefinition getDef(String key) {
        if (key == null) return null;
        return defs.get(key.toUpperCase());
    }

    public String pickForBaseType(String baseType, String fallback) {
    if (baseType == null) return fallback;

    String bt = baseType.toUpperCase();

    // durante a Madrugada do Terror, a gente aumenta o peso de boss/lendário sem precisar recarregar config
    if (isTerrorActive()) {
        Map<String, Integer> raw = spawnWeightsRaw.get(bt);
        if (raw != null && !raw.isEmpty()) {
            WeightedPicker tmp = new WeightedPicker();
            for (Map.Entry<String, Integer> e : raw.entrySet()) {
                String k = e.getKey();
                int w = e.getValue() == null ? 0 : e.getValue();
                if (w <= 0) continue;

                double mult = 1.0;
                if ("BOSS".equalsIgnoreCase(k)) mult = getTerrorBossWeightMultiplier();
                else if ("CD_ZOMBIE_LENDARIO".equalsIgnoreCase(k)) mult = getTerrorLegendaryWeightMultiplier();

                int ww = (int) Math.round(w * mult);
                if (ww <= 0) ww = 1;
                tmp.add(k, ww);
            }
            String picked = tmp.pickOrNull();
            return picked == null ? fallback : picked;
        }
    }

    WeightedPicker picker = spawnPickers.get(bt);
    if (picker == null) return fallback;
    String picked = picker.pickOrNull();
    return picked == null ? fallback : picked;
}

    public TransformRule getTransformRule(int itemId) {
        return transformRules.get(itemId);
    }

    public Reward getReward(String key) {
        if (key == null) return null;
        return rewards.get(key.toUpperCase());
    }

    public String getAppliedKey(LivingEntity ent) {
        if (ent == null) return null;
        if (!ent.hasMetadata(META_ZOMBIE_KEY)) return null;
        try {
            return ent.getMetadata(META_ZOMBIE_KEY).get(0).asString();
        } catch (Throwable ignored) { return null; }
    }

    // -----------------------
    // apply definition
    // -----------------------
    public void applyDefinition(LivingEntity ent, ZombieDefinition def) {
        if (ent == null || def == null) return;

        ent.setMetadata(META_ZOMBIE_KEY, new FixedMetadataValue(plugin, def.getKey()));

        // level (Nv.1-4) - só aparece no nome se existir regra pra essa key
        int level = getEntityLevel(ent);
        LevelRule lr = getLevelRule(def.getKey(), level);

        // nome (com nível)
        try {
            String nm = def.getName();
            if (lr != null) {
                nm = nm + CC.color(" &7[Nv." + level + "]");
            }
            ent.setCustomName(nm);
            ent.setCustomNameVisible(true);
        } catch (Throwable ignored) {}

        // TNT na cabeça apenas para o explosivo
        if (KEY_EXPLOSIVO.equalsIgnoreCase(def.getKey())) {
            try {
                if (ent.getEquipment() != null) {
                    ent.getEquipment().setHelmet(new ItemStack(46)); // TNT
                }
            } catch (Throwable ignored) {}
        }

        // vida (com multiplicador por nível)
        double hp = def.getBaseHealth();
        if (lr != null && lr.healthMultiplier > 0) {
            hp = hp * lr.healthMultiplier;
        }
        if (hp < 1) hp = 1;
        try {
            ent.setMaxHealth(hp);
            ent.setHealth(hp);
        } catch (Throwable ignored) {}

        // limpa efeitos
        try {
            for (PotionEffect pe : ent.getActivePotionEffects()) {
                ent.removePotionEffect(pe.getType());
            }
        } catch (Throwable ignored) {}

        // efeitos do config Zombies.<key>.Efeitos
        int baseSpeedLvl = 0; // level 1..n (não amplifier)
        for (String s : def.getEffects()) {
            try {
                if (s == null) continue;
                String[] split = s.split(",");
                if (split.length < 2) continue;

                int id = Integer.parseInt(split[0].trim());
                int lvl = Integer.parseInt(split[1].trim());
                if (lvl <= 0) lvl = 1;

                PotionEffectType type = PotionEffectType.getById(id);
                if (type == null) continue;

                // speed (id=1) a gente ajusta no final por causa do bônus de level
                if (id == 1) {
                    baseSpeedLvl = Math.max(baseSpeedLvl, lvl);
                    continue;
                }

                ent.addPotionEffect(new PotionEffect(type, 99999999, lvl - 1));
            } catch (Throwable ignored) {}
        }

        // aplica SPEED ajustado (base + bônus do nível, se existir)
        try {
            int bonus = (lr != null ? lr.speedBonusAmplifier : 0);
            int finalSpeedLvl = baseSpeedLvl;

            if (bonus > 0) {
                if (finalSpeedLvl <= 0) finalSpeedLvl = 1;
                finalSpeedLvl = finalSpeedLvl + bonus;
            }

            if (finalSpeedLvl > 0) {
                ent.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 99999999, Math.max(0, finalSpeedLvl - 1)));
            }
        } catch (Throwable ignored) {}

        // buffs extras do Combat (Speed/Strength) só para Boss/Lend
        String key = def.getKey();
        boolean boss = "BOSS".equalsIgnoreCase(key);
        boolean lend = "CD_ZOMBIE_LENDARIO".equalsIgnoreCase(key);

        if (boss || lend) {
            int speedAmp = boss ? bossSpeedAmplifier : legendarySpeedAmplifier;
            int strAmp = boss ? bossStrengthAmplifier : legendaryStrengthAmplifier;

            try {
                if (speedAmp > 0) {
                    ent.addPotionEffect(new PotionEffect(
                            PotionEffectType.SPEED, 99999999, Math.max(0, speedAmp - 1)
                    ));
                }
            } catch (Throwable ignored) {}

            try {
                if (strAmp > 0) {
                    ent.addPotionEffect(new PotionEffect(
                            PotionEffectType.INCREASE_DAMAGE, 99999999, Math.max(0, strAmp - 1)
                    ));
                }
            } catch (Throwable ignored) {}
        }
    }

    // -----------------------
    // IMPORTANT: handled + retry
    // -----------------------
    public void processZombieDeath(LivingEntity mob, Player killerParam) {
        if (mob == null) return;

        // só processa se for zumbi do sistema
        if (!mob.hasMetadata(META_ZOMBIE_KEY)) return;

        // se já foi processado, não duplica
        if (mob.hasMetadata(META_HANDLED_DEATH)) return;

        String key = getAppliedKey(mob);
        if (key == null) return;

        ZombieDefinition def = getDef(key);
        if (def == null) return;

        // resolve killer (bukkit + fallback last_hit)
        Player killer = killerParam;
        if (killer == null) killer = mob.getKiller();

        if (killer == null) {
            try {
                String name = com.avelar.cdacustomzombies.listeners.ZombieLastHitListener.getLastHitName(mob);
                if (name != null) killer = Bukkit.getPlayerExact(name);
            } catch (Throwable ignored) {}
        }

        // se ainda for null, NÃO marca handled
        // mas tenta de novo 2 ticks depois (muitas vezes o CD “atualiza” depois)
        if (killer == null) {
            dbg("Death sem killer (vai tentar novamente em 2 ticks). key=" + key);

            final LivingEntity mobFinal = mob;
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    try {
                        if (mobFinal == null) return;
                        if (mobFinal.hasMetadata(META_HANDLED_DEATH)) return;

                        Player k2 = mobFinal.getKiller();
                        if (k2 == null) {
                            String name2 = com.avelar.cdacustomzombies.listeners.ZombieLastHitListener.getLastHitName(mobFinal);
                            if (name2 != null) k2 = Bukkit.getPlayerExact(name2);
                        }

                        if (k2 != null) processZombieDeath(mobFinal, k2);
                    } catch (Throwable ignored) {}
                }
            }, 2L);

            return;
        }

        mob.setMetadata(META_HANDLED_DEATH, new FixedMetadataValue(plugin, true));

        // explosão do zumbi explosivo (sem fogo e sem quebrar blocos)
        if (KEY_EXPLOSIVO.equalsIgnoreCase(key)) {
            int lvl = getLevelFromEntity(mob);
            float power = getExplosionPower(key, lvl);
            try {
                createExplosionSafe(mob.getLocation(), power);
            } catch (Throwable ignored) {}
        }

        boolean boss = "BOSS".equalsIgnoreCase(key);
        boolean lend = "CD_ZOMBIE_LENDARIO".equalsIgnoreCase(key);

        // 1) paga dinheiro base (sempre)
        double totalMoney = 0.0;
        if (plugin.getEconomy() != null && def.getBaseMoney() > 0) {
            try {
                double moneyMult = getTerrorMoneyMultiplier();
                double pay = def.getBaseMoney() * moneyMult;
                plugin.getEconomy().depositPlayer(killer.getName(), pay);
                totalMoney += pay;
            } catch (Throwable t) {
                dbg("ERRO deposit baseMoney: " + t.getClass().getSimpleName() + " " + t.getMessage());
            }
        }

        // 2) itens base (por item com chance opcional)
        int baseGiven = giveItemsWithChance(killer, def.getItems(), getTerrorRareChanceMultiplier(), mob.getLocation());

        // 3) reward bonus só p/ boss/lend (Chance controla pacote Money+Items)
        int bonusGiven = 0;
        if (boss || lend) {
            Reward reward = getReward(key);
            if (reward != null && Math.random() <= reward.chance) {
                if (plugin.getEconomy() != null && reward.money > 0) {
                    try {
                        double moneyMult2 = getTerrorMoneyMultiplier();
                        double pay2 = reward.money * moneyMult2;
                        plugin.getEconomy().depositPlayer(killer.getName(), pay2);
                        totalMoney += pay2;
                    } catch (Throwable t) {
                        dbg("ERRO deposit rewardMoney: " + t.getClass().getSimpleName() + " " + t.getMessage());
                    }
                }
                bonusGiven = giveItemsSimple(killer, reward.items, key, mob.getLocation()); // reward items = 100% (sem chance)
            }

            // broadcast death (só aviso)
            if (boss) broadcastBossDeath(killer);
            else broadcastLegendaryDeath(killer);
        }

        // msg pro killer (se habilitado)
        if (notifyKillRewardMessage) {
            if (totalMoney > 0) {
                killer.sendMessage(CC.color(prefix + "&fVoce ganhou &a" + formatMoney(totalMoney) + " &fpor matar um zumbi."));
            }
        }

        dbg("Death OK: key=" + key + " killer=" + killer.getName()
                + " money=" + totalMoney + " itens=" + baseGiven + "+" + bonusGiven);
    }

    private String formatMoney(double value) {
        if (value == (long) value) return String.valueOf((long) value);
        return String.format(Locale.US, "%.2f", value).replace(".", ",");
    }

    // Itens BASE com chance:
    // "ID:DATA QTD CHANCE" ou "ID QTD CHANCE"
    // Se CHANCE não existir -> 100%
    // Itens base: "ID:DATA QTD CHANCE" ou "ID QTD CHANCE"
    // Se CHANCE não existir -> 100%
    private int giveItemsWithChance(Player p, List<String> items) {
        return giveItemsWithChance(p, items, 1.0);
    }

    private int giveItemsWithChance(Player p, List<String> items, double chanceMultiplier) {
        return giveItemsWithChance(p, items, chanceMultiplier, null);
    }

    private int giveItemsWithChance(Player p, List<String> items, double chanceMultiplier, Location dropLoc) {
        if (p == null || items == null || items.isEmpty()) return 0;
        int given = 0;
        if (chanceMultiplier <= 0) chanceMultiplier = 1.0;

        for (String s : items) {
            try {
                if (s == null) continue;
                s = s.trim();
                if (s.isEmpty()) continue;

                String[] split = s.split(" ");
                if (split.length < 2) continue;

                String idData = split[0];
                int amount = Integer.parseInt(split[1]);

                double chance = 100.0;
                if (split.length >= 3) {
                    chance = Double.parseDouble(split[2]);
                }

                // aumenta chance durante evento (cap 100)
                chance = Math.min(100.0, chance * chanceMultiplier);

                if (amount <= 0) continue;
                if (chance < 100.0) {
                    double roll = Math.random() * 100.0;
                    if (roll > chance) continue;
                }

                int id;
                byte data = 0;

                if (idData.contains(":")) {
                    String[] p2 = idData.split(":");
                    id = Integer.parseInt(p2[0]);
                    data = (byte) Integer.parseInt(p2[1]);
                } else {
                    id = Integer.parseInt(idData);
                }

                giveItemOrDrop(p, new ItemStack(id, amount, (short) 0, data), dropLoc);
                given++;
            } catch (Throwable ignored) {}
        }

        return given;
    }

        // Itens de reward bonus (100% quando reward passa)
        
    // Itens de reward bonus (100% quando reward passa)
    private int giveItemsSimple(Player p, List<String> items, String zombieKey) {
        return giveItemsSimple(p, items, zombieKey, null);
    }

    private int giveItemsSimple(Player p, List<String> items, String zombieKey, Location dropLoc) {
        if (p == null || items == null || items.isEmpty()) return 0;
        int given = 0;

        for (String s : items) {
            try {
                if (s == null) continue;
                s = s.trim();
                if (s.isEmpty()) continue;

                String[] split = s.split(" ");
                if (split.length < 2) continue;

                String idData = split[0];
                int amount = Integer.parseInt(split[1]);

                int id;
                byte data = 0;

                if (idData.contains(":")) {
                    String[] p2 = idData.split(":");
                    id = Integer.parseInt(p2[0]);
                    data = (byte) Integer.parseInt(p2[1]);
                } else {
                    id = Integer.parseInt(idData);
                }

                if (amount <= 0) continue;

                ItemStack item = new ItemStack(id, amount, (short) 0, data);

                // ==========================================
                // PICARETA LENDÁRIA (encanta só se for dropada pelo lendário e for exatamente 1 unidade)
                // ==========================================
                if ("CD_ZOMBIE_LENDARIO".equalsIgnoreCase(zombieKey) && item.getTypeId() == 278 && amount == 1) {
                    // Enchants (unsafe)
                    item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.DIG_SPEED, 4);           // Eficiência IV
                    item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.DURABILITY, 3);          // Inquebrável III
                    item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.LOOT_BONUS_BLOCKS, 2);   // Fortuna II

                    // Nome + Lore
                    org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                    meta.setDisplayName(com.avelar.cdacustomzombies.util.CC.color("&6Picareta Lendária"));

                    java.util.List<String> lore = new java.util.ArrayList<String>();
                    lore.add(com.avelar.cdacustomzombies.util.CC.color("&7Drop exclusivo de &6Zombie Lendário&7."));
                    lore.add(com.avelar.cdacustomzombies.util.CC.color("&7Encantamentos: &fEf IV, Inq III, For II"));
                    meta.setLore(lore);

                    item.setItemMeta(meta);
                }

                giveItemOrDrop(p, item, dropLoc);
                given++;
            } catch (Throwable ignored) {}
        }

    return given;
}

    private void giveItemOrDrop(Player p, ItemStack item, Location dropLoc) {
        if (p == null || item == null) return;

        Location loc = (dropLoc != null) ? dropLoc : p.getLocation();

        try {
            Map<Integer, ItemStack> leftover = p.getInventory().addItem(item);

            if (leftover != null && !leftover.isEmpty() && loc != null && loc.getWorld() != null) {
                for (ItemStack stack : leftover.values()) {
                    if (stack == null) continue;
                    try { loc.getWorld().dropItemNaturally(loc, stack); } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable t) {
            // fallback: se der qualquer erro, dropa o item inteiro
            if (loc != null && loc.getWorld() != null) {
                try { loc.getWorld().dropItemNaturally(loc, item); } catch (Throwable ignored) {}
            }
        }

        try { p.updateInventory(); } catch (Throwable ignored) {}
    }

    // knockup helper pro combat listener
    public void knockUp(Player victim, double power) {
        if (victim == null) return;
        Vector v = victim.getVelocity();
        v.setY(Math.max(v.getY(), power));
        victim.setVelocity(v);
    }

    // -----------------------
    // inner structs
    // -----------------------
    public static class LevelRule {
        public final double healthMultiplier;
        public final double damageMultiplier;
        public final int speedBonusAmplifier;
        public final float explosionPower;
        public final int fireTicks;
        public final double extraDamage;

        public LevelRule(double healthMultiplier, double damageMultiplier, int speedBonusAmplifier) {
            this(healthMultiplier, damageMultiplier, speedBonusAmplifier, -1f, -1, -1.0);
        }
        public LevelRule(double healthMultiplier, double damageMultiplier, int speedBonusAmplifier, float explosionPower, int fireTicks, double extraDamage) {
            this.healthMultiplier = healthMultiplier;
            this.damageMultiplier = damageMultiplier;
            this.speedBonusAmplifier = speedBonusAmplifier;
            this.explosionPower = explosionPower;
            this.fireTicks = fireTicks;
            this.extraDamage = extraDamage;
        }
    }

        public static class TransformRule {
        public final int itemId;
        public final String toZombieKey;
        public final boolean consume;
        public final String displayNameContains;
        public final String loreContains;

        public TransformRule(int itemId, String toZombieKey, boolean consume, String displayNameContains, String loreContains) {
            this.itemId = itemId;
            this.toZombieKey = toZombieKey;
            this.consume = consume;
            this.displayNameContains = displayNameContains == null ? "" : displayNameContains;
            this.loreContains = loreContains == null ? "" : loreContains;
        }
    }

    public static class Reward {
        public final double chance;
        public final double money;
        public final List<String> items;

        public Reward(double chance, double money, List<String> items) {
            this.chance = chance;
            this.money = money;
            this.items = items == null ? new ArrayList<String>() : items;
        }
    }

    // -----------------------
    // Optional: helpers
    // -----------------------
    public Location locOf(LivingEntity e) { return e == null ? null : e.getLocation(); }


private void broadcastMultiline(String msgColored) {
    if (msgColored == null) return;
    String[] lines = msgColored.split("\n");
    for (String line : lines) {
        if (line == null) continue;
        line = line.trim();
        // permite linhas " " para espaçamento
        if (line.length() == 0) {
            org.bukkit.Bukkit.broadcastMessage(" ");
        } else {
            org.bukkit.Bukkit.broadcastMessage(line);
        }
    }
}

    /**
     * Remove a tag [MADRUGADA] (case-insensitive) se existir na mensagem.
     * Mantemos a mensagem limpa para broadcast (remoção simples por regex).
     */
    private String stripMadrugadaPrefix(String msg) {
        if (msg == null) return null;
        try {
            String out = msg.replaceAll("(?i)\\[\\s*MADRUGADA\\s*\\]", "");
            // preserve leading/trailing spaces to keep layout/centering from config
            return out;
        } catch (Throwable ignored) {
            return msg;
        }
    }

    private void playTerrorSoundAll() {
        try {
            org.bukkit.Sound s = org.bukkit.Sound.ENDERDRAGON_GROWL;
            for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                try {
                    p.playSound(p.getLocation(), s, 1.0f, 0.1f);
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Explosão compatível com 1.6.4: tenta 4 args via reflection, fallback para 3 args.
     * Sempre sem fogo; sem quebra de blocos quando o método de 4 args existir.
     */
    private void createExplosionSafe(org.bukkit.Location loc, float power) {
        if (loc == null || loc.getWorld() == null) return;
        recordExplosiveExplosion(loc);
        try {
            java.lang.reflect.Method m = loc.getWorld().getClass().getMethod(
                    "createExplosion",
                    org.bukkit.Location.class, float.class, boolean.class, boolean.class
            );
            m.invoke(loc.getWorld(), loc, power, false, false);
            return;
        } catch (Throwable ignored) {}

        try {
            loc.getWorld().createExplosion(loc, power, false);
        } catch (Throwable ignored) {}
    }

    // registra explosões para limpar blocos no EntityExplodeEvent
    private void recordExplosiveExplosion(org.bukkit.Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        cleanupExplosiveMarks();
        explosiveMarks.add(new ExplosionMark(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), System.currentTimeMillis()));
    }

    public boolean consumeExplosiveExplosion(org.bukkit.Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        cleanupExplosiveMarks();
        String w = loc.getWorld().getName();
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        for (int i = 0; i < explosiveMarks.size(); i++) {
            ExplosionMark m = explosiveMarks.get(i);
            if (!m.world.equals(w)) continue;
            int dx = m.x - x;
            int dy = m.y - y;
            int dz = m.z - z;
            if ((dx * dx + dy * dy + dz * dz) <= 4) {
                explosiveMarks.remove(i);
                return true;
            }
        }
        return false;
    }

    private void cleanupExplosiveMarks() {
        long now = System.currentTimeMillis();
        for (int i = explosiveMarks.size() - 1; i >= 0; i--) {
            if (now - explosiveMarks.get(i).time > 3000L) {
                explosiveMarks.remove(i);
            }
        }
    }

    private static class ExplosionMark {
        final String world;
        final int x, y, z;
        final long time;

        ExplosionMark(String world, int x, int y, int z, long time) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.time = time;
        }
    }

}
