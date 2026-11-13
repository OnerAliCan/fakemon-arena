package fr.epitech.fakemon.core.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import fr.epitech.fakemon.core.api.BattleEndReason;
import fr.epitech.fakemon.core.api.BattleEndedEvent;
import fr.epitech.fakemon.core.api.BattleEngine;
import fr.epitech.fakemon.core.api.BattleEvent;
import fr.epitech.fakemon.core.api.BattleListener;
import fr.epitech.fakemon.core.api.BattleMode;
import fr.epitech.fakemon.core.api.BattleState;
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
import fr.epitech.fakemon.core.model.Fakemon;
import fr.epitech.fakemon.core.model.Move;
import fr.epitech.fakemon.core.model.Team;


/**
 * L'arbitre du combat : il reçoit l'action du joueur, demande celle de l'adversaire,
 * joue le tour et envoie chaque fait aux écouteurs, dans l'ordre.
 * Il sert aussi d'état du combat, en lecture seule, pour l'écran.
 */
public class DefaultBattleEngine implements  BattleEngine, BattleState{
    private final List<BattleListener> listeners = new ArrayList<>();
    private Team playerTeam;
    private Team opponentTeam;
    private Fakemon activePlayerFakemon;
    private Fakemon activeOpponentFakemon;
    private Opponent opponent;
    private DamageCalculator damageCalculator;
    private Random random;
    private int turnNumber;
    private BattlePhase phase;
    private final TypeChart typeChart = new TypeChart();
    
    @Override
    public Team getTeam(Side side) {
        if (side.equals(Side.PLAYER)) {
            return playerTeam;
        }
        return opponentTeam;
    }


    @Override
    public Fakemon getActiveFakemon(Side side) {
        if (side.equals(Side.PLAYER)) {
            return activePlayerFakemon;
        }
        return activeOpponentFakemon;
    }


    @Override
    public int getTurnNumber() {
       return turnNumber;
    }

    @Override
    public void addListener(BattleListener battleListener) {
       listeners.add(battleListener);
    }

    
    /**
     * Démarre le combat.
     * En mode {@link BattleMode#RANDOM}, le premier mon de chaque équipe entre en jeu et le tour 1 commence.
     * En mode {@link BattleMode#CONSTRUCTED}, l'arbitre demande au joueur son premier mon :
     * les deux mons n'entrent en jeu qu'après sa réponse.
     */
    @Override
    public void start(Team playerTeam, Team opponentTeam, BattleMode battleMode, Opponent opponent, Random random) {
        this.playerTeam = playerTeam;
        this.opponentTeam = opponentTeam;
        this.opponent = opponent;
        this.random = random;

        if (battleMode == BattleMode.RANDOM) {
            activePlayerFakemon = playerTeam.getFakemons().get(0);
            activeOpponentFakemon = opponentTeam.getFakemons().get(0);
            turnNumber = 1;
            notifyListeners(new FakemonEnteredEvent(Side.PLAYER, activePlayerFakemon));
            notifyListeners(new FakemonEnteredEvent(Side.OPPONENT, activeOpponentFakemon));
            notifyListeners(new TurnStartedEvent(turnNumber));
            phase = BattlePhase.WAITING_ACTION;
        } else {
            notifyListeners(new FakemonSelectionRequiredEvent(SelectionReason.FIRST_FAKEMON));
            phase = BattlePhase.WAITING_FIRST_FAKEMON;
        }
        this.damageCalculator = new DamageCalculator(random, typeChart);
    }


    /**
     * Reçoit l'action du joueur et fait avancer le combat.
     * En attente du premier mon, seul un envoi de mon est accepté : les deux mons entrent en jeu et le tour 1 commence.
     * En attente d'une action, seule une attaque est acceptée : l'arbitre demande son action à l'adversaire et joue le tour.
     *
     * @throws IllegalStateException si l'arbitre n'attend pas cette action à ce moment-là
     */
    @Override
    public void submitAction(PlayerAction playerAction) {
        if (playerAction instanceof SendFakemon sendfakemon && phase == BattlePhase.WAITING_FIRST_FAKEMON) {
            activePlayerFakemon = sendfakemon.getFakemon();
            activeOpponentFakemon = opponentTeam.getFakemons().get(0);
            turnNumber = 1;
            notifyListeners(new FakemonEnteredEvent(Side.PLAYER, activePlayerFakemon));
            notifyListeners(new FakemonEnteredEvent(Side.OPPONENT, activeOpponentFakemon));
            notifyListeners(new TurnStartedEvent(turnNumber));
            phase = BattlePhase.WAITING_ACTION;
        }else if (playerAction instanceof UseMove useMove && phase == BattlePhase.WAITING_ACTION ) {
            PlayerAction opponentAction = opponent.chooseAction(
                activeOpponentFakemon.getMoves(),
                opponentTeam.getStandingFakemons()
            );
            UseMove opponentUseMove = (UseMove) opponentAction;
            resolveTurn(opponentUseMove.getMove(), useMove.getMove());
        } else {
            throw new IllegalStateException("Action non autorisée.");
        }
    }

