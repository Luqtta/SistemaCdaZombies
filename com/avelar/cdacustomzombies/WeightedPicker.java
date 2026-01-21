package com.avelar.cdacustomzombies;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WeightedPicker {

    private static class Entry {
        final String key;
        final int weight;
        Entry(String key, int weight) { this.key = key; this.weight = weight; }
    }

    private final List<Entry> entries = new ArrayList<Entry>();
    private final Random random = new Random();

    public void add(String key, int weight) {
        if (key == null) return;
        if (weight <= 0) return;
        entries.add(new Entry(key, weight));
    }

    public String pickOrNull() {
        if (entries.isEmpty()) return null;

        int total = 0;
        for (Entry e : entries) total += e.weight;
        int r = random.nextInt(total);

        int cur = 0;
        for (Entry e : entries) {
            cur += e.weight;
            if (r < cur) return e.key;
        }
        return entries.get(entries.size() - 1).key;
    }
}
