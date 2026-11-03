package com.github.nutt1101.menu;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class EntityMenuModelTest {
    private final List<EntityMenuModel.Entry> catalog = List.of(
            new EntityMenuModel.Entry("SHEEP", "綿羊"),
            new EntityMenuModel.Entry("COW", "牛"),
            new EntityMenuModel.Entry("CHICKEN", "Chicken"));

    @Test void searchesIdsAndTranslatedNamesWithoutChangingOrderOrSource() {
        assertEquals(List.of(catalog.getFirst()), EntityMenuModel.filter(catalog, Set.of(), " 綿羊 ", EntityMenuModel.Filter.ALL));
        assertEquals(List.of(catalog.get(2)), EntityMenuModel.filter(catalog, Set.of(), "CHICK", EntityMenuModel.Filter.ALL));
        assertEquals(List.of("CHICKEN", "COW", "SHEEP"), EntityMenuModel.filter(catalog, Set.of(), "", EntityMenuModel.Filter.ALL)
                .stream().map(EntityMenuModel.Entry::id).toList());
        assertEquals("SHEEP", catalog.getFirst().id());
    }

    @Test void combinesSearchAndSelectionFilterAndHandlesEmptyResults() {
        assertEquals(List.of(catalog.get(1)), EntityMenuModel.filter(catalog, Set.of("COW"), "", EntityMenuModel.Filter.ALLOWED));
        assertEquals(2, EntityMenuModel.filter(catalog, Set.of("COW"), "", EntityMenuModel.Filter.BLOCKED).size());
        assertTrue(EntityMenuModel.filter(catalog, Set.of("COW"), "sheep", EntityMenuModel.Filter.ALLOWED).isEmpty());
        assertTrue(EntityMenuModel.filter(List.of(), Set.of(), "", EntityMenuModel.Filter.ALL).isEmpty());
    }
}
