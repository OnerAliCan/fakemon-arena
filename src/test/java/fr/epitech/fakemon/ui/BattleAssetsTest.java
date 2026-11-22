package fr.epitech.fakemon.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Vérifie la résolution des médias intégrés et des médias propres à l'utilisateur. */
class BattleAssetsTest {
    @TempDir Path folder;

    @Test
    void allFiveBackgroundsCanBeDrawnAndLoadedAsImages() throws Exception {
        List<String> expected = List.of("grassland", "forest", "beach", "desert", "snow");
        Random choices = new Random(0) {
            private int next;

            @Override
            public int nextInt(int bound) {
                assertEquals(expected.size(), bound);
                return next++;
            }
        };
        for (String name : expected) {
            var chosen = BattleAssets.randomBackground(choices);
            assertEquals(BattleAssets.class.getResource("/backgrounds/battle-" + name + ".png"), chosen);
            var image = ImageIO.read(chosen);
            assertNotNull(image);
            assertTrue(image.getWidth() > image.getHeight());
        }
    }

    @Test
    void resolvesBundledResourcesWithOrWithoutAnInitialSlash() {
        var resource = BattleAssets.class.getResource("/data/fakemons.json");
        assertNotNull(resource);
        assertEquals(resource.toExternalForm(), BattleAssets.source("/data/fakemons.json"));
        assertEquals(resource.toExternalForm(), BattleAssets.source("data/fakemons.json"));
    }

    @Test
    void resolvesUserMediaRelativeToTheGameFolder() {
        String relative = "species/Test/cry.wav";
        URI resolved = URI.create(BattleAssets.source(relative));
        assertEquals(BattleAssets.userFolder().resolve(relative).toAbsolutePath(), Path.of(resolved));
    }

    @Test
    void preservesAbsoluteMediaPaths() throws Exception {
        Path media = Files.createFile(folder.resolve("cry.wav"));
        URI resolved = URI.create(BattleAssets.source(media.toString()));
        assertEquals(media, Path.of(resolved));
        assertTrue(Files.isSameFile(media, Path.of(resolved)));
    }

    @Test
    void encodesSpacesAccentsAndFragmentCharactersInMediaPaths() throws Exception {
        Path media = Files.createFile(folder.resolve("cri écho #1.wav"));
        URI resolved = URI.create(BattleAssets.source(media.toString()));
        assertEquals(media, Path.of(resolved));
        assertNull(resolved.getFragment());
        assertTrue(Files.exists(Path.of(resolved)));
    }

    @Test
    void resolvesMissingMediaSoThePlayerCanReportTheFailure() {
        Path missing = folder.resolve("absent.wav");
        assertEquals(missing, Path.of(URI.create(BattleAssets.source(missing.toString()))));
    }
}
