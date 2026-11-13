package fr.epitech.fakemon.core.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.epitech.fakemon.core.api.BattleEndReason;
import fr.epitech.fakemon.core.api.BattleEndedEvent;
import fr.epitech.fakemon.core.api.BattleEvent;
import fr.epitech.fakemon.core.api.BattleListener;
import fr.epitech.fakemon.core.api.BattleMode;
import fr.epitech.fakemon.core.api.CriticalHitEvent;
import fr.epitech.fakemon.core.api.Effectiveness;
import fr.epitech.fakemon.core.api.EffectivenessEvent;
import fr.epitech.fakemon.core.api.FakemonEnteredEvent;
import fr.epitech.fakemon.core.api.FakemonFaintedEvent;
import fr.epitech.fakemon.core.api.FakemonSelectionRequiredEvent;
import fr.epitech.fakemon.core.api.HpLostEvent;
import fr.epitech.fakemon.core.api.MoveMissedEvent;
import fr.epitech.fakemon.core.api.MoveUsedEvent;
import fr.epitech.fakemon.core.api.Opponent;
import fr.epitech.fakemon.core.api.PlayerAction;
import fr.epitech.fakemon.core.api.SelectionReason;
import fr.epitech.fakemon.core.api.SendFakemon;
import fr.epitech.fakemon.core.api.Side;
import fr.epitech.fakemon.core.api.TurnStartedEvent;
import fr.epitech.fakemon.core.api.UseMove;
import fr.epitech.fakemon.core.model.BaseStats;
import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Move;
import fr.epitech.fakemon.core.model.MoveCategory;
import fr.epitech.fakemon.core.model.Species;
import fr.epitech.fakemon.core.model.Team;
import fr.epitech.fakemon.core.model.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Vérifie l'arbitre du combat : démarrage, ordre des actions, événements, KO et fin du combat. */
class DefaultBattleEngineTest {

    /** Avec cette graine, aucune des deux attaques du premier tour n'est un critique. */
    private static final long NO_CRITICAL_SEED = 0;

    /** Avec cette graine, la première attaque du premier tour est un critique. */
    private static final long CRITICAL_SEED = 12;

    private static final int SLOW = 50;
    private static final int FAST = 200;

    /** Touche toujours, sans jamais mettre KO un mon à 100 PV. */
    private static final Move WEAK = createMove("Pichenette", 10, 100, 0);

    /** Comme WEAK, mais avec une priorité de 1. */
    private static final Move QUICK = createMove("Vive-Pichenette", 10, 100, 1);

    /** Touche toujours et met KO un mon à 100 PV en un coup. */
    private static final Move KNOCK_OUT = createMove("Massue", 1000, 100, 0);

    /** Rate toujours. */
    private static final Move MISS = createMove("Coup dans le vide", 10, 0, 0);

    /** Attaque de type Foudre : sans effet sur un mon de type Roche. */
    private static final Move NO_EFFECT_ON_ROCK = new Move(
        "Étincelle",
        Type.LIGHTNING,
        MoveCategory.PHYSICAL,
        10,
        100,
        0
    );

    private final RecordingListener listener = new RecordingListener();
    private final FirstMoveOpponent opponent = new FirstMoveOpponent();
    private final DefaultBattleEngine engine = new DefaultBattleEngine();

