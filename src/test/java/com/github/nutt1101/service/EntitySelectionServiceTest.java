package com.github.nutt1101.service;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class EntitySelectionServiceTest {
    @Test void publishesOnlyAfterPersistenceAndDoesNotWriteNoOpEdits() throws Exception {
        var stored = new AtomicReference<>(Set.of("SHEEP"));
        var changes = new AtomicInteger();
        var service = new EntitySelectionService(stored::get, stored::set, changes::incrementAndGet);
        assertTrue(service.update(next -> next.add("COW")));
        assertEquals(Set.of("SHEEP", "COW"), service.snapshot());
        assertFalse(service.update(next -> next.add("COW")));
        assertEquals(1, changes.get());
        assertThrows(UnsupportedOperationException.class, () -> service.snapshot().clear());
    }

    @Test void failedWriteKeepsSelectionAndDoesNotNotifyMenus() {
        var changes = new AtomicInteger();
        var service = new EntitySelectionService(() -> Set.of("SHEEP"), next -> { throw new IOException("disk full"); }, changes::incrementAndGet);
        assertThrows(IOException.class, () -> service.update(next -> next.add("COW")));
        assertEquals(Set.of("SHEEP"), service.snapshot());
        assertEquals(0, changes.get());
    }

    @Test void concurrentEditorsDoNotLoseSuccessfulChanges() throws Exception {
        var stored = new AtomicReference<Set<String>>(Set.of());
        var service = new EntitySelectionService(stored::get, stored::set, () -> {});
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(4)) {
            var work = java.util.stream.IntStream.range(0, 40).mapToObj(i -> pool.submit(() -> {
                service.update(next -> next.add("ENTITY_" + i));
                return null;
            })).toList();
            for (var task : work) task.get();
        }
        assertEquals(40, stored.get().size());
    }
}
