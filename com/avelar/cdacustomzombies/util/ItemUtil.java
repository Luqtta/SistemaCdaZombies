package com.avelar.cdacustomzombies.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class ItemUtil {
    private ItemUtil() {}

    public static int getTypeIdSafe(ItemStack it) {
        return it == null ? 0 : it.getTypeId();
    }

    public static void consumeOneInHand(PlayerInventory inv) {
        if (inv == null) return;
        ItemStack it = inv.getItemInHand();
        if (it == null) return;

        int amt = it.getAmount();
        if (amt <= 1) inv.setItemInHand(null);
        else it.setAmount(amt - 1);
    }
}
