package fr.epitech.fakemon.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Vérifie le nombre d'attaques d'un Fakemon et les limites de ses PV. */
class FakemonTest {

    private Species createSpecies() {
        return new Species(
            "Test",
            "Test",
            List.of(),
            new BaseStats(100, 100, 100, 100, 100, 100),
            List.of()
        );
    }

    private Move createMove() {
        return new Move("Test", Type.FIRE, MoveCategory.PHYSICAL, 65, 100, 0);
    }

    private Fakemon createFakemon() {
        Move move = createMove();
        return new Fakemon(createSpecies(), List.of(move, move, move, move));
    }

    @Test
    void constructorRejectsThreeMoves() {
        Species species = createSpecies();
        Move move = createMove();

        assertThrows(IllegalArgumentException.class, () ->
            new Fakemon(species, List.of(move, move, move))
        );
    }

    @Test
    void constructorRejectsFiveMoves() {
        Species species = createSpecies();
        Move move = createMove();

        assertThrows(IllegalArgumentException.class, () ->
            new Fakemon(species, List.of(move, move, move, move, move))
        );
    }

    @Test
    void startsWithFullHp() {
        Fakemon fakemon = createFakemon();

        assertEquals(100, fakemon.getCurrentHp());
    }

    @Test
    void takeDamageRemovesHp() {
        Fakemon fakemon = createFakemon();

        fakemon.takeDamage(30);

        assertEquals(70, fakemon.getCurrentHp());
    }

    @Test
    void takeDamageStopsAtZero() {
        Fakemon fakemon = createFakemon();

        fakemon.takeDamage(150);

        assertEquals(0, fakemon.getCurrentHp());
    }

    @Test
    void isNotFaintedWhenHpRemain() {
        Fakemon fakemon = createFakemon();

        assertFalse(fakemon.isFainted());
    }

    @Test
    void isFaintedAtZeroHp() {
        Fakemon fakemon = createFakemon();

        fakemon.takeDamage(100);

        assertTrue(fakemon.isFainted());
    }

    @Test
    void healRestoresHp() {
        Fakemon fakemon = createFakemon();

        fakemon.takeDamage(50);
        fakemon.heal(20);

        assertEquals(70, fakemon.getCurrentHp());
    }

    @Test
    void healStopsAtMaxHp() {
        Fakemon fakemon = createFakemon();

        fakemon.takeDamage(10);
        fakemon.heal(50);

        assertEquals(100, fakemon.getCurrentHp());
    }
}
