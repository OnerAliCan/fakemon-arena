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
     * En attente d'une action, une attaque ou un changement de Fakemon est accepté :
     * l'arbitre demande son action à l'adversaire et joue le tour.
     * En attente d'un remplaçant, seul un envoi de Fakemon est accepté. Si le Fakemon de l'adversaire
     * est KO lui aussi, son remplaçant entre en même temps, le plus rapide d'abord. Puis le tour suivant commence.
     *
     * @throws IllegalStateException si l'arbitre n'attend pas cette action à ce moment-là,
     *     ou si le Fakemon envoyé ne peut pas entrer en jeu
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
        } else if (phase == BattlePhase.WAITING_ACTION
                && (playerAction instanceof UseMove || playerAction instanceof SendFakemon)) {
            if (playerAction instanceof SendFakemon playerSend) {
                checkCanEnter(Side.PLAYER, playerSend.getFakemon());
            }
            PlayerAction opponentAction = opponent.chooseAction(
                activeOpponentFakemon.getMoves(),
                opponentTeam.getStandingFakemons()
            );
            if (opponentAction instanceof SendFakemon opponentSend) {
                checkCanEnter(Side.OPPONENT, opponentSend.getFakemon());
            }
            resolveTurn(opponentAction, playerAction);
        } else if (playerAction instanceof SendFakemon playerSend && phase == BattlePhase.WAITING_REPLACEMENT) {
            checkCanEnter(Side.PLAYER, playerSend.getFakemon());
            if (activeOpponentFakemon.isFainted()) {
                Fakemon opponentReplacement = chooseOpponentReplacement();
                if (fasterSide() == Side.PLAYER) {
                    sendFakemon(Side.PLAYER, playerSend.getFakemon());
                    sendFakemon(Side.OPPONENT, opponentReplacement);
                } else {
                    sendFakemon(Side.OPPONENT, opponentReplacement);
                    sendFakemon(Side.PLAYER, playerSend.getFakemon());
                }
            } else {
                sendFakemon(Side.PLAYER, playerSend.getFakemon());
            }
            startNextTurn();
        } else {
            throw new IllegalStateException("Action non autorisée.");
        }
    }

    /**
     * Joue un tour. Les changements de Fakemon passent d'abord : si les deux camps changent,
     * le Fakemon le plus rapide sort en premier. Viennent ensuite les attaques : la plus grande priorité
     * passe d'abord ; à priorité égale, le Fakemon le plus rapide ; à vitesse égale, le hasard du combat décide.
     * Un Fakemon mis KO avant son tour n'attaque pas. Le tour se termine ensuite.
     *
     * @param opponentAction l'action de l'adversaire : une attaque ou un changement
     * @param playerAction l'action du joueur : une attaque ou un changement
     */
    private void resolveTurn(PlayerAction opponentAction, PlayerAction playerAction) {
        if (playerAction instanceof SendFakemon playerSend && opponentAction instanceof SendFakemon opponentSend) {
            if (fasterSide() == Side.PLAYER) {
                sendFakemon(Side.PLAYER, playerSend.getFakemon());
                sendFakemon(Side.OPPONENT, opponentSend.getFakemon());
            } else {
                sendFakemon(Side.OPPONENT, opponentSend.getFakemon());
                sendFakemon(Side.PLAYER, playerSend.getFakemon());
            }
        } else if (playerAction instanceof SendFakemon playerSend) {
            sendFakemon(Side.PLAYER, playerSend.getFakemon());
        } else if (opponentAction instanceof SendFakemon opponentSend) {
            sendFakemon(Side.OPPONENT, opponentSend.getFakemon());
        }

        if (playerAction instanceof UseMove playerUse && opponentAction instanceof UseMove opponentUse) {
            Move playerMove = playerUse.getMove();
            Move opponentMove = opponentUse.getMove();
            Side firstToPlay;
            if (opponentMove.getPriority() > playerMove.getPriority()) {
                firstToPlay = Side.OPPONENT;
            } else if (opponentMove.getPriority() < playerMove.getPriority()) {
                firstToPlay = Side.PLAYER;
            } else {
                firstToPlay = fasterSide();
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
        } else if (playerAction instanceof UseMove playerUse) {
            useMove(Side.PLAYER, playerUse.getMove());
        } else if (opponentAction instanceof UseMove opponentUse) {
            useMove(Side.OPPONENT, opponentUse.getMove());
        }
        applyEndOfTurn();
    }

    /**
     * Donne le camp dont le Fakemon sur le terrain est le plus rapide. À vitesse égale, le hasard du combat décide.
     *
     * @return le camp le plus rapide
     */
    private Side fasterSide() {
        double playerSpeed = activePlayerFakemon.getStats().getSpeed();
        double opponentSpeed = activeOpponentFakemon.getStats().getSpeed();
        if (opponentSpeed > playerSpeed) {
            return Side.OPPONENT;
        } else if (opponentSpeed < playerSpeed) {
            return Side.PLAYER;
        } else if (random.nextBoolean()) {
            return Side.OPPONENT;
        }
        return Side.PLAYER;
    }

    /**
     * Vérifie qu'un Fakemon peut entrer en jeu pour un camp : il doit faire partie de son équipe,
     * ne pas être KO, et ne pas être déjà sur le terrain.
     *
     * @param side le camp qui envoie le Fakemon
     * @param fakemon le Fakemon envoyé
     * @throws IllegalStateException si le Fakemon ne peut pas entrer en jeu
     */
    private void checkCanEnter(Side side, Fakemon fakemon) {
        if (!getTeam(side).getFakemons().contains(fakemon)
                || fakemon.isFainted()
                || fakemon == getActiveFakemon(side)) {
            throw new IllegalStateException("Ce Fakemon ne peut pas entrer en jeu.");
        }
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
     * Met un Fakemon sur le terrain pour un camp et annonce son entrée en jeu.
     *
     * @param side le camp qui envoie le Fakemon
     * @param fakemon le Fakemon qui entre
     */
    private void sendFakemon(Side side, Fakemon fakemon) {
        if (side == Side.PLAYER) {
            activePlayerFakemon = fakemon;
        } else {
            activeOpponentFakemon = fakemon;
        }
        notifyListeners(new FakemonEnteredEvent(side, fakemon));
    }

    /**
     * Termine le tour. Si une équipe n'a plus de Fakemon debout, le combat est fini et l'autre camp gagne.
     * Si le Fakemon du joueur est KO, l'arbitre lui demande un remplaçant et attend sa réponse.
     * Si seul le Fakemon de l'adversaire est KO, son remplaçant entre tout de suite.
     * Sinon, ou après ce remplacement, le tour suivant commence.
     */
    private void applyEndOfTurn() {
        if (!playerTeam.hasStandingFakemon()) {
            notifyListeners(new BattleEndedEvent(Side.OPPONENT, BattleEndReason.ALL_FAINTED));
            phase = BattlePhase.FINISHED;
        } else if (!opponentTeam.hasStandingFakemon()) {
            notifyListeners(new BattleEndedEvent(Side.PLAYER, BattleEndReason.ALL_FAINTED));
            phase = BattlePhase.FINISHED;
        } else if (activePlayerFakemon.isFainted()) {
            notifyListeners(new FakemonSelectionRequiredEvent(SelectionReason.REPLACEMENT));
            phase = BattlePhase.WAITING_REPLACEMENT;
        } else if (activeOpponentFakemon.isFainted()) {
            sendFakemon(Side.OPPONENT, chooseOpponentReplacement());
            startNextTurn();
        } else {
            startNextTurn();
        }
    }

    /**
     * Demande à l'adversaire son remplaçant, et vérifie qu'il peut entrer en jeu.
     *
     * @return le Fakemon choisi par l'adversaire
     * @throws IllegalStateException si l'adversaire ne renvoie pas un envoi de Fakemon valide
     */
    private Fakemon chooseOpponentReplacement() {
        PlayerAction opponentAction = opponent.chooseFakemon(opponentTeam.getStandingFakemons());
        if (!(opponentAction instanceof SendFakemon opponentSend)) {
            throw new IllegalStateException("L'adversaire doit envoyer un Fakemon.");
        }
        checkCanEnter(Side.OPPONENT, opponentSend.getFakemon());
        return opponentSend.getFakemon();
    }

    /** Fait avancer le numéro du tour, annonce le nouveau tour, et attend l'action du joueur. */
    private void startNextTurn() {
        turnNumber = turnNumber + 1;
        notifyListeners(new TurnStartedEvent(turnNumber));
        phase = BattlePhase.WAITING_ACTION;
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
