package fr.epitech.fakemon.core.engine;

import static org.junit.jupiter.api.Assertions.*;

import fr.epitech.fakemon.core.api.*;
import fr.epitech.fakemon.core.effects.Sleep;
import fr.epitech.fakemon.core.model.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Vérifie la durée du sommeil, le réveil et la pause du compteur pendant les changements. */
class SleepTest {
    private static final Move IDLE = new Move("Attente", Type.FORCE, MoveCategory.PHYSICAL, 0, 0, 0);
    private static final Move HIT = new Move("Frappe", Type.FORCE, MoveCategory.PHYSICAL, 10, 100, 0);
    private static final Move LULLABY = new Move("Berceuse", Type.ETHER, MoveCategory.STATUS,
            0, 100, 0, StatusType.SLEEP, 100);

    @Test
    void everyDurationBlocksExactlyThatManyAttemptsThenWakesBeforeAttacking() {
        for (int duration = 1; duration <= 3; duration++) {
            var random = new SleepRandom(duration);
            var battle = new Battle(HIT, Type.FORCE, IDLE, random);
            battle.player.applyStatus(new Sleep());
            for (int turn = 0; turn < duration; turn++) {
                battle.play(new UseMove(HIT));
                assertEquals(100, battle.target.getCurrentHp());
                assertEquals(Side.PLAYER, battle.events(AttackPreventedEvent.class).getFirst().getSide());
                assertNotNull(battle.player.getStatus());
            }
            battle.play(new UseMove(HIT));
            assertNull(battle.player.getStatus());
            assertEquals(95, battle.target.getCurrentHp());
            assertEquals(1, battle.events(StatusRemovedEvent.class).size());
            assertTrue(battle.recorded.indexOf(battle.events(StatusRemovedEvent.class).getFirst())
                    < battle.recorded.indexOf(battle.events(MoveUsedEvent.class).getFirst()));
            assertEquals(1, random.durationRolls);
        }
    }

    @Test
    void lullabyHasNoTypeImmunityAndDealsNeitherDirectNorResidualDamage() {
        for (Type type : Type.values()) {
            var battle = new Battle(LULLABY, type, HIT, new SleepRandom(1));
            battle.play(new UseMove(LULLABY));
            assertEquals(StatusType.SLEEP, battle.target.getStatus().getType());
            assertEquals(100, battle.target.getCurrentHp());
            assertEquals(100, battle.player.getCurrentHp());
            assertTrue(battle.events(HpLostEvent.class).isEmpty());
            assertTrue(battle.events(CriticalHitEvent.class).isEmpty());
            assertTrue(battle.events(StatusActivatedEvent.class).isEmpty());
            assertEquals(Side.OPPONENT, battle.events(AttackPreventedEvent.class).getFirst().getSide());
        }
    }

    @Test
    void takingDamageDoesNotWakeTheFakemon() {
        var battle = new Battle(HIT, Type.FORCE, HIT, new SleepRandom(3));
        battle.player.applyStatus(new Sleep());
        battle.play(new UseMove(HIT));
        assertEquals(95, battle.player.getCurrentHp());
        assertNotNull(battle.player.getStatus());
        assertTrue(battle.events(StatusRemovedEvent.class).isEmpty());
        battle.play(new UseMove(HIT));
        assertEquals(90, battle.player.getCurrentHp());
        assertEquals(100, battle.target.getCurrentHp());
        assertNotNull(battle.player.getStatus());
    }

    @Test
    void switchingDoesNotSpendSleepTurnsAndTheBenchCounterRemainsPaused() {
        var random = new SleepRandom(2);
        var battle = new Battle(HIT, Type.FORCE, IDLE, random);
        var sleep = new Sleep();
        battle.player.applyStatus(sleep);
        battle.play(new UseMove(HIT));
        battle.play(new SendFakemon(battle.substitute));
        assertSame(battle.substitute, battle.engine.getActiveFakemon(Side.PLAYER));
        assertSame(sleep, battle.player.getStatus());
        battle.play(new UseMove(HIT));
        assertEquals(95, battle.target.getCurrentHp());
        battle.play(new SendFakemon(battle.player));
        battle.play(new UseMove(HIT));
        assertEquals(95, battle.target.getCurrentHp());
        assertNotNull(battle.player.getStatus());
        battle.play(new UseMove(HIT));
        assertEquals(90, battle.target.getCurrentHp());
        assertNull(battle.player.getStatus());
        assertEquals(1, random.durationRolls);
    }