    @Test
    void randomModeSendsFirstFakemonOfEachTeam() {
        Fakemon playerFirst = createFakemon("Alpha", SLOW, WEAK);
        Fakemon opponentFirst = createFakemon("Gamma", SLOW, WEAK);
        Team playerTeam = new Team(
            List.of(playerFirst, createFakemon("Beta", SLOW, WEAK))
        );
        Team opponentTeam = new Team(
            List.of(opponentFirst, createFakemon("Delta", SLOW, WEAK))
        );

        engine.addListener(listener);
        engine.start(
            playerTeam,
            opponentTeam,
            BattleMode.RANDOM,
            opponent,
            new Random(NO_CRITICAL_SEED)
        );

        assertEquals(
            List.of(
                FakemonEnteredEvent.class,
                FakemonEnteredEvent.class,
                TurnStartedEvent.class
            ),
            listener.eventTypes()
        );
        FakemonEnteredEvent playerEntered =
            (FakemonEnteredEvent) listener.events.get(0);
        FakemonEnteredEvent opponentEntered =
            (FakemonEnteredEvent) listener.events.get(1);
        TurnStartedEvent turnStarted = (TurnStartedEvent) listener.events.get(2);
        assertEquals(Side.PLAYER, playerEntered.getSide());
        assertSame(playerFirst, playerEntered.getFakemon());
        assertEquals(Side.OPPONENT, opponentEntered.getSide());
        assertSame(opponentFirst, opponentEntered.getFakemon());
        assertEquals(1, turnStarted.getTurnNumber());

        assertSame(playerTeam, engine.getState().getTeam(Side.PLAYER));
        assertSame(opponentTeam, engine.getState().getTeam(Side.OPPONENT));
        assertSame(playerFirst, engine.getState().getActiveFakemon(Side.PLAYER));
        assertSame(
            opponentFirst,
            engine.getState().getActiveFakemon(Side.OPPONENT)
        );
        assertEquals(1, engine.getState().getTurnNumber());
    }

    @Test
    void constructedModeWaitsForFirstFakemonOfPlayer() {
        Fakemon playerSecond = createFakemon("Beta", SLOW, WEAK);
        Fakemon opponentFirst = createFakemon("Gamma", SLOW, WEAK);
        Team playerTeam = new Team(
            List.of(createFakemon("Alpha", SLOW, WEAK), playerSecond)
        );
        Team opponentTeam = new Team(
            List.of(opponentFirst, createFakemon("Delta", SLOW, WEAK))
        );

        engine.addListener(listener);
        engine.start(
            playerTeam,
            opponentTeam,
            BattleMode.CONSTRUCTED,
            opponent,
            new Random(NO_CRITICAL_SEED)
        );

        assertEquals(
            List.of(FakemonSelectionRequiredEvent.class),
            listener.eventTypes()
        );
        FakemonSelectionRequiredEvent selection =
            (FakemonSelectionRequiredEvent) listener.events.get(0);
        assertEquals(SelectionReason.FIRST_FAKEMON, selection.getReason());
        assertNull(engine.getState().getActiveFakemon(Side.PLAYER));
        assertNull(engine.getState().getActiveFakemon(Side.OPPONENT));

        listener.events.clear();
        engine.submitAction(new SendFakemon(playerSecond));

        assertEquals(
            List.of(
                FakemonEnteredEvent.class,
                FakemonEnteredEvent.class,
                TurnStartedEvent.class
            ),
            listener.eventTypes()
        );
        assertSame(playerSecond, engine.getState().getActiveFakemon(Side.PLAYER));
        assertSame(
            opponentFirst,
            engine.getState().getActiveFakemon(Side.OPPONENT)
        );
        assertEquals(1, engine.getState().getTurnNumber());
    }

    @Test
    void attackIsRefusedWhileWaitingForFirstFakemon() {
        Team playerTeam = new Team(List.of(createFakemon("Alpha", SLOW, WEAK)));
        Team opponentTeam = new Team(List.of(createFakemon("Gamma", SLOW, WEAK)));

        engine.start(
            playerTeam,
            opponentTeam,
            BattleMode.CONSTRUCTED,
            opponent,
            new Random(NO_CRITICAL_SEED)
        );

        assertThrows(IllegalStateException.class, () ->
            engine.submitAction(new UseMove(WEAK))
        );
    }

