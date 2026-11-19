package fr.epitech.fakemon.core.engine;

import static org.junit.jupiter.api.Assertions.*;

import fr.epitech.fakemon.core.api.*;
import fr.epitech.fakemon.core.effects.Confusion;
import fr.epitech.fakemon.core.model.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Vérifie les trois tentatives de confusion, les blessures et leurs K.O. éventuels. */
class ConfusionTest {
    private static final Move IDLE = new Move("Attente", Type.FORCE, MoveCategory.PHYSICAL, 0, 0, 0);
    private static final Move HIT = new Move("Frappe", Type.FORCE, MoveCategory.PHYSICAL, 10, 100, 0);
    private static final Move CONFUSE = new Move("Désorientation", Type.SHADOW, MoveCategory.STATUS,
            0, 100, 0, StatusType.CONFUSION, 100);

    @Test
    void exactlyOneOfTheThreeRollsPreventsAttackingAndTheThirdAttemptEndsConfusion() {
        int blocked = 0;
        for (int roll = 0; roll < 3; roll++) {
            var effect = new Confusion();
            var random = new FixedRandom(roll);
            for (int attempt = 0; attempt < 3; attempt++) {
                boolean prevented = effect.preventsAttack(random);
                assertEquals(roll == 0, prevented);
                assertEquals(attempt == 2, effect.hasEnded());
                if (prevented) { blocked++; }
            }
            assertFalse(effect.preventsAttack(random));
        }
        assertEquals(3, blocked);
    }

    @Test
    void woundsUseOneEighthFlooredWithAMinimumOfOneAndNeverExceedRemainingHealth() {
        var effect = new Confusion();
        var target = mon("Cible", Type.FORCE, 100, 100, HIT);
        assertEquals(12, effect.preventedAttackDamage(target));
        assertEquals(1, effect.preventedAttackDamage(mon("Petit", Type.FORCE, 7, 100, HIT)));
        target.takeDamage(98);
        assertEquals(2, effect.preventedAttackDamage(target));
        target.takeDamage(2);
        assertEquals(0, effect.preventedAttackDamage(target));
        assertEquals(0, effect.endOfTurnDamage(target));
    }

    @Test
    void confusionLastsThreeAttemptsWhetherTheFakemonAttacksOrHurtsItself() {
        for (int roll = 0; roll < 3; roll++) {
            var battle = new Battle(100, Type.FORCE, HIT, IDLE, roll, false);
            battle.player.applyStatus(new Confusion());
            for (int turn = 0; turn < 3; turn++) {
                battle.play(new UseMove(HIT));
                assertEquals(roll == 0 ? 1 : 0, battle.events(AttackPreventedEvent.class).size());
                assertEquals(turn == 2, battle.player.getStatus() == null);
            }
            assertEquals(roll == 0 ? 64 : 100, battle.player.getCurrentHp());
            assertEquals(roll == 0 ? 100 : 85, battle.target.getCurrentHp());
            assertEquals(1, battle.events(StatusRemovedEvent.class).size());
            battle.play(new UseMove(HIT));
            assertEquals(roll == 0 ? 95 : 80, battle.target.getCurrentHp());
            assertTrue(battle.events(AttackPreventedEvent.class).isEmpty());
        }
    }

    @Test
    void selfKnockoutNeverLetsTheOtherFakemonAttackAnAlreadyFaintedTarget() {
        for (Side side : Side.values()) {
            var battle = new Battle(side == Side.PLAYER ? 100 : 50, Type.FORCE, HIT, HIT, 0, false);
            Fakemon actor = side == Side.PLAYER ? battle.player : battle.target;
            actor.applyStatus(new Confusion());
            actor.takeDamage(90);
            battle.play(new UseMove(HIT));
            assertTrue(actor.isFainted());
            assertTrue(battle.events(MoveUsedEvent.class).isEmpty());
            assertEquals(10, battle.events(HpLostEvent.class).getFirst().getHpLost());
            assertEquals(side, battle.events(FakemonFaintedEvent.class).getFirst().getSide());
            var result = battle.events(BattleEndedEvent.class).getFirst();
            assertEquals(side == Side.PLAYER ? Side.OPPONENT : Side.PLAYER, result.getWinner());
            assertNull(actor.getStatus());
        }
    }

    @Test
    void selfKnockoutUsesTheNormalReplacementFlowForBothSides() {
        for (Side side : Side.values()) {
            var battle = new Battle(side == Side.PLAYER ? 100 : 50, Type.FORCE, HIT, HIT, 0, true);
            Fakemon actor = side == Side.PLAYER ? battle.player : battle.target;
            actor.applyStatus(new Confusion());
            actor.takeDamage(90);
            battle.play(new UseMove(HIT));
            assertTrue(actor.isFainted());
            assertTrue(battle.events(MoveUsedEvent.class).isEmpty());
            assertTrue(battle.events(BattleEndedEvent.class).isEmpty());
            if (side == Side.PLAYER) {
                assertEquals(SelectionReason.REPLACEMENT,
                        battle.events(FakemonSelectionRequiredEvent.class).getFirst().getReason());
                battle.play(new SendFakemon(battle.substitute));
                assertSame(battle.substitute, battle.engine.getActiveFakemon(Side.PLAYER));
            } else {
                assertSame(battle.enemySubstitute, battle.engine.getActiveFakemon(Side.OPPONENT));
            }
        }
    }

