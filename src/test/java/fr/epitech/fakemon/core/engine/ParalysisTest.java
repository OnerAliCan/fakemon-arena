package fr.epitech.fakemon.core.engine;

import static org.junit.jupiter.api.Assertions.*;

import fr.epitech.fakemon.core.api.*;
import fr.epitech.fakemon.core.effects.Paralysis;
import fr.epitech.fakemon.core.model.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Vérifie la vitesse, le blocage des attaques et les attaques de paralysie. */
class ParalysisTest {
    private static final Move IDLE = new Move("Attente", Type.FORCE, MoveCategory.PHYSICAL, 0, 0, 0);
    private static final Move HIT = new Move("Frappe", Type.FORCE, MoveCategory.PHYSICAL, 10, 100, 0);
    private static final Move CAGE = new Move("Cage électrique", Type.LIGHTNING, MoveCategory.STATUS,
            0, 100, 0, StatusType.PARALYSIS, 100);
    private static final Move DISCHARGE = new Move("Décharge statique", Type.LIGHTNING, MoveCategory.SPECIAL,
            50, 100, 0, StatusType.PARALYSIS, 20);

    @Test
    void speedIsHalvedAfterItemsWithoutStackingOrChangingOtherStats() {
        var mon = mon("Cible", Type.FORCE, 99, HIT);
        mon.equip(Item.PLUME);
        assertTrue(mon.applyStatus(new Paralysis()));
        assertEquals(59.4, mon.getStats().getSpeed(), 0.0001);
        assertEquals(59.4, mon.getStats().getSpeed(), 0.0001);
        assertFalse(mon.applyStatus(new Paralysis()));
        assertEquals(100, mon.getStats().getAttack());
        assertEquals(100, mon.getStats().getDefense());
        assertEquals(100, mon.getStats().getSpecialAttack());
        assertEquals(100, mon.getStats().getSpecialDefense());
        assertEquals(100, mon.getStats().getHp());
        assertEquals(99, mon.getSpecies().getStats().getSpeed());
        mon.equip(Item.ECU);
        assertEquals(49.5, mon.getStats().getSpeed());
        assertEquals(120, mon.getStats().getDefense());
        mon.clearStatus();
        assertEquals(99, mon.getStats().getSpeed());
    }

    @Test
    void exactlyTwentyFiveOfTheHundredRollsPreventAttacking() {
        var effect = new Paralysis();
        int blocked = 0;
        for (int roll = 0; roll < 100; roll++) {
            if (effect.preventsAttack(new FixedRandom(roll))) {
                blocked++;
            }
        }
        assertEquals(25, blocked);
        assertEquals(0, effect.endOfTurnDamage(mon("Cible", Type.FORCE, 100, HIT)));
    }

    @Test
    void blockedAttacksAreAnnouncedInsteadOfBeingUsedAndTwentyFiveAllowsActing() {
        for (int roll : List.of(24, 25)) {
            var battle = new Battle(HIT, Type.FORCE, IDLE, new FixedRandom(roll));
            battle.player.applyStatus(new Paralysis());
            battle.play(HIT);
            assertEquals(roll < 25 ? 1 : 0, battle.events(AttackPreventedEvent.class).size());
            assertEquals(roll < 25 ? 0 : 1, battle.events(MoveUsedEvent.class).stream()
                    .filter(event -> event.getSide() == Side.PLAYER).count());
            assertEquals(roll < 25 ? 100 : 95, battle.target.getCurrentHp());
        }
    }

    @Test
    void speedReductionChangesTurnOrderButMovePriorityStillComesFirst() {
        var battle = new Battle(HIT, Type.FORCE, HIT, new FixedRandom(25));
        battle.player.applyStatus(new Paralysis());
        battle.play(HIT);
        assertEquals(Side.OPPONENT, battle.events(MoveUsedEvent.class).getFirst().getSide());
        Move priority = new Move("Prioritaire", Type.FORCE, MoveCategory.PHYSICAL, 10, 100, 1);
        var priorityBattle = new Battle(priority, Type.FORCE, HIT, new FixedRandom(25));
        priorityBattle.player.applyStatus(new Paralysis());
        priorityBattle.play(priority);
        assertEquals(Side.PLAYER, priorityBattle.events(MoveUsedEvent.class).getFirst().getSide());
    }

