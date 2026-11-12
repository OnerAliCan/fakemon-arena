package fr.epitech.fakemon.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.epitech.fakemon.core.api.InvalidSpeciesException;
import fr.epitech.fakemon.core.engine.TeamDrafter;
import fr.epitech.fakemon.core.model.BaseStats;
import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Species;
import fr.epitech.fakemon.core.model.Team;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Vérifie le catalogue en mémoire : lecture, recherche, ajout et refus des doublons. */
class InMemorySpeciesRepositoryTest {

    private Species createSpecies(String name) {
        return new Species(
            name,
            name,
            List.of(),
            new BaseStats(100, 100, 100, 100, 100, 100),
            List.of()
        );
    }

    @Test
    void findAllReturnsTwelveSpecies() {
        InMemorySpeciesRepository repository = new InMemorySpeciesRepository();
        assertEquals(12, repository.findAll().size());
    }

    @Test
    void findAllReturnsACopy() {
        InMemorySpeciesRepository repository = new InMemorySpeciesRepository();
        repository.findAll().clear();
        assertEquals(12, repository.findAll().size());
    }

    @Test
    void everySpeciesHasAtLeastFourPossibleMoves() {
        InMemorySpeciesRepository repository = new InMemorySpeciesRepository();
        for (Species species : repository.findAll()) {
            assertTrue(species.getPossibleMoves().size() >= 4);
        }
    }

    @Test
    void isEnoughToDrawTwoTeams() {
        InMemorySpeciesRepository repository = new InMemorySpeciesRepository();
        TeamDrafter drafter = new TeamDrafter(repository, new Random(50));

        Team playerTeam = drafter.randomTeam(List.of());
        List<Species> playerSpecies = new ArrayList<>();
        for (Fakemon fakemon : playerTeam.getFakemons()) {
            playerSpecies.add(fakemon.getSpecies());
        }
        Team opponentTeam = drafter.randomTeam(playerSpecies);

        assertEquals(6, opponentTeam.getFakemons().size());
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
        repository.add(createSpecies("Zarlou"));
        assertTrue(repository.findByOriginalName("Zarlou").isPresent());
    }

    @Test
    void addRejectsDuplicate() {
        InMemorySpeciesRepository repository = new InMemorySpeciesRepository();
        assertThrows(InvalidSpeciesException.class, () ->
            repository.add(createSpecies("Anu"))
        );
    }
}
