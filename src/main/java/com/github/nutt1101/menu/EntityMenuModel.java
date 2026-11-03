package com.github.nutt1101.menu;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Immutable catalog entries and filtering, independent of Bukkit and window state. */
public final class EntityMenuModel {
    public record Entry(String id, String displayName) {}
    public enum Filter {
        ALL, ALLOWED, BLOCKED;
        public Filter next() { return values()[(ordinal() + 1) % values().length]; }
    }
    private EntityMenuModel() {}

    public static List<Entry> filter(List<Entry> catalog, Set<String> allowed, String query, Filter filter) {
        String needle = query.strip().toLowerCase(Locale.ROOT);
        return catalog.stream()
                .filter(entry -> filter == Filter.ALL || allowed.contains(entry.id()) == (filter == Filter.ALLOWED))
                .filter(entry -> entry.id().toLowerCase(Locale.ROOT).contains(needle)
                        || entry.displayName().toLowerCase(Locale.ROOT).contains(needle))
                .sorted(Comparator.comparing(Entry::id))
                .toList();
    }
}
