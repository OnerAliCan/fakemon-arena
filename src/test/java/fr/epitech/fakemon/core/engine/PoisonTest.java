package fr.epitech.fakemon.core.engine;

import static org.junit.jupiter.api.Assertions.*;

import fr.epitech.fakemon.core.api.*;
import fr.epitech.fakemon.core.effects.Burn;
import fr.epitech.fakemon.core.effects.Poison;
import fr.epitech.fakemon.core.model.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Vérifie les règles du poison et son application par ses deux attaques. */
class PoisonTest {
    private static final Move IDLE = new Move("Attente", Type.FORCE, MoveCategory.PHYSICAL, 0, 0, 0);
    private static final Move SPORES = new Move("Spores toxiques", Type.VENOM, MoveCategory.STATUS,
            0, 100, 0, StatusType.POISON, 100);
    private static final Move TOXIN = new Move("Toxine", Type.VENOM, MoveCategory.SPECIAL,
            50, 100, 0, StatusType.POISON, 20);

    @Test
    void poisonTakesOneEighthFlooredWithAMinimumOfOneAndClampsToRemainingHp() {
        Poison poison = new Poison();
        var target = mon("Cible", Type.FORCE, 100, 10, IDLE);
        assertEquals(StatusType.POISON, poison.getType());
        assertEquals(12, poison.endOfTurnDamage(target));
        assertEquals(1, poison.endOfTurnDamage(mon("Petit", Type.FORCE, 7, 10, IDLE)));
        target.takeDamage(98);
        assertEquals(2, poison.endOfTurnDamage(target));
        target.takeDamage(2);
        assertEquals(0, poison.endOfTurnDamage(target));
    }

    @Test
    void onlyVenomTypesAreImmuneAndBurnAndPoisonCannotReplaceEachOther() {
        assertFalse(mon("Venin", Type.VENOM, 100, 10, IDLE).applyStatus(new Poison()));
        assertTrue(mon("Feu", Type.FIRE, 100, 10, IDLE).applyStatus(new Poison()));
        var target = mon("Cible", Type.FORCE, 100, 10, IDLE);
        var poison = new Poison();
        assertTrue(target.applyStatus(poison));
        assertFalse(target.applyStatus(new Burn()));
        assertSame(poison, target.getStatus());
        target.clearStatus();
        var burn = new Burn();
        assertTrue(target.applyStatus(burn));
        assertFalse(target.applyStatus(new Poison()));
        assertSame(burn, target.getStatus());
    }

    @Test
    void sporesGuaranteePoisonButOnlyDealResidualDamage() {
        var duel = duel(SPORES, Type.FORCE, new Random(32));
        assertEquals(88, duel.target.getCurrentHp());
        assertEquals(StatusType.POISON, duel.target.getStatus().getType());
        assertEquals(1, duel.events.stream().filter(StatusAppliedEvent.class::isInstance).count());
        assertEquals(1, duel.events.stream().filter(StatusActivatedEvent.class::isInstance).count());
        var losses = duel.events.stream().filter(HpLostEvent.class::isInstance).map(HpLostEvent.class::cast).toList();
        assertEquals(1, losses.size());
        assertEquals(12, losses.getFirst().getHpLost());
        assertTrue(duel.events.stream().noneMatch(CriticalHitEvent.class::isInstance));
        duel.engine.submitAction(new UseMove(SPORES));
        assertEquals(76, duel.target.getCurrentHp());
        assertEquals(1, duel.events.stream().filter(StatusAppliedEvent.class::isInstance).count());
    }

    @Test
    void toxinsTwentyPercentRollUsesTheSameThresholdAsBurn() {
        for (int roll : List.of(19, 20)) {
            var duel = duel(TOXIN, Type.FORCE, new ThresholdRandom(roll));
            assertEquals(roll < 20, duel.target.getStatus() != null);
            var losses = duel.events.stream().filter(HpLostEvent.class::isInstance).map(HpLostEvent.class::cast).toList();
            assertEquals(roll < 20 ? 2 : 1, losses.size());
            assertTrue(losses.getFirst().getHpLost() > 0);
            if (roll < 20) {
                assertEquals(12, losses.getLast().getHpLost());
            }
        }
    }

    @Test
    void venomImmunityBlocksPureStatusButNotTheDirectDamageOfToxin() {
        var spores = duel(SPORES, Type.VENOM, new Random(32));
        assertEquals(100, spores.target.getCurrentHp());
        assertNull(spores.target.getStatus());
        var toxin = duel(TOXIN, Type.VENOM, new ThresholdRandom(19));
        assertTrue(toxin.target.getCurrentHp() < 100);
        assertNull(toxin.target.getStatus());
        assertTrue(toxin.events.stream().noneMatch(StatusAppliedEvent.class::isInstance));
    }

    @Test
    void poisonCanFinishTheBattleAndIsClearedWithTheResult() {
        var duel = duel(SPORES, Type.FORCE, new Random(32));
        for (int turn = 1; turn < 9; turn++) {
            duel.engine.submitAction(new UseMove(SPORES));
        }
        assertTrue(duel.target.isFainted());
        assertNull(duel.target.getStatus());
        assertEquals(1, duel.events.stream().filter(FakemonFaintedEvent.class::isInstance).count());
        assertEquals(1, duel.events.stream().filter(BattleEndedEvent.class::isInstance).count());
    }

    private static Duel duel(Move move, Type targetType, Random random) {
        var player = mon("Joueur", Type.FORCE, 100, 100, move);
        var target = mon("Cible", targetType, 100, 10, IDLE);
        var events = new ArrayList<BattleEvent>();
        var engine = new DefaultBattleEngine();
        engine.addListener(events::add);
        engine.start(new Team(List.of(player)), new Team(List.of(target)), BattleMode.RANDOM, new Opponent() {
            @Override
            public PlayerAction chooseAction(List<Move> moves, List<Fakemon> standing) { return new UseMove(IDLE); }

            @Override
            public PlayerAction chooseFakemon(List<Fakemon> standing) { return new SendFakemon(standing.getFirst()); }
        }, random);
        events.clear();
        engine.submitAction(new UseMove(move));
        return new Duel(target, engine, events);
    }

    private static Fakemon mon(String name, Type type, int hp, int speed, Move move) {
        var moves = List.of(move, IDLE, IDLE, IDLE);
        Species species = new Species.Builder().originalName(name).frenchName(name).types(List.of(type))
                .stats(new BaseStats(hp, 10, 100, 10, 100, speed)).possibleMoves(moves)
                .talent(Talent.CARAPACE).frontSprite("/sprites/front/test.png")
                .backSprite("/sprites/back/test.png").build();
        return new Fakemon(species, moves);
    }

    private record Duel(Fakemon target, DefaultBattleEngine engine, List<BattleEvent> events) { }

    private static final class ThresholdRandom extends Random {
        private final int roll;

        private ThresholdRandom(int roll) {
            super(32);
            this.roll = roll;
        }

        @Override
        public int nextInt(int bound) { return bound == 100 ? roll : 1; }
    }
}
