package fr.epitech.fakemon.ui;

import java.nio.file.Path;
import java.util.function.Consumer;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

/** Lecteur temporaire du cri choisi ; respecte le volume et libère les médias. */
final class CryPreview implements AutoCloseable {
    private final ReadOnlyDoubleProperty volume;
    private final Consumer<String> message;
    private MediaPlayer player;

    CryPreview(ReadOnlyDoubleProperty volume, Consumer<String> message) {
        this.volume = volume;
        this.message = message;
    }

    void play(Path file) {
        close();
        message.accept("");
        try {
            Media media = new Media(file.toUri().toString());
            MediaPlayer candidate = new MediaPlayer(media);
            player = candidate;
            candidate.volumeProperty().bind(volume.divide(100));
            Runnable failed = () -> {
                if (player == candidate) {
                    close();
                    message.accept("Impossible de lire ce cri. Choisis un fichier WAV ou MP3 valide.");
                }
            };
            media.setOnError(failed);
            candidate.setOnError(failed);
            candidate.setOnEndOfMedia(() -> {
                if (player == candidate) {
                    close();
                }
            });
            if (media.getError() != null || candidate.getError() != null) {
                failed.run();
            } else {
                candidate.play();
            }
        } catch (RuntimeException error) {
            close();
            message.accept("Impossible de lire ce cri. Choisis un fichier WAV ou MP3 valide.");
        }
    }

    /** Arrête la lecture et libère le fichier, notamment lors d'un changement d'écran. */
    @Override
    public void close() {
        if (player != null) {
            var previous = player;
            player = null;
            previous.volumeProperty().unbind();
            previous.setOnError(null);
            previous.setOnEndOfMedia(null);
            previous.getMedia().setOnError(null);
            previous.dispose();
        }
    }
}
