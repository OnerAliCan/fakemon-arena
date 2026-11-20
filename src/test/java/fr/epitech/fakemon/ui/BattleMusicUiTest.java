package fr.epitech.fakemon.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import fr.epitech.fakemon.data.JsonSpeciesRepository;
import fr.epitech.fakemon.data.SpeciesFactory;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

/** Vérifie l'enchaînement réel des MP3 sur un poste équipé de JavaFX Media. */
@EnabledIfSystemProperty(named = "fakemon.ui.tests", matches = "true")
class BattleMusicUiTest {
    @TempDir java.nio.file.Path userFolder;

    @Test
    void introPlaysOnceThenThemeLoopsAndStopsWhenLeavingBattle() throws Exception {
        var finished = new CompletableFuture<Void>();
        var scenes = FxTestSupport.run(() -> {
            var manager = new SceneManager(new Scene(new StackPane(), 960, 540));
            try {
                var setup = BattleSetup.random(new JsonSpeciesRepository(new SpeciesFactory(), userFolder), new Random(28));
                var screen = new BattleScreen(manager, setup, () -> {});
                manager.show(screen);
                var musicField = BattleScreen.class.getDeclaredField("music");
                musicField.setAccessible(true);
                var music = (BattleMusic) musicField.get(screen);
                var introField = BattleMusic.class.getDeclaredField("intro");
                introField.setAccessible(true);
                var themeField = BattleMusic.class.getDeclaredField("theme");
                themeField.setAccessible(true);
                var intro = (MediaPlayer) introField.get(music);
                var theme = (MediaPlayer) themeField.get(music);
                intro.setOnError(() -> finished.completeExceptionally(intro.getError()));
                theme.setOnError(() -> finished.completeExceptionally(theme.getError()));
                assertEquals(1, intro.getCycleCount());
                assertEquals(MediaPlayer.INDEFINITE, theme.getCycleCount());
                // Raccourcit seulement la boucle du test, sans modifier les fichiers audio.
                theme.setStopTime(Duration.millis(200));
                theme.setOnRepeat(() -> {
                    try {
                        assertEquals(MediaPlayer.Status.DISPOSED, intro.getStatus());
                        assertNull(introField.get(music));
                        var volumeField = SceneManager.class.getDeclaredField("volumeSettings");
                        volumeField.setAccessible(true);
                        var settings = (VolumeSettings) volumeField.get(manager);
                        settings.musicPercent().set(25);
                        assertEquals(0.25, theme.getVolume(), 0.0001);
                        manager.show(new StackPane());
                        assertEquals(MediaPlayer.Status.DISPOSED, theme.getStatus());
                        assertNull(themeField.get(music));
                        // Une revanche relance une seule introduction et ne conserve pas l'ancien lecteur.
                        var replay = new BattleScreen(manager, setup.rematch(), () -> {});
                        manager.show(replay);
                        var replayMusic = (BattleMusic) musicField.get(replay);
                        var replayIntro = (MediaPlayer) introField.get(replayMusic);
                        assertEquals(1, replayIntro.getCycleCount());
                        manager.show(new StackPane());
                        assertEquals(MediaPlayer.Status.DISPOSED, replayIntro.getStatus());
                        finished.complete(null);
                    } catch (Throwable error) {
                        finished.completeExceptionally(error);
                    }
                });
                return manager;
            } catch (Throwable error) {
                manager.show(new StackPane());
                manager.close();
                throw error;
            }
        });
        try {
            finished.get(25, TimeUnit.SECONDS);
        } finally {
            FxTestSupport.run(() -> {
                scenes.show(new StackPane());
                scenes.close();
                return null;
            });
        }
    }
}