    @Test
    void submitActionAsksOpponentAndResolvesTheTurn() {
        Fakemon player = createFakemon("Alpha", FAST, WEAK);
        Fakemon opponentMon = createFakemon("Gamma", SLOW, WEAK);
        startDuel(player, opponentMon, NO_CRITICAL_SEED);

        engine.submitAction(new UseMove(WEAK));

        assertEquals(1, opponent.askedActions);
        assertSame(opponentMon.getMoves(), opponent.lastMoveList);
        assertEquals(List.of(opponentMon), opponent.lastStandingFakemonList);
        assertEquals(2, listener.eventsOfType(MoveUsedEvent.class).size());
        assertTrue(player.getCurrentHp() < player.getMaxHp());
        assertTrue(opponentMon.getCurrentHp() < opponentMon.getMaxHp());
    }

    @Test
    void eventsOfATurnAreSentInOrder() {
        Fakemon player = createFakemon("Alpha", FAST, WEAK);
        Fakemon opponentMon = createFakemon("Gamma", SLOW, WEAK);
        startDuel(player, opponentMon, NO_CRITICAL_SEED);

        engine.submitAction(new UseMove(WEAK));

        assertEquals(
            List.of(
                MoveUsedEvent.class,
                HpLostEvent.class,
                MoveUsedEvent.class,
                HpLostEvent.class,
                TurnStartedEvent.class
            ),
            listener.eventTypes()
        );
        MoveUsedEvent moveUsed = (MoveUsedEvent) listener.events.get(0);
        assertEquals(Side.PLAYER, moveUsed.getSide());
        assertSame(player, moveUsed.getFakemon());
        assertSame(WEAK, moveUsed.getMove());

        HpLostEvent hpLost = (HpLostEvent) listener.events.get(1);
        assertEquals(Side.OPPONENT, hpLost.getSide());
        assertSame(opponentMon, hpLost.getFakemon());
        assertEquals(opponentMon.getCurrentHp(), hpLost.getRemainingHp());
        assertEquals(
            opponentMon.getMaxHp() - opponentMon.getCurrentHp(),
            hpLost.getHpLost()
        );

        TurnStartedEvent turnStarted = (TurnStartedEvent) listener.events.get(4);
        assertEquals(2, turnStarted.getTurnNumber());
        assertEquals(2, engine.getState().getTurnNumber());
    }

    @Test
    void higherPriorityMoveOfPlayerActsFirst() {
        Fakemon player = createFakemon("Alpha", SLOW, QUICK);
        Fakemon opponentMon = createFakemon("Gamma", FAST, WEAK);
        startDuel(player, opponentMon, NO_CRITICAL_SEED);

        engine.submitAction(new UseMove(QUICK));

        assertEquals(Side.PLAYER, firstSideToAct());
    }

    @Test
    void higherPriorityMoveOfOpponentActsFirst() {
        Fakemon player = createFakemon("Alpha", FAST, WEAK);
        Fakemon opponentMon = createFakemon("Gamma", SLOW, QUICK);
        startDuel(player, opponentMon, NO_CRITICAL_SEED);

        engine.submitAction(new UseMove(WEAK));

        assertEquals(Side.OPPONENT, firstSideToAct());
    }

    @Test
    void fasterPlayerActsFirstOnSamePriority() {
        Fakemon player = createFakemon("Alpha", FAST, WEAK);
        Fakemon opponentMon = createFakemon("Gamma", SLOW, WEAK);
        startDuel(player, opponentMon, NO_CRITICAL_SEED);

        engine.submitAction(new UseMove(WEAK));

        assertEquals(Side.PLAYER, firstSideToAct());
    }

    @Test
    void fasterOpponentActsFirstOnSamePriority() {
        Fakemon player = createFakemon("Alpha", SLOW, WEAK);
        Fakemon opponentMon = createFakemon("Gamma", FAST, WEAK);
        startDuel(player, opponentMon, NO_CRITICAL_SEED);

        engine.submitAction(new UseMove(WEAK));

        assertEquals(Side.OPPONENT, firstSideToAct());
    }

    @Test
    void speedTieGivesSameOrderWithSameSeed() {
        for (long seed = 0; seed < 20; seed++) {
            assertEquals(firstSideOfSpeedTie(seed), firstSideOfSpeedTie(seed));
        }
    }

