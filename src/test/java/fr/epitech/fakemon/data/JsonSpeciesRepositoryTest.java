package fr.epitech.fakemon.data;

import static org.junit.jupiter.api.Assertions.*;

import fr.epitech.fakemon.core.api.InvalidSpeciesException;
import fr.epitech.fakemon.core.model.BaseStats;
import fr.epitech.fakemon.core.model.Species;
import fr.epitech.fakemon.core.model.Type;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Vérifie le catalogue fourni et les sauvegardes utilisateur. */
class JsonSpeciesRepositoryTest {
    @TempDir
    Path tempDir;
    private Path userFolder;
    private Path front;
    private Path back;
    private JsonSpeciesRepository repository;

    @BeforeEach
    void setUp() throws IOException {
        userFolder = tempDir.resolve("catalogue");
        front = Files.writeString(tempDir.resolve("front.png"), "face");
        back = Files.writeString(tempDir.resolve("back.png"), "dos");
        repository = reload();
    }

    @Test
    void loadsBundledRosterAndResolvesMoves() {
        assertEquals(30, repository.findAll().size());
        var species = repository.findByOriginalName("Trantima").orElseThrow();
        assertEquals(1, species.getDexNumber());
        assertTrue(species.getPossibleMoves().size() >= 4);
        repository.findAll().clear();
        assertEquals(30, repository.findAll().size());
    }

    @Test
    void reloadsCopiedFilesAndMetadata() throws IOException {
        Path cry = Files.writeString(tempDir.resolve("cry.wav"), "cri");
        repository.add(candidate().concept("Concept").designer("Ali").cry(cry.toString()).build());
        assertTrue(repository.findByOriginalName("FakemonTest").isPresent());
        Files.delete(front);
        Files.delete(back);
        Files.delete(cry);

        Species saved = reload().findByOriginalName("FakemonTest").orElseThrow();
        assertEquals("Concept", saved.getConcept());
        assertEquals("Ali", saved.getDesigner());
        assertEquals(candidate().build().getPossibleMoves().stream().map(move -> move.getName()).toList(),
            saved.getPossibleMoves().stream().map(move -> move.getName()).toList());
        assertEquals("face", Files.readString(userFolder.resolve(saved.getFrontSprite())));
        assertEquals("dos", Files.readString(userFolder.resolve(saved.getBackSprite())));
        assertEquals("cri", Files.readString(userFolder.resolve(saved.getCry())));
    }

    @Test
    void leavesOptionalFieldsEmpty() {
        repository.add(candidate().build());
        Species saved = reload().findByOriginalName("FakemonTest").orElseThrow();
        assertNull(saved.getConcept());
        assertNull(saved.getDesigner());
        assertNull(saved.getCry());
    }

    @Test
    void rejectsDuplicateOriginalName() {
        assertRejected(candidate().originalName("Trantima").build(), "existe déjà");
    }

    @Test
    void rejectsInvalidTypes() {
        assertRejected(candidate().types(List.of()).build(), "types distincts");
        assertRejected(candidate().types(List.of(Type.FIRE, Type.FIRE)).build(), "types distincts");
        assertRejected(candidate().types(List.of(Type.FIRE, Type.WATER, Type.ROCK)).build(), "types distincts");
    }

    @Test
    void rejectsMissingTalent() {
        assertThrows(IllegalStateException.class, () -> candidate().talent(null).build());
    }

    @Test
    void rejectsTooFewDistinctMoves() {
        var move = repository.findAll().getFirst().getPossibleMoves().getFirst();
        assertRejected(candidate().possibleMoves(List.of(move, move, move, move)).build(), "quatre attaques");
    }

    @Test
    void rejectsInvalidStats() {
        assertRejected(candidate().stats(new BaseStats(49, 100, 100, 100, 100, 100)).build(), "50 et 150");
        assertRejected(candidate().stats(new BaseStats(101, 100, 100, 100, 100, 100)).build(), "600");
    }

    @Test
    void rejectsMissingFiles() {
        String missing = tempDir.resolve("absent").toString();
        assertRejected(candidate().frontSprite(missing).build(), "sprite de face");
        assertRejected(candidate().backSprite(missing).build(), "sprite de dos");
        assertRejected(candidate().cry(missing).build(), "cri");
    }

    @Test
    void refusesReloadWhenCopiedSpriteIsMissing() throws IOException {
        repository.add(candidate().build());
        Species saved = repository.findByOriginalName("FakemonTest").orElseThrow();
        Files.delete(userFolder.resolve(saved.getFrontSprite()));

        var error = assertThrows(InvalidSpeciesException.class, this::reload);
        assertTrue(error.getMessage().contains("FakemonTest"));
        assertTrue(error.getMessage().contains("sprite de face"));
        assertTrue(error.getMessage().contains(userFolder.resolve("species").toString()));
    }

    @Test
    void validatesStatsAgainOnReload() throws IOException {
        repository.add(candidate().build());
        Path file;
        try (var files = Files.list(userFolder.resolve("species"))) {
            file = files.findFirst().orElseThrow();
        }
        String json = Files.readString(file);
        Files.writeString(file, json.replace("\"hp\":100", "\"hp\":49"));
        var error = assertThrows(InvalidSpeciesException.class, this::reload);
        assertTrue(error.getMessage().contains("50 et 150"));
        assertTrue(error.getMessage().contains(file.toString()));
    }

    private void assertRejected(Species species, String expectedMessage) {
        var error = assertThrows(InvalidSpeciesException.class, () -> repository.add(species));
        assertTrue(error.getMessage().contains(expectedMessage), error.getMessage());
        assertEquals(30, repository.findAll().size());
        assertFalse(Files.exists(userFolder));
    }

    private Species.Builder candidate() {
        Species template = repository.findAll().getFirst();
        return new Species.Builder()
            .originalName("FakemonTest")
            .frenchName("Fakemon de test")
            .dexNumber(31)
            .types(template.getTypes())
            .stats(new BaseStats(100, 100, 100, 100, 100, 100))
            .possibleMoves(template.getPossibleMoves())
            .talent(template.getTalent())
            .frontSprite(front.toString())
            .backSprite(back.toString());
    }

    private JsonSpeciesRepository reload() {
        return new JsonSpeciesRepository(new SpeciesFactory(), userFolder);
    }
}
