package fr.epitech.fakemon.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.epitech.fakemon.core.api.InvalidSpeciesException;
import fr.epitech.fakemon.core.model.Species;
import org.junit.jupiter.api.Test;

/** Vérifie le catalogue en mémoire : lecture, recherche, ajout et refus des doublons. */
class InMemorySpeciesRepositoryTest {

    @Test
    void findAllReturnsThreeSpecies() {
        InMemorySpeciesRepository repository = new InMemorySpeciesRepository();
        assertEquals(3, repository.findAll().size());
    }

    @Test
    void findAllReturnsACopy() {
        InMemorySpeciesRepository repository = new InMemorySpeciesRepository();
        repository.findAll().clear();
        assertEquals(3, repository.findAll().size());
    }

    @Test
    void findByOriginalNameFindsExistingSpecies() {
        InMemorySpeciesRepository repository = new InMemorySpeciesRepository();
        assertTrue(repository.findByOriginalName("Anu").isPresent());
        assertEquals(
            "Anu",
            repository.findByOriginalName("Anu").get().getOriginalName()
        );
    }

    @Test
    void findByOriginalNameReturnsEmptyForUnknownName() {
        InMemorySpeciesRepository repository = new InMemorySpeciesRepository();
        assertFalse(repository.findByOriginalName("Bigachu").isPresent());
    }

    @Test
    void addAddsNewSpecies() {
        InMemorySpeciesRepository repository = new InMemorySpeciesRepository();
        repository.add(new Species("Zarlou"));
        assertTrue(repository.findByOriginalName("Zarlou").isPresent());
    }

    @Test
    void addRejectsDuplicate() {
        InMemorySpeciesRepository repository = new InMemorySpeciesRepository();
        assertThrows(InvalidSpeciesException.class, () ->
            repository.add(new Species("Anu"))
        );
    }
}
