package fr.epitech.fakemon.ui;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;

/** Partage le même thread JavaFX entre les vérifications d'interface d'une exécution. */
final class FxTestSupport {
    private static final CompletableFuture<Void> STARTED = new CompletableFuture<>();

    static {
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            STARTED.complete(null);
        });
    }

    private FxTestSupport() { }

    static void await(Callable<Boolean> condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!run(condition)) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("L'interface n'a pas atteint l'état attendu.");
            }
            Thread.sleep(25);
        }
    }

    static void awaitBattle(BattleScreen screen) throws Exception {
        await(() -> {
            var field = BattleScreen.class.getDeclaredField("view");
            field.setAccessible(true);
            return field.get(screen).toString().equals("ACTION");
        });
    }

    static <T> T run(Callable<T> task) throws Exception {
        STARTED.get(5, TimeUnit.SECONDS);
        var result = new CompletableFuture<T>();
        Platform.runLater(() -> {
            try {
                result.complete(task.call());
            } catch (Throwable error) {
                result.completeExceptionally(error);
            }
        });
        return result.get(5, TimeUnit.SECONDS);
    }
}
