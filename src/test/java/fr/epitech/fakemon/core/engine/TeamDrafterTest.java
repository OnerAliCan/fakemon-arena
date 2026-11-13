package fr.epitech.fakemon.core.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.epitech.fakemon.core.api.SpeciesRepository;
import fr.epitech.fakemon.core.model.BaseStats;
import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Move;
import fr.epitech.fakemon.core.model.MoveCategory;
import fr.epitech.fakemon.core.model.Species;
import fr.epitech.fakemon.core.model.Team;
import fr.epitech.fakemon.core.model.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Vérifie le tirage des équipes sur un faux catalogue, avec des graines fixes pour rester reproductible. */
class TeamDrafterTest {
    private static final long SEED = 50;

    @Test
    void twoTeamsOfSixWithoutDuplicate() {
        TeamDrafter drafter = new TeamDrafter(createRepository(12), new Random(SEED));

        Team teamA = drafter.randomTeam(List.of());
        Team teamB = drafter.randomTeam(speciesOf(teamA));

        assertEquals(6, teamA.getFakemons().size());
        assertEquals(6, teamB.getFakemons().size());

        Set<Species> allSpecies = new HashSet<>();
        allSpecies.addAll(speciesOf(teamA));
        allSpecies.addAll(speciesOf(teamB));
        assertEquals(12, allSpecies.size());
    }

    @Test
    void eachFakemonHasFourDifferentPossibleMoves() {
        TeamDrafter drafter = new TeamDrafter(createRepository(12), new Random(SEED));

        Team team = drafter.randomTeam(List.of());

        for (Fakemon fakemon : team.getFakemons()) {
            List<Move> moves = fakemon.getMoves();
            assertEquals(4, new HashSet<>(moves).size());
            assertTrue(fakemon.getSpecies().getPossibleMoves().containsAll(moves));
        }
    }

    @Test
    void sameSeedGivesSameTeam() {
        SpeciesRepository repository = createRepository(12);
        Team firstTeam = new TeamDrafter(repository, new Random(SEED)).randomTeam(List.of());
        Team secondTeam = new TeamDrafter(repository, new Random(SEED)).randomTeam(List.of());

        assertEquals(speciesOf(firstTeam), speciesOf(secondTeam));
        for (int i = 0; i < 6; i++) {
            assertEquals(
                firstTeam.getFakemons().get(i).getMoves(),
                secondTeam.getFakemons().get(i).getMoves()
            );
        }
    }

    @Test
    void lessThanTwelveSpeciesThrows() {
        TeamDrafter drafter = new TeamDrafter(createRepository(11), new Random(SEED));

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> drafter.randomTeam(List.of())
        );
        assertFalse(exception.getMessage().isBlank());
    }

    @Test
    void buildTeamKeepsChosenSpeciesInOrder() {
        SpeciesRepository repository = createRepository(12);
        TeamDrafter drafter = new TeamDrafter(repository, new Random(SEED));
        List<Species> chosenSpecies = repository.findAll().subList(0, 6);

        Team team = drafter.buildTeam(chosenSpecies);

        assertEquals(chosenSpecies, speciesOf(team));
        for (Fakemon fakemon : team.getFakemons()) {
            assertEquals(4, new HashSet<>(fakemon.getMoves()).size());
        }
    }

    private List<Species> speciesOf(Team team) {
        List<Species> speciesList = new ArrayList<>();
        for (Fakemon fakemon : team.getFakemons()) {
            speciesList.add(fakemon.getSpecies());
        }
        return speciesList;
    }

    private SpeciesRepository createRepository(int speciesCount) {
        List<Species> speciesList = new ArrayList<>();
        for (int i = 1; i <= speciesCount; i++) {
            speciesList.add(createSpecies("Espece" + i));
        }
        return new FakeSpeciesRepository(speciesList);
    }

    /** Crée une espèce avec 6 attaques possibles, pour que le tirage de 4 attaques ait du choix. */
    private Species createSpecies(String name) {
        List<Move> possibleMoves = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            possibleMoves.add(new Move(name + "-attaque" + i, Type.FIRE, MoveCategory.PHYSICAL, 40, 100, 0));
        }
        BaseStats stats = new BaseStats(100, 100, 100, 100, 100, 100);
        return new Species(name, name, List.of(Type.FIRE), stats, possibleMoves);
    }

    /** Faux catalogue : le générateur n'utilise que findAll. */
    private static class FakeSpeciesRepository implements SpeciesRepository {
        private final List<Species> speciesList;

        FakeSpeciesRepository(List<Species> speciesList) {
            this.speciesList = speciesList;
        }

        @Override
        public List<Species> findAll() {
            return new ArrayList<>(speciesList);
        }

        @Override
        public Optional<Species> findByOriginalName(String originalName) {
            throw new UnsupportedOperationException("Non utilisé par le générateur");
        }

        @Override
        public void add(Species species) {
            throw new UnsupportedOperationException("Non utilisé par le générateur");
        }
    }
}
