package fr.epitech.fakemon.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Vérifie l'ordre et les notifications de fin sans démarrer JavaFX. */
class BattleEventQueueTest {
    @Test
    void waitsForCompletionEvenWhenMoreEventsArrive() {
        List<Integer> shown = new ArrayList<>();
        List<Runnable> completions = new ArrayList<>();
        List<String> idle = new ArrayList<>();
        var queue = new BattleEventQueue((event, done) -> {
            event.run();
            completions.add(done);
        }, () -> idle.add("ready"));
        queue.add(() -> shown.add(1));
        queue.add(() -> shown.add(2));
        queue.start();
        queue.add(() -> shown.add(3));
        queue.start(); // Un paquet LAN ne doit pas couper l'animation en cours.
        assertEquals(List.of(1), shown);
        assertEquals(List.of(), idle);
        completions.get(0).run();
        assertEquals(List.of(1, 2), shown);
        completions.get(0).run(); // Une ancienne notification ne termine pas le suivant.
        assertEquals(List.of(1, 2), shown);
        completions.get(1).run();
        assertEquals(List.of(1, 2, 3), shown);
        assertEquals(List.of(), idle);
        completions.get(2).run();
        assertEquals(List.of("ready"), idle);
    }

    @Test
    void clearingIgnoresLateCallbacksAndDropsPendingEvents() {
        List<Integer> shown = new ArrayList<>();
        List<Runnable> completions = new ArrayList<>();
        var queue = new BattleEventQueue((event, done) -> {
            event.run();
            completions.add(done);
        }, () -> {});
        queue.add(() -> shown.add(1));
        queue.add(() -> shown.add(2));
        queue.start();
        queue.clear();
        queue.add(() -> shown.add(3));
        queue.start();
        queue.add(() -> shown.add(4));
        completions.get(0).run();
        assertEquals(List.of(1, 3), shown);
        completions.get(1).run();
        assertEquals(List.of(1, 3, 4), shown);
    }
}
