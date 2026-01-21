package com.avelar;

import com.avelar.cdacustomzombies.ZombieManager;
import com.avelar.cdacustomzombies.commands.CZCommand;
import com.avelar.cdacustomzombies.listeners.ZombieCombatListener;
import com.avelar.cdacustomzombies.listeners.ZombieDamageListener;
import com.avelar.cdacustomzombies.listeners.ZombieDeathListener;
import com.avelar.cdacustomzombies.listeners.ZombieLastHitListener;
import com.avelar.cdacustomzombies.listeners.ZombieSpawnListener;
import com.avelar.cdacustomzombies.listeners.ZombieTransformListener;
import com.avelar.cdacustomzombies.listeners.BossHealthScoreboardListener;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private ZombieManager zombieManager;
    private Economy economy;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        setupEconomy();

        this.zombieManager = new ZombieManager(this);
        this.zombieManager.reloadAll();

        // comando (plugin.yml: customzombies, alias: cz)
        if (getCommand("customzombies") != null) {
            getCommand("customzombies").setExecutor(new CZCommand(this));
        }

        // ===== listeners do sistema =====
        Bukkit.getPluginManager().registerEvents(new ZombieSpawnListener(this), this);       // aplica defs no spawn
        Bukkit.getPluginManager().registerEvents(new ZombieLastHitListener(this), this);    // rastreia last-hit (melee/projétil)
        Bukkit.getPluginManager().registerEvents(new ZombieDamageListener(this), this);     // bullets (ArzioLib) + headshot + last-hit
        Bukkit.getPluginManager().registerEvents(new ZombieDeathListener(this), this);      // reward/drops/broadcast na morte

        // ===== extras =====
        Bukkit.getPluginManager().registerEvents(new ZombieCombatListener(this), this);     // efeitos de Boss/Lendário quando bate
        Bukkit.getPluginManager().registerEvents(new ZombieTransformListener(this), this);  // transformar no clique com item (9937/9942)
        Bukkit.getPluginManager().registerEvents(new BossHealthScoreboardListener(this), this); // barra de HP no scoreboard

        
	// ===== Madrugada do Terror: relógio (checa hora e anuncia start/end) =====
	Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
	    @Override
	    public void run() {
	        try {
	            if (zombieManager != null) zombieManager.tickTerrorClock();
	        } catch (Throwable ignored) {}
	    }
	}, 20L, 20L * 30L); // a cada 30s

        Bukkit.getConsoleSender().sendMessage("§a[CDACustomZombies] Plugin habilitado.");
    }

    @Override
    public void onDisable() {
        Bukkit.getConsoleSender().sendMessage("§c[CDACustomZombies] Plugin desabilitado.");
    }

    public ZombieManager getZombieManager() {
        return zombieManager;
    }

    public Economy getEconomy() {
        return economy;
    }

    private void setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            Bukkit.getConsoleSender().sendMessage("§e[CDACustomZombies] Vault não encontrado. Dinheiro desativado.");
            return;
        }

        RegisteredServiceProvider<Economy> rsp =
                getServer().getServicesManager().getRegistration(Economy.class);

        if (rsp != null) {
            economy = rsp.getProvider();
            Bukkit.getConsoleSender().sendMessage("§a[CDACustomZombies] Economia carregada com Vault.");
        } else {
            Bukkit.getConsoleSender().sendMessage("§e[CDACustomZombies] Nenhum provider de economia encontrado no Vault.");
        }
    }
}
