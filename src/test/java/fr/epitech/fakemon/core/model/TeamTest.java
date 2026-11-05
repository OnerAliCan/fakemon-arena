package fr.epitech.fakemon.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Vérifie la taille d'une équipe et la liste de ses Fakemon encore debout. */
class TeamTest {

    private Fakemon createFakemon() {
        Species species = new Species(
            "Test",
            "Test",
            List.of(),
            new BaseStats(100, 100, 100, 100, 100, 100),
            List.of()
        );
        Move move = new Move(
            "Test",
            Type.FIRE,
            MoveCategory.PHYSICAL,
            65,
            100,
            0
        );
        return new Fakemon(species, List.of(move, move, move, move));
    }

    @Test
    void constructorAcceptsSixFakemons() {
        Fakemon f = createFakemon();

        Team team = new Team(List.of(f, f, f, f, f, f));

        assertEquals(6, team.getFakemons().size());
    }

    @Test
    void constructorRejectsSevenFakemons() {
        Fakemon f = createFakemon();

        assertThrows(IllegalArgumentException.class, () ->
            new Team(List.of(f, f, f, f, f, f, f))
        );
    }

    @Test
    void standingFakemonsExcludeFainted() {
        Fakemon standing = createFakemon();
        Fakemon fainted = createFakemon();
        fainted.takeDamage(100);

        Team team = new Team(List.of(standing, fainted));

        assertEquals(List.of(standing), team.getStandingFakemons());
    }

    @Test
    void standingFakemonsKeepTeamIntact() {
        Fakemon standing = createFakemon();
        Fakemon fainted = createFakemon();
        fainted.takeDamage(100);
        Team team = new Team(List.of(standing, fainted));

        team.getStandingFakemons();

        assertEquals(2, team.getFakemons().size());
    }

    @Test
    void hasStandingFakemonWhenOneRemains() {
        Fakemon standing = createFakemon();
        Fakemon fainted = createFakemon();
        fainted.takeDamage(100);

        Team team = new Team(List.of(standing, fainted));

        assertTrue(team.hasStandingFakemon());
    }

    @Test
    void hasNoStandingFakemonWhenAllFainted() {
        Fakemon first = createFakemon();
        Fakemon second = createFakemon();
        first.takeDamage(100);
        second.takeDamage(100);

        Team team = new Team(List.of(first, second));

        assertFalse(team.hasStandingFakemon());
    }
}
