package com.github.nutt1101.service;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Serializes GUI and command edits; publishes a selection only after it is saved. */
public final class EntitySelectionService {
    @FunctionalInterface
    public interface Writer { void save(Set<String> selection) throws IOException; }

    private final Object lock;
    private final Supplier<Set<String>> reader;
    private final Writer writer;
    private final Runnable changed;

    public EntitySelectionService(Supplier<Set<String>> reader, Writer writer, Runnable changed) {
        this(new Object(), reader, writer, changed);
    }

    public EntitySelectionService(Object lock, Supplier<Set<String>> reader, Writer writer, Runnable changed) {
        this.lock = lock;
        this.reader = reader;
        this.writer = writer;
        this.changed = changed;
    }

    public Set<String> snapshot() { return Set.copyOf(reader.get()); }

    public boolean update(Consumer<Set<String>> edit) throws IOException {
        synchronized (lock) {
            Set<String> before = snapshot();
            Set<String> candidate = new LinkedHashSet<>(before);
            edit.accept(candidate);
            if (candidate.equals(before)) return false;
            writer.save(Set.copyOf(candidate));
            changed.run();
            return true;
        }
    }
}