    @Test
    void switchingKeepsConfusionAndPausesTheThreeAttemptCounter() {
        var battle = new Battle(100, Type.FORCE, HIT, IDLE, 1, true);
        battle.player.applyStatus(new Confusion());
        battle.play(new UseMove(HIT));
        battle.play(new SendFakemon(battle.substitute));
        battle.play(new UseMove(HIT));
        battle.play(new SendFakemon(battle.player));
        battle.play(new UseMove(HIT));
        assertNotNull(battle.player.getStatus());
        battle.play(new UseMove(HIT));
        assertNull(battle.player.getStatus());
        assertEquals(80, battle.target.getCurrentHp());
    }

    @Test
    void disorientationDealsNoDirectDamageAndAllTypesCanReceiveConfusion() {
        for (Type type : Type.values()) {
            var battle = new Battle(100, type, CONFUSE, IDLE, 1, false);
            battle.play(new UseMove(CONFUSE));
            assertEquals(StatusType.CONFUSION, battle.target.getStatus().getType());
            assertEquals(100, battle.target.getCurrentHp());
            assertTrue(battle.events(HpLostEvent.class).isEmpty());
            assertTrue(battle.events(CriticalHitEvent.class).isEmpty());
        }
        var hurt = new Battle(100, Type.FORCE, CONFUSE, HIT, 0, false);
        hurt.play(new UseMove(CONFUSE));
        assertEquals(88, hurt.target.getCurrentHp());
        assertEquals(100, hurt.player.getCurrentHp());
        assertEquals(1, hurt.events(HpLostEvent.class).size());
        assertEquals(Side.OPPONENT, hurt.events(HpLostEvent.class).getFirst().getSide());
    }

    @Test
    void missedMovesStillSpendAConfusionAttempt() {
        var battle = new Battle(100, Type.FORCE, IDLE, IDLE, 1, false);
        battle.player.applyStatus(new Confusion());
        for (int turn = 0; turn < 3; turn++) {
            battle.play(new UseMove(IDLE));
        }
        assertNull(battle.player.getStatus());
        assertEquals(100, battle.target.getCurrentHp());
    }

    private static Fakemon mon(String name, Type type, int hp, int speed, Move move) {
        var moves = List.of(move, IDLE, HIT, HIT);
        var species = new Species.Builder().originalName(name).frenchName(name).types(List.of(type))
                .stats(new BaseStats(hp, 100, 100, 100, 100, speed)).possibleMoves(moves)
                .talent(Talent.CARAPACE).frontSprite("/sprites/front/test.png")
                .backSprite("/sprites/back/test.png").build();
        return new Fakemon(species, moves);
    }

    private static final class Battle {
        private final Fakemon player;
        private final Fakemon target;
        private final Fakemon substitute = mon("Banc", Type.FORCE, 100, 100, HIT);
        private final Fakemon enemySubstitute = mon("Banc adverse", Type.FORCE, 100, 100, HIT);
        private final DefaultBattleEngine engine = new DefaultBattleEngine();
        private final List<BattleEvent> recorded = new ArrayList<>();

        private Battle(int speed, Type targetType, Move move, Move enemyMove, int roll, boolean benches) {
            player = mon("Joueur", Type.FORCE, 100, speed, move);
            target = mon("Cible", targetType, 100, 75, enemyMove);
            engine.addListener(recorded::add);
            engine.start(new Team(benches ? List.of(player, substitute) : List.of(player)),
                    new Team(benches ? List.of(target, enemySubstitute) : List.of(target)), BattleMode.RANDOM,
                    new Opponent() {
                        @Override
                        public PlayerAction chooseAction(List<Move> moves, List<Fakemon> standing) {
                            return new UseMove(enemyMove);
                        }

                        @Override
                        public PlayerAction chooseFakemon(List<Fakemon> standing) {
                            return new SendFakemon(standing.getFirst());
                        }
                    }, new FixedRandom(roll));
            recorded.clear();
        }

        private void play(PlayerAction action) {
            recorded.clear();
            engine.submitAction(action);
        }

        private <T> List<T> events(Class<T> type) {
            return recorded.stream().filter(type::isInstance).map(type::cast).toList();
        }
    }

    private static final class FixedRandom extends Random {
        private final int roll;

        private FixedRandom(int roll) { super(32); this.roll = roll; }

        @Override
        public int nextInt(int bound) { return bound == 3 ? roll : bound == 100 ? 0 : 1; }

        @Override
        public double nextDouble() { return 0.5; }
    }
}