    /**
     * Les graines sont espacées de 100 000 : avec des graines voisines (0, 1, 2…),
     * le premier tirage de {@link Random#nextBoolean()} donne toujours le même résultat.
     */
    @Test
    void speedTieLetsBothSidesStart() {
        Set<Side> firstSides = new HashSet<>();
        for (long index = 0; index < 50; index++) {
            firstSides.add(firstSideOfSpeedTie(index * 100_000));
        }

        assertEquals(Set.of(Side.PLAYER, Side.OPPONENT), firstSides);
    }

    @Test
    void missedMoveDealsNoDamage() {
        Fakemon player = createFakemon("Alpha", FAST, MISS);
        Fakemon opponentMon = createFakemon("Gamma", SLOW, WEAK);
        startDuel(player, opponentMon, NO_CRITICAL_SEED);

        engine.submitAction(new UseMove(MISS));

        assertEquals(
            List.of(MoveUsedEvent.class, MoveMissedEvent.class),
            listener.eventTypes().subList(0, 2)
        );
        MoveMissedEvent missed = (MoveMissedEvent) listener.events.get(1);
        assertEquals(Side.PLAYER, missed.getSide());
        assertSame(player, missed.getFakemon());
        assertSame(MISS, missed.getMove());
        assertEquals(opponentMon.getMaxHp(), opponentMon.getCurrentHp());
    }

    @Test
    void criticalHitIsAnnouncedBeforeLostHp() {
        Fakemon player = createFakemon("Alpha", FAST, WEAK);
        Fakemon opponentMon = createFakemon("Gamma", SLOW, WEAK);
        startDuel(player, opponentMon, CRITICAL_SEED);

        engine.submitAction(new UseMove(WEAK));

        assertEquals(
            List.of(
                MoveUsedEvent.class,
                CriticalHitEvent.class,
                HpLostEvent.class
            ),
            listener.eventTypes().subList(0, 3)
        );
        CriticalHitEvent critical = (CriticalHitEvent) listener.events.get(1);
        assertEquals(Side.OPPONENT, critical.getSide());
        assertSame(opponentMon, critical.getFakemon());
    }

    @Test
    void superEffectiveMoveIsAnnouncedBeforeLostHp() {
        Fakemon player = createFakemon("Alpha", FAST, WEAK);
        Fakemon opponentMon = createFakemon("Gamma", Type.ROCK, SLOW, WEAK);
        startDuel(player, opponentMon, NO_CRITICAL_SEED);

        engine.submitAction(new UseMove(WEAK));

        assertEquals(
            List.of(
                MoveUsedEvent.class,
                EffectivenessEvent.class,
                HpLostEvent.class
            ),
            listener.eventTypes().subList(0, 3)
        );
        EffectivenessEvent effectiveness =
            (EffectivenessEvent) listener.events.get(1);
        assertEquals(Side.OPPONENT, effectiveness.getSide());
        assertSame(opponentMon, effectiveness.getFakemon());
        assertEquals(
            Effectiveness.SUPER_EFFECTIVE,
            effectiveness.getEffectiveness()
        );
    }

    /** La graine donne un critique à la première attaque : il ne doit pas être annoncé sur une cible immunisée. */
    @Test
    void immuneTargetOnlyGetsNoEffectEvent() {
        Fakemon player = createFakemon("Alpha", FAST, NO_EFFECT_ON_ROCK);
        Fakemon opponentMon = createFakemon("Gamma", Type.ROCK, SLOW, WEAK);
        startDuel(player, opponentMon, CRITICAL_SEED);

        engine.submitAction(new UseMove(NO_EFFECT_ON_ROCK));

        assertEquals(
            List.of(
                MoveUsedEvent.class,
                EffectivenessEvent.class,
                MoveUsedEvent.class
            ),
            listener.eventTypes().subList(0, 3)
        );
        EffectivenessEvent effectiveness =
            (EffectivenessEvent) listener.events.get(1);
        assertEquals(Side.OPPONENT, effectiveness.getSide());
        assertSame(opponentMon, effectiveness.getFakemon());
        assertEquals(Effectiveness.NO_EFFECT, effectiveness.getEffectiveness());
        assertEquals(opponentMon.getMaxHp(), opponentMon.getCurrentHp());
    }

