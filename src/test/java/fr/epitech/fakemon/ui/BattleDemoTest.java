package fr.epitech.fakemon.ui;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import fr.epitech.fakemon.core.api.*;

/** Vérifie les limites du scénario utilisé pour démontrer l'interface. */
class BattleDemoTest {
    @Test
    void exhaustedMoveDoesNotProduceEvents() {
        BattleDemo demo = new BattleDemo();
        assertTrue(demo.play(3).isEmpty());
        assertEquals(150, demo.opponent.getCurrentHp());
        assertEquals(1, ((TurnStartedEvent) demo.play(0).get(0)).getTurnNumber());
    }

    @Test
    void scenarioConsumesPpAndClampsDamageAtZero() {
        BattleDemo demo = new BattleDemo();
        for (int index = 0; index < 4; index++) {
            var events = demo.play(0);
            assertInstanceOf(TurnStartedEvent.class, events.get(0));
            assertInstanceOf(MoveUsedEvent.class, events.get(1));
            assertEquals(150 - 30 * (index + 1), ((HpLostEvent) events.get(2)).getRemainingHp());
        }
        assertEquals(0, demo.pp[0]);
        assertTrue(demo.play(0).isEmpty());
        demo.play(1);
        var events = demo.play(1);
        assertEquals(0, ((HpLostEvent) events.get(2)).getHpLost());
        assertEquals(0, demo.opponent.getCurrentHp());
    }
}
