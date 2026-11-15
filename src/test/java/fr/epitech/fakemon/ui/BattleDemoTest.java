package fr.epitech.fakemon.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.epitech.fakemon.core.api.BattleEndedEvent;
import fr.epitech.fakemon.core.api.BattleEndReason;
import fr.epitech.fakemon.core.api.Side;
import fr.epitech.fakemon.core.api.FakemonEnteredEvent;
import fr.epitech.fakemon.core.api.FakemonFaintedEvent;
import fr.epitech.fakemon.core.api.FakemonSelectionRequiredEvent;
import fr.epitech.fakemon.core.api.HpLostEvent;
import fr.epitech.fakemon.core.api.MoveUsedEvent;
import fr.epitech.fakemon.core.api.SelectionReason;
import fr.epitech.fakemon.core.api.SendFakemon;
import fr.epitech.fakemon.core.api.TurnStartedEvent;
import org.junit.jupiter.api.Test;

class BattleDemoTest {
    @Test
    void exhaustedMoveDoesNotProduceEvents() {
        BattleDemo demo = new BattleDemo();
        assertTrue(demo.play(3).isEmpty());
        assertEquals(150, demo.opponent.getCurrentHp());
        assertEquals(1, assertInstanceOf(TurnStartedEvent.class, demo.play(0).getFirst()).getTurnNumber());
    }

    @Test
    void voluntarySwitchConsumesATurnAndPreservesEachMembersPp() {
        BattleDemo demo = new BattleDemo();
        assertEquals(6, demo.playerTeam.size());
        var initial = demo.activePlayer();
        var incoming = demo.playerTeam.get(1);
        var attack = demo.play(0);
        assertInstanceOf(MoveUsedEvent.class, attack.get(1));
        assertEquals(120, assertInstanceOf(HpLostEvent.class, attack.get(2)).getRemainingHp());
        assertEquals(3, demo.remainingPp(0));
        var switched = demo.send(new SendFakemon(incoming));
        assertEquals(2, assertInstanceOf(TurnStartedEvent.class, switched.getFirst()).getTurnNumber());
        assertSame(incoming, assertInstanceOf(FakemonEnteredEvent.class, switched.get(1)).getFakemon());
        assertEquals(60, incoming.getCurrentHp());
        assertEquals(4, demo.remainingPp(0));
        assertTrue(demo.send(new SendFakemon(incoming)).isEmpty());
        assertTrue(demo.send(new SendFakemon(demo.opponent)).isEmpty());
        demo.send(new SendFakemon(initial));
        assertEquals(3, demo.remainingPp(0));
    }

    @Test
    void faintingRequiresAValidReplacementBeforeTheNextTurn() {
        BattleDemo demo = new BattleDemo();
        var fainted = demo.activePlayer();
        for (int index = 0; index < 3; index++) {
            demo.play(0);
        }
        var lastTurn = demo.play(0);
        assertInstanceOf(FakemonFaintedEvent.class, lastTurn.get(lastTurn.size() - 2));
        assertEquals(SelectionReason.REPLACEMENT,
                assertInstanceOf(FakemonSelectionRequiredEvent.class, lastTurn.getLast()).getReason());
        assertEquals(0, fainted.getCurrentHp());
        assertFalse(demo.canAct());
        assertTrue(demo.play(1).isEmpty());
        var replacement = demo.playerTeam.get(1);
        var entered = demo.send(new SendFakemon(replacement));
        assertEquals(1, entered.size());
        assertSame(replacement, assertInstanceOf(FakemonEnteredEvent.class, entered.getFirst()).getFakemon());
        assertEquals(replacement.getMaxHp(), replacement.getCurrentHp());
        assertTrue(demo.send(new SendFakemon(fainted)).isEmpty());
        assertEquals(5, assertInstanceOf(TurnStartedEvent.class, demo.play(0).getFirst()).getTurnNumber());
    }

    @Test
    void victoryStopsTheScenario() {
        BattleDemo demo = new BattleDemo(new java.util.Random(42));
        for (int index = 0; index < 4; index++) {
            demo.play(0);
        }
        assertEquals(0, demo.remainingPp(0));
        assertTrue(demo.play(0).isEmpty());
        var events = demo.play(1);
        BattleEndedEvent result = (BattleEndedEvent) events.getLast();
        assertEquals(Side.PLAYER, result.getWinner());
        assertEquals(0, demo.opponent.getCurrentHp());
        assertTrue(demo.play(1).isEmpty());
        assertTrue(demo.forfeit().isEmpty());
    }
    @Test
    void forfeitProducesOneDefeatAndStopsTheScenario() {
        BattleDemo demo = new BattleDemo();
        demo.play(0);
        BattleEndedEvent result = (BattleEndedEvent) demo.forfeit().get(0);
        assertEquals(Side.OPPONENT, result.getWinner());
        assertEquals(BattleEndReason.FORFEIT, result.getReason());
        assertEquals(1, demo.getTurn());
        assertTrue(demo.forfeit().isEmpty());
        assertTrue(demo.play(0).isEmpty());
    }

    @Test
    void rematchDrawsFreshTeamsWithResetHpPpAndTurns() {
        BattleDemo first = new BattleDemo(new java.util.Random(42));
        BattleDemo second = new BattleDemo(new java.util.Random(43));
        first.play(0);
        assertEquals(6, second.playerTeam.size());
        assertEquals(6, second.opponentTeam.size());
        java.util.Set<String> names = new java.util.HashSet<>();
        for (var mon : second.playerTeam) {
            assertTrue(names.add(mon.getSpecies().getOriginalName()));
            assertEquals(mon.getMaxHp(), mon.getCurrentHp());
        }
        for (var mon : second.opponentTeam) {
            assertTrue(names.add(mon.getSpecies().getOriginalName()));
            assertEquals(mon.getMaxHp(), mon.getCurrentHp());
        }
        assertEquals(0, second.getTurn());
        assertEquals(4, second.remainingPp(0));
        assertNotSame(first.activePlayer(), second.activePlayer());
        assertNotEquals(first.playerTeam.stream().map(mon -> mon.getSpecies().getOriginalName()).toList(),
                second.playerTeam.stream().map(mon -> mon.getSpecies().getOriginalName()).toList());
    }
}
