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
        var events = demo.play(1);
        BattleEndedEvent result = (BattleEndedEvent) events.get(3);
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
        assertEquals(6, second.playerTeam.getFakemons().size());
        assertEquals(6, second.opponentTeam.getFakemons().size());
        java.util.Set<String> names = new java.util.HashSet<>();
        for (var mon : second.playerTeam.getFakemons()) {
            assertTrue(names.add(mon.getSpecies().getOriginalName()));
            assertEquals(mon.getMaxHp(), mon.getCurrentHp());
        }
        for (var mon : second.opponentTeam.getFakemons()) {
            assertTrue(names.add(mon.getSpecies().getOriginalName()));
            assertEquals(mon.getMaxHp(), mon.getCurrentHp());
        }
        assertEquals(0, second.getTurn());
        assertEquals(4, second.pp[0]);
        assertNotSame(first.player, second.player);
        assertNotEquals(first.player.getSpecies().getOriginalName(), second.player.getSpecies().getOriginalName());
    }
}
