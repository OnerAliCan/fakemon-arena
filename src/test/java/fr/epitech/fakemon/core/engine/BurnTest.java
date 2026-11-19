package fr.epitech.fakemon.core.engine;

import static org.junit.jupiter.api.Assertions.*;

import fr.epitech.fakemon.core.api.*;
import fr.epitech.fakemon.core.effects.Burn;
import fr.epitech.fakemon.core.model.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Vérifie la brûlure seule, ses deux attaques et son cycle de vie dans le moteur. */
class BurnTest {
    private static final Move IDLE = new Move("Attente", Type.FORCE, MoveCategory.PHYSICAL, 0, 0, 0);
    private static final Move MARK = new Move("Marque ardente", Type.FIRE, MoveCategory.STATUS,
            0, 100, 0, StatusType.BURN, 100);
    private static final Move EMBER = new Move("Braise tenace", Type.FIRE, MoveCategory.SPECIAL,
            50, 100, 0, StatusType.BURN, 20);

    @Test
    void damageUsesOneSixteenthFlooredWithAMinimumOfOneAndNeverExceedsRemainingHp() {
        Burn burn = new Burn();
        Fakemon regular = mon("Cible", Type.FORCE, 100, 10, IDLE);
        assertEquals(6, burn.endOfTurnDamage(regular));
        assertEquals(1, burn.endOfTurnDamage(mon("Petit", Type.FORCE, 15, 10, IDLE)));
        regular.takeDamage(98);
        assertEquals(2, burn.endOfTurnDamage(regular));
        regular.takeDamage(2);
        assertEquals(0, burn.endOfTurnDamage(regular));
    }

    @Test
    void fireAndFaintedTargetsAreImmuneAndAnExistingStatusIsNotReplaced() {
        Fakemon target = mon("Cible", Type.FORCE, 100, 10, IDLE);
        Burn first = new Burn();
        assertTrue(target.applyStatus(first));
        assertFalse(target.applyStatus(new Burn()));
        assertSame(first, target.getStatus());
        assertFalse(mon("Feu", Type.FIRE, 100, 10, IDLE).applyStatus(new Burn()));
        Fakemon fainted = mon("KO", Type.FORCE, 100, 10, IDLE);
        fainted.takeDamage(100);
        assertFalse(fainted.applyStatus(new Burn()));
    }

    @Test
    void guaranteedBurnDealsOnlyEndOfTurnDamageWithoutCriticalsOrDirectDamage() {
        var battle = new Battle(MARK, Type.FORCE, new Random(32));
        battle.play(MARK);
        assertEquals(94, battle.target.getCurrentHp());
        assertEquals(1, battle.events(StatusAppliedEvent.class).size());
        assertEquals(1, battle.events(StatusActivatedEvent.class).size());
        assertEquals(1, battle.events(HpLostEvent.class).size());
        assertEquals(6, battle.events(HpLostEvent.class).getFirst().getHpLost());
        assertTrue(battle.events(CriticalHitEvent.class).isEmpty());
        battle.play(MARK);
        assertEquals(88, battle.target.getCurrentHp());
        assertTrue(battle.events(StatusAppliedEvent.class).isEmpty());
    }

    @Test
    void damagingMoveAppliesBurnAtNineteenButNotTwentyOnTheHundredPointRoll() {
        for (int roll : List.of(19, 20)) {
            var battle = new Battle(EMBER, Type.FORCE, new ThresholdRandom(roll));
            battle.play(EMBER);
            assertEquals(roll < 20, battle.target.getStatus() != null);
            assertTrue(battle.target.getCurrentHp() < 100);
            if (roll < 20) {
                var losses = battle.events(HpLostEvent.class);
                assertEquals(2, losses.size());
                assertEquals(6, losses.getLast().getHpLost());
            }
        }
    }

    @Test
    void missedMovesDoNotApplyBurn() {
        Move miss = new Move("Raté", Type.FIRE, MoveCategory.SPECIAL, 50, 0, 0, StatusType.BURN, 100);
        var battle = new Battle(miss, Type.FORCE, new Random(32));
        battle.play(miss);
        assertNull(battle.target.getStatus());
        assertEquals(100, battle.target.getCurrentHp());
        assertTrue(battle.events(StatusAppliedEvent.class).isEmpty());
    }

    @Test
    void aFireTargetTakesNeitherDirectNorResidualDamageFromThePureStatusMove() {
        var battle = new Battle(MARK, Type.FIRE, new Random(32));
        battle.play(MARK);
        assertNull(battle.target.getStatus());
        assertEquals(100, battle.target.getCurrentHp());
        assertEquals(Effectiveness.NO_EFFECT, battle.events(EffectivenessEvent.class).getFirst().getEffectiveness());
    }

    @Test
    void aKnockoutDoesNotApplyAStatusAfterDamage() {
        Move fatal = new Move("KO", Type.FIRE, MoveCategory.SPECIAL, 100000, 100, 0, StatusType.BURN, 100);
        var battle = new Battle(fatal, Type.FORCE, new Random(32));
        battle.play(fatal);
        assertTrue(battle.target.isFainted());
        assertTrue(battle.events(StatusAppliedEvent.class).isEmpty());
    }

