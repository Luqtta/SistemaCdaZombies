package com.avelar.cdacustomzombies.util;

import org.bukkit.ChatColor;

public final class CC {
    private CC() {}

    public static final String LINE = "§8§m----------------------------";

    public static String color(String s) {
        if (s == null) return "";
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static String strip(String s) {
        if (s == null) return null;
        return ChatColor.stripColor(s);
    }
}
