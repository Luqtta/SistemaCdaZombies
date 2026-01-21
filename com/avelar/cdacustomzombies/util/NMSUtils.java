package com.avelar.cdacustomzombies.util;

import com.arzio.arziolib.api.util.CauldronUtils;
import org.bukkit.craftbukkit.v1_6_R3.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

public class NMSUtils {
  public static ItemStack getEntityStack(Entity entity) {
    return CraftItemStack.asBukkitCopy(CauldronUtils.getNMSEntity(entity).getDataWatcher().getItemStack(10));
  }
}