    @Test
    void faintedFakemonDoesNotAct() {
        Fakemon player = createFakemon("Alpha", FAST, KNOCK_OUT);
        Fakemon opponentFirst = createFakemon("Gamma", SLOW, WEAK);
        Team playerTeam = new Team(List.of(player));
        Team opponentTeam = new Team(
            List.of(opponentFirst, createFakemon("Delta", SLOW, WEAK))
        );
        startBattle(playerTeam, opponentTeam, NO_CRITICAL_SEED);

        engine.submitAction(new UseMove(KNOCK_OUT));

        List<MoveUsedEvent> movesUsed = listener.eventsOfType(MoveUsedEvent.class);
        assertEquals(1, movesUsed.size());
        assertEquals(Side.PLAYER, movesUsed.get(0).getSide());
        assertEquals(player.getMaxHp(), player.getCurrentHp());

        HpLostEvent hpLost = listener.eventsOfType(HpLostEvent.class).get(0);
        assertEquals(opponentFirst.getMaxHp(), hpLost.getHpLost());
        assertEquals(0, hpLost.getRemainingHp());

        FakemonFaintedEvent fainted = listener
            .eventsOfType(FakemonFaintedEvent.class)
            .get(0);
        assertEquals(Side.OPPONENT, fainted.getSide());
        assertSame(opponentFirst, fainted.getFakemon());
        assertTrue(listener.eventsOfType(BattleEndedEvent.class).isEmpty());
    }

    @Test
    void battleEndsWhenOpponentHasNoStandingFakemon() {
        Fakemon player = createFakemon("Alpha", FAST, KNOCK_OUT);
        Fakemon opponentMon = createFakemon("Gamma", SLOW, WEAK);
        startDuel(player, opponentMon, NO_CRITICAL_SEED);

        engine.submitAction(new UseMove(KNOCK_OUT));

        assertEquals(
            List.of(
                MoveUsedEvent.class,
                HpLostEvent.class,
                FakemonFaintedEvent.class,
                BattleEndedEvent.class
            ),
            listener.eventTypes()
        );
        BattleEndedEvent ended = (BattleEndedEvent) listener.events.get(3);
        assertEquals(Side.PLAYER, ended.getWinner());
        assertEquals(BattleEndReason.ALL_FAINTED, ended.getReason());
    }

    @Test
    void battleEndsWhenPlayerHasNoStandingFakemon() {
        Fakemon player = createFakemon("Alpha", SLOW, WEAK);
        Fakemon opponentMon = createFakemon("Gamma", FAST, KNOCK_OUT);
        startDuel(player, opponentMon, NO_CRITICAL_SEED);

        engine.submitAction(new UseMove(WEAK));

        BattleEndedEvent ended = listener
            .eventsOfType(BattleEndedEvent.class)
            .get(0);
        assertEquals(Side.OPPONENT, ended.getWinner());
        assertEquals(BattleEndReason.ALL_FAINTED, ended.getReason());
        assertSame(ended, listener.events.get(listener.events.size() - 1));
    }

    @Test
    void actionIsRefusedOnceBattleIsOver() {
        Fakemon player = createFakemon("Alpha", FAST, KNOCK_OUT);
        Fakemon opponentMon = createFakemon("Gamma", SLOW, WEAK);
        startDuel(player, opponentMon, NO_CRITICAL_SEED);
        engine.submitAction(new UseMove(KNOCK_OUT));

        assertThrows(IllegalStateException.class, () ->
            engine.submitAction(new UseMove(KNOCK_OUT))
        );
    }

