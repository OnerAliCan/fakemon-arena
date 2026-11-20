package fr.epitech.fakemon.ui;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.sound.sampled.AudioSystem;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Vérifie que chaque son embarqué est un WAV lisible et non vide. */
class MenuSoundAssetsTest {
    @ParameterizedTest
    @ValueSource(strings = {"Error", "Open", "LoadSave", "Close", "Confirm", "Cursor"})
    void bundledSoundsContainReadableAudio(String name) throws Exception {
        var source = MenuSounds.class.getResource("/sounds/ui/" + name + ".wav");
        assertNotNull(source);
        try (var audio = AudioSystem.getAudioInputStream(source)) {
            assertTrue(audio.getFrameLength() > 0);
            assertTrue(audio.getFormat().getSampleRate() > 0);
        }
    }
}
