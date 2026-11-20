package fr.epitech.fakemon.ui;

import static org.junit.jupiter.api.Assertions.*;
import fr.epitech.fakemon.core.api.InvalidSpeciesException;
import fr.epitech.fakemon.core.model.*;
import fr.epitech.fakemon.data.JsonSpeciesRepository;
import fr.epitech.fakemon.data.SpeciesFactory;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Vérifie la construction des fiches et la sauvegarde du dos miroir sans interface graphique. */
class SpeciesDraftTest {
    @TempDir Path folder;
    private JsonSpeciesRepository repository;
    private Path front;

    @BeforeEach
    void setup() throws Exception {
        repository = new JsonSpeciesRepository(new SpeciesFactory(), folder.resolve("catalogue"));
        front = folder.resolve("avant.png");
        var image = new BufferedImage(2, 1, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0xffff0000);
        image.setRGB(1, 0, 0xff0000ff);
        ImageIO.write(image, "png", front.toFile());
    }

    @Test
    void savesMirroredBackAndReloadsWithoutSourceFiles() throws Exception {
        var draft = draft("  Nouveau  ", List.of(Type.FIRE), new BaseStats(100, 100, 100, 100, 100, 100));
        var species = draft.build(repository);
        assertEquals(31, species.getDexNumber());
        assertEquals("Nouveau", species.getOriginalName());
        repository.add(species);
        Files.delete(front);
        var reloaded = new JsonSpeciesRepository(new SpeciesFactory(), folder.resolve("catalogue"));
        var saved = reloaded.findByOriginalName("Nouveau").orElseThrow();
        var backImage = ImageIO.read(folder.resolve("catalogue").resolve(saved.getBackSprite()).toFile());
        assertEquals(0xff0000ff, backImage.getRGB(0, 0));
        assertEquals(0xffff0000, backImage.getRGB(1, 0));
        assertEquals("Créateur", saved.getDesigner());
        assertNull(saved.getConcept());
        assertEquals(4, saved.getPossibleMoves().size());
    }

    @Test
    void repositoryRejectsInvalidTypesAndStatsWithoutAdding() {
        var stats = new BaseStats(100, 100, 100, 100, 100, 100);
        assertThrows(InvalidSpeciesException.class, () -> repository.add(draft("Vide", List.of(), stats).build(repository)));
        assertThrows(InvalidSpeciesException.class, () -> repository.add(draft("Total", List.of(Type.FIRE),
                new BaseStats(150, 100, 100, 100, 100, 100)).build(repository)));
        assertEquals(30, repository.findAll().size());
    }

    @Test
    void requiresNamesFrontAndTalentBeforeBuilding() {
        var stats = new BaseStats(100, 100, 100, 100, 100, 100);
        assertThrows(InvalidSpeciesException.class, () -> draft(" ", List.of(Type.FIRE), stats).build(repository));
        var moves = SpeciesDraft.availableMoves(repository).subList(0, 4);
        assertThrows(InvalidSpeciesException.class, () -> new SpeciesDraft("Nom", "FR", List.of(Type.FIRE), stats,
                moves, null, front, null, null, null, null).build(repository));
        assertThrows(InvalidSpeciesException.class, () -> new SpeciesDraft("Nom", "FR", List.of(Type.FIRE), stats,
                moves, Talent.CARAPACE, null, null, null, null, null).build(repository));
    }

    @Test
    void filteringIsAccentInsensitiveAndKeepsCatalogueMoveInstances() {
        var moves = SpeciesDraft.availableMoves(repository);
        assertEquals(38, moves.size());
        assertTrue(moves.stream().allMatch(move -> repository.findAll().stream()
                .anyMatch(species -> species.getPossibleMoves().contains(move))));
        var ether = moves.stream().filter(move -> move.getType() == Type.ETHER).findFirst().orElseThrow();
        assertTrue(SpeciesDraft.matches(ether, "  ETHER "));
        assertFalse(SpeciesDraft.matches(ether, "introuvable"));
        assertTrue(moves.stream().anyMatch(move -> SpeciesDraft.matches(move, "eclipse")));
    }

    @Test
    void rejectsUnreadableFrontAndCleansTheFailedMirroring() throws Exception {
        Files.writeString(front, "pas une image");
        assertThrows(InvalidSpeciesException.class, () -> repository.add(
                draft("Invalide", List.of(Type.FIRE), new BaseStats(100, 100, 100, 100, 100, 100)).build(repository)));
        assertEquals(30, repository.findAll().size());
        try (var paths = Files.list(folder.resolve("catalogue/assets"))) {
            assertEquals(0, paths.count());
        }
    }

    private SpeciesDraft draft(String name, List<Type> types, BaseStats stats) {
        return new SpeciesDraft(name, "Nouveau FR", types, stats, SpeciesDraft.availableMoves(repository).subList(0, 4),
                Talent.CARAPACE, front, null, null, " Créateur ", " ");
    }
}
