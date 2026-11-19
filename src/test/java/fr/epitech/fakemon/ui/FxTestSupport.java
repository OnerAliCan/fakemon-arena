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