    @Test
    void switchingIsNeverPreventedAndTheBenchedFakemonKeepsItsStatus() {
        var battle = new Battle(HIT, Type.FORCE, IDLE, new FixedRandom(0));
        battle.player.applyStatus(new Paralysis());
        battle.engine.submitAction(new SendFakemon(battle.substitute));
        assertSame(battle.substitute, battle.engine.getActiveFakemon(Side.PLAYER));
        assertNotNull(battle.player.getStatus());
        assertEquals(50, battle.player.getStats().getSpeed());
        assertTrue(battle.events(AttackPreventedEvent.class).isEmpty());
    }

    @Test
    void bothLightningAndRockAreImmuneIncludingAgainstTheGuaranteedMove() {
        for (Type type : List.of(Type.LIGHTNING, Type.ROCK)) {
            var battle = new Battle(CAGE, type, IDLE, new FixedRandom(19));
            battle.play(CAGE);
            assertNull(battle.target.getStatus());
            assertEquals(100, battle.target.getCurrentHp());
            assertEquals(Effectiveness.NO_EFFECT, battle.events(EffectivenessEvent.class).getFirst().getEffectiveness());
        }
    }

    @Test
    void guaranteedParalysisDoesNoDamageAndCanStopTheOpponentsAttackImmediately() {
        var battle = new Battle(CAGE, Type.FORCE, HIT, new FixedRandom(24));
        battle.play(CAGE);
        assertEquals(StatusType.PARALYSIS, battle.target.getStatus().getType());
        assertEquals(37.5, battle.target.getStats().getSpeed());
        assertEquals(100, battle.target.getCurrentHp());
        assertEquals(1, battle.events(StatusAppliedEvent.class).size());
        assertEquals(Side.OPPONENT, battle.events(AttackPreventedEvent.class).getFirst().getSide());
        assertTrue(battle.events(HpLostEvent.class).isEmpty());
        assertTrue(battle.events(StatusActivatedEvent.class).isEmpty());
    }

    @Test
    void dischargeUsesTheTwentyPercentThresholdAndImmunityStillAllowsDirectResistedDamage() {
        for (int roll : List.of(19, 20)) {
            var battle = new Battle(DISCHARGE, Type.FORCE, IDLE, new FixedRandom(roll));
            battle.play(DISCHARGE);
            assertEquals(roll < 20, battle.target.getStatus() != null);
            assertTrue(battle.target.getCurrentHp() < 100);
        }
        var immune = new Battle(DISCHARGE, Type.LIGHTNING, IDLE, new FixedRandom(19));
        immune.play(DISCHARGE);
        assertNull(immune.target.getStatus());
        assertTrue(immune.target.getCurrentHp() < 100);
        var rock = new Battle(DISCHARGE, Type.ROCK, IDLE, new FixedRandom(19));
        rock.play(DISCHARGE);
        assertNull(rock.target.getStatus());
        assertEquals(100, rock.target.getCurrentHp());
    }

    @Test
    void forfeitClearsTheStatusAndRestoresSpeed() {
        var battle = new Battle(HIT, Type.FORCE, IDLE, new Random(32));
        battle.player.applyStatus(new Paralysis());
        battle.engine.submitAction(new Forfeit());
        assertNull(battle.player.getStatus());
        assertEquals(100, battle.player.getStats().getSpeed());
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

        private void play(Move move) { engine.submitAction(new UseMove(move)); }

        private <T> List<T> events(Class<T> type) {
            return recorded.stream().filter(type::isInstance).map(type::cast).toList();
        }
    }

    private static final class FixedRandom extends Random {
        private final int roll;

        private FixedRandom(int roll) {
            super(32);
            this.roll = roll;
        }

        @Override
        public int nextInt(int bound) { return bound == 100 ? roll : 1; }

        @Override
        public double nextDouble() { return 0.5; }
    }
}
