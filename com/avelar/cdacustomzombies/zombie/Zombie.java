package com.avelar.cdacustomzombies.zombie;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import com.avelar.cdacustomzombies.util.ConfigAPI;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

public class Zombie {
  public static ConfigAPI config = null;
  
  private String name;
  
  private String type;
  
  private double money = 0.0D;
  
  private int points = 0;
  
  private double life;
  
  private boolean headshot;
  
  private List<String> efeitos = new ArrayList<>();
  
  private List<String> drops = new ArrayList<>();
  
  public Zombie(String zombie) {
    this.name = ChatColor.translateAlternateColorCodes('&', config.getString("Zombies." + zombie + ".Nome"));
    this.type = zombie;
    this.money = config.getInt("Zombies." + zombie + ".DinheiroBase");
    this.points = config.getInt("Zombies." + zombie + ".Pontos");
    this.life = config.getInt("Zombies." + zombie + ".VidaBase");
    this.headshot = config.getBoolean("Zombies." + zombie + ".HeadShot");
    this.efeitos = config.getStringList("Zombies." + zombie + ".Efeitos");
    this.drops = config.getStringList("Zombies." + zombie + ".Itens");
  }
  
  public String getType() {
    return this.type;
  }
  
  public String getName() {
    return this.name;
  }
  
  public int getPoins() {
    return this.points;
  }
  
  public double getMoney() {
    return this.money;
  }
  
  public double getLife() {
    return this.life;
  }
  
  public boolean allowsHeadShot() {
    return this.headshot;
  }
  
  public List<String> getPotions() {
    return this.efeitos;
  }
  
  public List<String> getDrops() {
    return this.drops;
  }
  
  public List<ItemStack> nextDrops() {
    List<ItemStack> stack = new ArrayList<>();
    for (String s : getDrops()) {
      String[] split = s.split(" ");
      double chance = Math.random();
      double valor = Double.valueOf(split[2]).doubleValue() / 100.0D;
      if (chance <= valor) {
        if (split[0].contains(":")) {
          String[] data = split[0].split(":");
          ItemStack iStack = new ItemStack(Integer.valueOf(data[0]).intValue(), Integer.valueOf(split[1]).intValue(), (short)1, Byte.valueOf(data[1]));
          stack.add(iStack);
          break;
        } 
        stack.add(new ItemStack(Integer.valueOf(split[0]).intValue(), Integer.valueOf(split[1]).intValue()));
        break;
      } 
    } 
    return stack;
  }
  
  public int getRandomLevel() {
    Random random = new Random();
    int i = random.nextInt(4) + 1;
    if (i > 1 && i < 4)
      if (random.nextBoolean()) {
        i--;
      } else if (random.nextBoolean()) {
        i = 1;
      }  
    return i;
  }
}
