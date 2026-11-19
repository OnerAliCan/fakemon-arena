package fr.epitech.fakemon.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import fr.epitech.fakemon.core.api.BattleEndedEvent;
import fr.epitech.fakemon.core.api.BattleEvent;
import fr.epitech.fakemon.core.api.FakemonEnteredEvent;
import fr.epitech.fakemon.core.api.FakemonSelectionRequiredEvent;
import fr.epitech.fakemon.core.api.HpLostEvent;
import fr.epitech.fakemon.core.api.MoveUsedEvent;
import fr.epitech.fakemon.core.api.SelectionReason;
import fr.epitech.fakemon.core.api.SendFakemon;
import fr.epitech.fakemon.core.api.Side;
import fr.epitech.fakemon.core.api.UseMove;
import fr.epitech.fakemon.core.engine.DefaultBattleEngine;
import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Species;
import fr.epitech.fakemon.data.JsonSpeciesRepository;
import fr.epitech.fakemon.data.SpeciesFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BattleSetupTest {
    @TempDir Path userFolder;

    @Test
    void networkTeamsKeepBothSelectionsAndStartersEvenWithSharedSpecies() {
        var roster = new JsonSpeciesRepository(new SpeciesFactory(), userFolder);
        var host = roster.findAll().subList(0, 6);
        var guest = new ArrayList<>(host);
        Collections.rotate(guest, 2);
        var setup = BattleSetup.networkConstructed(roster, host, guest, new Random(35));
        assertEquals(host, setup.player.getFakemons().stream().map(Fakemon::getSpecies).toList());
        assertEquals(guest, setup.opponent.getFakemons().stream().map(Fakemon::getSpecies).toList());
        assertNotSame(setup.player.getFakemons().getFirst(), setup.opponent.getFakemons().get(2));
        for (var team : List.of(setup.player, setup.opponent)) {
            assertTrue(team.getFakemons().stream().allMatch(mon -> mon.getItem() != null));
        }
        assertThrows(IllegalArgumentException.class,
                () -> BattleSetup.networkConstructed(roster, host, Collections.nCopies(6, host.getFirst()), new Random()));
        var foreignRoster = new JsonSpeciesRepository(new SpeciesFactory(), userFolder.resolve("other"));
        assertThrows(IllegalArgumentException.class,
                () -> BattleSetup.networkConstructed(roster, host, foreignRoster.findAll().subList(0, 6), new Random()));
    }

    @Test
    void sixDifferentSpeciesAreRequiredForAConstructedMatch() {
        var roster = new JsonSpeciesRepository(new SpeciesFactory(), userFolder);
        var species = roster.findAll().getFirst();
        assertThrows(IllegalArgumentException.class,
                () -> BattleSetup.constructed(roster, List.of(species), new Random(26)));
        assertThrows(IllegalArgumentException.class,
                () -> BattleSetup.constructed(roster, Collections.nCopies(6, species), new Random(26)));
    }

    @Test
    void constructedMatchWaitsForStarterAndKeepsTheLoadoutOnRematch() {
        var roster = new JsonSpeciesRepository(new SpeciesFactory(), userFolder);
        List<Species> chosen = roster.findAll().subList(0, 6);
        BattleSetup match = BattleSetup.constructed(roster, chosen, new Random(26));
        var engine = new DefaultBattleEngine();
        List<BattleEvent> events = new ArrayList<>();
        engine.addListener(events::add);
        match.start(engine);
        assertEquals(SelectionReason.FIRST_FAKEMON, ((FakemonSelectionRequiredEvent) events.getFirst()).getReason());
        var starter = match.player.getFakemons().getLast();
        engine.submitAction(new SendFakemon(starter));
        assertSame(starter, engine.getActiveFakemon(Side.PLAYER));

        var bench = match.player.getFakemons().getFirst();
        events.clear();
        engine.submitAction(new SendFakemon(bench));
        assertSame(bench, engine.getActiveFakemon(Side.PLAYER));
        assertTrue(events.stream().anyMatch(event -> event instanceof FakemonEnteredEvent entered
                && entered.getSide() == Side.PLAYER && entered.getFakemon() == bench));
        assertTrue(events.stream().anyMatch(event -> event instanceof MoveUsedEvent move
                && move.getSide() == Side.OPPONENT));
        var replay = match.rematch();
        assertEquals(chosen, replay.player.getFakemons().stream().map(Fakemon::getSpecies).toList());
        assertNotEquals(match.opponent.getFakemons().stream().map(Fakemon::getSpecies).toList(),
                replay.opponent.getFakemons().stream().map(Fakemon::getSpecies).toList());
        assertTrue(replay.opponent.getFakemons().stream().noneMatch(mon -> chosen.contains(mon.getSpecies())));
        for (int i = 0; i < 6; i++) {
            var oldMon = match.player.getFakemons().get(i);
            var newMon = replay.player.getFakemons().get(i);
            assertNotSame(oldMon, newMon);
            assertEquals(oldMon.getMoves(), newMon.getMoves());
            assertEquals(match.item(oldMon), replay.item(newMon));
            assertEquals(newMon.getMaxHp(), newMon.getCurrentHp());
        }
    }

    @Test
    void randomMatchPlaysThroughToAResultWithRequiredPlayerReplacements() {
        var roster = new JsonSpeciesRepository(new SpeciesFactory(), userFolder);
        BattleSetup match = BattleSetup.random(roster, new Random(319));
        assertEquals(6, match.player.getFakemons().size());
        assertEquals(6, match.opponent.getFakemons().size());
        var engine = new DefaultBattleEngine();
        List<BattleEvent> events = new ArrayList<>();
        engine.addListener(events::add);
        match.start(engine);
        BattleEndedEvent result = null;
        int replacements = 0;
        boolean realDamage = false;
        for (int turn = 0; turn < 250 && result == null; turn++) {
            events.clear();
            var active = engine.getActiveFakemon(Side.PLAYER);
            engine.submitAction(new UseMove(active.getMoves().get(turn % 4)));
            realDamage |= events.stream().anyMatch(event -> event instanceof HpLostEvent loss
                    && loss.getHpLost() != 30 && loss.getHpLost() != 25);
            result = events.stream().filter(BattleEndedEvent.class::isInstance)
                    .map(BattleEndedEvent.class::cast).findFirst().orElse(null);
            boolean mustReplace = events.stream().anyMatch(event -> event instanceof FakemonSelectionRequiredEvent ask
                    && ask.getReason() == SelectionReason.REPLACEMENT);
            if (mustReplace) {
                assertTrue(active.isFainted());
                assertThrows(IllegalStateException.class,
                        () -> engine.submitAction(new UseMove(active.getMoves().getFirst())));
                var next = match.player.getStandingFakemons().getFirst();
                engine.submitAction(new SendFakemon(next));
                assertSame(next, engine.getActiveFakemon(Side.PLAYER));
                replacements++;
            }
        }
        assertNotNull(result);
        assertTrue(realDamage);
        assertTrue(replacements > 0);
        assertFalse(engine.getTeam(result.getWinner() == Side.PLAYER ? Side.OPPONENT : Side.PLAYER)
                .hasStandingFakemon());
    }
}