    @Test
    void repeatedLullabyDoesNotRestartSleepOrPreventTheWakeUpAttack() {
        var battle = new Battle(LULLABY, Type.FORCE, HIT, new SleepRandom(2));
        battle.play(new UseMove(LULLABY));
        assertEquals(1, battle.events(StatusAppliedEvent.class).size());
        battle.play(new UseMove(LULLABY));
        assertTrue(battle.events(StatusAppliedEvent.class).isEmpty());
        assertNotNull(battle.target.getStatus());
        battle.play(new UseMove(LULLABY));
        assertTrue(battle.events(StatusAppliedEvent.class).isEmpty());
        assertEquals(1, battle.events(StatusRemovedEvent.class).size());
        assertNull(battle.target.getStatus());
        assertEquals(95, battle.player.getCurrentHp());
    }

    @Test
    void aMissDoesNotApplySleepAndForfeitClearsItFromTheBench() {
        Move miss = new Move("Berceuse ratée", Type.ETHER, MoveCategory.STATUS, 0, 0, 0, StatusType.SLEEP, 100);
        var battle = new Battle(miss, Type.FORCE, IDLE, new Random(32));
        battle.play(new UseMove(miss));
        assertNull(battle.target.getStatus());
        battle.substitute.applyStatus(new Sleep());
        battle.play(new Forfeit());
        assertNull(battle.substitute.getStatus());
    }

    @Test
    void aFixedSeedReproducesSleepDurationAndTheFollowingMoves() {
        var first = new Battle(HIT, Type.FORCE, IDLE, new Random(32));
        var second = new Battle(HIT, Type.FORCE, IDLE, new Random(32));
        first.player.applyStatus(new Sleep());
        second.player.applyStatus(new Sleep());
        for (int turn = 0; turn < 5; turn++) {
            first.play(new UseMove(HIT));
            second.play(new UseMove(HIT));
            assertEquals(first.player.getStatus() == null, second.player.getStatus() == null);
            assertEquals(first.target.getCurrentHp(), second.target.getCurrentHp());
        }
    }

    private static Fakemon mon(String name, Type type, int speed, Move move) {
        var moves = List.of(move, IDLE, HIT, HIT);
        var species = new Species.Builder().originalName(name).frenchName(name).types(List.of(type))
                .stats(new BaseStats(100, 100, 100, 100, 100, speed)).possibleMoves(moves)
                .talent(Talent.CARAPACE).frontSprite("/sprites/front/test.png")
                .backSprite("/sprites/back/test.png").build();
        return new Fakemon(species, moves);
    }

    private static final class Battle {
        private final Fakemon player;
        private final Fakemon target;
        private final Fakemon substitute = mon("Banc", Type.FORCE, 100, HIT);
        private final DefaultBattleEngine engine = new DefaultBattleEngine();
        private final List<BattleEvent> recorded = new ArrayList<>();

        private Battle(Move move, Type targetType, Move enemyMove, Random random) {
            player = mon("Joueur", Type.FORCE, 100, move);
            target = mon("Cible", targetType, 75, enemyMove);
            engine.addListener(recorded::add);
            engine.start(new Team(List.of(player, substitute)), new Team(List.of(target)), BattleMode.RANDOM,
                    new Opponent() {
                        @Override
                        public PlayerAction chooseAction(List<Move> moves, List<Fakemon> standing) {
                            return new UseMove(enemyMove);
                        }

                        @Override
                        public PlayerAction chooseFakemon(List<Fakemon> standing) {
                            return new SendFakemon(standing.getFirst());
                        }
                    }, random);
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

    private static final class SleepRandom extends Random {
        private final int duration;
        private int durationRolls;

        private SleepRandom(int duration) {
            super(32);
            this.duration = duration;
        }

        @Override
        public int nextInt(int bound) {
            if (bound == 3) {
                durationRolls++;
                return duration - 1;
            }
            return bound == 100 ? 0 : 1;
        }

        @Override
        public double nextDouble() { return 0.5; }
    }
}