    @Test
    void switchingPreservesBurnWithoutDamagingTheBench() {
        var battle = new Battle(IDLE, Type.FORCE, new Random(32));
        Fakemon substitute = mon("Remplaçant", Type.FORCE, 100, 10, IDLE);
        battle.restart(List.of(battle.player), List.of(battle.target, substitute));
        assertTrue(battle.target.applyStatus(new Burn()));
        battle.opponent.action = new SendFakemon(substitute);
        battle.play(IDLE);
        assertEquals(100, battle.target.getCurrentHp());
        assertNotNull(battle.target.getStatus());
        battle.opponent.action = new SendFakemon(battle.target);
        battle.play(IDLE);
        assertEquals(94, battle.target.getCurrentHp());
        assertEquals(100, substitute.getCurrentHp());
    }

    @Test
    void residualKnockoutTriggersTheUsualReplacement() {
        var battle = new Battle(MARK, Type.FORCE, new Random(32));
        Fakemon substitute = mon("Remplaçant", Type.FORCE, 100, 10, IDLE);
        battle.restart(List.of(battle.player), List.of(battle.target, substitute));
        battle.target.takeDamage(96);
        battle.play(MARK);
        assertTrue(battle.target.isFainted());
        assertEquals(4, battle.events(HpLostEvent.class).getFirst().getHpLost());
        assertEquals(1, battle.events(FakemonFaintedEvent.class).size());
        assertSame(substitute, battle.engine.getActiveFakemon(Side.OPPONENT));
    }

    @Test
    void residualKnockoutOfThePlayerRequestsAReplacement() {
        var battle = new Battle(IDLE, Type.FORCE, new Random(32));
        Fakemon substitute = mon("Remplaçant", Type.FORCE, 100, 10, IDLE);
        battle.restart(List.of(battle.player, substitute), List.of(battle.target));
        battle.player.applyStatus(new Burn());
        battle.player.takeDamage(96);
        battle.play(IDLE);
        assertEquals(SelectionReason.REPLACEMENT,
                battle.events(FakemonSelectionRequiredEvent.class).getFirst().getReason());
        battle.engine.submitAction(new SendFakemon(substitute));
        assertSame(substitute, battle.engine.getActiveFakemon(Side.PLAYER));
    }

    @Test
    void victoryAndForfeitClearAllStatusesIncludingTheBench() {
        for (boolean forfeit : List.of(false, true)) {
            var battle = new Battle(MARK, Type.FORCE, new Random(32));
            Fakemon bench = mon("Banc", Type.FORCE, 100, 10, IDLE);
            battle.restart(List.of(battle.player, bench), List.of(battle.target));
            bench.applyStatus(new Burn());
            battle.target.applyStatus(new Burn());
            if (forfeit) {
                battle.engine.submitAction(new Forfeit());
            } else {
                battle.target.takeDamage(96);
                battle.play(IDLE);
            }
            assertEquals(1, battle.events(BattleEndedEvent.class).size());
            assertNull(bench.getStatus());
            assertNull(battle.target.getStatus());
        }
    }

    @Test
    void theSameSeedReproducesStatusAndDamage() {
        var first = new Battle(EMBER, Type.FORCE, new Random(32));
        var second = new Battle(EMBER, Type.FORCE, new Random(32));
        for (int turn = 0; turn < 5; turn++) {
            first.play(EMBER);
            second.play(EMBER);
            assertEquals(first.target.getCurrentHp(), second.target.getCurrentHp());
            assertEquals(first.target.getStatus() != null, second.target.getStatus() != null);
        }
    }

    @Test
    void pureStatusDamageCalculationIsAlwaysZero() {
        var battle = new Battle(MARK, Type.FORCE, new Random(32));
        var result = new DamageCalculator(new Random(32), new TypeChart())
                .calculate(battle.player, battle.target, MARK);
        assertEquals(0, result.getDamage());
        assertFalse(result.isCritical());
    }

    private static Fakemon mon(String name, Type type, int hp, int speed, Move move) {
        var moves = List.of(move, IDLE, IDLE, IDLE);
        Species species = new Species.Builder().originalName(name).frenchName(name).types(List.of(type))
                .stats(new BaseStats(hp, 10, 100, 10, 100, speed)).possibleMoves(moves)
                .talent(Talent.CARAPACE).frontSprite("/sprites/front/test.png")
                .backSprite("/sprites/back/test.png").build();
        return new Fakemon(species, moves);
    }

    private static final class Battle {
        private final Fakemon player;
        private final Fakemon target;
        private final WaitingOpponent opponent = new WaitingOpponent();
        private final List<BattleEvent> recorded = new ArrayList<>();
        private final Random random;
        private DefaultBattleEngine engine;

        private Battle(Move move, Type targetType, Random random) {
            this.random = random;
            player = mon("Joueur", Type.FORCE, 100, 100, move);
            target = mon("Cible", targetType, 100, 10, IDLE);
            restart(List.of(player), List.of(target));
        }

        private void restart(List<Fakemon> playerTeam, List<Fakemon> opponentTeam) {
            engine = new DefaultBattleEngine();
            engine.addListener(recorded::add);
            engine.start(new Team(playerTeam), new Team(opponentTeam), BattleMode.RANDOM, opponent, random);
            recorded.clear();
        }

        private void play(Move move) {
            recorded.clear();
            engine.submitAction(new UseMove(move));
        }

        private <T> List<T> events(Class<T> type) {
            return recorded.stream().filter(type::isInstance).map(type::cast).toList();
        }
    }

    private static final class WaitingOpponent implements Opponent {
        private PlayerAction action = new UseMove(IDLE);

        @Override
        public PlayerAction chooseAction(List<Move> moves, List<Fakemon> standing) { return action; }

        @Override
        public PlayerAction chooseFakemon(List<Fakemon> standing) {
            return new SendFakemon(standing.getFirst());
        }
    }

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
