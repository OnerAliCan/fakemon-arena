package fr.epitech.fakemon.ui;

import static org.junit.jupiter.api.Assertions.*;

import fr.epitech.fakemon.data.BundledRoster;
import fr.epitech.fakemon.core.api.SpeciesRepository;
import fr.epitech.fakemon.core.model.Species;
import java.util.List;
import java.util.Set;
import java.util.Collections;
import java.util.Random;
import org.junit.jupiter.api.Test;

class ConstructedMatchTest {
    private final SpeciesRepository roster = BundledRoster.load();
    private final List<Species> selection = roster.findAll().subList(0, 6);

    @Test
    void requiresSixDistinctSpeciesFromRoster() {
        assertEquals(30, roster.findAll().size());
        assertThrows(IllegalArgumentException.class, () -> ConstructedMatch.start(
                roster, selection.subList(0, 5), new Random(46)));
        assertThrows(IllegalArgumentException.class, () -> ConstructedMatch.start(
                roster, Collections.nCopies(6, selection.getFirst()), new Random(46)));
    }

    @Test
    void drawsValidLoadoutsAndRequiresFirstChoice() {
        var match = ConstructedMatch.start(roster, selection, new Random(46));
        assertEquals(selection, match.playerLoadouts.stream().map(ConstructedMatch.Loadout::species).toList());
        assertEquals(6, match.opponentLoadouts.stream().map(ConstructedMatch.Loadout::species).distinct().count());
        assertTrue(match.opponentLoadouts.stream().noneMatch(loadout -> selection.contains(loadout.species())));
        for (var loadout : match.playerLoadouts) {
            assertEquals(4, loadout.moves().stream().distinct().count());
            assertTrue(loadout.species().getPossibleMoves().containsAll(loadout.moves()));
            assertTrue(Set.of("Gantelet", "Écu", "Plume").contains(loadout.item()));
        }
        assertNull(match.active);
        assertFalse(match.choose(match.opponent()));
        var starter = match.playerTeam.getLast();
        assertTrue(match.choose(starter));
        assertSame(starter, match.active);
        assertEquals(0, match.turn);
    }

    @Test
    void rematchKeepsPlayerLoadoutsAndRedrawsOpponent() {
        var match = ConstructedMatch.start(roster, selection, new Random(46));
        assertTrue(match.choose(match.playerTeam.getFirst()));
        match.attack(match.active.getMoves().getFirst());
        var revenge = match.rematch();
        assertEquals(match.playerLoadouts, revenge.playerLoadouts);
        assertNotEquals(match.opponentLoadouts.stream().map(ConstructedMatch.Loadout::species).toList(),
                revenge.opponentLoadouts.stream().map(ConstructedMatch.Loadout::species).toList());
        assertNull(revenge.active);
        assertEquals(0, revenge.turn);
        assertNotSame(match.playerTeam.getFirst(), revenge.playerTeam.getFirst());
        assertTrue(revenge.playerTeam.stream().allMatch(mon -> mon.getCurrentHp() == mon.getMaxHp()));
    }
}