    /** Démarre un combat aléatoire, puis vide la liste des événements : les tests ne voient que le tour joué. */
    private void startBattle(Team playerTeam, Team opponentTeam, long seed) {
        engine.addListener(listener);
        engine.start(
            playerTeam,
            opponentTeam,
            BattleMode.RANDOM,
            opponent,
            new Random(seed)
        );
        listener.events.clear();
    }

    /** Démarre un combat à un mon contre un. */
    private void startDuel(Fakemon player, Fakemon opponentMon, long seed) {
        startBattle(
            new Team(List.of(player)),
            new Team(List.of(opponentMon)),
            seed
        );
    }

    private Side firstSideToAct() {
        return listener.eventsOfType(MoveUsedEvent.class).get(0).getSide();
    }

    /** Joue un tour entre deux mons de même vitesse, avec la même priorité, et renvoie le camp qui a attaqué en premier. */
    private Side firstSideOfSpeedTie(long seed) {
        RecordingListener tieListener = new RecordingListener();
        DefaultBattleEngine tieEngine = new DefaultBattleEngine();
        tieEngine.addListener(tieListener);
        tieEngine.start(
            new Team(List.of(createFakemon("Alpha", SLOW, WEAK))),
            new Team(List.of(createFakemon("Gamma", SLOW, WEAK))),
            BattleMode.RANDOM,
            new FirstMoveOpponent(),
            new Random(seed)
        );

        tieEngine.submitAction(new UseMove(WEAK));

        return tieListener.eventsOfType(MoveUsedEvent.class).get(0).getSide();
    }

    private static Move createMove(
        String name,
        int power,
        int accuracy,
        int priority
    ) {
        return new Move(
            name,
            Type.FORCE,
            MoveCategory.PHYSICAL,
            power,
            accuracy,
            priority
        );
    }

    /** Crée un mon de type Force : les attaques des tests, de type Force aussi, ont sur lui un effet normal. */
    private static Fakemon createFakemon(String name, int speed, Move firstMove) {
        return createFakemon(name, Type.FORCE, speed, firstMove);
    }

    private static Fakemon createFakemon(
        String name,
        Type type,
        int speed,
        Move firstMove
    ) {
        Species species = new Species(
            name,
            name,
            List.of(type),
            new BaseStats(100, 100, 100, 100, 100, speed),
            List.of(firstMove, WEAK, WEAK, WEAK)
        );
        return new Fakemon(species, List.of(firstMove, WEAK, WEAK, WEAK));
    }

    /** Un écouteur qui garde tous les événements reçus, dans l'ordre. */
    private static class RecordingListener implements BattleListener {

        private final List<BattleEvent> events = new ArrayList<>();

        @Override
        public void onEvent(BattleEvent event) {
            events.add(event);
        }

        private List<Class<?>> eventTypes() {
            List<Class<?>> types = new ArrayList<>();
            for (BattleEvent event : events) {
                types.add(event.getClass());
            }
            return types;
        }

        private <T extends BattleEvent> List<T> eventsOfType(Class<T> type) {
            List<T> result = new ArrayList<>();
            for (BattleEvent event : events) {
                if (type.isInstance(event)) {
                    result.add(type.cast(event));
                }
            }
            return result;
        }
    }

    /** Un adversaire qui joue toujours la première attaque de son mon, et note ce que l'arbitre lui donne. */
    private static class FirstMoveOpponent implements Opponent {

        private int askedActions = 0;
        private List<Move> lastMoveList;
        private List<Fakemon> lastStandingFakemonList;

        @Override
        public PlayerAction chooseAction(
            List<Move> moveList,
            List<Fakemon> standingFakemonList
        ) {
            askedActions++;
            lastMoveList = moveList;
            lastStandingFakemonList = standingFakemonList;
            return new UseMove(moveList.get(0));
        }

        @Override
        public PlayerAction chooseFakemon(List<Fakemon> standingFakemonList) {
            return new SendFakemon(standingFakemonList.get(0));
        }
    }
}