    /**
     * Joue un tour : choisit le camp qui attaque en premier, fait attaquer les deux camps, puis termine le tour.
     * L'attaque de plus grande priorité passe d'abord ; à priorité égale, le mon le plus rapide ;
     * à vitesse égale, le hasard du combat décide. Un mon mis KO avant son tour n'attaque pas.
     *
     * @param opponentMove l'attaque de l'adversaire
     * @param playerMove l'attaque du joueur
     */
    private void resolveTurn(Move opponentMove, Move playerMove) {
        Side firstToPlay;
        if (opponentMove.getPriority() > playerMove.getPriority()) {
            firstToPlay = Side.OPPONENT;
        } else if (opponentMove.getPriority() < playerMove.getPriority()) {
            firstToPlay = Side.PLAYER;
        } else {
            if (activeOpponentFakemon.getStats().getSpeed() > activePlayerFakemon.getStats().getSpeed()) {
                firstToPlay = Side.OPPONENT;
            } else if (activeOpponentFakemon.getStats().getSpeed() < activePlayerFakemon.getStats().getSpeed()) {
                firstToPlay = Side.PLAYER;
            } else {
                if (random.nextBoolean()) {
                    firstToPlay = Side.OPPONENT;
                } else {
                    firstToPlay = Side.PLAYER;
                }
            }
        }
    
        if (firstToPlay == Side.PLAYER) {
            useMove(Side.PLAYER, playerMove);
            if (!activeOpponentFakemon.isFainted()) {
                useMove(Side.OPPONENT, opponentMove);
            }
        } else {
            useMove(Side.OPPONENT, opponentMove);
            if (!activePlayerFakemon.isFainted()) {
                useMove(Side.PLAYER, playerMove);
            }
        }
        applyEndOfTurn();
    }

    /**
     * Fait attaquer un camp et annonce chaque étape : l'attaque utilisée, puis le raté,
     * ou bien le coup critique, l'efficacité, les PV perdus et le KO.
     * Sur une cible immunisée, seule l'absence d'effet est annoncée.
     *
     * @param side le camp qui attaque
     * @param move l'attaque utilisée
     */
    private void useMove(Side side, Move move) {
    Fakemon attacker;
    Fakemon target;
    Side targetSide;
    if (side == Side.PLAYER) {
        attacker = activePlayerFakemon;
        target = activeOpponentFakemon;
        targetSide = Side.OPPONENT;
    } else {
        attacker = activeOpponentFakemon;
        target = activePlayerFakemon;
        targetSide = Side.PLAYER;
    }

    notifyListeners(new MoveUsedEvent(side, attacker, move));

    if (!damageCalculator.rollAccuracy(move)) {
        notifyListeners(new MoveMissedEvent(side, attacker, move));
        return;
    }

    DamageResult result = damageCalculator.calculate(attacker, target, move);
    if (result.getMultiplier() == 0) {
        notifyListeners(new EffectivenessEvent(targetSide, target, Effectiveness.NO_EFFECT));
        return;
    }
    int hpBefore = target.getCurrentHp();
    target.takeDamage(result.getDamage());
    int hpLost = hpBefore - target.getCurrentHp();

    if (result.isCritical()) {
        notifyListeners(new CriticalHitEvent(targetSide, target));
    }
    Optional<Effectiveness> effectiveness = typeChart.toEffectiveness(result.getMultiplier());
    if (effectiveness.isPresent()) {
        notifyListeners(new EffectivenessEvent(targetSide, target, effectiveness.get()));
    }
    notifyListeners(new HpLostEvent(targetSide, target, hpLost, target.getCurrentHp()));

    if (target.isFainted()) {
        notifyListeners(new FakemonFaintedEvent(targetSide, target));
    }
}
    
    /**
     * Termine le tour. Si une équipe n'a plus de mon debout, le combat est fini et l'autre camp gagne.
     * Si un mon sur le terrain est KO, l'arbitre attend un remplaçant. Sinon, le tour suivant commence.
     */
    private void applyEndOfTurn() {
        if (!playerTeam.hasStandingFakemon()) {
            notifyListeners(new BattleEndedEvent(Side.OPPONENT, BattleEndReason.ALL_FAINTED));
            phase = BattlePhase.FINISHED;
        } else if (!opponentTeam.hasStandingFakemon()) {
            notifyListeners(new BattleEndedEvent(Side.PLAYER, BattleEndReason.ALL_FAINTED));
            phase = BattlePhase.FINISHED;
        } else if (activePlayerFakemon.isFainted() || activeOpponentFakemon.isFainted()) {
            phase = BattlePhase.WAITING_REPLACEMENT;
        } else {
            turnNumber = turnNumber + 1;
            notifyListeners(new TurnStartedEvent(turnNumber));
        }
    }


    @Override
    public BattleState getState() {
        return this;
    }
    
    /** Envoie un événement à tous les écouteurs inscrits. */
    private void notifyListeners(BattleEvent event) {
    for (BattleListener listener : listeners) {
        listener.onEvent(event);
    }
    
}
    
}
