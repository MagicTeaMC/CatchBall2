package com.github.nutt1101.menu;

import net.momirealms.sparrow.ui.pane.page.Page;
import net.momirealms.sparrow.ui.state.Signal;
import net.momirealms.sparrow.ui.state.Signals;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class MenuStateTest {
    @Test void sharedSelectionUpdatesResultsWhileEachViewKeepsItsOwnQuery() {
        var catalog = List.of(new EntityMenuModel.Entry("COW", "Cow"), new EntityMenuModel.Entry("SHEEP", "Sheep"));
        var allowed = Signal.of(Set.of("COW"));
        var firstQuery = Signal.of("");
        var secondQuery = Signal.of("sheep");
        var filter = Signal.of(EntityMenuModel.Filter.ALLOWED);
        var first = Signals.combine(allowed, firstQuery, filter, (a, q, f) -> EntityMenuModel.filter(catalog, a, q, f));
        var second = Signals.combine(allowed, secondQuery, filter, (a, q, f) -> EntityMenuModel.filter(catalog, a, q, f));
        var pages = Page.of(first, 1);
        assertEquals(1, pages.count().get());
        assertTrue(second.get().isEmpty());
        allowed.set(Set.of("COW", "SHEEP"));
        assertEquals(2, pages.count().get());
        assertEquals("SHEEP", second.get().getFirst().id());
        pages.setPage(1);
        assertEquals("SHEEP", pages.content().get().getFirst().id());
        firstQuery.set("cow");
        pages.setPage(0);
        assertEquals(1, pages.count().get());
        assertEquals("COW", pages.content().get().getFirst().id());
        assertEquals("sheep", secondQuery.get());
    }
}
