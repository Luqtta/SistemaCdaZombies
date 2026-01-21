package com.avelar.cdacustomzombies.util;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class PlayerUtils {
  public static void playMassiveSound(Sound s) {
    for (Player p : Bukkit.getServer().getOnlinePlayers())
      p.playSound(p.getLocation(), s, 10.0F, 1.0F); 
  }
}
