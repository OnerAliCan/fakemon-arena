package fr.epitech.fakemon.ui;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.BiConsumer;

/** Sérialise les présentations ; seule leur notification de fin autorise la suivante. */
final class BattleEventQueue {
    private final Deque<Runnable> events = new ArrayDeque<>();
    private final BiConsumer<Runnable, Runnable> presenter;
    private final Runnable idle;
    private boolean running;
    private long generation;
    private long presentation;

    BattleEventQueue(BiConsumer<Runnable, Runnable> presenter, Runnable idle) {
        this.presenter = presenter;
        this.idle = idle;
    }

    void add(Runnable event) {
        events.addLast(event);
    }

    void start() {
        if (running) {
            return;
        }
        if (events.isEmpty()) {
            idle.run();
            return;
        }
        running = true;
        long expectedGeneration = generation;
        long expectedPresentation = ++presentation;
        presenter.accept(events.removeFirst(), () -> {
            if (running && generation == expectedGeneration && presentation == expectedPresentation) {
                running = false;
                start();
            }
        });
    }

    void clear() {
        generation++;
        running = false;
        events.clear();
    }
}
