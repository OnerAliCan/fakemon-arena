package fr.epitech.fakemon.ui;

import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

/** Joue l'introduction une fois, puis le thème de combat en boucle. */
final class BattleMusic implements AutoCloseable {
    private final ReadOnlyDoubleProperty volumePercent;
    private MediaPlayer intro;
    private MediaPlayer theme;

    BattleMusic(ReadOnlyDoubleProperty volumePercent) {
        this.volumePercent = volumePercent;
    }

    /** Précharge les deux pistes avant de lancer l'introduction. */
    void play() {
        if (intro != null || theme != null) {
            return;
        }
        try {
            theme = player("/music/main-theme.mp3");
            intro = player("/music/fight-intro.mp3");
            MediaPlayer opening = intro;
            MediaPlayer loop = theme;
            loop.setCycleCount(MediaPlayer.INDEFINITE);
            opening.setOnEndOfMedia(() -> {
                if (intro == opening && theme == loop) {
                    intro = null;
                    dispose(opening);
                    loop.play();
                }
            });
            loop.setOnReady(opening::play);
            if (opening.getError() != null || loop.getError() != null) {
                failed();
            }
        } catch (RuntimeException error) {
            failed();
        }
    }

    private MediaPlayer player(String resource) {
        var media = new Media(BattleMusic.class.getResource(resource).toExternalForm());
        var player = new MediaPlayer(media);
        player.volumeProperty().bind(volumePercent.divide(100));
        media.setOnError(this::failed);
        player.setOnError(this::failed);
        return player;
    }

    private void failed() {
        close();
        System.getLogger(BattleMusic.class.getName()).log(System.Logger.Level.WARNING,
                "Impossible de lire la musique de combat.");
    }

    /** Arrête la lecture et libère les deux lecteurs en quittant le combat. */
    @Override
    public void close() {
        MediaPlayer opening = intro;
        MediaPlayer loop = theme;
        intro = null;
        theme = null;
        dispose(opening);
        dispose(loop);
    }

    private void dispose(MediaPlayer player) {
        if (player != null) {
            player.setOnReady(null);
            player.setOnEndOfMedia(null);
            player.setOnError(null);
            player.getMedia().setOnError(null);
            player.volumeProperty().unbind();
            player.dispose();
        }
    }
}
