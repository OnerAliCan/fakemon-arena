package fr.epitech.fakemon.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
