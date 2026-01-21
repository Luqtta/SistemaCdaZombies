package com.avelar.cdacustomzombies;

import java.util.ArrayList;
import java.util.List;

public class ZombieDefinition {

    private final String key;
    private final String name;
    private final double baseHealth;
    private final boolean headShotAllowed;
    private final List<String> effects;

    private final double baseMoney;
    private final List<String> items;

    public ZombieDefinition(String key, String name, double baseHealth, boolean headShotAllowed,
                            List<String> effects, double baseMoney, List<String> items) {
        this.key = key;
        this.name = name;
        this.baseHealth = baseHealth;
        this.headShotAllowed = headShotAllowed;
        this.effects = effects == null ? new ArrayList<String>() : effects;
        this.baseMoney = baseMoney;
        this.items = items == null ? new ArrayList<String>() : items;
    }

    public String getKey() { return key; }
    public String getName() { return name; }
    public double getBaseHealth() { return baseHealth; }
    public List<String> getEffects() { return effects; }

    public boolean isHeadshotAllowed() { return headShotAllowed; }

    public double getBaseMoney() { return baseMoney; }
    public List<String> getItems() { return items; }
}
