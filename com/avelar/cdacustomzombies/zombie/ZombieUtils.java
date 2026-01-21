package com.avelar.cdacustomzombies.zombie;

import com.arzio.arziolib.api.util.CDEntityType;
import java.util.Arrays;
import java.util.HashMap;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class ZombieUtils {
  private static HashMap<String, Zombie> zombies = new HashMap<>();
  
  public static Zombie getZombie(CDEntityType type) {
    if (type == null)
      return zombies.get("BOSS"); 
    switch (type) {
      case CD_ZOMBIE:
        return zombies.get("CD_ZOMBIE");
      case CD_ZOMBIE_FAST:
        return zombies.get("CD_ZOMBIE_FAST");
      case CD_ZOMBIE_WEAK:
        return zombies.get("CD_ZOMBIE_WEAK");
      case CD_ZOMBIE_TANK:
        return zombies.get("CD_ZOMBIE_TANK");
    } 
    return zombies.get("BOSS");
  }
  
  public static int getZombieLevel(Entity zombie) {
    if (isZombieBoss(zombie))
      return 4; 
    return Integer.valueOf(((LivingEntity)zombie).getCustomName().split("lvl" + Character.toString('.'))[1].replaceAll("[^0-9]", "")).intValue();
  }
  
  public static boolean isZombie(Entity zombie) {
    return Arrays.<CDEntityType>asList(CDEntityType.getZombieTypes()).contains(CDEntityType.getTypeOf(zombie));
  }
  
  public static boolean isZombieBoss(Entity zombie) {
    if (!(zombie instanceof LivingEntity) || ((LivingEntity)zombie).getCustomName() == null)
      return false; 
    return ((LivingEntity)zombie).getCustomName().contains(ChatColor.stripColor(getZombie(null).getName()));
  }
  
  public static void loadDefaults() {
    if (zombies.size() > 0)
      zombies.clear(); 
    zombies.put("CD_ZOMBIE", new Zombie("CD_ZOMBIE"));
    zombies.put("CD_ZOMBIE_FAST", new Zombie("CD_ZOMBIE_FAST"));
    zombies.put("CD_ZOMBIE_TANK", new Zombie("CD_ZOMBIE_TANK"));
    zombies.put("CD_ZOMBIE_WEAK", new Zombie("CD_ZOMBIE_WEAK"));
    zombies.put("BOSS", new Zombie("BOSS"));
  }
}
